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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.Map;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.response.GML32WFS3OutputFormat;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;

public class CollectionTest extends WFS3TestSupport {

    @Test
    public void testCollectionJson() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("wfs3/collections/" + roadSegments, 200);

        assertEquals("cite__RoadSegments", json.read("$.name", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));
        assertEquals(-180, json.read("$.extent.spatial[0]", Double.class), 0d);
        assertEquals(-90, json.read("$.extent.spatial[1]", Double.class), 0d);
        assertEquals(180, json.read("$.extent.spatial[2]", Double.class), 0d);
        assertEquals(90, json.read("$.extent.spatial[3]", Double.class), 0d);

        // check we have the expected number of links and they all use the right "rel" relation
        List<String> formats =
                DefaultWebFeatureService30.getAvailableFormats(FeatureCollectionResponse.class);
        assertThat(
                (int) json.read("$.links.length()", Integer.class),
                Matchers.greaterThanOrEqualTo(formats.size()));
        for (String format : formats) {
            // check title and rel.
            List items = json.read("$.links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("cite__RoadSegments items as " + format, item.get("title"));
            assertEquals("item", item.get("rel"));
        }
        // the WFS3 specific GML3.2 output format is available
        assertNotNull(json.read("$.links[?(@.type=='" + GML32WFS3OutputFormat.FORMAT + "')]"));

        // tiling scheme extension
        Map tilingScheme = (Map) json.read("links[?(@.rel=='tilingScheme')]", List.class).get(0);
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections/"
                        + roadSegments
                        + "/tiles/{tilingSchemeId}",
                tilingScheme.get("href"));
        Map tiles = (Map) json.read("links[?(@.rel=='tiles')]", List.class).get(0);
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections/"
                        + roadSegments
                        + "/tiles/{tilingSchemeId}/{level}/{row}/{col}",
                tiles.get("href"));
    }

    @Test
    public void testCollectionVirtualWorkspace() throws Exception {
        String roadSegments = MockData.ROAD_SEGMENTS.getLocalPart();
        DocumentContext json = getAsJSONPath("cite/wfs3/collections/" + roadSegments, 200);

        assertEquals("RoadSegments", json.read("$.name", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));

        // check we have the expected number of links and they all use the right "rel" relation
        List<String> formats =
                DefaultWebFeatureService30.getAvailableFormats(FeatureCollectionResponse.class);
        assertThat(
                (int) json.read("$.links.length()", Integer.class),
                Matchers.greaterThanOrEqualTo(formats.size()));
        for (String format : formats) {
            // check title and rel.
            List items = json.read("$.links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("RoadSegments items as " + format, item.get("title"));
            assertEquals("item", item.get("rel"));
        }
        // the WFS3 specific GML3.2 output format is available
        assertNotNull(json.read("$.links[?(@.type=='" + GML32WFS3OutputFormat.FORMAT + "')]"));

        // tiling scheme extension
        Map tilingScheme = (Map) json.read("links[?(@.rel=='tilingScheme')]", List.class).get(0);
        assertEquals(
                "http://localhost:8080/geoserver/cite/wfs3/collections/"
                        + roadSegments
                        + "/tiles/{tilingSchemeId}",
                tilingScheme.get("href"));
        Map tiles = (Map) json.read("links[?(@.rel=='tiles')]", List.class).get(0);
        assertEquals(
                "http://localhost:8080/geoserver/cite/wfs3/collections/"
                        + roadSegments
                        + "/tiles/{tilingSchemeId}/{level}/{row}/{col}",
                tiles.get("href"));
    }

    @Test
    public void testCollectionsXML() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs3/collections/"
                                + getEncodedName(MockData.ROAD_SEGMENTS)
                                + "?f=application/xml");
        print(dom);
        String expected =
                "http://localhost:8080/geoserver/wfs3/collections/cite__RoadSegments/items?f=application%2Fjson";
        XMLAssert.assertXpathEvaluatesTo(
                expected,
                "//wfs:Collection[wfs:Name='cite__RoadSegments']/atom:link[@atom:type='application/json']/@atom:href",
                dom);
    }

    @Test
    public void testCollectionYaml() throws Exception {
        String yaml =
                getAsString(
                        "wfs3/collections/"
                                + getEncodedName(MockData.ROAD_SEGMENTS)
                                + "?f=application/x-yaml");
        // System.out.println(yaml);
    }
}
