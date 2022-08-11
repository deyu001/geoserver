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

package org.geoserver.wps;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;

public class ResourceAccessManagerWPSTest extends WPSTestSupport {

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wps/ResourceAccessManagerContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo buildings =
                catalog.getFeatureTypeByName(getLayerId(SystemTestData.BUILDINGS));

        // limits make the layer be visible when logged in as the cite user, but not when
        // running as the anonymous one (the TestResourceAccessManager does not allow
        // to run tests against un-recognized users)
        tam.putLimits(
                "cite",
                buildings,
                new VectorAccessLimits(CatalogMode.HIDE, null, Filter.INCLUDE, null, null));
        tam.putLimits(
                "anonymous",
                buildings,
                new VectorAccessLimits(CatalogMode.HIDE, null, Filter.EXCLUDE, null, null));
    }

    @Test
    public void testDenyAccess() throws Exception {
        Document dom = runBuildingsRequest();
        // print(dom);

        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("0", xp.evaluate("count(//wps:ProcessSucceded)", dom));
    }

    @Test
    public void testAllowAccess() throws Exception {
        setRequestAuth("cite", "cite");
        Document dom = runBuildingsRequest();
        // print(dom);

        assertEquals("0", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("1", xp.evaluate("count(//wps:ProcessSucceeded)", dom));
        String[] lc =
                xp.evaluate(
                                "//wps:Output[ows:Identifier = 'bounds']/wps:Data/wps:BoundingBoxData/ows:LowerCorner",
                                dom)
                        .split("\\s+");
        assertEquals(8.0E-4, Double.parseDouble(lc[0]), 0d);
        assertEquals(5.0E-4, Double.parseDouble(lc[1]), 0d);
        String[] uc =
                xp.evaluate(
                                "//wps:Output[ows:Identifier = 'bounds']/wps:Data/wps:BoundingBoxData/ows:UpperCorner",
                                dom)
                        .split("\\s+");
        assertEquals(0.0024, Double.parseDouble(uc[0]), 0d);
        assertEquals(0.001, Double.parseDouble(uc[1]), 0d);
    }

    private Document runBuildingsRequest() throws Exception {
        // @formatter:off
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\""
                        + getLayerId(MockData.BUILDINGS)
                        + "\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:Output>\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:Output>\n"
                        + " </wps:ResponseForm>\n"
                        + "</wps:Execute>";
        // @formatter:on

        Document dom = postAsDOM("wps", xml);
        return dom;
    }
}
