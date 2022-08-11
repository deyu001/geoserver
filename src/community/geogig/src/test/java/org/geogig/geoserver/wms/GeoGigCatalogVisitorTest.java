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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.config.GeoServerGeoGigRepositoryResolver;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.repository.IndexInfo;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.storage.IndexDatabase;
import org.springframework.mock.web.MockHttpServletResponse;

/** */
@TestSetup(run = TestSetupFrequency.REPEAT)
public class GeoGigCatalogVisitorTest extends CatalogRESTTestSupport {

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        geogigData
                .init() //
                .config("user.name", "geogig") //
                .config("user.email", "geogig@test.com") //
                .createTypeTree(
                        "lines",
                        "geom:LineString:srid=4326,S_TIME:String,E_TIME:String,S_ELEV:String,E_ELEV:String") //
                .add() //
                .commit("created type trees") //
                .get();

        geogigData.insert(
                "lines", //
                "l1=geom:LINESTRING(-10 0, 10 0);S_TIME:startTime;E_TIME:endTime;S_ELEV:startElev;E_TIME:endElev", //
                "l2=geom:LINESTRING(0 0, 180 0);S_TIME:startTime;E_TIME:endTime;S_ELEV:startElev;E_TIME:endElev");

        geogigData.add().commit("Added test features");
        // need to instantiate the listerner so it can register with the test GeoServer instance
        new GeogigLayerIntegrationListener(getGeoServer());
        catalog = getCatalog();
    }

    @After
    public void after() {
        RepositoryManager.close();
    }

    private List<IndexInfo> waitForIndexes(
            final IndexDatabase indexDb, final String layerName, final int expectedSize)
            throws InterruptedException {
        assertNotNull("Expected a non null Layer Name", layerName);
        List<IndexInfo> indexInfos = indexDb.getIndexInfos(layerName);
        assertNotNull("Expected IndexInfo objects from Index database", indexInfos);
        int infoSize = indexInfos.size();
        final int maxWaitInSeconds = 10;
        int waitCount = 0;
        while (infoSize < expectedSize && waitCount++ < maxWaitInSeconds) {
            // wait a second for the index
            Thread.sleep(1_000);
            // get the infos again
            indexInfos = indexDb.getIndexInfos(layerName);
            infoSize = indexInfos.size();
        }
        // make sure the ocunt matches
        assertEquals(
                String.format("Expected exactly %s IndexInfo", expectedSize),
                expectedSize,
                indexInfos.size());
        return indexInfos;
    }

    @Test
    public void testGetAttribute_SapitalIndexOnly() throws Exception {
        addAvailableGeogigLayers();
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        GeoGigCatalogVisitor visitor = new GeoGigCatalogVisitor();
        visitor.visit(lineLayerInfo);
        GeoGIG geoGig = geogigData.getGeogig();
        IndexDatabase indexDatabase = geoGig.getRepository().indexDatabase();
        List<IndexInfo> indexInfos = waitForIndexes(indexDatabase, "lines", 1);
        IndexInfo indexInfo = indexInfos.get(0);
        Set<String> materializedAttributeNames = IndexInfo.getMaterializedAttributeNames(indexInfo);
        assertTrue("Expected empty extra Attributes set", materializedAttributeNames.isEmpty());
    }

    @Test
    public void testGetAttribute_SpatialIndexWithExtraAttributes() throws Exception {
        addAvailableGeogigLayers();
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        // set the layer up with some time/elevation metadata
        MetadataMap metadata = lineLayerInfo.getResource().getMetadata();
        DimensionInfo timeInfo = new DimensionInfoImpl();
        timeInfo.setAttribute("S_TIME");
        timeInfo.setEndAttribute("E_TIME");
        DimensionInfo elevationInfo = new DimensionInfoImpl();
        elevationInfo.setAttribute("S_ELEV");
        elevationInfo.setEndAttribute("E_ELEV");
        metadata.put("time", timeInfo);
        metadata.put("elevation", elevationInfo);
        GeoGigCatalogVisitor visitor = new GeoGigCatalogVisitor();
        visitor.visit(lineLayerInfo);
        GeoGIG geoGig = geogigData.getGeogig();
        IndexDatabase indexDatabase = geoGig.getRepository().indexDatabase();
        List<IndexInfo> indexInfos = waitForIndexes(indexDatabase, "lines", 1);
        IndexInfo indexInfo = indexInfos.get(0);
        Set<String> materializedAttributeNames = IndexInfo.getMaterializedAttributeNames(indexInfo);
        assertFalse(
                "Expected non-empty extra Attributes set", materializedAttributeNames.isEmpty());
        assertEquals("Expected 4 extra attributes", 4, materializedAttributeNames.size());
    }

    private void addAvailableGeogigLayers() throws IOException {
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().build();
        // set the DataStore to auto-index
        DataStoreInfo dataStore =
                catalog.getDataStoreByName(
                        GeoGigTestData.CatalogBuilder.WORKSPACE,
                        GeoGigTestData.CatalogBuilder.STORE);
        dataStore.getConnectionParameters().put(GeoGigDataStoreFactory.AUTO_INDEXING.key, true);
        catalog.save(dataStore);
    }

    @Test
    public void testGetAttribute_SapitalIndexOnlyUsingRest() throws Exception {
        addAvailableGeoGigLayersWithDataStoreAddedViaRest();
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        String layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        GeoGigCatalogVisitor visitor = new GeoGigCatalogVisitor();
        visitor.visit(lineLayerInfo);
        GeoGIG geoGig = geogigData.getGeogig();
        IndexDatabase indexDatabase = geoGig.getRepository().indexDatabase();
        List<IndexInfo> indexInfos = waitForIndexes(indexDatabase, "lines", 1);
        IndexInfo indexInfo = indexInfos.get(0);
        Set<String> materializedAttributeNames = IndexInfo.getMaterializedAttributeNames(indexInfo);
        assertTrue("Expected empty extra Attributes set", materializedAttributeNames.isEmpty());
    }

    private void addAvailableGeoGigLayersWithDataStoreAddedViaRest() throws Exception {
        GeoGigTestData.CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().buildWithoutDataStores();
        // create dtatastore via REST, with autoIndexing
        String message =
                "<dataStore>\n" //
                        + " <name>"
                        + GeoGigTestData.CatalogBuilder.STORE
                        + "</name>\n" //
                        + " <type>GeoGIG</type>\n" //
                        + " <connectionParameters>\n" //
                        + "   <entry key=\"geogig_repository\">${repository}</entry>\n" //
                        + "   <entry key=\"autoIndexing\">true</entry>\n"
                        + " </connectionParameters>\n" //
                        + "</dataStore>\n";
        GeoGIG geogig = geogigData.getGeogig();
        // make sure the Repository is in the Repo Manager
        RepositoryInfo info = new RepositoryInfo();
        info.setLocation(geogig.getRepository().getLocation());
        RepositoryManager.get().save(info);
        final String repoName = info.getRepoName();
        message =
                message.replace(
                        "${repository}", GeoServerGeoGigRepositoryResolver.getURI(repoName));
        final String uri =
                "/rest/workspaces/" + GeoGigTestData.CatalogBuilder.WORKSPACE + "/datastores";
        MockHttpServletResponse response = postAsServletResponse(uri, message, "text/xml");
        assertEquals(
                "POST new DataStore config failed: " + response.getContentAsString(),
                201,
                response.getStatus());
        // now add the layers
        DataStoreInfo ds = catalog.getDataStoreByName(GeoGigTestData.CatalogBuilder.STORE);
        catalogBuilder.setUpLayers(ds);
    }
}
