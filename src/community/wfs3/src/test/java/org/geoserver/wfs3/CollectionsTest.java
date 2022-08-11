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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.junit.Test;
import org.w3c.dom.Document;

public class CollectionsTest extends WFS3TestSupport {

    public static final String BASIC_POLYGONS_TITLE = "Basic polygons";
    public static final String BASIC_POLYGONS_DESCRIPTION = "I love basic polygons!";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        basicPolygons.setTitle(BASIC_POLYGONS_TITLE);
        basicPolygons.setAbstract(BASIC_POLYGONS_DESCRIPTION);
        getCatalog().save(basicPolygons);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("wfs3/collections", 200);
        testCollectionsJson(json);
    }

    private void testCollectionsJson(DocumentContext json) throws Exception {
        int expected = getCatalog().getFeatureTypes().size();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel" relation
        List<String> formats =
                DefaultWebFeatureService30.getAvailableFormats(FeatureCollectionResponse.class);
        assertThat(
                formats.size(),
                lessThanOrEqualTo((int) json.read("collections[0].links.length()", Integer.class)));
        for (String format : formats) {
            // check title and rel.
            List items = json.read("collections[0].links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            assertEquals("item", item.get("rel"));
        }
        // tiling scheme extension
        Map tilingScheme =
                (Map)
                        json.read("collections[0].links[?(@.rel=='tilingScheme')]", List.class)
                                .get(0);
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections/cgf__Lines/tiles/{tilingSchemeId}",
                tilingScheme.get("href"));
        Map tiles = (Map) json.read("collections[0].links[?(@.rel=='tiles')]", List.class).get(0);
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections/cgf__Lines/tiles/{tilingSchemeId}/{level}/{row}/{col}",
                tiles.get("href"));
    }

    @Test
    public void testCollectionsWorkspaceSpecificJson() throws Exception {
        DocumentContext json = getAsJSONPath("cdf/wfs3/collections", 200);
        long expected =
                getCatalog()
                        .getFeatureTypes()
                        .stream()
                        .filter(ft -> "cdf".equals(ft.getStore().getWorkspace().getName()))
                        .count();
        // check the filtering
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));
        // check the workspace prefixes have been removed
        assertThat(json.read("collections[?(@.name=='Deletes')]"), not(empty()));
        assertThat(json.read("collections[?(@.name=='cdf__Deletes')]"), empty());
        // check the url points to a ws qualified url
        final String deleteHrefPath =
                "collections[?(@.name=='Deletes')].links[?(@.rel=='item' && @.type=='application/geo+json')].href";
        assertEquals(
                "http://localhost:8080/geoserver/cdf/wfs3/collections/Deletes/items?f=application%2Fgeo%2Bjson",
                ((JSONArray) json.read(deleteHrefPath)).get(0));
    }

    @Test
    public void testCollectionsXML() throws Exception {
        Document dom = getAsDOM("wfs3/collections?f=application/xml");
        print(dom);
        // TODO: add actual tests
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("wfs3/collections/?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json);
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("wfs3/collections?f=html");

        // check collection links
        List<FeatureTypeInfo> featureTypes = getCatalog().getFeatureTypes();
        for (FeatureTypeInfo featureType : featureTypes) {
            String encodedName = NCNameResourceCodec.encode(featureType);
            assertNotNull(document.select("#html_" + encodedName + "_link"));
            assertEquals(
                    "http://localhost:8080/geoserver/wfs3/collections/"
                            + encodedName
                            + "/items?f=text%2Fhtml&limit=50",
                    document.select("#html_" + encodedName + "_link").attr("href"));
        }

        // go and check a specific collection title and description
        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        String basicPolygonsName = NCNameResourceCodec.encode(basicPolygons);
        assertEquals(
                BASIC_POLYGONS_TITLE, document.select("#" + basicPolygonsName + "_title").text());
        assertEquals(
                BASIC_POLYGONS_DESCRIPTION,
                document.select("#" + basicPolygonsName + "_description").text());
    }
}
