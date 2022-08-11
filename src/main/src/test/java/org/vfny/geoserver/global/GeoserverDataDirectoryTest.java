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

package org.vfny.geoserver.global;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import javax.xml.namespace.QName;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests covering the former functionality of GeoServerDataDirectory.
 *
 * <p>Much of this functionality depends on the availability of GeoServerResourceLoader in the
 * application context as the bean "resourceLoader".
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@Category(SystemTest.class)
public class GeoserverDataDirectoryTest extends GeoServerSystemTestSupport {

    private static final String EXTERNAL_ENTITIES = "externalEntities";

    private static final char SEPARATOR_CHAR = File.separatorChar;

    private static final String RAIN_DATA_PATH =
            "rain" + SEPARATOR_CHAR + "rain" + SEPARATOR_CHAR + "rain.asc";

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addStyle(EXTERNAL_ENTITIES, "externalEntities.sld", TestData.class, getCatalog());
    }

    @Test
    public void testFindDataFile() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File file =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()),
                                "file:" + RAIN_DATA_PATH),
                        true);
        assertNotNull(file);
    }

    @Test
    public void testFindDataFileForAbsolutePath() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File dataDir = loader.getBaseDirectory();
        final String absolutePath = dataDir.getCanonicalPath() + SEPARATOR_CHAR + RAIN_DATA_PATH;
        final File file =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()), absolutePath),
                        true);
        assertNotNull(file);
    }

    @Test
    public void testFindDataFileForCustomUrl() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File file =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()),
                                "sde://user:password@server:port"),
                        true);
        assertNull(file); // Before GEOS-5931 it would have been returned a file again
    }

    @Test
    public void testStyleWithExternalEntities() throws Exception {
        GeoServerDataDirectory dd = getDataDirectory();
        StyleInfo si = getCatalog().getStyleByName(EXTERNAL_ENTITIES);
        try {
            dd.parsedStyle(si);
            fail("Should have failed with a parse error");
        } catch (Exception e) {
            String message = e.getMessage();
            assertThat(message, containsString("Entity resolution disallowed"));
            assertThat(message, containsString("/this/file/does/not/exist"));
        }
    }
}
