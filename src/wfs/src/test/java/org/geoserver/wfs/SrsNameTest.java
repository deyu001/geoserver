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

package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.GMLInfo.SrsNameStyle;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SrsNameTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {

        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);
    }

    @Test
    public void testWfs10() throws Exception {
        String q = "wfs?request=getfeature&service=wfs&version=1.0.0" + "&typename=cgf:Points";
        Document d = getAsDOM(q);
        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());

        print(d);
        NodeList boxes = d.getElementsByTagName("gml:Box");
        assertNotEquals(0, boxes.getLength());
        for (int i = 0; i < boxes.getLength(); i++) {
            Element box = (Element) boxes.item(i);
            assertEquals(
                    "http://www.opengis.net/gml/srs/epsg.xml#32615", box.getAttribute("srsName"));
        }

        NodeList points = d.getElementsByTagName("gml:Point");
        assertNotEquals(0, points.getLength());
        for (int i = 0; i < points.getLength(); i++) {
            Element point = (Element) points.item(i);
            assertEquals(
                    "http://www.opengis.net/gml/srs/epsg.xml#32615", point.getAttribute("srsName"));
        }
    }

    @Test
    public void testWfs11() throws Exception {
        WFSInfo wfs = getWFS();
        boolean oldFeatureBounding = wfs.isFeatureBounding();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        try {
            String q = "wfs?request=getfeature&service=wfs&version=1.1.0" + "&typename=cgf:Points";
            Document d = getAsDOM(q);
            assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());

            NodeList boxes = d.getElementsByTagName("gml:Envelope");
            assertNotEquals(0, boxes.getLength());
            for (int i = 0; i < boxes.getLength(); i++) {
                Element box = (Element) boxes.item(i);
                assertEquals("urn:x-ogc:def:crs:EPSG:32615", box.getAttribute("srsName"));
            }

            NodeList points = d.getElementsByTagName("gml:Point");
            assertNotEquals(0, points.getLength());
            for (int i = 0; i < points.getLength(); i++) {
                Element point = (Element) points.item(i);
                assertEquals("urn:x-ogc:def:crs:EPSG:32615", point.getAttribute("srsName"));
            }
        } finally {
            wfs.setFeatureBounding(oldFeatureBounding);
            getGeoServer().save(wfs);
        }
    }

    @Test
    public void testSrsNameSyntax11() throws Exception {
        doTestSrsNameSyntax11(SrsNameStyle.URN, false);
        doTestSrsNameSyntax11(SrsNameStyle.URN2, true);
        doTestSrsNameSyntax11(SrsNameStyle.URL, true);
        doTestSrsNameSyntax11(SrsNameStyle.NORMAL, true);
        doTestSrsNameSyntax11(SrsNameStyle.XML, true);
    }

    void doTestSrsNameSyntax11(SrsNameStyle srsNameStyle, boolean doSave) throws Exception {
        if (doSave) {
            WFSInfo wfs = getWFS();
            GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_11);
            gml.setSrsNameStyle(srsNameStyle);
            getGeoServer().save(wfs);
        }

        String q = "wfs?request=getfeature&service=wfs&version=1.1.0&typename=cgf:Points";
        Document d = getAsDOM(q);
        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());

        XMLAssert.assertXpathExists(
                "//gml:Envelope[@srsName = '" + srsNameStyle.getPrefix() + "32615']", d);
        XMLAssert.assertXpathExists(
                "//gml:Point[@srsName = '" + srsNameStyle.getPrefix() + "32615']", d);
    }
}
