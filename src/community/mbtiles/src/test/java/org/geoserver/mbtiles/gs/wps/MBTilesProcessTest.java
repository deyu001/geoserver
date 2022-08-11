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

package org.geoserver.mbtiles.gs.wps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.junit.Test;

public class MBTilesProcessTest extends WPSTestSupport {

    private static final Logger LOGGER = Logging.getLogger(MBTilesProcessTest.class);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testMBTilesProcess() throws Exception {
        File path = getDataDirectory().findOrCreateDataRoot();

        String urlPath = string(post("wps", getXml(path))).trim();
        File file = new File(path, "World.mbtiles");
        // File file = getDataDirectory().findFile("data", "test.mbtiles");
        assertNotNull(file);
        assertTrue(file.exists());

        MBTilesFile mbtiles = new MBTilesFile(file);
        MBTilesMetadata metadata = mbtiles.loadMetaData();
        assertEquals(11, mbtiles.maxZoom());
        assertEquals(10, mbtiles.minZoom());
        assertEquals("World", metadata.getName());

        assertEquals(-0.17578125, metadata.getBounds().getMinimum(0), 0.0001);
        assertEquals(-0.087890625, metadata.getBounds().getMinimum(1), 0.0001);
        assertEquals(0.17578125, metadata.getBounds().getMaximum(0), 0.0001);
        assertEquals(0.087890625, metadata.getBounds().getMaximum(1), 0.0001);

        try {
            mbtiles.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public String getXml(File temp) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:MBTiles</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>path</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>"
                + URLs.fileToUrl(temp)
                + "</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>layers</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>wcs:World</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>layers</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>cite:Lakes</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>format</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>image/png</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>boundingbox</ows:Identifier>"
                + "    <wps:Data>"
                + "    <wps:BoundingBoxData crs=\"EPSG:4326\" dimensions=\"2\">"
                + "     <ows:LowerCorner>-0.17578125 -0.087890625</ows:LowerCorner>"
                + "    <ows:UpperCorner>0.17578125 0.087890625</ows:UpperCorner>"
                + "    </wps:BoundingBoxData>"
                + "    </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>minZoom</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>10</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>maxZoom</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>12</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>bgColor</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>#FFFFFF</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  </wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>transparency</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>true</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  <wps:ResponseForm>"
                + "    <wps:RawDataOutput>"
                + "      <ows:Identifier>mbtile</ows:Identifier>"
                + "    </wps:RawDataOutput>"
                + "  </wps:ResponseForm>"
                + "</wps:Execute>";
    }
}
