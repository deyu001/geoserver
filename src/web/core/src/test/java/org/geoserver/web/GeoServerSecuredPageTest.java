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

package org.geoserver.web;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.protocol.http.WebSession;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.data.layer.LayerPage;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.savedrequest.SavedRequest;

public class GeoServerSecuredPageTest extends GeoServerWicketTestSupport {

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testSecuredPageGivesRedirectWhenLoggedOut() throws UnsupportedEncodingException {
        logout();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(GeoServerLoginPage.class);
        // make sure the spring security emulation is properly setup
        SavedRequest sr =
                (SavedRequest)
                        tester.getHttpSession().getAttribute(GeoServerSecuredPage.SAVED_REQUEST);
        assertNotNull(sr);
        String redirectUrl = new URLDecoder().decode(sr.getRedirectUrl(), "UTF8");
        assertTrue(
                redirectUrl.contains("wicket/bookmarkable/org.geoserver.web.data.layer.LayerPage"));
    }

    @Test
    public void testSecuredPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(LayerPage.class);
        tester.assertRenderedPage(LayerPage.class);
    }

    @Test
    public void testToolPageAllowsAccessWhenLoggedIn() {
        login();
        tester.startPage(ToolPage.class);
        tester.assertRenderedPage(ToolPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testSessionFixationAvoidance() throws Exception {
        tester.startPage(GeoServerHomePage.class);
        final WebSession session = WebSession.get();
        session.bind(); // fore session creation
        session.setAttribute("test", "whatever");
        // login, this will invalidate the session
        tester.startPage(GeoServerHomePage.class);
        MockHttpServletRequest request = createRequest("login");
        request.setMethod("POST");
        request.setParameter("username", "admin");
        request.setParameter("password", "geoserver");
        String oldSessionId = request.getSession().getId();
        dispatch(request);
        // verify that the session ID changed
        assertNotEquals(oldSessionId, request.getSession().getId());
        // the session in wicket tester mock does not disappear, the only
        // way to see if it has been invalidated is to check that the attributes are gone...
        assertNull(session.getAttribute("test"));
    }
}
