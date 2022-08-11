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

package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;
import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableSet;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.junit.Before;
import org.junit.Test;

public class LegacyTileLayerInfoLoaderTest {

    private GWCConfig defaults;

    private GeoServerTileLayerInfo defaultVectorInfo;

    @Before
    public void setup() {
        defaults = GWCConfig.getOldDefaults();
        defaultVectorInfo = TileLayerInfoUtil.create(defaults);
        defaultVectorInfo.getMimeFormats().clear();
        defaultVectorInfo.getMimeFormats().addAll(defaults.getDefaultVectorCacheFormats());
    }

    @Test
    public void testLoadLayerInfo() {
        LayerInfoImpl layer = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);

        assertNull(LegacyTileLayerInfoLoader.load(layer));

        TileLayerInfoUtil.checkAutomaticStyles(layer, defaultVectorInfo);

        LegacyTileLayerInfoLoader.save(defaultVectorInfo, layer.getMetadata());

        GeoServerTileLayerInfo info2 = LegacyTileLayerInfoLoader.load(layer);

        defaultVectorInfo.setId(layer.getId());
        defaultVectorInfo.setName(tileLayerName(layer));
        assertEquals(defaultVectorInfo, info2);
    }

    @Test
    public void testLoadLayerInfoExtraStyles() {
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setAutoCacheStyles(false);
        TileLayerInfoUtil.setCachedStyles(info, "default", ImmutableSet.of("style1"));

        LayerInfoImpl layer =
                mockLayer("testLayer", new String[] {"style1", "style2"}, PublishedType.RASTER);
        TileLayerInfoUtil.checkAutomaticStyles(layer, info);

        assertNull(LegacyTileLayerInfoLoader.load(layer));

        LegacyTileLayerInfoLoader.save(info, layer.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(layer);

        info.setId(layer.getId());
        info.setName(tileLayerName(layer));
        assertEquals(info, actual);

        layer.setDefaultStyle(null);
        TileLayerInfoUtil.setCachedStyles(info, null, ImmutableSet.of("style1"));
        LegacyTileLayerInfoLoader.save(info, layer.getMetadata());
        actual = LegacyTileLayerInfoLoader.load(layer);
        assertEquals(ImmutableSet.of("style1"), actual.cachedStyles());
    }

    @Test
    public void testLoadLayerInfoAutoCacheStyles() {
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setAutoCacheStyles(true);

        LayerInfoImpl layer =
                mockLayer("testLayer", new String[] {"style1", "style2"}, PublishedType.RASTER);
        assertNull(LegacyTileLayerInfoLoader.load(layer));

        TileLayerInfoUtil.checkAutomaticStyles(layer, defaultVectorInfo);

        LegacyTileLayerInfoLoader.save(info, layer.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(layer);

        TileLayerInfoUtil.setCachedStyles(info, "default", ImmutableSet.of("style1", "style2"));

        info.setId(layer.getId());
        info.setName(tileLayerName(layer));
        assertEquals(info, actual);

        layer.setDefaultStyle(null);
        TileLayerInfoUtil.setCachedStyles(info, null, ImmutableSet.of("style1", "style2"));

        actual = LegacyTileLayerInfoLoader.load(layer);
        assertEquals(ImmutableSet.of("style1", "style2"), actual.cachedStyles());
    }

    @Test
    public void testLoadLayerGroup() {
        LayerGroupInfoImpl lg =
                mockGroup(
                        "tesGroup",
                        mockLayer("L1", new String[] {}, PublishedType.RASTER),
                        mockLayer("L2", new String[] {}, PublishedType.RASTER));

        assertNull(LegacyTileLayerInfoLoader.load(lg));
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.getMimeFormats().clear();
        info.getMimeFormats().addAll(defaults.getDefaultOtherCacheFormats());

        LegacyTileLayerInfoLoader.save(info, lg.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(lg);

        info.setId(lg.getId());
        info.setName(GWC.tileLayerName(lg));
        assertEquals(info, actual);
    }

    @Test
    public void testClear() {
        LayerGroupInfoImpl lg =
                mockGroup(
                        "tesGroup",
                        mockLayer("L1", new String[] {}, PublishedType.RASTER),
                        mockLayer("L2", new String[] {}, PublishedType.RASTER));

        assertNull(LegacyTileLayerInfoLoader.load(lg));
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.getMimeFormats().clear();
        info.getMimeFormats().addAll(defaults.getDefaultOtherCacheFormats());

        LegacyTileLayerInfoLoader.save(info, lg.getMetadata());

        GeoServerTileLayerInfo actual;
        actual = LegacyTileLayerInfoLoader.load(lg);
        assertNotNull(actual);

        LegacyTileLayerInfoLoader.clear(lg.getMetadata());
        assertNull(LegacyTileLayerInfoLoader.load(lg));
    }
}
