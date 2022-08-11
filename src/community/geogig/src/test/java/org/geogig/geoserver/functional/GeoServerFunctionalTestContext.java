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

package org.geogig.geoserver.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.rest.GeoServerRepositoryProvider;
import org.geogig.web.functional.FunctionalTestContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.data.DataAccess;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.test.TestData;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Context for running GeoGIG web API functional tests from the plugin endpoints. */
public class GeoServerFunctionalTestContext extends FunctionalTestContext {

    private static final SecureRandom rnd = new SecureRandom();

    private MockHttpServletResponse lastResponse = null;

    private GeoServerRepositoryProvider repoProvider = null;

    private GeoGigTestData testData;

    // set by the JUnit test that runs the scenarios on its @BeforeClass set up so the geoserver
    // instance helper is shared among all feature scenarios
    static GeoServerTestSupport helper;

    /** Set up the context for a scenario. */
    @Override
    protected void setUp() throws Exception {
        testData = new GeoGigTestData(this.tempFolder);

        repoProvider = new GeoServerRepositoryProvider();
        RepositoryManager.get().setCatalog(helper.getCatalog());
        setVariable("@systemTempPath", tempFolder.getRoot().getCanonicalPath().replace("\\", "/"));
    }

    /** Clean up resources used in the scenario. */
    @Override
    protected void tearDown() throws Exception {
        try {
            RepositoryManager manager = RepositoryManager.get();
            List<RepositoryInfo> all = manager.getAll();
            for (RepositoryInfo ri : all) {
                String id = ri.getId();
                manager.delete(id);
            }
            RepositoryManager.close();
            if (testData != null) {
                testData.tearDown();
            }
        } finally {
            // helper = null;
        }
        System.runFinalization();
    }

    /**
     * Return the {@link GeoGIG} that corresponds to the given repository name.
     *
     * @param name the repository to get
     * @return the repository
     */
    @Override
    public Repository getRepo(String name) {
        return repoProvider.getGeogig(name).orNull();
    }

    /**
     * Initialize a repository with the given name. Does not register it with the Catalog or
     * associate it to a DataStore.
     *
     * @param name the repository name
     */
    void initRepo(String name) throws Exception {
        testData.setUp(name);
        testData.init().config("user.name", "John").config("user.email", "John.Doe@example.com");
    }

    @Override
    protected TestData createUnmanagedRepo(String name) throws Exception {
        initRepo(name);
        return new TestData(testData.getGeogig());
    }

    protected TestData createUnManagedRepoWithAltRoot(String name) throws Exception {
        File unmanagedRoot = testData.tmpFolder().newFolder("unmanagedRoot");
        testData.setUp(name, unmanagedRoot);
        testData.init().config("user.name", "John").config("user.email", "John.Doe@example.com");
        return new TestData(testData.getGeogig());
    }

    /**
     * Create a repository with the given name for testing.
     *
     * @param name the repository name
     * @return a newly created {@link TestData} for the repository.
     */
    @Override
    protected TestData createRepo(String name) throws Exception {
        initRepo(name);
        GeoGIG geogig = testData.getGeogig();

        Catalog catalog = helper.getCatalog();
        CatalogBuilder catalogBuilder = testData.newCatalogBuilder(catalog);
        int i = rnd.nextInt();
        catalogBuilder
                .namespace("geogig.org/" + i)
                .workspace("geogigws" + i)
                .store("geogigstore" + i);
        catalogBuilder.addAllRepoLayers().build();

        String workspaceName = catalogBuilder.workspaceName();
        String storeName = catalogBuilder.storeName();
        DataStoreInfo dsInfo = catalog.getDataStoreByName(workspaceName, storeName);
        assertNotNull(dsInfo);
        assertEquals(GeoGigDataStoreFactory.DISPLAY_NAME, dsInfo.getType());
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = dsInfo.getDataStore(null);
        assertNotNull(dataStore);
        assertTrue(dataStore instanceof GeoGigDataStore);

        String repoStr =
                (String)
                        dsInfo.getConnectionParameters().get(GeoGigDataStoreFactory.REPOSITORY.key);
        // resolve the repo
        URI repoURI = new URI(repoStr);
        RepositoryResolver resolver = RepositoryResolver.lookup(repoURI);
        String repoName = resolver.getName(repoURI);
        RepositoryInfo repositoryInfo = RepositoryManager.get().getByRepoName(repoName);
        assertNotNull(repositoryInfo);
        catalog.dispose();
        return new TestData(geogig);
    }

    /**
     * Helper function that asserts that there is a last response and returns it.
     *
     * @return the last response
     */
    private MockHttpServletResponse getLastResponse() {
        assertNotNull(lastResponse);
        return lastResponse;
    }

    /**
     * Issue a POST request to the provided URL with the given file passed as form data.
     *
     * @param resourceUri the url to issue the request to
     * @param formFieldName the form field name for the file to be posted
     * @param file the file to post
     */
    @Override
    protected void postFileInternal(String resourceUri, String formFieldName, File file) {
        resourceUri = replaceVariables(resourceUri);
        try {
            lastResponse = helper.postFile("/geogig" + resourceUri, formFieldName, file);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    /**
     * Issue a request with the given {@link HttpMethod} to the provided resource URI.
     *
     * @param method the http method to use
     * @param resourceUri the uri to issue the request to
     */
    @Override
    protected void callInternal(HttpMethod method, String resourceUri) {
        try {
            // resourceUri = replaceVariables(resourceUri);
            this.lastResponse = helper.callInternal(method, "/geogig" + resourceUri);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    /** @return the content of the last response as text */
    @Override
    public String getLastResponseText() {
        try {
            return getLastResponse().getContentAsString();
        } catch (UnsupportedEncodingException ex) {
            Throwables.propagate(ex);
        }
        return null;
    }

    /** @return the content type of the last response */
    @Override
    public String getLastResponseContentType() {
        return getLastResponse().getContentType();
    }

    /** @return the content of the last response as a {@link Document} */
    @Override
    public Document getLastResponseAsDom() {
        Document result = null;
        try {
            result = helper.getDom(getLastResponseInputStream());
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return result;
    }

    /** @return the status code of the last response */
    @Override
    public int getLastResponseStatus() {
        MockHttpServletResponse response = getLastResponse();
        // int code = response.getStatusCode();
        // if (response.getStatusCode() == 200) {
        // code = response.getErrorCode();
        // }
        // return code;
        return response.getStatus();
    }

    /** @return the content of the last response as an {@link InputStream} */
    @Override
    public InputStream getLastResponseInputStream() throws Exception {
        return new ByteArrayInputStream(getBinary(getLastResponse()));
    }

    /** @return the allowed http methods of the last response */
    @Override
    public Set<String> getLastResponseAllowedMethods() {
        return Sets.newHashSet(getLastResponse().getHeader("ALLOW").replace(" ", "").split(","));
    }

    protected byte[] getBinary(MockHttpServletResponse response) {
        // try {
        return response.getContentAsByteArray();
        // } catch (Exception e) {
        // throw new RuntimeException("Whoops, did you change the MockRunner version? " +
        // "If so, you might want to change this method too", e);
        // }
    }

    /**
     * Invokes URI request with specified Content-Type.
     *
     * @param method HTTP Method to invoke
     * @param resourceUri URI address to which to send the request
     * @param payload payload to encode into the request
     * @param contentType Specific Content-Type header value to send
     */
    @Override
    public void callInternal(
            HttpMethod method, String resourceUri, String payload, String contentType) {
        try {
            resourceUri = replaceVariables(resourceUri);
            this.lastResponse =
                    helper.callWithContentTypeInternal(
                            method, "/geogig" + resourceUri, payload, contentType);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public String getHttpLocation(String repoName) {
        return String.format("http://localhost:%d/geoserver/geogig/repos/%s", 8080, repoName);
    }

    @Override
    protected void postContentInternal(String contentType, String resourceUri, String postContent) {
        resourceUri = replaceVariables(resourceUri);
        postContent = replaceVariables(postContent);
        try {
            lastResponse = helper.postContent(contentType, "/geogig" + resourceUri, postContent);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    protected void serveHttpRepos() throws Exception {
        // Do Nothing
    }
}
