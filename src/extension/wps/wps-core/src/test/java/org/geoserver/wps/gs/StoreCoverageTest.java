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

package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.mock.web.MockHttpServletResponse;

public class StoreCoverageTest extends WPSTestSupport {

    private static final QName CUST_WATTEMP =
            new QName(MockData.DEFAULT_URI, "watertemp", MockData.DEFAULT_PREFIX);
    static final double EPS = 1e-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
        testData.addRasterLayer(
                CUST_WATTEMP, "custwatertemp.zip", null, null, SystemTestData.class, getCatalog());
    }

    @Test
    public void testStore() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:StoreCoverage</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>coverage</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                        + "            <ows:Identifier>"
                        + getLayerId(MockData.TASMANIA_DEM)
                        + "</ows:Identifier>\n"
                        + "            <wcs:DomainSubset>\n"
                        + "              <gml:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                        + "                <ows:LowerCorner>-180.0 -90.0</ows:LowerCorner>\n"
                        + "                <ows:UpperCorner>180.0 90.0</ows:UpperCorner>\n"
                        + "              </gml:BoundingBox>\n"
                        + "            </wcs:DomainSubset>\n"
                        + "            <wcs:Output format=\"image/tiff\"/>\n"
                        + "          </wcs:GetCoverage>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput>\n"
                        + "      <ows:Identifier>coverageLocation</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        String url = response.getContentAsString();

        // System.out.println(url);

        Map<String, Object> query = KvpUtils.parseQueryString(url);
        assertEquals("GetExecutionResult", query.get("request"));
        String executionId = (String) query.get("executionId");
        String fileName = (String) query.get("outputId");

        WPSResourceManager resources = applicationContext.getBean(WPSResourceManager.class);
        Resource outputResource = resources.getOutputResource(executionId, fileName);
        File tiffFile = outputResource.file();

        assertTrue(tiffFile.exists());

        // read and check
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2D gc = format.getReader(tiffFile).read(null);
        scheduleForDisposal(gc);
        GridCoverage original =
                getCatalog()
                        .getCoverageByName(getLayerId(MockData.TASMANIA_DEM))
                        .getGridCoverage(null, null);
        scheduleForDisposal(original);

        //
        // check the envelope did not change
        assertEquals(original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
        assertEquals(original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
        assertEquals(original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
        assertEquals(original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);
    }

    @Test
    public void testStoreWCS10() throws Exception {
        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<wps:Execute service=\"WPS\" version=\"1.0.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\">"
                        + "<ows:Identifier>gs:StoreCoverage</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "  <wps:Input>"
                        + "    <ows:Identifier>coverage</ows:Identifier>"
                        + "    <wps:Reference xlink:href=\"http://geoserver/wcs\" method=\"POST\" mimeType=\"image/grib\">"
                        + "      <wps:Body>"
                        + "        <wcs:GetCoverage service=\"WCS\" version=\"1.0.0\" xmlns:wcs=\"http://www.opengis.net/wcs\" xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "          <wcs:sourceCoverage>"
                        + getLayerId(CUST_WATTEMP)
                        + "</wcs:sourceCoverage>"
                        + "          <wcs:domainSubset>"
                        + "            <wcs:spatialSubset>"
                        + "              <gml:Envelope srsName=\"EPSG:4326\">"
                        + "                <gml:pos>0.0 -91.0</gml:pos>"
                        + "                <gml:pos>360.0 90.0</gml:pos>"
                        + "              </gml:Envelope>"
                        + "              <gml:Grid dimension=\"2\">"
                        + "                <gml:limits>"
                        + "                  <gml:GridEnvelope>"
                        + "                    <gml:low>0 0</gml:low>"
                        + "                    <gml:high>360 181</gml:high>"
                        + "                  </gml:GridEnvelope>"
                        + "                </gml:limits>"
                        + "                <gml:axisName>x</gml:axisName>"
                        + "                <gml:axisName>y</gml:axisName>"
                        + "              </gml:Grid>"
                        + "            </wcs:spatialSubset>"
                        + "          </wcs:domainSubset>"
                        + "          <wcs:output>"
                        + "            <wcs:crs>EPSG:4326</wcs:crs>"
                        + "            <wcs:format>GEOTIFF</wcs:format>"
                        + "          </wcs:output>"
                        + "        </wcs:GetCoverage>"
                        + "      </wps:Body>"
                        + "    </wps:Reference>"
                        + "  </wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "  <wps:RawDataOutput>"
                        + "    <ows:Identifier>coverageLocation</ows:Identifier>"
                        + "  </wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        String url = response.getContentAsString();

        Map<String, Object> query = KvpUtils.parseQueryString(url);
        assertEquals("GetExecutionResult", query.get("request"));
        String executionId = (String) query.get("executionId");
        String fileName = (String) query.get("outputId");

        WPSResourceManager resources = applicationContext.getBean(WPSResourceManager.class);
        Resource outputResource = resources.getOutputResource(executionId, fileName);
        File tiffFile = outputResource.file();

        Assert.assertTrue(tiffFile.exists());

        // read and check
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2D gc = format.getReader(tiffFile).read(null);
        scheduleForDisposal(gc);
        GridCoverage original =
                getCatalog()
                        .getCoverageByName(getLayerId(CUST_WATTEMP))
                        .getGridCoverage(null, null);
        scheduleForDisposal(original);

        //
        // check the envelope did not change
        Assert.assertEquals(
                original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
        Assert.assertEquals(
                original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
        Assert.assertEquals(
                original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
        Assert.assertEquals(
                original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);
    }
}
