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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.Cookie;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.ows.Request;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class RateFlowControllerTest extends AbstractFlowControllerTest {

    @Test
    public void testCookieRateControl() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 1000, new CookieKeyGenerator());

        // run the first request
        Request firstRequest = buildCookieRequest(null);
        assertTrue(controller.requestIncoming(firstRequest, Integer.MAX_VALUE));
        checkHeaders(firstRequest, "Any OGC request", 2, 1);

        // grab the cookie
        Cookie cookie = ((MockHttpServletResponse) firstRequest.getHttpResponse()).getCookies()[0];
        String cookieValue = cookie.getValue();

        // second request
        Request request = buildCookieRequest(cookieValue);
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        checkHeaders(request, "Any OGC request", 2, 0);

        // third one, this one will have to wait
        long start = System.currentTimeMillis();
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        long end = System.currentTimeMillis();
        long delay = end - start;
        assertTrue("Request was not delayed enough: " + delay, delay >= 1000);
        checkHeaders(request, "Any OGC request", 2, 0);

        // fourth one, this one will bail out immediately because we give it not enough wait
        assertFalse(controller.requestIncoming(request, 500));
        checkHeaders(request, "Any OGC request", 2, 0);
    }

    private void checkHeaders(Request request, String context, int limit, int remaining) {
        MockHttpServletResponse response = (MockHttpServletResponse) request.getHttpResponse();
        assertEquals(context, response.getHeader(RateFlowController.X_RATE_LIMIT_CONTEXT));
        assertEquals(
                String.valueOf(limit), response.getHeader(RateFlowController.X_RATE_LIMIT_LIMIT));
        assertEquals(
                String.valueOf(remaining),
                response.getHeader(RateFlowController.X_RATE_LIMIT_REMAINING));
    }

    @Test
    public void testCookie429() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 0, new CookieKeyGenerator());

        // run the first request
        Request firstRequest = buildCookieRequest(null);
        assertTrue(controller.requestIncoming(firstRequest, Integer.MAX_VALUE));

        // grab the cookie
        Cookie cookie = ((MockHttpServletResponse) firstRequest.getHttpResponse()).getCookies()[0];
        String cookieValue = cookie.getValue();

        // second request
        Request request = buildCookieRequest(cookieValue);
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));

        // this one should fail with a 429
        try {
            assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        } catch (HttpErrorCodeException e) {
            assertEquals(429, e.getErrorCode());
        }
    }

    @Test
    public void testIpRateControl() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 1000, new IpKeyGenerator());

        // run two requests
        Request request = buildIpRequest("127.0.0.1", "");
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));

        // third one, this one will have to wait
        long start = System.currentTimeMillis();
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        long end = System.currentTimeMillis();
        long delay = end - start;
        assertTrue("Request was not delayed enough: " + delay, delay >= 1000);

        // fourth one, this one will bail out immediately because we give it not enough wait
        assertFalse(controller.requestIncoming(request, 500));
    }

    @Test
    public void testIp429() {
        RateFlowController controller =
                new RateFlowController(
                        new OWSRequestMatcher(), 2, Long.MAX_VALUE, 0, new IpKeyGenerator());

        // run two requests
        Request request = buildIpRequest("127.0.0.1", "");
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));

        // this one should fail with a 429
        try {
            assertTrue(controller.requestIncoming(request, Integer.MAX_VALUE));
        } catch (HttpErrorCodeException e) {
            assertEquals(429, e.getErrorCode());
        }
    }
}
