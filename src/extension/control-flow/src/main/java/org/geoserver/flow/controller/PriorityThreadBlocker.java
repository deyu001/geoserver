/*
*==Description==
*GeoServer is an open source software server written in Java that allows users
*          to share and edit geospatial data.Designed for interoperability,
*          it publishes data from any major spatial data source using open standards.
*
*Being a community-driven project, GeoServer is developed, tested, and supported by
*      a diverse group of individuals and organizations from around the world.
*
*GeoServer is the reference implementation of the Open Geospatial Consortium (OGC)
*          Web Feature Service (WFS) and Web Coverage Service (WCS) standards, as well as
*          a high performance certified compliant Web Map Service (WMS), compliant
*          Catalog Service for the Web (CSW) and implementing Web Processing Service (WPS).
*          GeoServer forms a core component of the Geospatial Web.
*
*==License==
*GeoServer is distributed under the GNU General Public License Version 2.0 license:
*
*    GeoServer, open geospatial information server
*    Copyright (C) 2014-2020 Open Source Geospatial Foundation.
*    Copyright (C) 2001-2014 OpenPlans
*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2 of the License, or
*    (at your option) any later version (collectively, "GPL").
*
*    As an exception to the terms of the GPL, you may copy, modify,
*    propagate, and distribute a work formed by combining GeoServer with the
*    EMF and XSD Libraries, or a work derivative of such a combination, even if
*    such copying, modification, propagation, or distribution would otherwise
*    violate the terms of the GPL. Nothing in this exception exempts you from
*    complying with the GPL in all respects for all of the code used other
*    than the EMF and XSD Libraries. You may include this exception and its grant
*    of permissions when you distribute GeoServer.  Inclusion of this notice
*    with such a distribution constitutes a grant of such permissions.  If
*    you do not wish to grant these permissions, remove this paragraph from
*    your distribution. "GeoServer" means the GeoServer software licensed
*    under version 2 or any later version of the GPL, or a work based on such
*    software and licensed under the GPL. "EMF and XSD Libraries" means
*    Eclipse Modeling Framework Project and XML Schema Definition software
*    distributed by the Eclipse Foundation, all licensed
*    under the Eclipse Public License Version 1.0 ("EPL"), or a work based on
*    such software and licensed under the EPL.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA 02110-1335  USA
*
*==More Information==
*Visit the website or read the docs.
*/

package org.geoserver.flow.controller;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * Blocking queue based blocker, a request gets blocked if there are already <code>queueSize</code>
 * requests running. Unlike {@link SimpleThreadBlocker} here threads that got blocked due to full
 * queue will be awaken in priority order, highest to lowest
 */
public class PriorityThreadBlocker implements ThreadBlocker {

    static final Logger LOGGER = Logging.getLogger(PriorityThreadBlocker.class);

    private final PriorityProvider priorityProvider;
    private final int maxRunningRequests;
    // unlike the SimpleThreadBlock this does not contain the requests that were freed to go onto
    // the next
    // controller or execution, but the ones blocked waiting
    private final PriorityQueue<WaitToken> queue = new PriorityQueue<>();
    // This holds the requests actually running on this blocker. Flow controllers
    // might not all be called if one fails, but all get a "requestComplete" for cleanup,
    // so need to know if this blocker was called before, or not
    private final Set<Request> runningQueue = new HashSet<>();

    public PriorityThreadBlocker(int queueSize, PriorityProvider priorityProvider) {
        this.maxRunningRequests = queueSize;
        this.priorityProvider = priorityProvider;
    }

    @Override
    public int getRunningRequestsCount() {
        return queue.size();
    }

    public boolean requestIncoming(Request request, long timeout) throws InterruptedException {
        WaitToken token = null;

        boolean result = false;

        // protect shared data structures from MT access
        synchronized (this) {
            if (runningQueue.size() < maxRunningRequests) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(
                            Level.FINER,
                            "Running requests at " + runningQueue.size() + ", no block");
                }
                result = true;
            } else {
                int priority = priorityProvider.getPriority(request);
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(
                            Level.FINER,
                            "Running requests at "
                                    + runningQueue.size()
                                    + ", Queuing request with "
                                    + "priority "
                                    + priority);
                }
                token = new WaitToken(priority);
                queue.add(token);
            }
        }

        // if this request entered the queue, wait for the latch to be released
        if (token != null) {
            if (timeout > 0) {
                result = token.latch.await(timeout, TimeUnit.MILLISECONDS);
                synchronized (this) {
                    // if timeout out, just remove from the queue
                    if (!result) {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.log(
                                    Level.FINER,
                                    "Request with priority "
                                            + token.priority
                                            + " timed out, removing"
                                            + " from"
                                            + " queue");
                        }
                        boolean removed = queue.remove(token);
                        if (!removed) {
                            if (LOGGER.isLoggable(Level.FINER)) {
                                LOGGER.log(
                                        Level.FINER,
                                        "Request was not found in queue, releasing next");
                            }
                            // has already been removed by releaseNext, release the next one then
                            if (runningQueue.size() < maxRunningRequests) {
                                releaseNext();
                            }
                        }
                    }
                }
            } else {
                token.latch.await();
                result = true;
            }
        }

        // the code will call requestComplete also in case of timeout, need to keep the balance
        synchronized (this) {
            runningQueue.add(request);
        }

        return result;
    }

    public void requestComplete(Request request) {
        // protect shared data structures from MT
        synchronized (this) {
            runningQueue.remove(request);
            if (runningQueue.size() < maxRunningRequests) {
                releaseNext();
            }
        }
    }

    private void releaseNext() {
        // this needs to be called within a synchronized section
        assert Thread.holdsLock(this);

        WaitToken token;
        token = queue.poll();
        if (token != null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "Releasing request with priority " + token.priority);
            }

            token.latch.countDown();
        }
    }

    /** Returns the priority provider, issuing a priority for each request to be put in queue */
    public PriorityProvider getPriorityProvider() {
        return priorityProvider;
    }

    /**
     * Simple token for the priority queue, holds the priority, sorts on it higher to lower, and
     * holds the latch blocking the thread
     */
    private static class WaitToken implements Comparable<WaitToken> {
        CountDownLatch latch = new CountDownLatch(1);
        long created = System.currentTimeMillis();
        int priority;

        public WaitToken(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(WaitToken o) {
            // to have the highest priority first (smallest) in the queue
            int diff = o.priority - this.priority;
            if (diff != 0) {
                return diff;
            } else {
                // in case of same priority, first come first served
                return Long.signum(this.created - o.created);
            }
        }
    }
}
