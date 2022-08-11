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

package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class CatalogBuilderIntTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testLargeNDMosaic() throws Exception {
        // build a mosaic with 1025 files (the standard ulimit is 1024)
        File mosaic = new File("./target/largeMosaic");
        try {
            createTimeMosaic(mosaic, 1025);

            // now configure a new store based on it
            Catalog cat = getCatalog();
            CatalogBuilder cb = new CatalogBuilder(cat);
            CoverageStoreInfo store = cb.buildCoverageStore("largeMosaic");
            store.setURL(mosaic.getAbsolutePath());
            store.setType("ImageMosaic");
            cat.add(store);

            // and configure also the coverage
            cb.setStore(store);
            CoverageInfo ci = cb.buildCoverage();
            cat.add(ci);
            cat.getResourcePool().dispose();
        } finally {
            if (mosaic.exists() && mosaic.isDirectory()) {
                FileUtils.deleteDirectory(mosaic);
            }
        }
    }

    @Test
    public void testMosaicParameters() throws Exception {
        // build a mosaic with 1025 files (the standard ulimit is 1024)
        File mosaic = new File("./target/smallMosaic");
        try {
            createTimeMosaic(mosaic, 4);

            // now configure a new store based on it
            Catalog cat = getCatalog();
            CatalogBuilder cb = new CatalogBuilder(cat);
            CoverageStoreInfo store = cb.buildCoverageStore("smallMosaic");
            store.setURL(mosaic.getAbsolutePath());
            store.setType("ImageMosaic");
            cat.add(store);

            // and configure also the coverage
            cb.setStore(store);
            CoverageInfo ci = cb.buildCoverage();
            cat.add(ci);

            // check the parameters have the default values
            assertEquals(
                    String.valueOf(-1),
                    ci.getParameters()
                            .get(ImageMosaicFormat.MAX_ALLOWED_TILES.getName().toString()));
            assertEquals("", ci.getParameters().get(ImageMosaicFormat.FILTER.getName().toString()));
            cat.getResourcePool().dispose();
        } finally {
            if (mosaic.exists() && mosaic.isDirectory()) {
                FileUtils.deleteDirectory(mosaic);
            }
        }
    }

    private void createTimeMosaic(File mosaic, int fileCount) throws Exception {
        if (mosaic.exists()) {
            if (mosaic.isDirectory()) {
                FileUtils.deleteDirectory(mosaic);
            } else {
                mosaic.delete();
            }
        }
        mosaic.mkdir();
        // System.out.println(mosaic.getAbsolutePath());

        // build the reference coverage into a byte array
        GridCoverageFactory factory = new GridCoverageFactory();
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        ReferencedEnvelope envelope = new ReferencedEnvelope(0, 10, 0, 10, CRS.decode("EPSG:4326"));
        GridCoverage2D test = factory.create("test", bi, envelope);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GeoTiffWriter writer = new GeoTiffWriter(bos);
        writer.write(test, null);
        writer.dispose();

        // create the lot of files
        byte[] bytes = bos.toByteArray();
        for (int i = 0; i < fileCount; i++) {
            String pad = "";
            if (i < 10) {
                pad = "000";
            } else if (i < 100) {
                pad = "00";
            } else if (i < 1000) {
                pad = "0";
            }
            File target = new File(mosaic, "tile_" + pad + i + ".tiff");
            FileUtils.writeByteArrayToFile(target, bytes);
        }

        // create the mosaic indexer property file
        Properties p = new Properties();
        p.put("ElevationAttribute", "elevation");
        p.put("Schema", "*the_geom:Polygon,location:String,elevation:Integer");
        p.put("PropertyCollectors", "IntegerFileNameExtractorSPI[elevationregex](elevation)");
        try (FileOutputStream fos = new FileOutputStream(new File(mosaic, "indexer.properties"))) {
            p.store(fos, null);
        }
        // and the regex itself
        p.clear();
        p.put("regex", "(?<=_)(\\d{4})");
        try (FileOutputStream fos =
                new FileOutputStream(new File(mosaic, "elevationregex.properties"))) {
            p.store(fos, null);
        }
    }
}
