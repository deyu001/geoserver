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

package org.geoserver.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.servlet.ServletContext;
import org.easymock.EasyMock;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link GeoServerResourceLoader}. */
public class GeoServerResourceLoaderTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for a single file that
     * exists.
     */
    @Test
    public void testRequireSingleExistingFile() {
        GeoServerResourceLoader.requireFile("pom.xml", "Test fixture");
    }

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for two files that exist.
     */
    @Test
    public void testRequireTwoExistingFiles() {
        GeoServerResourceLoader.requireFile("pom.xml" + File.pathSeparator + "src", "Test fixture");
    }

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for a single file that does
     * not exist.
     */
    @Test
    public void testRequireSingleMissingFile() {
        assertThrows(
                IllegalArgumentException.class,
                new ThrowingRunnable() {

                    @Override
                    public void run() throws Throwable {
                        GeoServerResourceLoader.requireFile("does-not-exist", "Test fixture");
                    }
                });
    }

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for two files where one does
     * not exist.
     */
    @Test
    public void testRequireSingleMissingFileOfTwo() {
        assertThrows(
                IllegalArgumentException.class,
                new ThrowingRunnable() {

                    @Override
                    public void run() throws Throwable {
                        GeoServerResourceLoader.requireFile(
                                "pom.xml" + File.pathSeparator + "does-not-exist", "Test fixture");
                    }
                });
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a
     * single required file that exists specified in the Java environment.
     */
    @Test
    public void testLookupRequireExistingFileJava() {
        Assume.assumeThat(System.getenv("GEOSERVER_DATA_DIR"), CoreMatchers.nullValue());
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        System.setProperty("GEOSERVER_REQUIRE_FILE", "pom.xml");
        System.clearProperty("GEOSERVER_DATA_DIR");
        try {
            Assert.assertEquals(
                    "data", GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
        } finally {
            System.clearProperty("GEOSERVER_REQUIRE_FILE");
        }
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a
     * single required file that does not exist specified in the Java environment.
     */
    @Test
    public void testLookupRequireMissingFileJava() {
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        System.setProperty("GEOSERVER_REQUIRE_FILE", "does-not-exist");
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    new ThrowingRunnable() {

                        @Override
                        public void run() throws Throwable {
                            GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
                        }
                    });
        } finally {
            System.clearProperty("GEOSERVER_REQUIRE_FILE");
        }
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a
     * single required file that does not exist specified in the servlet context.
     */
    @Test
    public void testLookupRequireMissingFileServlet() {
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE"))
                .andReturn("does-not-exist");
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        assertThrows(
                IllegalArgumentException.class,
                new ThrowingRunnable() {

                    @Override
                    public void run() throws Throwable {
                        GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
                    }
                });
    }

    @Test
    public void testSetBaseDirectory() throws IOException {
        GeoServerResourceLoader loader = new GeoServerResourceLoader();
        assertNull(loader.getBaseDirectory());
        assertEquals(ResourceStore.EMPTY, loader.getResourceStore());

        tempFolder.create();
        File tempDir = tempFolder.getRoot();
        loader.setBaseDirectory(tempDir);
        assertEquals(tempDir, loader.getBaseDirectory());
        assertTrue(loader.getResourceStore() instanceof FileSystemResourceStore);

        ResourceStore mockStore = EasyMock.createMock(ResourceStore.class);
        loader = new GeoServerResourceLoader(mockStore);
        assertNull(loader.getBaseDirectory());
        assertEquals(mockStore, loader.getResourceStore());

        loader.setBaseDirectory(tempDir);
        assertEquals(tempDir, loader.getBaseDirectory());
        assertEquals(mockStore, loader.getResourceStore());
    }

    @Test
    public void fromRelativeURLTest() throws IOException {
        GeoServerResourceLoader loader = new GeoServerResourceLoader();
        tempFolder.create();
        File tempDir = tempFolder.getRoot();
        loader.setBaseDirectory(tempDir);

        Resource res = loader.fromURL("file:relative/with/special+characters%23%C3%BC");
        assertEquals("relative/with/special characters#ü", res.path());

        // test it is writeable
        try (OutputStream out = res.out()) {
            out.write("someText".getBytes());
        }

        assertEquals("someText", Files.asCharSource(res.file(), Charset.defaultCharset()).read());
    }
}
