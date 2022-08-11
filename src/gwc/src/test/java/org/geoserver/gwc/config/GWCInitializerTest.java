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

import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.LegacyTileLayerInfoLoader;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.gwc.wmts.WMTSInfoImpl;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class GWCInitializerTest {

    private GWCInitializer initializer;

    private GWCConfigPersister configPersister;

    private GeoServer geoServer;

    private Catalog rawCatalog;

    private TileLayerCatalog tileLayerCatalog;

    private GeoServerFacade geoServerFacade;

    private WMTSInfo wmtsInfo = new WMTSInfoImpl();

    @Before
    public void setUp() throws Exception {

        configPersister = mock(GWCConfigPersister.class);
        GWCConfig config = GWCConfig.getOldDefaults();
        config.setWMTSEnabled(false);
        when(configPersister.getConfig()).thenReturn(config);

        rawCatalog = mock(Catalog.class);
        tileLayerCatalog = mock(TileLayerCatalog.class);
        initializer = new GWCInitializer(configPersister, rawCatalog, tileLayerCatalog);

        wmtsInfo.setEnabled(true);
        geoServerFacade = mock(GeoServerFacade.class);
        when(geoServerFacade.getService(WMTSInfo.class)).thenReturn(wmtsInfo);

        geoServer = mock(GeoServer.class);
        when(geoServer.getFacade()).thenReturn(geoServerFacade);
    }

    @Test
    public void testInitializeLayersToOldDefaults() throws Exception {
        // no gwc-gs.xml exists
        when(configPersister.findConfigFile()).thenReturn(null);
        // ignore the upgrade of the direct wms integration flag on this test
        when(geoServer.getService(eq(WMSInfo.class))).thenReturn(null);

        // let the catalog have something to initialize
        LayerInfo layer = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        LayerGroupInfo group = mockGroup("testGroup", layer);
        when(rawCatalog.getLayers()).thenReturn(Lists.newArrayList(layer));
        when(rawCatalog.getLayerGroups()).thenReturn(Lists.newArrayList(group));

        // run layer initialization
        initializer.initialize(geoServer);

        // make sure default tile layers were created
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        GeoServerTileLayerInfo tileLayer = TileLayerInfoUtil.loadOrCreate(layer, oldDefaults);
        GeoServerTileLayerInfo tileLayerGroup = TileLayerInfoUtil.loadOrCreate(group, oldDefaults);

        verify(tileLayerCatalog, times(1)).save(eq(tileLayer));
        verify(tileLayerCatalog, times(1)).save(eq(tileLayerGroup));
    }

    @Test
    public void testUpgradeDirectWMSIntegrationFlag() throws Exception {
        // no gwc-gs.xml exists, so that initialization runs
        when(configPersister.findConfigFile()).thenReturn(null);

        // no catalog layers for this test
        List<LayerInfo> layers = ImmutableList.of();
        List<LayerGroupInfo> groups = ImmutableList.of();
        when(rawCatalog.getLayers()).thenReturn(layers);
        when(rawCatalog.getLayerGroups()).thenReturn(groups);

        WMSInfoImpl wmsInfo = new WMSInfoImpl();
        // initialize wmsInfo with a value for the old direct wms integration flag
        wmsInfo.getMetadata().put(GWCInitializer.WMS_INTEGRATION_ENABLED_KEY, Boolean.TRUE);

        // make sure WMSInfo exists
        when(geoServer.getService(eq(WMSInfo.class))).thenReturn(wmsInfo);

        ArgumentCaptor<GWCConfig> captor = ArgumentCaptor.forClass(GWCConfig.class);
        // run layer initialization
        initializer.initialize(geoServer);

        verify(configPersister, times(3)).save(captor.capture());
        assertTrue(captor.getAllValues().get(0).isDirectWMSIntegrationEnabled());

        assertFalse(wmsInfo.getMetadata().containsKey(GWCInitializer.WMS_INTEGRATION_ENABLED_KEY));
        verify(geoServer).save(same(wmsInfo));
    }

    @Test
    public void testUpgradeFromTileLayerInfosToTileLayerCatalog() throws Exception {
        // do have gwc-gs.xml, so it doesn't go through the createDefaultTileLayerInfos path
        Resource fakeConfig = Files.asResource(new File("target", "gwc-gs.xml"));
        when(configPersister.findConfigFile()).thenReturn(fakeConfig);

        GWCConfig defaults = GWCConfig.getOldDefaults();
        defaults.setCacheLayersByDefault(true);
        when(configPersister.getConfig()).thenReturn(defaults);

        // let the catalog have something to initialize
        LayerInfo layer = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        LayerGroupInfo group = mockGroup("testGroup", layer);
        when(rawCatalog.getLayers()).thenReturn(Lists.newArrayList(layer));
        when(rawCatalog.getLayerGroups()).thenReturn(Lists.newArrayList(group));

        GeoServerTileLayerInfoImpl layerInfo = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        GeoServerTileLayerInfoImpl groupInfo = TileLayerInfoUtil.loadOrCreate(group, defaults);

        LegacyTileLayerInfoLoader.save(layerInfo, layer.getMetadata());
        LegacyTileLayerInfoLoader.save(groupInfo, group.getMetadata());

        // run layer initialization
        initializer.initialize(geoServer);

        verify(tileLayerCatalog, times(1)).save(eq(layerInfo));
        assertFalse(LegacyTileLayerInfoLoader.hasTileLayerDef(layer.getMetadata()));
        verify(rawCatalog, times(1)).save(eq(layer));

        verify(tileLayerCatalog, times(1)).save(eq(groupInfo));
        assertFalse(LegacyTileLayerInfoLoader.hasTileLayerDef(group.getMetadata()));
        verify(rawCatalog, times(1)).save(eq(group));
    }

    @Test
    public void testUpgradeWithWmtsEnablingInfo() throws Exception {
        // force configuration initialisation
        when(configPersister.findConfigFile()).thenReturn(null);
        assertTrue(wmtsInfo.isEnabled());
        // run layer initialization
        initializer.initialize(geoServer);
        // checking that the configuration was saved
        verify(geoServer).save(same(wmtsInfo));
        verify(configPersister, times(2)).save(configPersister.getConfig());
        // checking that the service info have been updated with gwc configuration value
        assertFalse(wmtsInfo.isEnabled());
    }
}
