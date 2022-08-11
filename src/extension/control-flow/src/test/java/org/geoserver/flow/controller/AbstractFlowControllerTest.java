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

import static org.junit.Assert.fail;

import java.lang.Thread.State;
import javax.servlet.http.Cookie;
import org.geoserver.flow.controller.FlowControllerTestingThread.ThreadState;
import org.geoserver.ows.Request;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Base class providing utilities to test flow controllers
 *
 * @author Andrea Aime - OpenGeo
 */
public abstract class AbstractFlowControllerTest {

    protected static final long MAX_WAIT = 5000;

    /**
     * Waits until the thread enters in WAITING or TIMED_WAITING state
     *
     * @param t the thread
     * @param maxWait max amount of time we'll wait
     */
    void waitBlocked(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.getState() != State.WAITING && t.getState() != State.TIMED_WAITING) {
                if (System.currentTimeMillis() > (start + maxWait))
                    fail("Waited for the thread to be blocked more than maxWait: " + maxWait);
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            fail("Sometime interrupeted our wait: " + e);
        }
    }

    /**
     * Waits until the thread is terminated
     *
     * @param t the thread
     * @param maxWait max amount of time we'll wait
     */
    void waitTerminated(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.getState() != State.TERMINATED) {
                if (System.currentTimeMillis() > (start + maxWait))
                    fail("Waited for the thread to be terminated more than maxWait: " + maxWait);
                Thread.sleep(20);
            }
        } catch (Exception e) {
            // System.out.println("Could not terminate thread " + t);
        }
    }

    /** Waits maxWait for the thread to finish by itself, then forcefully kills it */
    void waitAndKill(Thread t, long maxWait) {
        try {
            long start = System.currentTimeMillis();
            while (t.isAlive()) {
                if (System.currentTimeMillis() > (start + maxWait)) {
                    // forcefully destroy the thread
                    t.interrupt();
                }

                Thread.sleep(20);
            }
        } catch (InterruptedException e) {
            fail("Sometime interrupeted our wait: " + e);
        }
    }

    protected Request buildCookieRequest(String gsCookieValue) {
        Request request = new Request();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(new MockHttpServletResponse());

        if (gsCookieValue != null) {
            httpRequest.setCookies(new Cookie(CookieKeyGenerator.COOKIE_NAME, gsCookieValue));
        }
        return request;
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    Request buildIpRequest(String ipAddress, String proxyIp) {
        Request request = new Request();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(new MockHttpServletResponse());

        if (ipAddress != null && !ipAddress.equals("")) {
            httpRequest.setRemoteAddr(ipAddress);
        } else {
            httpRequest.setRemoteAddr("127.0.0.1");
        }
        if (!proxyIp.equals("")) {
            httpRequest.addHeader("x-forwarded-for", proxyIp + ", " + ipAddress);
        }
        return request;
    }

    /**
     * Waits for he flow controller testing thread to get into a specified state for a max given
     * amount of time, fail otherwise
     */
    protected void waitState(ThreadState state, FlowControllerTestingThread tt, long maxWait)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!state.equals(tt.state) && System.currentTimeMillis() - start < maxWait) {
            Thread.sleep(20);
        }

        ThreadState finalState = tt.state;
        if (!state.equals(finalState)) {
            fail(
                    "Waited "
                            + maxWait
                            + "ms for FlowControllerTestingThread to get into "
                            + state
                            + ", but it is still in state "
                            + finalState);
        }
    }
}
