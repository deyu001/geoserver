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

package org.geoserver.geopkg;

import static org.geoserver.data.test.MockData.LAKES;
import static org.geoserver.data.test.MockData.WORLD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

/**
 * Test For WMS GetMap Output Format for GeoPackage
 *
 * @author Justin Deoliveira, Boundless
 */
public class GeoPackageGetMapOutputFormatTest extends WMSTestSupport {

    GeoPackageGetMapOutputFormat format;

    @Before
    public void setUpFormat() {
        format = new GeoPackageGetMapOutputFormat(getWebMapService(), getWMS(), GWC.get());
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

        WebMap map = format.produceMap(mapContent);
        GeoPackage geopkg = createGeoPackage(map);

        assertTrue(geopkg.features().isEmpty());
        assertEquals(1, geopkg.tiles().size());
        assertNotNull(geopkg.tile("World_Lakes"));
    }

    @Test
    /*
     * From the OGC GeoPackage Specification [1]:
     *
     * "The tile coordinate (0,0) always refers to the tile in the upper left corner of the tile matrix at any zoom
     * level, regardless of the actual availability of that tile"
     *
     * [1]: http://www.geopackage.org/spec/#tile_matrix
     */
    public void testTopLeftTile() throws Exception {
        WMSMapContent mapContent = createMapContent(WORLD);
        mapContent.getRequest().setBbox(new Envelope(-180, 180, -90, 90));

        WebMap map = format.produceMap(mapContent);
        GeoPackage geopkg = createGeoPackage(map);

        assertTrue(geopkg.features().isEmpty());
        assertEquals(1, geopkg.tiles().size());

        Tile topLeftTile = geopkg.reader(geopkg.tiles().get(0), 1, 1, 0, 0, 0, 0).next();

        /*
        FileOutputStream fous = new FileOutputStream("toplefttile.png");
        fous.write(topLeftTile.getData());
        fous.flush();
        fous.close();
        */

        BufferedImage tileImg = ImageIO.read(new ByteArrayInputStream(topLeftTile.getData()));

        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("toplefttile.png")), tileImg, 250);
    }

    GeoPackage createGeoPackage(WebMap map) throws IOException {
        assertTrue(map instanceof RawMap);

        RawMap rawMap = (RawMap) map;
        File f = File.createTempFile("temp", ".gpkg", new File("target"));
        FileOutputStream fout = new FileOutputStream(f);
        rawMap.writeTo(fout);
        fout.flush();
        fout.close();

        return new GeoPackage(f);
        //        File f = File.createTempFile("geopkg", "zip", new File("target"));
        //        FileOutputStream fout = new FileOutputStream(f);
        //        rawMap.writeTo(fout);
        //        fout.flush();
        //        fout.close();
        //
        //        File g = File.createTempFile("geopkg", "db", new File("target"));
        //        g.delete();
        //        g.mkdir();
        //
        //        IOUtils.decompress(f, g);
        //        return new GeoPackage(g.listFiles(new FileFilter() {
        //            @Override
        //            public boolean accept(File file) {
        //                return file.getName().endsWith(".geopackage");
        //            }
        //        })[0]);
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

    /*public static void main(String[] args) throws Exception {
        GeoPackage geopkg = new GeoPackage(new File(
            "/Users/jdeolive/geopkg.db"));;
        File d = new File("/Users/jdeolive/tiles");
        d.mkdir();

        TileEntry te = geopkg.tiles().get(0);
        TileReader r = geopkg.reader(te, null, null, null, null, null, null);
        while(r.hasNext()) {
            Tile t = r.next();
            File f = new File(d, String.format("%d-%d-%d.png", t.getZoom(), t.getColumn(), t.getRow()));

            FileUtils.writeByteArrayToFile(f, t.getData());
        }
    }*/
}
