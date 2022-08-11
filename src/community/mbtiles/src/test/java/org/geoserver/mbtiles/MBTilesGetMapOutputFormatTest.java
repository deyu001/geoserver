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

package org.geoserver.mbtiles;

import static org.geoserver.data.test.MockData.LAKES;
import static org.geoserver.data.test.MockData.WORLD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.mbtiles.MBTilesTile;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test For WMS GetMap Output Format for MBTiles
 *
 * @author Niels Charlier
 */
public class MBTilesGetMapOutputFormatTest extends WMSTestSupport {

    MBTilesGetMapOutputFormat format;

    @Before
    public void setUpFormat() {
        format = new MBTilesGetMapOutputFormat(getWebMapService(), getWMS(), GWC.get());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testTileEntries() throws Exception {
        WMSMapContent mapContent = createMapContent(WORLD, LAKES);
        mapContent
                .getRequest()
                .setBbox(new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625));
        mapContent.getRequest().getFormatOptions().put("min_zoom", "10");
        mapContent.getRequest().getFormatOptions().put("max_zoom", "11");

        WebMap map = format.produceMap(mapContent);
        MBTilesFile mbtiles = createMbTilesFiles(map);

        MBTilesMetadata metadata = mbtiles.loadMetaData();

        assertEquals("World_Lakes", metadata.getName());
        assertEquals("0", metadata.getVersion());
        assertEquals("World, null", metadata.getDescription());
        assertEquals(-0.17578125, metadata.getBounds().getMinimum(0), 0.001);
        assertEquals(-0.087890625, metadata.getBounds().getMaximum(0), 0.001);
        assertEquals(0.17578125, metadata.getBounds().getMaximum(1), 0.001);
        assertEquals(0.087890625, metadata.getBounds().getMinimum(1), 0.001);
        assertEquals(MBTilesMetadata.t_type.OVERLAY, metadata.getType());
        assertEquals(MBTilesMetadata.t_format.PNG, metadata.getFormat());

        assertEquals(1, mbtiles.numberOfTiles());

        MBTilesFile.TileIterator tiles = mbtiles.tiles();
        assertTrue(tiles.hasNext());
        MBTilesTile e = tiles.next();
        assertEquals(10, e.getZoomLevel());
        assertEquals(511, e.getTileColumn());
        assertEquals(512, e.getTileRow());
        assertNotNull(e.getData());
        tiles.close();

        mbtiles.close();
    }

    @Test
    public void testTileEntriesWithAddTiles() throws Exception {
        // Create a getMap request
        WMSMapContent mapContent = createMapContent(WORLD, LAKES);
        mapContent
                .getRequest()
                .setBbox(new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625));
        mapContent.getRequest().getFormatOptions().put("min_zoom", "10");
        mapContent.getRequest().getFormatOptions().put("max_zoom", "11");
        // Create a temporary file for the mbtiles
        File f = File.createTempFile("temp2", ".mbtiles", new File("target"));
        MBTilesFile mbtiles = new MBTilesFile(f);
        mbtiles.init();
        // Add tiles to the file(Internally uses the MBtilesFileWrapper)
        format.addTiles(mbtiles, mapContent.getRequest(), null);
        // Ensure everything is correct
        MBTilesMetadata metadata = mbtiles.loadMetaData();

        assertEquals("World_Lakes", metadata.getName());
        assertEquals("0", metadata.getVersion());
        assertEquals("World, null", metadata.getDescription());
        assertEquals(-0.17578125, metadata.getBounds().getMinimum(0), 0.001);
        assertEquals(-0.087890625, metadata.getBounds().getMaximum(0), 0.001);
        assertEquals(0.17578125, metadata.getBounds().getMaximum(1), 0.001);
        assertEquals(0.087890625, metadata.getBounds().getMinimum(1), 0.001);
        assertEquals(MBTilesMetadata.t_type.OVERLAY, metadata.getType());
        assertEquals(MBTilesMetadata.t_format.PNG, metadata.getFormat());

        assertEquals(1, mbtiles.numberOfTiles());

        MBTilesFile.TileIterator tiles = mbtiles.tiles();
        assertTrue(tiles.hasNext());
        MBTilesTile e = tiles.next();
        assertEquals(10, e.getZoomLevel());
        assertEquals(511, e.getTileColumn());
        assertEquals(512, e.getTileRow());
        assertNotNull(e.getData());
        tiles.close();
        // Closure of the files
        mbtiles.close();
        FileUtils.deleteQuietly(f);
    }

    @Test
    public void testDifferentBbox() throws NoSuchAuthorityCodeException, FactoryException {
        // Instantiate a request
        GetMapRequest req = new GetMapRequest();
        // Define CRS
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        // Create the first bbox
        ReferencedEnvelope bbox1 = new ReferencedEnvelope(0, 1, 0, 1, crs);
        req.setBbox(bbox1);
        req.setCrs(crs);
        ReferencedEnvelope bounds1 = format.bounds(req);
        // Create the second bbox
        ReferencedEnvelope bbox2 = new ReferencedEnvelope(1, 2, 1, 2, crs);
        req.setBbox(bbox2);
        ReferencedEnvelope bounds2 = format.bounds(req);
        // Ensure that the 2 generated bbox are not the same so that they are not cached
        double tolerance = 0.1d;
        assertNotSame(bounds1, bounds2);
        assertNotEquals(bounds1.getMinX(), bounds2.getMinX(), tolerance);
        assertNotEquals(bounds1.getMinY(), bounds2.getMinY(), tolerance);
        assertNotEquals(bounds1.getMaxX(), bounds2.getMaxX(), tolerance);
        assertNotEquals(bounds1.getMaxY(), bounds2.getMaxY(), tolerance);
    }

    MBTilesFile createMbTilesFiles(WebMap map) throws IOException {
        assertTrue(map instanceof RawMap);

        RawMap rawMap = (RawMap) map;
        File f = File.createTempFile("temp", ".mbtiles", new File("target"));
        FileOutputStream fout = new FileOutputStream(f);
        rawMap.writeTo(fout);
        fout.flush();
        fout.close();

        return new MBTilesFile(f);
    }

    protected GetMapRequest createGetMapRequest(QName[] layerNames) {
        GetMapRequest request = super.createGetMapRequest(layerNames);
        request.setBbox(new Envelope(-180, 180, -90, 90));
        return request;
    };

    WMSMapContent createMapContent(QName... layers) throws IOException {
        GetMapRequest mapRequest = createGetMapRequest(layers);
        WMSMapContent map = new WMSMapContent(mapRequest);
        for (QName l : layers) {
            map.addLayer(createMapLayer(l));
        }
        return map;
    }
}
