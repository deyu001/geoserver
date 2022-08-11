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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * A flow controller that throttles concurrent requests made from the same ip (any ip)
 *
 * @author Juan Marin, OpenGeo
 */
public class IpFlowController extends QueueController {

    /**
     * Thread local holding the current request queue id TODO: consider having a user map in {@link
     * Request} instead
     */
    static ThreadLocal<String> QUEUE_ID = new ThreadLocal<>();

    /**
     * A flow controller that throttles concurrent requests made from the same ip (any ip)
     *
     * @author Juan Marin, OpenGeo
     */
    static final Logger LOGGER = Logging.getLogger(IpFlowController.class);

    public IpFlowController(int queueSize) {
        this.queueSize = queueSize;
    }

    @Override
    public void requestComplete(Request request) {
        String queueId = QUEUE_ID.get();
        QUEUE_ID.remove();
        if (queueId != null) {
            BlockingQueue<Request> queue = queues.get(queueId);
            if (queue != null) queue.remove(request);
        }
    }

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        // check if this client already made other connections
        final String incomingIp;
        {
            String ip = getRemoteAddr(request.getHttpRequest());
            if (null == ip || "".equals(ip)) {
                // may this happen? hope not, but if someone is trying to trick us lets not let him
                // and pool it on the "empty IP" queue
                incomingIp = "";
            } else {
                incomingIp = ip;
            }
        }

        // see if we have that queue already
        TimedBlockingQueue queue = queues.get(incomingIp);

        // generate a unique queue id for this client if none was found
        if (queue == null) {
            // beware of multiple concurrent requests...
            synchronized (this) {
                queue = queues.get(incomingIp);
                if (queue == null) {
                    queue = new TimedBlockingQueue(queueSize, true);
                    queues.put(incomingIp, queue);
                }
            }
        }
        QUEUE_ID.set(incomingIp);

        // queue token handling
        try {
            if (timeout > 0) {
                retval = queue.offer(request, timeout, TimeUnit.MILLISECONDS);
            } else {
                queue.put(request);
            }
        } catch (InterruptedException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Unexpected interruption while " + "blocking on the request queue");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "IpFlowController("
                            + queueSize
                            + ","
                            + incomingIp
                            + ") queue size "
                            + queue.size());
            LOGGER.fine(
                    "IpFlowController("
                            + queueSize
                            + ","
                            + incomingIp
                            + ") total queues "
                            + queues.size());
        }
        return retval;
    }

    static String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            if (-1 == forwardedFor.indexOf(',')) {
                return forwardedFor;
            }
            String[] ips = forwardedFor.split(", ");
            return ips[0];
        } else {
            return req.getRemoteAddr();
        }
    }
}
