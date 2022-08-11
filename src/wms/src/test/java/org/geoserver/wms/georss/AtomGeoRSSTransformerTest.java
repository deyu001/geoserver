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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.Query;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AtomGeoRSSTransformerTest extends WMSTestSupport {
    static WMSMapContent map;

    @Before
    public void initializeMap() throws Exception {

        map = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.addLayer(createMapLayer(MockData.BASIC_POLYGONS));
    }

    @org.junit.Test
    public void testLatLongInternal() throws Exception {
        AtomGeoRSSTransformer tx = new AtomGeoRSSTransformer(getWMS());
        tx.setGeometryEncoding(AtomGeoRSSTransformer.GeometryEncoding.LATLONG);
        tx.setIndentation(2);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(map, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("geo:lat").getLength());
            assertEquals(1, entry.getElementsByTagName("geo:long").getLength());
        }
    }

    @org.junit.Test
    public void testLatLongWMS() throws Exception {
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:latlong&format=application/atom+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("geo:lat").getLength());
            assertEquals(1, entry.getElementsByTagName("geo:long").getLength());
        }
    }

    @org.junit.Test
    public void testSimpleInternal() throws Exception {
        AtomGeoRSSTransformer tx = new AtomGeoRSSTransformer(getWMS());
        tx.setGeometryEncoding(AtomGeoRSSTransformer.GeometryEncoding.SIMPLE);
        tx.setIndentation(2);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(map, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:where").getLength());
            assertEquals(1, entry.getElementsByTagName("georss:polygon").getLength());
        }
    }

    @org.junit.Test
    public void testSimpleWMS() throws Exception {
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:simple&format=application/atom+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:where").getLength());
            assertEquals(1, entry.getElementsByTagName("georss:polygon").getLength());
        }
    }

    @org.junit.Test
    public void testGmlWMS() throws Exception {
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:gml&format=application/atom+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:where").getLength());
            assertEquals(1, entry.getElementsByTagName("gml:Polygon").getLength());
        }
    }
}
