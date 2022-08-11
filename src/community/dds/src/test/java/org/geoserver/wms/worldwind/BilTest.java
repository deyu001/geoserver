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

package org.geoserver.wms.worldwind;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test case for producing Raw bil images out of an elevation model.
 *
 * @author Tishampati Dhar
 * @since 2.0.x
 */
public class BilTest extends WMSTestSupport {
    /** This is a READ ONLY TEST so we can use one time setup */
    public static String WCS_PREFIX = "wcs";

    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName AUS_DEM = new QName(WCS_URI, "Ausdem", WCS_PREFIX);
    public static QName SF_DEM = new QName(MockData.SF_URI, "sfdem", MockData.SF_PREFIX);

    private final int width = 64;
    private final int height = 64;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle("raster", "raster.sld", BilTest.class, catalog);
        testData.addRasterLayer(AUS_DEM, "aus_dem.tiff", "tiff", null, BilTest.class, catalog);
        testData.addRasterLayer(SF_DEM, "sfdem.tiff", "tiff", null, BilTest.class, catalog);

        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);
    }

    @Test
    public void testBil() throws Exception {
        byte[] response = getStandardRequest("application/bil");

        int expected = width * height * 2; // Native encoding, 2 bytes/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    @Test
    public void testBil8() throws Exception {
        byte[] response = getStandardRequest("application/bil8");

        int expected = width * height; // 1 byte/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    @Test
    public void testBil16() throws Exception {
        byte[] response = getStandardRequest("application/bil16");

        int expected = width * height * 2; // 2 bytes/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    @Test
    public void testBil32() throws Exception {
        byte[] response = getStandardRequest("application/bil32");

        int expected = width * height * 4; // 4 bytes/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    private byte[] getStandardRequest(String mimeType) throws Exception {
        String layer = getLayerId(AUS_DEM);
        String request =
                "wms?service=wms&request=GetMap&version=1.1.1"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=108.3,-46.3,160.3,-4.2"
                        + "&width="
                        + width
                        + "&height="
                        + height
                        + "&format="
                        + mimeType
                        + "&srs=EPSG:4326";

        MockHttpServletResponse servletResponse = getAsServletResponse(request);
        return getBinary(servletResponse);
    }

    @Test
    public void testLargeRequest() throws Exception {
        String layer = getLayerId(AUS_DEM);
        String request =
                "wms?service=wms&request=GetMap&version=1.1.1"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=108.3,-46.3,160.3,-4.2&width=600&height=600"
                        + "&format=image/bil&srs=EPSG:4326";

        String exceptstr = getAsString(request);
        assertTrue("testLargeRequest", exceptstr.contains("512x512"));
    }

    @Test
    public void testWms13Request() throws Exception {
        String layer = getLayerId(AUS_DEM);

        String request =
                "wms?service=wms&request=GetMap&version=1.3.0"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-46.3,108.3,-4.2,160.3"
                        + "&width="
                        + width
                        + "&height="
                        + height
                        + "&format=application/bil&crs=EPSG:4326";

        MockHttpServletResponse servletResponse = getAsServletResponse(request);
        byte[] bytes1_3_0 = getBinary(servletResponse);

        // Check response length in bytes
        int expected = width * height * 2; // 2 bytes/pixel
        assertEquals("testStandardRequest", expected, bytes1_3_0.length);

        // Check that the WMS 1.3.0 response is equivalent to the WMS 1.1.1 response.
        byte[] bytes1_1_1 = getStandardRequest("application/bil16");
        assertArrayEquals("WMS 1.3.0 response different from WMS 1.1.1", bytes1_1_1, bytes1_3_0);
    }

    @Test
    public void testDefaultDataType() throws Exception {
        // Int8 default type
        setConfiguration(AUS_DEM, BilConfig.DEFAULT_DATA_TYPE, "application/bil8");
        byte[] bytes = getStandardRequest("application/bil");

        int expected = width * height * 1; // 1 bytes/pixel
        assertEquals("testStandardRequest", expected, bytes.length);

        // Int16 default type
        setConfiguration(AUS_DEM, BilConfig.DEFAULT_DATA_TYPE, "application/bil16");
        bytes = getStandardRequest("application/bil");

        expected = width * height * 2; // 2 bytes/pixel
        assertEquals("testStandardRequest", expected, bytes.length);

        // Int32 default type
        setConfiguration(AUS_DEM, BilConfig.DEFAULT_DATA_TYPE, "application/bil32");
        bytes = getStandardRequest("application/bil");

        expected = width * height * 4; // 4 bytes/pixel
        assertEquals("testStandardRequest", expected, bytes.length);
    }

    @Test
    public void testByteOrder() throws Exception {
        byte[] bytesDefault = getStandardRequest("application/bil16");

        setConfiguration(AUS_DEM, BilConfig.BYTE_ORDER, ByteOrder.BIG_ENDIAN.toString());
        byte[] bytesBigEndian = getStandardRequest("application/bil16");

        assertArrayEquals(
                "Big endian byte order should return same result as default",
                bytesDefault,
                bytesBigEndian);

        // Change byte order of big endian response to get expected little endian response.
        ShortBuffer expected =
                ByteBuffer.wrap(bytesBigEndian).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

        setConfiguration(AUS_DEM, BilConfig.BYTE_ORDER, ByteOrder.LITTLE_ENDIAN.toString());
        byte[] bytesLittleEndian = getStandardRequest("application/bil16");
        ShortBuffer actual = ByteBuffer.wrap(bytesLittleEndian).asShortBuffer();

        assertEquals(
                "Little endian byte order should return reverse of big endian byte order",
                expected,
                actual);
    }

    @Test
    public void testNoDataTranslation() throws Exception {
        final float inNoData = -9.99999993381581251e+36f;
        final float outNoData = -9999;

        String layer = getLayerId(SF_DEM);

        String request =
                "wms?service=wms&request=GetMap&version=1.1.0"
                        + "&layers="
                        + layer
                        + "&bbox=-103.871,44.370,-103.629,44.501"
                        + "&width="
                        + width
                        + "&height="
                        + width
                        + "&format=application/bil32&crs=EPSG:4326";

        // Issue request without no-data translation.
        MockHttpServletResponse servletResponse = getAsServletResponse(request);
        byte[] bytes = getBinary(servletResponse);
        FloatBuffer orig = ByteBuffer.wrap(bytes).asFloatBuffer();

        // Issue request again, recoding no-data values.
        setConfiguration(SF_DEM, BilConfig.NO_DATA_OUTPUT, String.valueOf(outNoData));
        servletResponse = getAsServletResponse(request);
        bytes = getBinary(servletResponse);
        FloatBuffer translated = ByteBuffer.wrap(bytes).asFloatBuffer();

        // Content length should be the same, though content will be different.
        assertEquals(
                "Unexpected content length after translating no-data values",
                orig.limit(),
                translated.limit());

        // Assert that each no-data value was recoded properly.
        int noDataCount = 0;
        for (int i = 0; i < orig.limit(); i++) {
            if (orig.get(i) == inNoData) {
                assertEquals(outNoData, translated.get(i), 1e-6);
                noDataCount += 1;
            }
        }

        // Make sure that the test actually did something.
        assertTrue("Did not find any no-data values", noDataCount > 0);
    }

    @Test
    public void testNonIntersectiongBbox() throws Exception {
        String layer = getLayerId(AUS_DEM);

        String bbox = "0,0,10,10"; // Does not intersection coverage area
        String request =
                "wms?service=wms&request=GetMap&version=1.1.1"
                        + "&layers="
                        + layer
                        + "&styles=&bbox="
                        + bbox
                        + "&width="
                        + width
                        + "&height="
                        + height
                        + "&format=application/bil16&srs=EPSG:4326";

        MockHttpServletResponse servletResponse = getAsServletResponse(request);
        byte[] response = getBinary(servletResponse);

        int expected = width * height * 2; // 2 bytes/pixel
        assertEquals("testStandardRequest", expected, response.length);
    }

    private void setConfiguration(QName layerQname, String key, String value) {
        String layer = getLayerId(layerQname);
        Catalog catalog = getCatalog();

        CoverageInfo coverage = catalog.getCoverageByName(layer);
        coverage.getMetadata().put(key, value);
        catalog.save(coverage);
    }

    /** Need to override since we are in the community folder */
    protected void copySchemaFile(String file) throws IOException {
        File f = new File("../../web/app/src/main/webapp/schemas/" + file);
        FileUtils.copyFile(f, getResourceLoader().createFile("WEB-INF/schemas/" + file));
    }
}
