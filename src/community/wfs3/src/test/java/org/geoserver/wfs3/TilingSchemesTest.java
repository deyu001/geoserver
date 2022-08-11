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

package org.geoserver.wfs3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import org.geoserver.data.test.MockData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TilingSchemesTest extends WFS3TestSupport {

    private Locale originalLocale;

    @Before
    public void setup() {
        originalLocale = Locale.getDefault();
        // locale setting to test coordinate encode on US locale
        Locale.setDefault(Locale.ITALY);
    }

    @After
    public void onFinish() {
        Locale.setDefault(originalLocale);
    }

    /** Tests the "wfs3/tilingScheme" json response */
    @Test
    public void testTilingSchemesResponse() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes", 200);
        String scheme1 = jsonDoc.read("$.tilingSchemes[0]", String.class);
        String scheme2 = jsonDoc.read("$.tilingSchemes[1]", String.class);
        HashSet<String> schemesSet = new HashSet<>(Arrays.asList(scheme1, scheme2));
        assertTrue(schemesSet.contains("GlobalCRS84Geometric"));
        assertTrue(schemesSet.contains("GoogleMapsCompatible"));
        assertTrue(schemesSet.size() == 2);
    }

    @Test
    public void testTilingSchemeDescriptionGoogleMapsCompatible() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/GoogleMapsCompatible", 200);
        checkTilingSchemeData(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/3857",
                new Double[] {-20037508.34, -20037508.34},
                "http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible",
                559082263.9508929d,
                0.001d,
                1073741824,
                "tileMatrix[30].matrixWidth");
    }

    @Test
    public void testTilingSchemeDescriptionGoogleMapsCompatibleOnCollections() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext jsonDoc =
                getAsJSONPath(
                        "wfs3/collections/" + roadSegments + "/tiles/GoogleMapsCompatible", 200);
        checkTilingSchemeData(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/3857",
                new Double[] {-20037508.34d, -20037508.34d},
                "http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible",
                559082263.9508929d,
                0.001d,
                1073741824,
                "tileMatrix[30].matrixWidth");
    }

    public void checkTilingSchemeData(
            DocumentContext jsonDoc,
            String s,
            Double[] bboxLowerCorner,
            String s3,
            double v,
            double v2,
            int i,
            String s4) {
        assertEquals(s, jsonDoc.read("boundingBox.crs", String.class));
        assertEquals(
                bboxLowerCorner[0],
                jsonDoc.read("boundingBox.lowerCorner[0]", Double.class),
                0.001d);
        assertEquals(
                bboxLowerCorner[1],
                jsonDoc.read("boundingBox.lowerCorner[1]", Double.class),
                0.001d);
        assertEquals(s3, jsonDoc.read("wellKnownScaleSet", String.class));
        assertEquals(v, jsonDoc.read("tileMatrix[0].scaleDenominator", Double.class), v2);
        assertEquals(Integer.valueOf(i), jsonDoc.read(s4, Integer.class));
    }

    @Test
    public void testTilingSchemeDescriptionGlobalCRS84Geometric() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/GlobalCRS84Geometric", 200);
        checkTilingSchemeData(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/4326",
                new Double[] {-180d, -90d},
                "http://www.opengis.net/def/wkss/OGC/1.0/GlobalCRS84Geometric",
                2.795411320143589E8d,
                0.000000000000001E8d,
                4194304,
                "tileMatrix[21].matrixWidth");
        assertEquals(90d, jsonDoc.read("tileMatrix[21].topLeftCorner[0]", Double.class), 0.001d);
        assertEquals(-180d, jsonDoc.read("tileMatrix[21].topLeftCorner[1]", Double.class), 0.001d);
    }

    @Test
    public void testTilingSchemeDescriptionError() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/errorNameX", 500);
        assertEquals("Invalid gridset name errorNameX", jsonDoc.read("description", String.class));
    }
}
