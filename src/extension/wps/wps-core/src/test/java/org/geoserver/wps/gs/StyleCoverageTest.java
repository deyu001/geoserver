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

import java.awt.image.IndexColorModel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.util.IOUtils;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.mock.web.MockHttpServletResponse;

public class StyleCoverageTest extends WPSTestSupport {

    static final double EPS = 1e-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
    }

    @Test
    public void testStyle() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:StyleCoverage</ows:Identifier>\n"
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
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>style</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"text/xml; subtype=sld/1.0.0\"><![CDATA[<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                        + "<StyledLayerDescriptor version=\"1.0.0\" xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                        + "  xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "  xsi:schemaLocation=\"http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd\">\n"
                        + "  <NamedLayer>\n"
                        + "    <Name>rain</Name>\n"
                        + "    <UserStyle>\n"
                        + "      <Name>rain</Name>\n"
                        + "      <Title>Rain distribution</Title>\n"
                        + "      <FeatureTypeStyle>\n"
                        + "        <Rule>\n"
                        + "          <RasterSymbolizer>\n"
                        + "            <Opacity>1.0</Opacity>\n"
                        + "            <ColorMap>\n"
                        + "              <ColorMapEntry color=\"#FF0000\" quantity=\"0\" />\n"
                        + "              <ColorMapEntry color=\"#FFFFFF\" quantity=\"100\"/>\n"
                        + "              <ColorMapEntry color=\"#00FF00\" quantity=\"2000\"/>\n"
                        + "            </ColorMap>\n"
                        + "          </RasterSymbolizer>\n"
                        + "        </Rule>\n"
                        + "      </FeatureTypeStyle>\n"
                        + "    </UserStyle>\n"
                        + "  </NamedLayer>\n"
                        + "</StyledLayerDescriptor>]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"image/tiff\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        assertEquals("attachment; filename=result.tiff", response.getHeader("Content-Disposition"));
        try (InputStream is = getBinaryInputStream(response)) {
            try (FileOutputStream fos = new FileOutputStream("target/testfile.tiff")) {
                IOUtils.copy(is, fos);
            }

            GeoTiffFormat format = new GeoTiffFormat();
            try (InputStream fis = new FileInputStream("target/testfile.tiff")) {
                GridCoverage2D gc = format.getReader(fis).read(null);

                GridCoverage original =
                        getCatalog()
                                .getCoverageByName(getLayerId(MockData.TASMANIA_DEM))
                                .getGridCoverage(null, null);

                // check the envelope did not change
                assertEquals(
                        original.getEnvelope().getMinimum(0), gc.getEnvelope().getMinimum(0), EPS);
                assertEquals(
                        original.getEnvelope().getMinimum(1), gc.getEnvelope().getMinimum(1), EPS);
                assertEquals(
                        original.getEnvelope().getMaximum(0), gc.getEnvelope().getMaximum(0), EPS);
                assertEquals(
                        original.getEnvelope().getMaximum(1), gc.getEnvelope().getMaximum(1), EPS);

                // check the color model is the expected one
                assertTrue(gc.getRenderedImage().getColorModel() instanceof IndexColorModel);
                assertEquals(1, gc.getRenderedImage().getSampleModel().getNumBands());
            }
        }
    }
}
