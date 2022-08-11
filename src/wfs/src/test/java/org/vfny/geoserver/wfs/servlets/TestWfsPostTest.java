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

package org.vfny.geoserver.wfs.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.ServletException;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class TestWfsPostTest {

    /** The proxy base url variable */
    static final String PROXY_BASE_URL = "PROXY_BASE_URL";

    @Test
    public void testEscapeXMLReservedChars() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.addHeader("Host", "localhost:8080");
        request.setQueryString(ResponseUtils.getQueryString("form_hf_0=&url=vjoce<>:garbage"));
        request.setParameter("url", "vjoce<>:garbage");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // System.out.println(response.getContentAsString());
        // check xml chars have been escaped
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "java.net.MalformedURLException: no protocol: vjoce&lt;&gt;:garbage"));
    }

    @Test
    public void testDisallowOpenProxy() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://www.google.com");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // checking that reqauest is disallowed
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Invalid url requested, the demo requests should be hitting: http://localhost:8080/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithProxyBase() throws Exception {
        TestWfsPost servlet = buildMockServlet("http://geoserver.org/geoserver");
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://localhost:1234/internalApp");
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // checking that reqauest is disallowed
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Invalid url requested, the demo requests should be hitting: http://geoserver.org/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithSupersetNameWithProxyBase() throws Exception {
        TestWfsPost servlet = buildMockServlet("http://geoserver.org");
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://geoserver.org.other");
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // checking that request is disallowed
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Invalid url requested, the demo requests should be hitting: http://geoserver.org"));
    }

    @Test
    public void testValidateURL() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://localhost:1234/internalApp");
        request.setMethod("GET");

        try {
            servlet.validateURL(
                    request, "http://localhost:1234/internalApp", "http://geoserver.org/geoserver");
            fail("Requests should be limited by proxyBaseURL");
        } catch (IllegalArgumentException expected) {
            assertTrue(
                    expected.getMessage()
                            .contains(
                                    "Invalid url requested, the demo requests should be hitting: http://geoserver.org/geoserver"));
        }
    }

    @Test
    public void testGetProxyBaseURL() {
        SettingsInfo settings = new SettingsInfoImpl();
        settings.setProxyBaseUrl("https://foo.com/geoserver");

        GeoServerInfo info = new GeoServerInfoImpl();
        info.setSettings(settings);

        GeoServer gs = new GeoServerImpl();
        gs.setGlobal(info);

        TestWfsPost servlet =
                new TestWfsPost() {
                    @Override
                    protected GeoServer getGeoServer() {
                        return gs;
                    }
                };
        assertEquals("https://foo.com/geoserver", servlet.getProxyBaseURL());
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    protected static MockHttpServletRequest buildMockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("/geoserver");
        request.setServletPath("/TestWfsPost");
        request.setRequestURI(
                ResponseUtils.stripQueryString(ResponseUtils.appendPath("/geoserver/TestWfsPost")));
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    protected static TestWfsPost buildMockServlet() throws ServletException {
        return buildMockServlet(null);
    }

    protected static TestWfsPost buildMockServlet(final String proxyBaseUrl)
            throws ServletException {
        TestWfsPost testWfsPost;
        if (proxyBaseUrl == null) {
            testWfsPost = new TestWfsPost();
        } else {
            testWfsPost =
                    new TestWfsPost() {
                        String getProxyBaseURL() {
                            return proxyBaseUrl;
                        }
                    };
        }
        MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath("/geoserver");
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        testWfsPost.init(servletConfig);

        return testWfsPost;
    }
}
