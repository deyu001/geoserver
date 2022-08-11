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

package org.geoserver.importer;

import static org.geoserver.importer.ImporterTestUtils.unpack;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DirectoryTest {

    @Before
    public void setUp() {
        GeoServerExtensionsHelper.singleton(
                "spatialFileExtensionsProvider",
                new SpatialFileExtensionsProvider(),
                SupplementalFileExtensionsProvider.class);
    }

    @Test
    public void testMosaicAuxiliaryFiles() throws Exception {
        File unpack = ImporterTestUtils.unpack("mosaic/bm.zip");

        // all types of junk!
        String[] aux = new String[] {"aux", "rrd", "xml", "tif.aux.xml", "tfw"};
        File[] tifs = unpack.listFiles();
        for (File file : tifs) {
            for (String s : aux) {
                new File(unpack, file.getName().replace("tif", s)).createNewFile();
            }
        }

        Mosaic m = new Mosaic(unpack);
        m.prepare();

        Assert.assertEquals(4, m.getFiles().size());
        for (int i = 0; i < m.getFiles().size(); i++) {
            Assert.assertEquals("GeoTIFF", m.getFiles().get(1).getFormat().getName());
        }
        // make sure the junk was actually picked up
        for (FileData f : m.getFiles()) {
            Assert.assertEquals(aux.length, ((SpatialFile) f).getSuppFiles().size());
        }
    }

    @Test
    public void testSingleSpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        Directory d = new Directory(dir);
        d.prepare();

        List<FileData> files = d.getFiles();

        Assert.assertEquals(1, files.size());
        Assert.assertTrue(files.get(0) instanceof SpatialFile);

        SpatialFile spatial = (SpatialFile) files.get(0);
        Assert.assertEquals("shp", FilenameUtils.getExtension(spatial.getFile().getName()));

        Assert.assertNotNull(spatial.getPrjFile().getName());
        Assert.assertEquals("prj", FilenameUtils.getExtension(spatial.getPrjFile().getName()));

        Assert.assertEquals(2, spatial.getSuppFiles().size());

        Set<String> exts = new HashSet<>(Arrays.asList("shx", "dbf"));
        for (File supp : spatial.getSuppFiles()) {
            exts.remove(FilenameUtils.getExtension(supp.getName()));
        }

        Assert.assertTrue(exts.isEmpty());
    }

    @Test
    public void testShapefileWithMacOSXDirectory() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        File osxDir = new File(dir, "__MACOSX");
        osxDir.mkdir();
        new File(osxDir, ".archsites.shp").createNewFile();
        new File(osxDir, ".archsites.dbf").createNewFile();
        new File(osxDir, ".archsites.prj").createNewFile();

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertNotNull(d.getFormat());
        Assert.assertEquals(DataStoreFormat.class, d.getFormat().getClass());
        List<FileData> files = d.getFiles();
        Assert.assertEquals(1, files.size());
        Assert.assertEquals(DataStoreFormat.class, files.get(0).getFormat().getClass());
    }

    @Test
    public void testShapefileWithExtraFiles() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        // 'extra' files
        new File(dir, "archsites.shp.xml").createNewFile();
        new File(dir, "archsites.sbx").createNewFile();
        new File(dir, "archsites.sbn").createNewFile();
        new File(dir, "archsites.shp.ed.lock").createNewFile();

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertNotNull(d.getFormat());
        Assert.assertEquals(DataStoreFormat.class, d.getFormat().getClass());
        List<FileData> files = d.getFiles();
        Assert.assertEquals(1, files.size());
        Assert.assertEquals(DataStoreFormat.class, files.get(0).getFormat().getClass());
    }

    @Test
    public void testMultipleSpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertEquals(2, d.getFiles().size());
        Assert.assertTrue(d.getFiles().get(0) instanceof SpatialFile);
        Assert.assertTrue(d.getFiles().get(1) instanceof SpatialFile);
    }

    @Test
    public void testMultipleSpatialASpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        FileUtils.touch(new File(dir, "foo.txt")); // TODO: don't rely on alphabetical order

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertEquals(3, d.getFiles().size());
        Assert.assertTrue(d.getFiles().get(0) instanceof SpatialFile);
        Assert.assertTrue(d.getFiles().get(1) instanceof SpatialFile);
        Assert.assertTrue(d.getFiles().get(2) instanceof ASpatialFile);
    }
}
