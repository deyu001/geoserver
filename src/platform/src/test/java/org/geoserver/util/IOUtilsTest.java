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

package org.geoserver.util;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.io.Files;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IOUtilsTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder(new File("target"));

    @Test
    public void testZipUnzip() throws IOException {
        Path p1 = temp.newFolder("d1").toPath();
        p1.resolve("foo/bar").toFile().mkdirs();
        Files.touch(p1.resolve("foo/bar/bar.txt").toFile());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(bout);

        IOUtils.zipDirectory(p1.toFile(), zout, null);

        Path p2 = temp.newFolder("d2").toPath();
        p2.toFile().mkdirs();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        IOUtils.decompress(bin, p2.toFile());

        assertTrue(p2.resolve("foo/bar/bar.txt").toFile().exists());
    }

    @Test
    public void testDecompressStreamBadEntryName() throws IOException {
        File destDir = temp.newFolder("d3").toPath().toFile();
        destDir.mkdirs();
        try (InputStream input = ZipTestUtil.getZipSlipInput()) {
            IOUtils.decompress(input, destDir);
            fail("Expected decompression to fail");
        } catch (IOException e) {
            assertThat(e.getMessage(), startsWith("Entry is outside of the target directory"));
        }
    }

    @Test
    public void testDecompressFileBadEntryName() throws IOException {
        File destDir = temp.newFolder("d4").toPath().toFile();
        destDir.mkdirs();
        File input = ZipTestUtil.initZipSlipFile(temp.newFile("d4.zip"));
        try {
            IOUtils.decompress(input, destDir);
            fail("Expected decompression to fail");
        } catch (IOException e) {
            assertThat(e.getMessage(), startsWith("Entry is outside of the target directory"));
        }
    }
}
