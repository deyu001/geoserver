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

package org.geoserver.wfs.response;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class Ogr2OgrWfsTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // copy the test custom configuration
        try (InputStream is = Ogr2OgrWfsTest.class.getResourceAsStream("/ogr2ogr.xml")) {
            testData.copyTo(is, "ogr2ogr.xml");
        }

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("", "http://www.opengis.net/wfs");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Before
    public void setup() {
        Assume.assumeTrue(Ogr2OgrTestUtil.isOgrAvailable());
        OgrConfiguration.DEFAULT.ogr2ogrLocation = Ogr2OgrTestUtil.getOgr2Ogr();
        OgrConfiguration.DEFAULT.gdalData = Ogr2OgrTestUtil.getGdalData();

        // force reload of the config, some tests alter it
        Ogr2OgrConfigurator configurator = applicationContext.getBean(Ogr2OgrConfigurator.class);
        configurator.loadConfiguration();
    }

    @Test
    public void testCapabilities() throws Exception {
        String request = "wfs?request=GetCapabilities&version=1.0.0";
        Document dom = getAsDOM(request);
        // print(dom);

        // while we cannot know what formats are available, the other tests won't pass if KML is not
        // there
        assertXpathEvaluatesTo("1", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:OGR-KML)", dom);
    }

    @Test
    public void testEmptyCapabilities() throws Exception {
        Ogr2OgrOutputFormat of = applicationContext.getBean(Ogr2OgrOutputFormat.class);
        of.clearFormats();

        String request = "wfs?request=GetCapabilities&version=1.0.0";
        Document dom = getAsDOM(request);
        // print(dom);

        // this used to NPE
        assertXpathEvaluatesTo("0", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:OGR-KML)", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:SHAPE-ZIP)", dom);
    }

    @Test
    public void testSimpleRequest() throws Exception {
        String request =
                "wfs?request=GetFeature&typename="
                        + getLayerId(MockData.BUILDINGS)
                        + "&version=1.0.0&service=wfs&outputFormat=OGR-KML";
        MockHttpServletResponse resp = getAsServletResponse(request);

        // check content type
        assertEquals("application/vnd.google-earth.kml", resp.getContentType());
        assertEquals("inline; filename=Buildings.kml", resp.getHeader("Content-Disposition"));

        // read back
        Document dom = dom(getBinaryInputStream(resp));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testSimpleRequestGeopackage() throws Exception {
        Assume.assumeTrue(Ogr2OgrOutputFormat.formats.containsKey("OGR-GPKG"));
        String request =
                "wfs?request=GetFeature&typename="
                        + getLayerId(MockData.BUILDINGS)
                        + "&version=1.0.0&service=wfs&outputFormat=OGR-GPKG";
        MockHttpServletResponse resp = getAsServletResponse(request);

        // check content type
        assertEquals("application/octet-stream", resp.getContentType());
        assertEquals("attachment; filename=Buildings.db", resp.getHeader("Content-Disposition"));
    }

    @Test
    public void testSimpleRequest20() throws Exception {
        String request =
                "wfs?request=GetFeature&typename="
                        + getLayerId(MockData.BUILDINGS)
                        + "&version=2.0.0&service=wfs&outputFormat=OGR-KML&srsName=EPSG:4326";
        MockHttpServletResponse resp = getAsServletResponse(request);

        // check content type
        assertEquals("application/vnd.google-earth.kml", resp.getContentType());

        // read back
        Document dom = dom(getBinaryInputStream(resp));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testDoubleRequest() throws Exception {
        String request =
                "wfs?request=GetFeature&typename="
                        + getLayerId(MockData.BUILDINGS)
                        + ","
                        + getLayerId(MockData.BRIDGES)
                        + "&version=1.0.0&service=wfs&outputFormat=OGR-KML";
        MockHttpServletResponse resp = getAsServletResponse(request);

        // check content type
        assertEquals("application/zip", resp.getContentType());

        // check content disposition
        assertEquals("attachment; filename=Buildings.zip", resp.getHeader("Content-Disposition"));

        // read back
        try (ZipInputStream zis = new ZipInputStream(getBinaryInputStream(resp))) {

            // get buildings entry
            ZipEntry entry = null;
            entry = zis.getNextEntry();
            while (entry != null) {
                if (entry.getName().equals("Buildings.kml")) {
                    break;
                }
                entry = zis.getNextEntry();
            }

            assertNotNull(entry);
            assertEquals("Buildings.kml", entry.getName());

            // parse the kml to check it's really xml...
            Document dom = dom(zis);
            // print(dom);

            // some very light assumptions on the contents, since we
            // cannot control how ogr encodes the kml... let's just assess
            // it's kml with the proper number of features
            assertEquals("kml", dom.getDocumentElement().getTagName());
            assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
        }
    }
}
