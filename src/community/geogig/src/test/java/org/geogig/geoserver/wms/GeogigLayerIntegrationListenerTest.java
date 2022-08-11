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

package org.geogig.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class GeogigLayerIntegrationListenerTest extends GeoServerSystemTestSupport {

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
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

        // add a branch for the explicit HEAD test
        geogigData.branch("fakeBranch");
        // need to instantiate the listerner so it can register with the test GeoServer instance
        new GeogigLayerIntegrationListener(getGeoServer());
    }

    @After
    public void after() {
        RepositoryManager.close();
    }

    @Test
    public void testAddGeogigLayerForcesCreationOfRootAuthURL() {
        addAvailableGeogigLayers();

        WMSInfo service = getGeoServer().getService(WMSInfo.class);
        List<AuthorityURLInfo> authorityURLs = service.getAuthorityURLs();
        AuthorityURLInfo expected = null;
        for (AuthorityURLInfo auth : authorityURLs) {
            if (GeogigLayerIntegrationListener.AUTHORITY_URL_NAME.equals(auth.getName())) {
                expected = auth;
                break;
            }
        }
        assertNotNull("No geogig auth url found: " + authorityURLs, expected);
    }

    @Test
    public void testAddGeogigLayerAddsLayerIdentifier() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testAddGeogigLayerAddsLayerIdentifierWithExplicitBranch() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        DataStoreInfo store = catalog.getDataStoreByName(catalogBuilder.storeName());
        store.getConnectionParameters().put(GeoGigDataStoreFactory.BRANCH.key, "master");
        catalog.save(store);

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testAddGeogigLayerAddsLayerIdentifierWithExplicitHead() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        DataStoreInfo store = catalog.getDataStoreByName(catalogBuilder.storeName());

        store.getConnectionParameters().put(GeoGigDataStoreFactory.HEAD.key, "fakeBranch");
        catalog.save(store);

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testRenameStore() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String storeName = catalogBuilder.storeName();
        DataStoreInfo store = catalog.getStoreByName(storeName, DataStoreInfo.class);
        store.setName("new_store_name");
        catalog.save(store);

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    @Test
    public void testRenameWorkspace() {
        addAvailableGeogigLayers();

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String wsName = catalogBuilder.workspaceName();
        WorkspaceInfo ws = catalog.getWorkspaceByName(wsName);
        String newWsName = "new_ws_name";
        ws.setName(newWsName);
        catalog.save(ws);

        String layerName = newWsName + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(pointLayerInfo);

        layerName = newWsName + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertIdentifier(lineLayerInfo);
    }

    private void assertIdentifier(LayerInfo layer) {
        assertNotNull(layer);

        final ResourceInfo resource = layer.getResource();
        final DataStoreInfo store = (DataStoreInfo) resource.getStore();
        final Map<String, Serializable> params = store.getConnectionParameters();
        final String repoId = (String) params.get(REPOSITORY.key);

        List<LayerIdentifierInfo> identifiers = layer.getIdentifiers();
        LayerIdentifierInfo expected = null;
        for (LayerIdentifierInfo idinfo : identifiers) {
            if (GeogigLayerIntegrationListener.AUTHORITY_URL_NAME.equals(idinfo.getAuthority())) {
                expected = idinfo;
            }
        }

        assertNotNull("No geogig identifier added for layer " + layer, expected);

        String expectedId = repoId + ":" + resource.getNativeName();
        if (params.containsKey(GeoGigDataStoreFactory.BRANCH.key)) {
            String branch = (String) params.get(GeoGigDataStoreFactory.BRANCH.key);
            expectedId += ":" + branch;
        } else if (params.containsKey(GeoGigDataStoreFactory.HEAD.key)) {
            String head = (String) params.get(GeoGigDataStoreFactory.HEAD.key);
            expectedId += ":" + head;
        }

        assertEquals(expectedId, expected.getIdentifier());
    }

    private void addAvailableGeogigLayers() {
        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().build();
    }
}
