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

package org.geoserver.wms.svg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assume;
import org.junit.Test;
import org.w3c.dom.Document;

public class SVGTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle("multifts", "./polyMultiFts.sld", getClass(), getCatalog());
    }

    @Test
    public void testBasicSvgGenerator() throws Exception {
        getWMS().setSvgRenderer(WMS.SVG_SIMPLE);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertEquals(1, doc.getElementsByTagName("g").getLength());
    }

    @Test
    public void testBasicSvgGeneratorMultipleFts() throws Exception {
        getWMS().setSvgRenderer(WMS.SVG_SIMPLE);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles=multifts"
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertEquals(1, doc.getElementsByTagName("g").getLength());
    }

    @Test
    public void testBatikSvgGenerator() throws Exception {
        Assume.assumeTrue(isw3OrgReachable());

        getWMS().setSvgRenderer(WMS.SVG_BATIK);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertTrue(doc.getElementsByTagName("g").getLength() > 1);
    }

    private boolean isw3OrgReachable() {
        // batik includes DTD reference which forces us to be online, skip test
        // in offline case
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL("http://www.w3.org").openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();
            connection.disconnect();
            return true;
        } catch (Exception e) {
            LOGGER.warning("Unable to contact http://www.w3.org - " + e.getMessage());
            return false;
        }
    }

    @Test
    public void testBatikMultipleFts() throws Exception {
        Assume.assumeTrue(isw3OrgReachable());

        getWMS().setSvgRenderer(WMS.SVG_BATIK);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles=multifts"
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertTrue(doc.getElementsByTagName("g").getLength() > 1);
    }
}
