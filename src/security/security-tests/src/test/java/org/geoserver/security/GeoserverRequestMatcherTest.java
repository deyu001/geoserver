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

package org.geoserver.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** @author christian */
public class GeoserverRequestMatcherTest extends GeoServerMockTestSupport {

    GeoServerSecurityFilterChainProxy proxy;

    @Before
    public void setUp() {
        proxy = new GeoServerSecurityFilterChainProxy(getSecurityManager());
    }

    @Test
    public void testMacher() {
        // match all
        VariableFilterChain chain = new ServiceLoginFilterChain("/**");
        RequestMatcher matcher = proxy.matcherForChain(chain);
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));

        // set methods, but match is inactvie
        chain = new ServiceLoginFilterChain("/**");
        chain.getHttpMethods().add(HTTPMethod.GET);
        chain.getHttpMethods().add(HTTPMethod.POST);
        matcher = proxy.matcherForChain(chain);
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wms")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.PUT, "/wms")));

        // active method matching
        chain.setMatchHTTPMethod(true);
        matcher = proxy.matcherForChain(chain);
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wms")));
        assertFalse(matcher.matches(createRequest(HTTPMethod.PUT, "/wms")));

        chain = new ServiceLoginFilterChain("/wfs/**,/web/**");
        matcher = proxy.matcherForChain(chain);

        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms/abc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/wfs/acc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.GET, "/web/abc")));

        chain.getHttpMethods().add(HTTPMethod.GET);
        chain.getHttpMethods().add(HTTPMethod.POST);
        matcher = proxy.matcherForChain(chain);

        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms/abc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wfs/acc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.PUT, "/web/abc")));

        chain.setMatchHTTPMethod(true);
        matcher = proxy.matcherForChain(chain);

        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms/abc")));
        assertTrue(matcher.matches(createRequest(HTTPMethod.POST, "/wfs/acc")));
        assertFalse(matcher.matches(createRequest(HTTPMethod.PUT, "/web/abc")));
    }

    @Test
    public void testMacherWithQueryString() {
        VariableFilterChain chain =
                new ServiceLoginFilterChain("/wms/**|.*request=getcapabilities.*");
        RequestMatcher matcher = proxy.matcherForChain(chain);

        assertFalse(matcher.matches(createRequest(HTTPMethod.GET, "/wms")));
        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&request=GetCapabilities")));
        assertFalse(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET, "/wms?service=WMS&version=1.1.1&request=GetMap")));

        // regex for parameters in any order
        chain = new ServiceLoginFilterChain("/wms/**|(?=.*request=getmap)(?=.*format=image/png).*");
        matcher = proxy.matcherForChain(chain);

        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&request=GetMap&format=image/png")));
        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/png&request=GetMap")));
        assertFalse(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/jpg&request=GetMap")));

        // regex for parameters not contained
        chain = new ServiceLoginFilterChain("/wms/**|(?=.*request=getmap)(?!.*format=image/png).*");
        matcher = proxy.matcherForChain(chain);
        assertTrue(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/jpg&request=GetMap")));
        assertFalse(
                matcher.matches(
                        createRequest(
                                HTTPMethod.GET,
                                "/wms?service=WMS&version=1.1.1&format=image/png&request=GetMap")));
    }

    MockHttpServletRequest createRequest(HTTPMethod method, String pathInfo) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("");
        String queryString = null;
        if (pathInfo.indexOf("?") != -1) {
            queryString = pathInfo.substring(pathInfo.indexOf("?") + 1);
            pathInfo = pathInfo.substring(0, pathInfo.indexOf("?"));
        }
        request.setPathInfo(pathInfo);
        if (queryString != null) {
            request.setQueryString(queryString);
        }
        request.setMethod(method.toString());
        return request;
    }
}
