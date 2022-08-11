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

package org.geoserver.wms.georss;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.georss.GeoRSSTransformerBase.GeometryEncoding;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.util.factory.GeoTools;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RSSGeoRSSTransformerTest extends WMSTestSupport {
    FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());

    @Test
    public void testChannelDescription() throws Exception {
        WMSMapContent map = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.addLayer(createMapLayer(MockData.BASIC_POLYGONS));
        map.layers().get(0).getUserData().put("abstract", "Test Abstract");

        Document document;
        try {
            document = getRSSResponse(map, AtomGeoRSSTransformer.GeometryEncoding.LATLONG);
        } finally {
            map.dispose();
        }
        Element element = document.getDocumentElement();
        assertEquals("rss", element.getNodeName());

        Element channel = (Element) element.getElementsByTagName("channel").item(0);
        NodeList description = channel.getElementsByTagName("description");
        assertEquals("Test Abstract", description.item(0).getChildNodes().item(0).getNodeValue());
    }

    @Test
    public void testLinkTemplate() throws Exception {
        WMSMapContent map = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.addLayer(createMapLayer(MockData.BASIC_POLYGONS));

        try {
            File linkFile =
                    new File(
                            testData.getDataDirectoryRoot().getAbsolutePath()
                                    + "/workspaces/cite/cite/BasicPolygons/link.ftl");
            try (FileOutputStream out = new FileOutputStream(linkFile)) {
                out.write("http://dummp.com".getBytes());
            }
        } catch (Exception e) {
            LOGGER.severe("Error writing link.ftl: " + e);
        }

        Document document;
        try {
            document = getRSSResponse(map, AtomGeoRSSTransformer.GeometryEncoding.LATLONG);
        } finally {
            map.dispose();
        }
        Element element = document.getDocumentElement();
        assertEquals("rss", element.getNodeName());

        NodeList items = element.getElementsByTagName("item");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, items.getLength());
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            assertThat(
                    item.getElementsByTagName("link").item(0).getTextContent(),
                    Matchers.containsString("http://dummp.com"));
        }
    }

    @Test
    public void testLatLongInternal() throws Exception {
        WMSMapContent map = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.addLayer(createMapLayer(MockData.BASIC_POLYGONS));

        Document document;
        try {
            document = getRSSResponse(map, AtomGeoRSSTransformer.GeometryEncoding.LATLONG);
        } finally {
            map.dispose();
        }
        Element element = document.getDocumentElement();
        assertEquals("rss", element.getNodeName());

        NodeList items = element.getElementsByTagName("item");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, items.getLength());

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            assertEquals(1, item.getElementsByTagName("geo:lat").getLength());
            assertEquals(1, item.getElementsByTagName("geo:long").getLength());
        }
    }

    @Test
    public void testLatLongWMS() throws Exception {
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:latlong&format=application/rss+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("rss", element.getNodeName());

        NodeList items = element.getElementsByTagName("item");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, items.getLength());

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            assertEquals(1, item.getElementsByTagName("geo:lat").getLength());
            assertEquals(1, item.getElementsByTagName("geo:long").getLength());
        }
    }

    @Test
    public void testSimpleInternal() throws Exception {
        WMSMapContent map = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.addLayer(createMapLayer(MockData.BASIC_POLYGONS));

        Document document;
        try {
            // print(document);
            document = getRSSResponse(map, GeoRSSTransformerBase.GeometryEncoding.SIMPLE);
        } finally {
            map.dispose();
        }
        Element element = document.getDocumentElement();
        assertEquals("rss", element.getNodeName());

        NodeList entries = element.getElementsByTagName("item");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:polygon").getLength());
        }
    }

    @Test
    public void testSimpleWMS() throws Exception {
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:simple&format=application/rss+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("rss", element.getNodeName());

        NodeList entries = element.getElementsByTagName("item");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:polygon").getLength());
        }
    }

    @Test
    public void testGmlWMS() throws Exception {
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:gml&format=application/rss+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("rss", element.getNodeName());

        NodeList entries = element.getElementsByTagName("item");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("gml:Polygon").getLength());
        }
    }

    @Test
    public void testFilter() throws Exception {
        // Set up a map context with a filtered layer
        WMSMapContent map = new WMSMapContent(createGetMapRequest(MockData.BUILDINGS));
        Document document;
        try {
            FeatureLayer layer = (FeatureLayer) createMapLayer(MockData.BUILDINGS);
            Filter f = ff.equals(ff.property("ADDRESS"), ff.literal("215 Main Street"));
            layer.setQuery(new Query(MockData.BUILDINGS.getLocalPart(), f));
            map.addLayer(layer);

            document = getRSSResponse(map, AtomGeoRSSTransformer.GeometryEncoding.LATLONG);
        } finally {
            map.dispose();
        }
        NodeList items = document.getDocumentElement().getElementsByTagName("item");
        assertEquals(1, items.getLength());
    }

    @Test
    public void testReproject() throws Exception {
        // Set up a map context with a projected layer
        WMSMapContent map = new WMSMapContent(createGetMapRequest(MockData.LINES));
        map.addLayer(createMapLayer(MockData.LINES));

        Document document;
        try {
            document = getRSSResponse(map, AtomGeoRSSTransformer.GeometryEncoding.LATLONG);
        } finally {
            map.dispose();
        }
        NodeList items = document.getDocumentElement().getElementsByTagName("item");

        // check all items are there
        assertEquals(1, items.getLength());

        // check coordinates are in wgs84, originals aren't
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            double lat = Double.parseDouble(getOrdinate(item, "geo:lat"));
            double lon = Double.parseDouble(getOrdinate(item, "geo:long"));
            assertTrue("Expected valid latitude value: " + lat, lat >= -90 && lat <= 90);
            assertTrue("Expected valid longitude value: " + lon, lon >= -180 && lon <= 180);
        }
    }

    String getOrdinate(Element item, String ordinate) {
        return item.getElementsByTagName(ordinate).item(0).getChildNodes().item(0).getNodeValue();
    }

    /** Returns a DOM given a map context and a geometry encoder */
    Document getRSSResponse(WMSMapContent map, GeometryEncoding encoding)
            throws TransformerException, ParserConfigurationException, FactoryConfigurationError,
                    SAXException, IOException {
        RSSGeoRSSTransformer tx = new RSSGeoRSSTransformer(getWMS());
        tx.setGeometryEncoding(encoding);
        tx.setIndentation(2);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(map, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));
        return document;
    }
}
