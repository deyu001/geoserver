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

package org.geoserver.monitor;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.wms.map.RenderTimeStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class MonitorFilterTest {

    DummyMonitorDAO dao;
    MonitorFilter filter;
    MockFilterChain chain;

    static final int MAX_BODY_SIZE = 10;
    static final int LONG_BODY_SIZE = 3 * MAX_BODY_SIZE;

    @Before
    public void setUp() throws Exception {
        dao = new DummyMonitorDAO();

        filter = new MonitorFilter(new Monitor(dao), new MonitorRequestFilter());

        chain =
                new MockFilterChain(
                        new HttpServlet() {
                            @Override
                            public void service(ServletRequest req, ServletResponse res)
                                    throws ServletException, IOException {
                                req.getInputStream().read(new byte[LONG_BODY_SIZE]);
                                res.getOutputStream().write(new byte[0]);
                            }
                        });

        filter.monitor.config.props.put(
                "maxBodySize",
                Integer.toString(
                        MAX_BODY_SIZE)); // Ensure the configured property is correct for the tests
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testSimple() throws Exception {

        HttpServletRequest req = request("GET", "/foo/bar", "12.34.56.78", null, null);
        filter.doFilter(req, response(), chain);

        RequestData data = dao.getLast();
        assertEquals("GET", data.getHttpMethod());
        assertEquals("/foo/bar", data.getPath());
        assertEquals("12.34.56.78", data.getRemoteAddr());
        assertNull(data.getHttpReferer());
    }

    @Test
    public void testWithBody() throws Exception {
        chain =
                new MockFilterChain(
                        new HttpServlet() {
                            @Override
                            public void service(ServletRequest req, ServletResponse res)
                                    throws ServletException, IOException {
                                req.getInputStream().read(new byte[LONG_BODY_SIZE]);
                                res.getOutputStream().write("hello".getBytes());
                            }
                        });

        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", "baz", null);
        filter.doFilter(req, response(), chain);

        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertNull(data.getHttpReferer());

        assertEquals(new String(data.getBody()), "baz");
        assertEquals(3, data.getBodyContentLength());
        assertEquals(5, data.getResponseLength());
    }

    @Test
    public void testWithLongBody() throws Exception {
        chain =
                new MockFilterChain(
                        new HttpServlet() {
                            @Override
                            public void service(ServletRequest req, ServletResponse res)
                                    throws ServletException, IOException {
                                req.getInputStream().read(new byte[LONG_BODY_SIZE]);
                                res.getOutputStream().write("hello".getBytes());
                            }
                        });

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < MAX_BODY_SIZE; i++) {
            b.append('b');
        }
        String wanted_body = b.toString();
        for (int i = MAX_BODY_SIZE; i < LONG_BODY_SIZE; i++) {
            b.append('b');
        }
        String given_body = b.toString();

        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", given_body, null);
        filter.doFilter(req, response(), chain);

        RequestData data = dao.getLast();

        assertEquals(
                wanted_body, new String(data.getBody())); // Should be trimmed to the maximum length
        assertEquals(
                LONG_BODY_SIZE,
                data.getBodyContentLength()); // Should be the full length, not the trimmed one
    }

    @Test
    public void testWithUnboundedBody() throws Exception {
        final int UNBOUNDED_BODY_SIZE = 10000; // Something really big

        filter.monitor.config.props.put(
                "maxBodySize",
                Integer.toString(
                        UNBOUNDED_BODY_SIZE)); // Ensure the configured property is correct for the
        // tests

        chain =
                new MockFilterChain(
                        new HttpServlet() {
                            @Override
                            @SuppressWarnings("PMD.EmptyWhileStmt")
                            public void service(ServletRequest req, ServletResponse res)
                                    throws ServletException, IOException {
                                while (req.getInputStream().read() != -1)
                                    ; // "read" the stream until the end.
                                req.getInputStream().read();
                                res.getOutputStream().write("hello".getBytes());
                            }
                        });

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < UNBOUNDED_BODY_SIZE; i++) {
            b.append(i % 10);
        }

        String wanted_body = b.toString();
        String given_body = b.toString();

        HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", given_body, null);
        filter.doFilter(req, response(), chain);

        RequestData data = dao.getLast();

        assertEquals(
                wanted_body, new String(data.getBody())); // Should be trimmed to the maximum length
        assertEquals(
                UNBOUNDED_BODY_SIZE,
                data.getBodyContentLength()); // Should be the full length, not the trimmed one
    }

    @Test
    public void testReferer() throws Exception {
        HttpServletRequest req =
                request("GET", "/foo/bar", "12.34.56.78", null, "http://testhost/testpath");
        filter.doFilter(req, response(), chain);

        RequestData data = dao.getLast();
        assertEquals("GET", data.getHttpMethod());
        assertEquals("/foo/bar", data.getPath());
        assertEquals("12.34.56.78", data.getRemoteAddr());
        assertEquals("http://testhost/testpath", data.getHttpReferer());
    }

    @Test
    public void testReferrer() throws Exception {
        // "Referrer" was misspelled in the HTTP spec, check if it works with the "correct"
        // spelling.
        MockHttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
        req.addHeader("Referrer", "http://testhost/testpath");
        filter.doFilter(req, response(), chain);

        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertEquals("http://testhost/testpath", data.getHttpReferer());
        assertNull(data.getCacheResult());
        assertNull(data.getMissReason());
    }

    @Test
    public void testGWCHeaders() throws Exception {
        MockHttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
        MockHttpServletResponse response = response();
        String cacheResult = "Miss";
        response.addHeader(MonitorFilter.GEOWEBCACHE_CACHE_RESULT, cacheResult);
        String missReason = "Wrong planet alignment";
        response.addHeader(MonitorFilter.GEOWEBCACHE_MISS_REASON, missReason);
        filter.doFilter(req, response, chain);

        RequestData data = dao.getLast();
        assertEquals("POST", data.getHttpMethod());
        assertEquals("/bar/foo", data.getPath());
        assertEquals("78.56.34.12", data.getRemoteAddr());
        assertEquals(cacheResult, data.getCacheResult());
        assertEquals(missReason, data.getMissReason());
    }

    @Test
    public void testUserRemoteUser() throws Exception {
        Object principal = new User("username", "", Collections.emptyList());

        testRemoteUser(principal);
    }

    @Test
    public void testUserDetailsRemoteUser() throws Exception {
        UserDetails principal = createMock(UserDetails.class);

        expect(principal.getUsername()).andReturn("username");
        replay(principal);

        testRemoteUser(principal);
    }

    @Test
    public void testGetStatistics() throws IOException, ServletException {

        MockHttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
        MockHttpServletResponse response = response();
        RenderTimeStatistics statistics =
                (RenderTimeStatistics) req.getAttribute(RenderTimeStatistics.ID);
        filter.doFilter(req, response, chain);
        RequestData data = this.dao.getLast();
        String layerNamesList = statistics.getLayerNames().toString();
        assertEquals(
                data.getResourcesList(), layerNamesList.substring(1, layerNamesList.length() - 1));
        assertNotEquals(
                data.getResourcesProcessingTimeList()
                        .indexOf(statistics.getRenderingTime(0).toString()),
                -1);
        assertEquals(data.getLabellingProcessingTime().longValue(), statistics.getLabellingTime());
    }

    @Test
    public void testDisableReverseDNSProcessor() throws Exception {
        // step 1 : verify DND lookup working without configuration option
        Object principal = new User("username", "", Collections.emptyList());
        RequestData data = testRemoteUser(principal);
        assertNotNull(data.getRemoteHost());
        try {
            // step 2 : verify DND lookup is disabled when run with ignore option
            filter = new MonitorFilter(new Monitor(dao), new MonitorRequestFilter());
            filter.monitor.config.props.put("ignorePostProcessors", "reverseDNS");
            chain =
                    new MockFilterChain(
                            new HttpServlet() {
                                @Override
                                public void service(ServletRequest req, ServletResponse res)
                                        throws ServletException, IOException {
                                    req.getInputStream().read(new byte[LONG_BODY_SIZE]);
                                    res.getOutputStream().write(new byte[0]);
                                }
                            });

            principal = new User("username", "", Collections.emptyList());
            data = testRemoteUser(principal);
            assertNull(data.getRemoteHost());
        } finally {
            // reset
            filter.monitor.config.props.remove("ignorePostProcessors");
        }
    }

    RenderTimeStatistics createStatistcis() {
        RenderTimeStatistics stats = createMock(RenderTimeStatistics.class);
        expect(stats.getRenderingLayersIdxs()).andReturn(Arrays.asList(0)).anyTimes();
        expect(stats.getRenderingTime(0)).andReturn(100L).anyTimes();
        expect(stats.getLabellingTime()).andReturn(100L).anyTimes();
        expect(stats.getLayerNames()).andReturn(Arrays.asList("Layer1")).anyTimes();
        replay(stats);

        return stats;
    }

    private RequestData testRemoteUser(Object principal) throws Exception {
        try {
            Authentication authentication = new TestingAuthenticationToken(principal, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            final CompletableFuture<Authentication> authFuture = new CompletableFuture<>();
            filter.setExecutionAudit(
                    (data, auth) -> {
                        authFuture.complete(auth);
                    });
            HttpServletRequest req = request("POST", "/bar/foo", "78.56.34.12", null, null);
            filter.doFilter(req, response(), chain);

            RequestData data = dao.getLast();
            assertEquals("username", data.getRemoteUser());
            assertEquals(authentication, authFuture.get());
            return data;
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
            filter.setExecutionAudit(null);
        }
    }

    MockHttpServletRequest request(
            String method, String path, String remoteAddr, String body, String referer)
            throws UnsupportedEncodingException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setServerName("localhost");
        req.setServletPath(path.substring(0, path.indexOf('/', 1)));
        req.setPathInfo(path.substring(path.indexOf('/', 1)));
        req.setRemoteAddr(remoteAddr);
        if (body == null)
            body = ""; // MockHttpServletRequest#getInputStream doesn't like null bodies
        // and throws NullPointerException. It should probably do something useful like return an
        // empty stream or throw
        // IOException.
        req.setContent(body.getBytes("UTF-8"));
        req.setAttribute(RenderTimeStatistics.ID, createStatistcis());
        if (referer != null) req.addHeader("Referer", referer);
        return req;
    }

    MockHttpServletResponse response() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        return response;
    }
}
