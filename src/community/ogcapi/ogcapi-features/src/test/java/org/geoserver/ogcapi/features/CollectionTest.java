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

package org.geoserver.ogcapi.features;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;

public class CollectionTest extends FeaturesTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // customize metadata and set custom CRS too
        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        basicPolygons.setOverridingServiceSRS(true);
        basicPolygons.getResponseSRS().addAll(Arrays.asList("3857", "32632"));
        getCatalog().save(basicPolygons);
    }

    @Test
    public void testCollectionJson() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/features/collections/" + roadSegments, 200);

        assertEquals("cite:RoadSegments", json.read("$.id", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));
        assertEquals(-180, json.read("$.extent.spatial.bbox[0][0]", Double.class), 0d);
        assertEquals(-90, json.read("$.extent.spatial.bbox[0][1]", Double.class), 0d);
        assertEquals(180, json.read("$.extent.spatial.bbox[0][2]", Double.class), 0d);
        assertEquals(90, json.read("$.extent.spatial.bbox[0][3]", Double.class), 0d);
        assertEquals(
                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                json.read("$.extent.spatial.crs", String.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats = getFeaturesResponseFormats();
        assertThat(
                (int) json.read("$.links.length()", Integer.class),
                Matchers.greaterThanOrEqualTo(formats.size()));
        for (MediaType format : formats) {
            // check title and rel.
            List items = json.read("$.links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("cite:RoadSegments items as " + format, item.get("title"));
            assertEquals("items", item.get("rel"));
        }
        // the ogc/features specific GML3.2 output format is available
        readSingle(json, "links[?(@.type=='application/gml+xml;version=3.2')]");

        // check the queryables link
        assertThat(
                readSingle(
                        json, "links[?(@.rel=='queryables' && @.type=='application/json')].href"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/features/collections/cite%3ARoadSegments/queryables?f=application%2Fjson"));

        // check the CRS list, this feature type shares the top level list
        List<String> crs = json.read("crs");
        assertThat(
                crs.size(),
                Matchers.greaterThan(
                        5000)); // lots... the list is growing, hopefully will stay above 5k
        assertThat(
                crs,
                hasItems(
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        "http://www.opengis.net/def/crs/EPSG/0/4326",
                        "http://www.opengis.net/def/crs/EPSG/0/3857"));
        crs.remove("http://www.opengis.net/def/crs/OGC/1.3/CRS84");
        for (String c : crs) {
            assertThat(c, Matchers.startsWith("http://www.opengis.net/def/crs/EPSG/0"));
            assertTrue(
                    c + " is not using a numeric code",
                    c.substring("http://www.opengis.net/def/crs/EPSG/0/".length()).matches("\\d+"));
        }
    }

    @Test
    public void testCollectionJsonCustomCRSList() throws Exception {
        String roadSegments = getLayerId(MockData.BASIC_POLYGONS);
        DocumentContext json = getAsJSONPath("ogc/features/collections/" + roadSegments, 200);

        assertEquals("cite:BasicPolygons", json.read("$.id", String.class));

        // check the CRS list, this feature type shares the top level list
        List<String> crs = json.read("crs");
        assertThat(
                crs,
                contains(
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        "http://www.opengis.net/def/crs/EPSG/0/3857",
                        "http://www.opengis.net/def/crs/EPSG/0/32632"));
    }

    private List<MediaType> getFeaturesResponseFormats() {
        return GeoServerExtensions.bean(
                        APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                .getProducibleMediaTypes(FeaturesResponse.class, true);
    }

    @Test
    public void testCollectionVirtualWorkspace() throws Exception {
        String roadSegments = MockData.ROAD_SEGMENTS.getLocalPart();
        DocumentContext json = getAsJSONPath("cite/ogc/features/collections/" + roadSegments, 200);

        assertEquals("RoadSegments", json.read("$.id", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats = getFeaturesResponseFormats();
        assertThat(
                (int) json.read("$.links.length()", Integer.class),
                Matchers.greaterThanOrEqualTo(formats.size()));
        for (MediaType format : formats) {
            // check title and rel.
            List items = json.read("$.links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("RoadSegments items as " + format, item.get("title"));
            assertEquals("items", item.get("rel"));
        }
        // the ogc/features specific GML3.2 output format is available
        readSingle(json, "$.links[?(@.type=='application/gml+xml;version=3.2')]");
    }

    @Test
    @Ignore // ignoring XML output for the moment, we need to migrated it to use JAXB2 to be of any
    // usefulness
    public void testCollectionXML() throws Exception {
        Document dom =
                getAsDOM(
                        "ogc/features/collections/"
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "?f=application/xml");
        print(dom);
        String expected =
                "http://localhost:8080/geoserver/ogc/features/collections/cite%3ARoadSegments/items?f=application%2Fjson";
        XMLAssert.assertXpathEvaluatesTo(
                expected,
                "//wfs:Collection[wfs:id='cite:RoadSegments']/atom:link[@atom:type='application/json']/@atom:href",
                dom);
    }

    @Test
    public void testCollectionYaml() throws Exception {
        String yaml =
                getAsString(
                        "ogc/features/collections/"
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "?f=application/x-yaml");
        // System.out.println(yaml);
    }

    @Test
    public void testQueryables() throws Exception {
        String roadSegments = MockData.ROAD_SEGMENTS.getLocalPart();
        DocumentContext json =
                getAsJSONPath("cite/ogc/features/collections/" + roadSegments + "/queryables", 200);
        assertThat(readSingle(json, "queryables[?(@.id == 'the_geom')].type"), equalTo("geometry"));
        assertThat(readSingle(json, "queryables[?(@.id == 'FID')].type"), equalTo("string"));
        assertThat(readSingle(json, "queryables[?(@.id == 'NAME')].type"), equalTo("string"));
    }

    @Test
    public void testQueryablesHTML() throws Exception {
        String roadSegments = MockData.ROAD_SEGMENTS.getLocalPart();
        org.jsoup.nodes.Document document =
                getAsJSoup("cite/ogc/features/collections/" + roadSegments + "/queryables?f=html");
        assertEquals("the_geom: geometry", document.select("#queryables li:eq(0)").text());
    }
}
