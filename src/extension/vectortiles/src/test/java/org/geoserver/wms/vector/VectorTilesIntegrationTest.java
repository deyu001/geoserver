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

package org.geoserver.wms.vector;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import net.minidev.json.JSONArray;
import no.ecc.vectortile.VectorTileDecoder;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class VectorTilesIntegrationTest extends WMSTestSupport {

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        if (!isQuietTests()) {
            LOGGER.info(response.getContentAsString());
        }

        assertEquals(expectedHttpCode, response.getStatus());
        assertThat(response.getContentType(), startsWith("application/json"));
        return JsonPath.parse(response.getContentAsString());
    }

    @Test
    public void testSimple() throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(5, ((JSONArray) json.read("$.features")).size());
        assertEquals(5, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(
                3, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(
                1,
                ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]"))
                        .size());
        assertEquals(
                1,
                ((JSONArray)
                                json.read(
                                        "$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]"))
                        .size());
    }

    @Test
    public void testSimpleMVT() throws Exception {
        checkSimpleMVT(MapBoxTileBuilderFactory.MIME_TYPE);
    }

    @Test
    public void testSimpleMVTLegacyMime() throws Exception {
        checkSimpleMVT(MapBoxTileBuilderFactory.LEGACY_MIME_TYPE);
    }

    public void checkSimpleMVT(String mimeType) throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&format="
                        + mimeType;
        MockHttpServletResponse response = getAsServletResponse(request);
        // the standard mime type is returned
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, response.getContentType());
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        List<VectorTileDecoder.Feature> featuresList = decoder.decode(responseBytes).asList();
        assertEquals(5, featuresList.size());
        assertEquals(
                3,
                featuresList
                        .stream()
                        .filter(f -> "Route 5".equals(f.getAttributes().get("NAME")))
                        .count());
        assertEquals(
                1,
                featuresList
                        .stream()
                        .filter(f -> "Main Street".equals(f.getAttributes().get("NAME")))
                        .count());
        assertEquals("Extent should be 12288", 12288, featuresList.get(0).getExtent());
    }

    @Test
    public void testCqlFilter() throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&CQL_FILTER=NAME='Main Street'&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(1, ((JSONArray) json.read("$.features")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(
                0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(
                1,
                ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]"))
                        .size());
        assertEquals(
                0,
                ((JSONArray)
                                json.read(
                                        "$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]"))
                        .size());
    }

    @Test
    public void testCqlFilterNoMatch() throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&CQL_FILTER=1=0&format="
                        + MapBoxTileBuilderFactory.MIME_TYPE;
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(200, response.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, response.getContentType());
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        List<VectorTileDecoder.Feature> featuresList = decoder.decode(responseBytes).asList();
        assertEquals(0, featuresList.size());
    }

    @Test
    public void testFilterById() throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&featureId=RoadSegments.1107532045091&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(1, ((JSONArray) json.read("$.features")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(
                0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(
                0,
                ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]"))
                        .size());
        assertEquals(
                1,
                ((JSONArray)
                                json.read(
                                        "$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]"))
                        .size());
    }
}
