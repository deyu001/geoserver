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

package org.geoserver.geofence;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ServicesTest extends GeofenceBaseTest {

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    /** Enable the Spring Security auth filters, otherwise there will be no auth */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testAdmin() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        this.username = "admin";
        this.password = "geoserver";

        // check from the caps he can access everything
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1&service=WMS");
        // print(dom);

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("3", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("8", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testCiteCapabilities() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        //        loginAsCite();
        this.username = "cite";
        this.password = "cite";

        // check from the caps he can access cite and sf, but not others
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.1&service=WMS");
        print(dom);

        assertXpathEvaluatesTo("11", "count(//Layer[starts-with(Name, 'cite:')])", dom);
        assertXpathEvaluatesTo("3", "count(//Layer[starts-with(Name, 'sf:')])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer[starts-with(Name, 'cdf:')])", dom);
    }

    @Test
    public void testCiteLayers() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        loginAsCite();

        // try a getfeature on a sf layer
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=wfs&version=1.0.0&request=getfeature&typeName="
                                + getLayerId(MockData.GENERICENTITY));
        assertEquals(200, response.getStatus());
        assertEquals("text/xml", response.getContentType());
        String content = response.getContentAsString();
        LOGGER.info("Content: " + content);
        //        assertTrue(content.contains("Unknown namespace [sf]"));
        assertTrue(content.contains("Feature type sf:GenericEntity unknown"));
    }
}
