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

package org.geoserver.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.flow.controller.BasicOWSController;
import org.geoserver.flow.controller.SimpleThreadBlocker;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HttpServletBean;

public class ControlFlowCallbackTest {

    @Test
    public void testBasicFunctionality() throws IOException, ServletException {
        final ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        final CountingController controller = new CountingController(1, 0);
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);
        callback.doFilter(
                null,
                null,
                new FilterChain() {

                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        callback.operationDispatched(null, null);
                        assertEquals(1, controller.requestIncomingCalls);
                        assertEquals(0, controller.requestCompleteCalls);
                    }
                });

        assertEquals(1, controller.requestIncomingCalls);
        assertEquals(1, controller.requestCompleteCalls);
    }

    @Test
    public void testTimeout() {
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        tc.timeout = 300;
        CountingController c1 = new CountingController(2, 200);
        CountingController c2 = new CountingController(1, 200);
        tc.controllers.add(c1);
        tc.controllers.add(c2);
        callback.provider = new DefaultFlowControllerProvider(tc);

        try {
            callback.operationDispatched(null, null);
            fail("A HTTP 503 should have been raised!");
        } catch (HttpErrorCodeException e) {
            assertEquals(503, e.getErrorCode());
        }
        assertEquals(1, c1.requestIncomingCalls);
        assertEquals(0, c1.requestCompleteCalls);
        assertEquals(1, c2.requestIncomingCalls);
        assertEquals(0, c1.requestCompleteCalls);
        callback.finished(null);
    }

    @Test
    public void testDelayHeader() {
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        tc.timeout = Integer.MAX_VALUE;
        CountingController cc = new CountingController(2, 50);
        tc.controllers.add(cc);
        callback.provider = new DefaultFlowControllerProvider(tc);

        Request request = new Request();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        request.setHttpResponse(httpResponse);

        callback.operationDispatched(request, null);
        callback.finished(null);

        String delayHeader = httpResponse.getHeader(ControlFlowCallback.X_RATELIMIT_DELAY);
        assertNotNull(delayHeader);
        long delay = Long.parseLong(delayHeader);
        assertTrue("Delay should be greater than 50 " + delay, delay >= 50);
    }

    @Test
    public void testFailBeforeOperationDispatch() {
        ControlFlowCallback callback = new ControlFlowCallback();
        callback.init((Request) null);
        callback.finished(null);
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
    }

    @Test
    public void testRequestReplaced() {
        // setup a controller hitting on GWC
        ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        BasicOWSController controller =
                new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        Request r1 = new Request();
        r1.setService("GWC");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        r1.setHttpResponse(httpResponse);

        // setup external request
        callback.operationDispatched(r1, null);
        assertEquals(1, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        // fake a nested WMS request with some code that
        Request r2 = new Request(r1);
        r2.setService("WMS");
        callback.operationDispatched(r2, null);
        // no locking happened on the nested one
        assertEquals(1, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        // finish nested
        callback.finished(r2);
        assertEquals(1, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        // finish outer, but simulate code that does set back the outer request (so it's again
        // called with r2)
        callback.finished(r2);
        // the callback machinery is not fooled and clear stuff anyways
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
    }

    @Test
    public void testFinishedNotCalled() throws IOException, ServletException {
        // setup a controller hitting on GWC
        final ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        final BasicOWSController controller =
                new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        // outer request
        final Request r1 = new Request();
        r1.setService("GWC");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setMethod("GET");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        r1.setHttpRequest(httpRequest);
        r1.setHttpResponse(httpResponse);
        final AtomicBoolean servletCalled = new AtomicBoolean(false);
        MockFilterChain filterChain =
                new MockFilterChain(
                        new HttpServletBean() {
                            @Override
                            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                                    throws ServletException, IOException {
                                servletCalled.set(true);

                                // setup external request
                                callback.operationDispatched(r1, null);
                                assertEquals(1, callback.getRunningRequests());
                                assertEquals(0, callback.getBlockedRequests());
                                assertEquals(1, controller.getRequestsInQueue());

                                // fail to call finished
                            }
                        },
                        callback);
        filterChain.doFilter(httpRequest, httpResponse);
        // check the servlet doing the test has been called
        assertTrue(servletCalled.get());
        // the callback machinery is not fooled and clears stuff anyways
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        assertEquals(0, controller.getRequestsInQueue());
    }

    @Test
    public void testFailNestedRequestParse() throws IOException, ServletException {
        // setup a controller hitting on GWC
        final ControlFlowCallback callback = new ControlFlowCallback();
        TestingConfigurator tc = new TestingConfigurator();
        final BasicOWSController controller =
                new BasicOWSController("GWC", 1, new SimpleThreadBlocker(1));
        tc.controllers.add(controller);
        callback.provider = new DefaultFlowControllerProvider(tc);

        // outer request
        final Request r1 = new Request();
        r1.setService("GWC");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setMethod("GET");
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        r1.setHttpRequest(httpRequest);
        r1.setHttpResponse(httpResponse);

        final AtomicBoolean servletCalled = new AtomicBoolean(false);
        MockFilterChain filterChain =
                new MockFilterChain(
                        new HttpServletBean() {
                            @Override
                            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                                    throws ServletException, IOException {
                                servletCalled.set(true);

                                // setup external request
                                callback.operationDispatched(r1, null);
                                assertEquals(1, callback.getRunningRequests());
                                assertEquals(0, callback.getBlockedRequests());

                                // call the nested one
                                Request r2 = new Request(r1);
                                callback.operationDispatched(r2, null);
                                assertEquals(1, callback.getRunningRequests());
                                assertEquals(0, callback.getBlockedRequests());
                                assertEquals(1, controller.getRequestsInQueue());

                                // fail to call finished on either
                            }
                        },
                        callback);
        filterChain.doFilter(httpRequest, httpResponse);
        // check the servlet doing the test has been called
        assertTrue(servletCalled.get());
        // the callback machinery is not fooled and clears stuff anyways
        assertEquals(0, callback.getRunningRequests());
        assertEquals(0, callback.getBlockedRequests());
        assertEquals(0, controller.getRequestsInQueue());
    }

    /** A wide open configurator to be used for testing */
    static class TestingConfigurator implements ControlFlowConfigurator {
        List<FlowController> controllers = new ArrayList<>();
        long timeout;
        boolean stale = true;

        public Collection<FlowController> buildFlowControllers() throws Exception {
            stale = false;
            return controllers;
        }

        public long getTimeout() {
            return timeout;
        }

        public boolean isStale() {
            return stale;
        }
    }

    /** A controller counting requests, can also be used to check for timeouts */
    static class CountingController implements FlowController {

        int priority;
        long delay;
        int requestCompleteCalls;
        int requestIncomingCalls;

        public CountingController(int priority, long delay) {
            this.priority = priority;
            this.delay = delay;
        }

        public int getPriority() {
            return priority;
        }

        public void requestComplete(Request request) {
            requestCompleteCalls++;
        }

        public boolean requestIncoming(Request request, long timeout) {
            requestIncomingCalls++;
            if (delay > 0)
                if (timeout > delay) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("This is unexpected");
                    }
                } else {
                    return false;
                }
            return true;
        }
    }
}
