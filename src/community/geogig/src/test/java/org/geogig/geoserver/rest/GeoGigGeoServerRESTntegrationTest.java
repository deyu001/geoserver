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

package org.geogig.geoserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geogig.geoserver.config.GeoServerGeoGigRepositoryResolver;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geotools.data.DataStore;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.plumbing.ResolveGeogigDir;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.springframework.mock.web.MockHttpServletResponse;

/** Integration test suite with GeoServer's REST API */
public class GeoGigGeoServerRESTntegrationTest extends CatalogRESTTestSupport {

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        geogigData
                .init() //
                .config("user.name", "gabriel") //
                .config("user.email", "gabriel@test.com") //
                .createTypeTree("lines", "geom:LineString:srid=4326") //
                .createTypeTree("points", "geom:Point:srid=4326") //
                .add() //
                .commit("created type trees") //
                .get();

        geogigData.insert(
                "points", //
                "p1=geom:POINT(0 0)", //
                "p2=geom:POINT(1 1)", //
                "p3=geom:POINT(2 2)");

        geogigData.insert(
                "lines", //
                "l1=geom:LINESTRING(-10 0, 10 0)", //
                "l2=geom:LINESTRING(0 0, 180 0)");

        geogigData.add().commit("Added test features");

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.setUpWorkspace("gigws");
    }

    @After
    public void after() {
        RepositoryManager.close();
        getCatalog().dispose();
    }

    /** Override so that default layers are not added */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        //
    }

    private void checkNewConfig(DataStoreInfo ds) throws IOException, URISyntaxException {
        Map<String, Serializable> params = ds.getConnectionParameters();
        String repository = (String) params.get(REPOSITORY.key);
        assertNotNull(repository);
        // get a resolver to get the ID
        URI repoURI = new URI(repository);
        RepositoryResolver resolver = RepositoryResolver.lookup(repoURI);
        assertTrue(
                String.format(
                        "Expected GeoGig DataStoreInfo to containg a '%s' URI value with scheme '%s'",
                        REPOSITORY.key, GeoServerGeoGigRepositoryResolver.GEOSERVER_URI_SCHEME),
                resolver.canHandle(repoURI));
    }

    @Test
    public void createDataStoreNewConfig() throws Exception {
        String message =
                "<dataStore>\n" //
                        + " <name>repo_new_config</name>\n" //
                        + " <type>GeoGIG</type>\n" //
                        + " <connectionParameters>\n" //
                        + "   <entry key=\"geogig_repository\">${repository}</entry>\n" //
                        + " </connectionParameters>\n" //
                        + "</dataStore>\n";

        GeoGIG geogig = geogigData.createRepository("new_repo");
        try {
            geogig.command(InitOp.class).call();
            File repo = geogig.command(ResolveGeogigDir.class).getFile().get();
            final URI location = repo.getParentFile().getAbsoluteFile().toURI();
            RepositoryManager manager = RepositoryManager.get();
            RepositoryInfo info = new RepositoryInfo();
            info.setLocation(location);
            info = manager.save(info);

            final String repoName = info.getRepoName();
            message =
                    message.replace(
                            "${repository}", GeoServerGeoGigRepositoryResolver.getURI(repoName));

            // System.err.println(message);

            Catalog catalog = getCatalog();
            CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
            catalogBuilder.setUpWorkspace("new_ws");

            final String uri = "/rest/workspaces/new_ws/datastores";
            MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");

            assertEquals(201, response.getStatus());

            String locationHeader = response.getHeader("Location");
            assertNotNull(locationHeader);
            assertTrue(locationHeader.endsWith("/workspaces/new_ws/datastores/repo_new_config"));

            DataStoreInfo newDataStore = catalog.getDataStoreByName("repo_new_config");
            assertNotNull(newDataStore);

            DataStore ds = (DataStore) newDataStore.getDataStore(null);
            assertNotNull(ds);

            checkNewConfig(newDataStore);
        } finally {
            geogig.close();
        }
    }

    @Test
    public void createDataStoreCustomURIWithName() throws Exception {
        String message =
                "<dataStore>\n" //
                        + " <name>repo_new_config2</name>\n" //
                        + " <type>GeoGIG</type>\n" //
                        + " <connectionParameters>\n" //
                        + "   <entry key=\"geogig_repository\">${repository}</entry>\n" //
                        + " </connectionParameters>\n" //
                        + "</dataStore>\n";

        GeoGIG geogig = geogigData.createRepository("new_repo1");
        try {
            geogig.command(InitOp.class).call();
            File repo = geogig.command(ResolveGeogigDir.class).getFile().get();
            final URI location = repo.getParentFile().getAbsoluteFile().toURI();
            RepositoryManager manager = RepositoryManager.get();
            RepositoryInfo info = new RepositoryInfo();
            info.setLocation(location);
            info = manager.save(info);

            final String customURI = GeoServerGeoGigRepositoryResolver.getURI("new_repo1");
            message = message.replace("${repository}", customURI);

            final String uri = "/rest/workspaces/gigws/datastores";
            MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");

            assertEquals(201, response.getStatus());

            String locationHeader = response.getHeader("Location");
            assertNotNull(locationHeader);
            assertTrue(locationHeader.endsWith("/workspaces/gigws/datastores/repo_new_config2"));

            DataStoreInfo newDataStore = catalog.getDataStoreByName("repo_new_config2");
            assertNotNull(newDataStore);

            DataStore ds = (DataStore) newDataStore.getDataStore(null);
            assertNotNull(ds);

            checkNewConfig(newDataStore);
        } finally {
            geogig.close();
        }
    }
}
