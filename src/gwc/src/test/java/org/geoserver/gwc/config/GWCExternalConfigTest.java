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

package org.geoserver.gwc.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geoserver.util.PropertyRule;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that is possible to set a different GWC configuration directory using properties
 * GEOWEBCACHE_CONFIG_DIR_PROPERTY and GEOWEBCACHE_CACHE_DIR_PROPERTY.
 */
public final class GWCExternalConfigTest extends GeoServerSystemTestSupport {

    @Rule
    public PropertyRule configProp =
            PropertyRule.system(GeoserverXMLResourceProvider.GEOWEBCACHE_CONFIG_DIR_PROPERTY);

    @Rule
    public PropertyRule cacheProp =
            PropertyRule.system(GeoserverXMLResourceProvider.GEOWEBCACHE_CACHE_DIR_PROPERTY);

    private static final File rootTempDirectory;

    private static final String tempDirectory1;
    private static final String tempDirectory2;
    private static final String tempDirectory3;
    private static final String tempDirectory4;

    static {
        try {
            // init target directories
            rootTempDirectory = IOUtils.createTempDirectory("gwc");
            tempDirectory1 = new File(rootTempDirectory, "test-case-1").getCanonicalPath();
            tempDirectory2 = new File(rootTempDirectory, "test-case-2").getCanonicalPath();
            tempDirectory3 = new File(rootTempDirectory, "test-case-3").getCanonicalPath();
            tempDirectory4 = new File(rootTempDirectory, "test-case-4").getCanonicalPath();
        } catch (Exception exception) {
            throw new RuntimeException("Error initializing temporary directory.", exception);
        }
    }

    @Test
    public void testThatExternalDirectoryIsUsed() throws Exception {
        testUseCase(tempDirectory1, null, tempDirectory1);
        testUseCase(null, tempDirectory2, tempDirectory2);
        testUseCase(tempDirectory3, tempDirectory4, tempDirectory3);
    }

    /**
     * Helper method that setup the correct configuration variables, force Spring beans to be
     * reloaded and checks GWC configuration beans.
     */
    private void testUseCase(
            String configDirPath, String cacheDirPath, String expectedConfigFirPath) {
        // set or clear the gwc configuration directory property
        if (configDirPath == null) {
            configProp.clearValue();
        } else {
            configProp.setValue(configDirPath);
        }
        // set or clear the gwc cache directory property
        if (cacheDirPath == null) {
            cacheProp.clearValue();
        } else {
            cacheProp.setValue(cacheDirPath);
        }
        // rebuild the spring beans
        applicationContext.refresh();
        // check that the correct configuration directory is used
        applicationContext
                .getBeansOfType(GeoserverXMLResourceProvider.class)
                .values()
                .forEach(
                        bean -> {
                            try {
                                // check that configuration files are located in our custom
                                // directory
                                assertThat(bean.getConfigDirectory(), notNullValue());
                                assertThat(
                                        bean.getConfigDirectory().dir().getCanonicalPath(),
                                        is(expectedConfigFirPath));
                                // rely on canonical path for comparisons
                                assertThat(
                                        new File(bean.getLocation()).getCanonicalPath(),
                                        is(
                                                new File(
                                                                expectedConfigFirPath,
                                                                bean.getConfigFileName())
                                                        .getCanonicalPath()));
                            } catch (Exception exception) {
                                throw new RuntimeException(exception);
                            }
                        });
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        // remove the root temporary directory we created
        IOUtils.delete(rootTempDirectory);
    }
}
