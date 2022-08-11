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

package org.geoserver;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.geoserver.ManifestLoader.AboutModel;
import org.geoserver.ManifestLoader.AboutModel.ManifestModel;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests for ManifestLoader, AboutModel and ManifestModel
 *
 * @author Carlo Cancellieri - Geo-Solutions SAS
 */
@Category(SystemTest.class)
public class ManifestLoaderTest extends GeoServerSystemTestSupport {

    // jar resource name to use for tests
    private static String resourceName = "freemarker-.*";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        try {
            new ManifestLoader(getResourceLoader());
        } catch (Exception e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
            org.junit.Assert.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void manifestLoaderVersionsTest() {
        assertNotNull(ManifestLoader.getVersions());
    }

    @Test
    public void manifestLoaderResourcesTest() {
        assertNotNull(ManifestLoader.getResources());
    }

    @Test
    public void filterNameByRegex() throws IllegalArgumentException {

        AboutModel resources = ManifestLoader.getResources();
        AboutModel filtered = resources.filterNameByRegex(resourceName);

        // extract first resource
        ManifestModel mm = filtered.getManifests().first();
        if (mm != null) {
            assertTrue(mm.getName().matches(resourceName));
        } else {
            LOGGER.log(
                    Level.WARNING,
                    "Unable to test with this resource name: "
                            + resourceName
                            + "\nNo resource found.");
        }
    }

    @Test
    public void filterPropertyByKeyOrValueTest() throws IllegalArgumentException {
        AboutModel resources = ManifestLoader.getResources();

        // extract first resource
        ManifestModel mm = resources.getManifests().first();
        if (mm == null) {
            LOGGER.log(
                    Level.WARNING,
                    "Unable to test with this resource name: "
                            + resourceName
                            + "\nNo resource found.");
            return;
        }

        // extract first property
        Iterator<Entry<String, String>> it = mm.getEntries().entrySet().iterator();
        if (!it.hasNext()) {
            LOGGER.log(
                    Level.WARNING,
                    "Unable to test with this resource name which does not has properties.");
            return;
        }
        Entry<String, String> entry = it.next();
        String propertyKey = entry.getKey();
        String propertyVal = entry.getValue();

        // check keys
        AboutModel filtered = resources.filterPropertyByKey(propertyKey);
        Iterator<ManifestModel> mit = filtered.getManifests().iterator();
        while (mit.hasNext()) {
            final ManifestModel model = mit.next();
            assertTrue(model.getEntries().containsKey(propertyKey));
        }

        // check values
        filtered = resources.filterPropertyByValue(propertyVal);
        mit = filtered.getManifests().iterator();
        while (mit.hasNext()) {
            final ManifestModel model = mit.next();
            assertTrue(model.getEntries().containsValue(propertyVal));
        }
    }

    @Test
    public void filterPropertyByKeyAndValueTest() throws IllegalArgumentException {
        AboutModel resources = ManifestLoader.getResources();

        // extract first resource
        ManifestModel mm = resources.getManifests().first();
        if (mm == null) {
            LOGGER.log(
                    Level.WARNING,
                    "Unable to test with this resource name: "
                            + resourceName
                            + "\nNo resource found.");
            return;
        }

        // extract first property
        Iterator<Entry<String, String>> it = mm.getEntries().entrySet().iterator();
        if (!it.hasNext()) {
            LOGGER.log(
                    Level.WARNING,
                    "Unable to test with this resource name which does not has properties.");
            return;
        }
        Entry<String, String> entry = it.next();
        String propertyKey = entry.getKey();
        String propertyVal = entry.getValue();

        // extract models
        AboutModel filtered = resources.filterPropertyByKeyValue(propertyKey, propertyVal);
        // check keys and values
        Iterator<ManifestModel> mit = filtered.getManifests().iterator();
        while (mit.hasNext()) {
            final ManifestModel model = mit.next();
            // check keys
            assertTrue(model.getEntries().containsKey(propertyKey));
            String value = model.getEntries().get(propertyKey);
            // check value
            assertEquals(value, propertyVal);
        }
    }

    @Test
    public void removeTest() {

        AboutModel resources = ManifestLoader.getResources();
        AboutModel newResources = ManifestLoader.getResources();

        ManifestModel mm = newResources.getManifests().first();

        assertTrue(resources.getManifests().contains(mm));

        // test remove
        resources.remove(mm.getName());

        assertFalse(resources.getManifests().contains(mm));
    }

    /**
     * SubTests to check properties personalizations
     *
     * @author Carlo Cancellieri - GeoSolutions
     */
    @Category(SystemTest.class)
    @TestSetup(run = TestSetupFrequency.REPEAT)
    public static class ManifestPropertiesTest extends GeoServerSystemTestSupport {

        private File properties;

        String propertyKey;

        @Override
        protected void setUpTestData(SystemTestData testData) throws Exception {}

        @Before
        public void paranoidCleanup() {
            // this file randomly shows up on the main module root and breaks the test
            // could not find where it's coming from, just going to remove it if it's there.
            File rootMonitor = new File(".", "manifest.properties");
            if (rootMonitor.exists()) {
                rootMonitor.delete();
            }
        }

        @Override
        protected void onSetUp(SystemTestData testData) throws Exception {
            AboutModel resources = ManifestLoader.getResources();

            // extract first resource
            ManifestModel mm = resources.getManifests().first();
            if (mm == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to test with this resource name: "
                                + resourceName
                                + "\nNo resource found.");
                return;
            }

            // extract a property
            Iterator<Entry<String, String>> it = mm.getEntries().entrySet().iterator();
            if (!it.hasNext()) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to test with this resource name which does not has properties.");
                return;
            }
            Entry<String, String> entry = it.next();
            propertyKey = entry.getKey();

            properties = new File(testData.getDataDirectoryRoot(), ManifestLoader.PROPERTIES_FILE);
            try (FileWriter writer = new FileWriter(properties)) {
                writer.write(
                        ManifestLoader.VERSION_ATTRIBUTE_INCLUSIONS + "=" + propertyKey + "\n");
                writer.write(ManifestLoader.RESOURCE_ATTRIBUTE_EXCLUSIONS + "=" + propertyKey);
                writer.flush();
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Unable to write test data to:" + testData.getDataDirectoryRoot());
                org.junit.Assert.fail(e.getLocalizedMessage());
            }

            // rebuild loader with new configuration
            try {
                new ManifestLoader(getResourceLoader());
            } catch (Exception e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
                org.junit.Assert.fail(e.getLocalizedMessage());
            }
        }

        @Override
        protected void onTearDown(SystemTestData testData) throws Exception {
            FileUtils.deleteQuietly(properties);
        }

        @Test
        public void filterExcludingAttributes() {
            // load resources filtering attributes EXCLUDING propertyKey
            final AboutModel resources = ManifestLoader.getResources();

            // extract resources
            final Iterator<ManifestModel> mmit = resources.getManifests().iterator();
            while (mmit.hasNext()) {
                // extract properties
                Iterator<Entry<String, String>> it = mmit.next().getEntries().entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, String> entry = it.next();
                    // the propertyKey should NOT be present
                    assertFalse(propertyKey.equals(entry.getKey()));
                }
            }
        }

        @Test
        public void filterIncludingAttributes() {
            // load resources filtering attributes INCLUDING propertyKey
            final AboutModel versions = ManifestLoader.getVersions();

            // extract resources
            final Iterator<ManifestModel> mmit = versions.getManifests().iterator();
            while (mmit.hasNext()) {
                // extract first property
                Iterator<Entry<String, String>> it = mmit.next().getEntries().entrySet().iterator();
                while (it.hasNext()) {
                    Entry<String, String> entry = it.next();
                    // the propertyKey MUST be present
                    assertEquals(propertyKey, entry.getKey());
                }
            }
        }
    }
}
