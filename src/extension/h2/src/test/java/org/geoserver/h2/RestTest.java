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

package org.geoserver.h2;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.data.DataAccess;
import org.geotools.data.Query;
import org.geotools.feature.NameImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.mock.web.MockHttpServletResponse;

/** Contains tests that invoke REST resources that will use H2 data store. */
public final class RestTest extends GeoServerSystemTestSupport {

    private static final String WORKSPACE_NAME = "h2-tests";
    private static final String WORKSPACE_URI = "http://h2-tests.org";

    private static File ROOT_DIRECTORY;
    private static File DATABASE_DIR;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // create a test workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName(WORKSPACE_NAME);
        getCatalog().add(workspace);
        // create test workspace namespace
        NamespaceInfoImpl nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix(WORKSPACE_NAME);
        nameSpace.setURI(WORKSPACE_URI);
        getCatalog().add(nameSpace);
    }

    @BeforeClass
    public static void prepareDirectories() throws Throwable {
        // create root tests directory
        ROOT_DIRECTORY = IOUtils.createTempDirectory("h2-tests");
        // create the test database
        DATABASE_DIR = new File(ROOT_DIRECTORY, "testdb");
        DATABASE_DIR.mkdirs();
    }

    @AfterClass
    public static void cleanupDirectories() throws Throwable {
        // check if the root directory was initiated (test may have been skipped)
        if (ROOT_DIRECTORY != null) {
            // remove root tests directory
            IOUtils.delete(ROOT_DIRECTORY);
        }
    }

    @Before
    public void login() {
        // make sure we perform all requests logged as admin
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void createDataStoreUsingRestSingleFile() throws Exception {
        String dataStoreName = "h2-test-db-single";
        // send only the database file
        byte[] content = readSqLiteDatabaseFile();
        genericCreateDataStoreUsingRestTest(dataStoreName, "application/octet-stream", content);
    }

    @Test
    public void createDataStoreUsingRestZipFile() throws Exception {
        String dataStoreName = "h2-test-db-zip";
        // send the database directory
        byte[] content = readSqLiteDatabaseDir();
        genericCreateDataStoreUsingRestTest(dataStoreName, "application/zip", content);
    }

    public void genericCreateDataStoreUsingRestTest(
            String dataStoreName, String mimeType, byte[] content) throws Exception {
        // perform a PUT request, a new H2 data store should be created
        // we also require that all available feature types should be created
        String path =
                String.format(
                        "/rest/workspaces/%s/datastores/%s/file.h2?configure=all",
                        WORKSPACE_NAME, dataStoreName);
        MockHttpServletResponse response = putAsServletResponse(path, content, mimeType);
        // we should get a HTTP 201 status code meaning that the data store was created
        assertThat(response.getStatus(), is(201));
        // let's see if the data store was correctly created
        DataStoreInfo storeInfo = getCatalog().getDataStoreByName(dataStoreName);
        assertThat(storeInfo, notNullValue());
        DataAccess<? extends FeatureType, ? extends Feature> store = storeInfo.getDataStore(null);
        assertThat(store, notNullValue());
        List<Name> names = store.getNames();
        assertThat(store, notNullValue());
        // check that at least the table points is available
        Name found =
                names.stream()
                        .filter(name -> name != null && name.getLocalPart().equals("points"))
                        .findFirst()
                        .orElse(null);
        assertThat(found, notNullValue());
        // check that the points layer was correctly created
        LayerInfo layerInfo = getCatalog().getLayerByName(new NameImpl(WORKSPACE_URI, "points"));
        assertThat(layerInfo, notNullValue());
        assertThat(layerInfo.getResource(), notNullValue());
        assertThat(layerInfo.getResource(), instanceOf(FeatureTypeInfo.class));
        // check that we have the expected features
        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layerInfo.getResource();
        int count = featureTypeInfo.getFeatureSource(null, null).getCount(Query.ALL);
        assertThat(count, is(4));
    }

    /**
     * Helper method that just reads the test H2 database file and stores it in a array of bytes.
     */
    private static byte[] readSqLiteDatabaseFile() throws Exception {
        // open the database file
        try (InputStream input = RestTest.class.getResourceAsStream("/test-database.data.db");
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // copy the input stream to the output stream
            IOUtils.copy(input, output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Error reading SQLite database file to byte array.", exception);
        }
    }

    /** Helper method that zips the H2 data directory and returns it as an array of bytes. */
    private static byte[] readSqLiteDatabaseDir() throws Exception {
        // copy database file to database directory
        File outputFile = new File(DATABASE_DIR, "test-database.data.db");
        try (InputStream input = RestTest.class.getResourceAsStream("/test-database.data.db")) {
            IOUtils.copy(input, new FileOutputStream(outputFile));
        }
        // zip the database directory
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            // ignore the lock files
            IOUtils.zipDirectory(
                    DATABASE_DIR, zip, (dir, name) -> !name.toLowerCase().contains("lock"));
            zip.close();
            // just return the output stream content
            return output.toByteArray();
        }
    }
}
