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

package org.geoserver.wps.sextante;

import static org.junit.Assert.assertEquals;

import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class SextanteIntegrationTest extends WPSTestSupport {

    /** Tests raster input and output as arcgrid */
    @Test
    public void testArcGridInOut() throws Exception {
        // the baby that we want to parse
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>sxt:divide</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>LAYER</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/arcgrid\"><![CDATA[ncols         2\n"
                        + "nrows         2\n"
                        + "xllcorner     0.0\n"
                        + "yllcorner     0.0\n"
                        + "cellsize      1.0\n"
                        + "NODATA_value  -9999\n"
                        + "1 2\n"
                        + "3 4\n]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>LAYER2</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/arcgrid\"><![CDATA[ncols         2\n"
                        + "nrows         2\n"
                        + "xllcorner     0.0\n"
                        + "yllcorner     0.0\n"
                        + "cellsize      1.0\n"
                        + "NODATA_value  -9999\n"
                        + "2 2\n"
                        + "4 4\n]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"application/arcgrid\">\n"
                        + "      <ows:Identifier>RESULT</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        MockHttpServletResponse sr = postAsServletResponse(root(), xml);

        String expected =
                "NCOLS 2\n"
                        + "NROWS 2\n"
                        + "XLLCORNER 0.0\n"
                        + "YLLCORNER 0.0\n"
                        + "CELLSIZE 1.0\n"
                        + "NODATA_VALUE -9999\n"
                        + "0.5 1.0\n"
                        + "0.75 1.0\n";

        String actual = sr.getContentAsString();
        actual = actual.replace("\r", "");

        assertEquals(expected, actual);
    }
}
