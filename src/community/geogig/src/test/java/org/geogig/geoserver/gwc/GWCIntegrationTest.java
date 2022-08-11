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

package org.geogig.geoserver.gwc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import java.util.Map;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.conveyor.Conveyor.CacheResult;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeature;

/** Integration test for GeoServer cached layers using the GWC REST API */
@TestSetup(run = TestSetupFrequency.ONCE)
public class GWCIntegrationTest extends GeoServerSystemTestSupport {

    private static GWC mediator;

    private static GeoServerTileLayer pointsLayer;

    private static GeoServerTileLayer linesLayer;

    private static StorageBroker storageBroker;

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

        mediator = GWC.get();
        assertNotNull(mediator);
        storageBroker = GeoWebCacheExtensions.bean(StorageBroker.class);
        assertNotNull(storageBroker);

        GWCConfig config = mediator.getConfig();
        config.setCacheLayersByDefault(true);
        mediator.saveConfig(config);

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().build();

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertNotNull(pointLayerInfo);
        pointsLayer = mediator.getTileLayer(pointLayerInfo);
        assertNotNull(pointsLayer);
        pointsLayer.getInfo().setExpireCache(10 * 1000);
        mediator.save(pointsLayer);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertNotNull(lineLayerInfo);
        linesLayer = mediator.getTileLayer(lineLayerInfo);
        assertNotNull(lineLayerInfo);
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        getCatalog().dispose();
    }

    /** Override so that default layers are not added */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        //
    }

    @Test
    public void testRemoveSingleFeature() throws Exception {

        ConveyorTile tile = createTileProto(pointsLayer);

        SimpleFeature feature = geogigData.getFeature("points/p2"); // POINT(1 1)

        Envelope bounds = (Envelope) feature.getBounds();
        org.geowebcache.grid.BoundingBox featureBounds =
                new org.geowebcache.grid.BoundingBox(
                        bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());

        GridSubset gridSubset = pointsLayer.getGridSubset(tile.getGridSetId());

        long[][] featureCoverages = gridSubset.getCoverageIntersections(featureBounds);
        int level = 4;
        long[] levelCoverageIntersection = featureCoverages[level];

        final long tileX = levelCoverageIntersection[0];
        final long tileY = levelCoverageIntersection[1];

        long[] xyz = tile.getStorageObject().getXYZ();
        xyz[0] = tileX;
        xyz[1] = tileY;
        xyz[2] = level;

        ConveyorTile result = pointsLayer.getTile(tile);
        CacheResult cacheResult = result.getCacheResult();
        assertEquals(CacheResult.MISS, cacheResult);

        result = pointsLayer.getTile(tile);
        cacheResult = result.getCacheResult();
        assertEquals(CacheResult.HIT, cacheResult);

        geogigData
                .update("points/p1", "geom", "POINT(-1 -1)") // 9570
                .add() //
                .commit("moved POINT(1 1) to POINT(-1 -1)") //
                .update("lines/l1", "geom", "LINESTRING(0 10, 0 -10)") //
                .add() //
                .commit("moved LINESTRING(-10 0, 10 0) to LINESTRING(0 10, 0 -10)");

        // give the hook some time to run
        Thread.sleep(100);
        result = pointsLayer.getTile(tile);
        cacheResult = result.getCacheResult();
        assertEquals(CacheResult.MISS, cacheResult);
    }

    public ConveyorTile createTileProto(GeoServerTileLayer tileLayer) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        String layerName = tileLayer.getName();

        MimeType mimeType = tileLayer.getDefaultMimeType();
        String gridsetId = tileLayer.getGridSubsets().iterator().next();
        Map<String, String> filteringParameters = null;

        long[] tileIndex = new long[3];
        ConveyorTile tile =
                new ConveyorTile(
                        storageBroker,
                        layerName,
                        gridsetId,
                        tileIndex,
                        mimeType,
                        filteringParameters,
                        req,
                        resp);
        return tile;
    }
}
