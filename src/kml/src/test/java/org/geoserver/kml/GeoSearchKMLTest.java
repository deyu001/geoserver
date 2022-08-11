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

package org.geoserver.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GeoSearchKMLTest extends RegionatingTestSupport {

    @Before
    public void resetMetadata() throws IOException {
        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().remove("kml.regionateFeatureLimit");
        getCatalog().save(fti);
    }

    @After
    public void cleanupRegionationDatabases() throws IOException {
        File dir = getDataDirectory().findOrCreateDir("geosearch");
        FileUtils.deleteDirectory(dir);
    }

    @Test
    public void testSelfLinks() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.BASIC_POLYGONS.getPrefix()
                        + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart()
                        + "&styles="
                        + MockData.BASIC_POLYGONS.getLocalPart()
                        + "&height=1024&width=1024&bbox=-180,-90,0,90&srs=EPSG:4326"
                        + "&featureid=BasicPolygons.1107531493643&format_options=selfLinks:true";

        Document document = getAsDOM(path);
        // print(document);
        assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:Placemark)", document);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/cite/BasicPolygons/1107531493643.kml",
                "//kml:Placemark/atom:link/@href",
                document);
        assertXpathEvaluatesTo("self", "//kml:Placemark/atom:link/@rel", document);
    }

    /** Test that requests regionated by data actually return stuff. */
    @Test
    public void testDataRegionator() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:external-sorting;regionateAttr:NUM_LANES";

        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        int westCount = document.getDocumentElement().getElementsByTagName("Placemark").getLength();

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");

        assertEquals(1, westCount);
    }

    /** Test that requests regionated by geometry actually return stuff. */
    @Test
    public void testGeometryRegionator() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:geometry;regionateAttr:the_geom";
        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(
                1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");
    }

    /** Test that requests regionated by random criteria actually return stuff. */
    @Test
    public void testRandomRegionator() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:random";
        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(
                1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");
    }

    /**
     * Test that when a bogus regionating strategy is requested things still work. TODO: Evaluate
     * whether an error message should be returned instead.
     */
    @Test
    public void testBogusRegionator() throws Exception {
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:bogus";
        Document document = getAsDOM(path + "&bbox=0,-90,180,90", true);
        assertEquals("ServiceExceptionReport", document.getDocumentElement().getTagName());
    }

    /** Test whether geometries that cross tiles get put into both of them. */
    @Test
    public void testBigGeometries() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + CENTERED_POLY.getPrefix()
                        + ":"
                        + CENTERED_POLY.getLocalPart()
                        + "&styles="
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:external-sorting;regionateattr:foo";

        assertStatusCodeForGet(204, path + "&bbox=-180,-90,0,90");

        Document document = getAsDOM(path + "&bbox=0,-90,180,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(
                1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());
    }

    /** Test whether specifying different regionating strategies changes the results. */
    @Test
    public void testStrategyChangesStuff() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + TILE_TESTS.getPrefix()
                        + ":"
                        + TILE_TESTS.getLocalPart()
                        + "&bbox=-180,-90,0,90&styles="
                        + "&height=1024&width=1024&srs=EPSG:4326";

        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().put("kml.regionateFeatureLimit", 2);
        getCatalog().save(fti);

        Document geo =
                getAsDOM(path + "&format_options=regionateBy:geometry;regionateattr:location");
        assertEquals("kml", geo.getDocumentElement().getTagName());

        NodeList geoPlacemarks = geo.getDocumentElement().getElementsByTagName("Placemark");
        assertEquals(2, geoPlacemarks.getLength());

        Document data =
                getAsDOM(path + "&format_options=regionateBy:external-sorting;regionateAttr:z");
        assertEquals("kml", data.getDocumentElement().getTagName());

        NodeList dataPlacemarks = data.getDocumentElement().getElementsByTagName("Placemark");
        assertEquals(2, dataPlacemarks.getLength());

        for (int i = 0; i < geoPlacemarks.getLength(); i++) {
            String geoName = ((Element) geoPlacemarks.item(i)).getAttribute("id");
            String dataName = ((Element) dataPlacemarks.item(i)).getAttribute("id");

            assertFalse(
                    geoName + " and " + dataName + " should not be the same!",
                    geoName.equals(dataName));
        }
    }

    /** Test whether specifying different regionating strategies changes the results. */
    @Test
    public void testDuplicateAttribute() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + TILE_TESTS.getPrefix()
                        + ":"
                        + TILE_TESTS.getLocalPart()
                        + "&bbox=-180,-90,0,90&styles="
                        + "&height=1024&width=1024&srs=EPSG:4326";

        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().put("kml.regionateFeatureLimit", 2);

        Document geo =
                getAsDOM(path + "&format_options=regionateBy:best_guess;regionateattr:the_geom");
        assertEquals("kml", geo.getDocumentElement().getTagName());
    }
}
