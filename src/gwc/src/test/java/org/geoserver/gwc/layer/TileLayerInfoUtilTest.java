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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.junit.Before;
import org.junit.Test;

/** Unit test suite for {@link TileLayerInfoUtil} */
public class TileLayerInfoUtilTest {

    private GWCConfig defaults;

    private GeoServerTileLayerInfo defaultVectorInfo;

    @Before
    public void setup() throws Exception {
        defaults = GWCConfig.getOldDefaults();
        defaultVectorInfo = TileLayerInfoUtil.create(defaults);
        defaultVectorInfo.getMimeFormats().clear();
        defaultVectorInfo.getMimeFormats().addAll(defaults.getDefaultVectorCacheFormats());
    }

    @Test
    public void testCreateLayerInfo() {
        LayerInfoImpl layer = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        defaultVectorInfo.setId(layer.getId());
        defaultVectorInfo.setName(tileLayerName(layer));
        assertNotNull(info);
        assertEquals(defaultVectorInfo, info);
    }

    @Test
    public void testCreateLayerGroupInfo() {
        LayerGroupInfoImpl group =
                mockGroup(
                        "testGroup", mockLayer("testLayer", new String[] {}, PublishedType.RASTER));

        defaults.getDefaultOtherCacheFormats().clear();
        defaults.getDefaultOtherCacheFormats().add("image/png8");
        defaults.getDefaultOtherCacheFormats().add("image/jpeg");

        GeoServerTileLayerInfo expected = TileLayerInfoUtil.create(defaults);
        expected.setId(group.getId());
        expected.setName(GWC.tileLayerName(group));

        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(group, defaults);
        assertNotNull(info);
        assertEquals(expected, info);
    }

    @Test
    public void testCreateLayerInfoAutoCacheStyles() {
        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setAutoCacheStyles(true);

        defaults.setCacheNonDefaultStyles(true);

        LayerInfoImpl layer =
                mockLayer("testLayer", new String[] {"style1", "style2"}, PublishedType.RASTER);

        GeoServerTileLayerInfo actual;
        actual = TileLayerInfoUtil.loadOrCreate(layer, defaults);

        TileLayerInfoUtil.checkAutomaticStyles(layer, info);

        TileLayerInfoUtil.setCachedStyles(info, "default", ImmutableSet.of("style1", "style2"));

        layer.setDefaultStyle(null);
        TileLayerInfoUtil.setCachedStyles(info, "", ImmutableSet.of("style1", "style2"));

        actual = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        assertEquals(ImmutableSet.of("style1", "style2"), actual.cachedStyles());
    }

    @Test
    public void testCreateLayerGroup() {
        LayerGroupInfoImpl lg =
                mockGroup(
                        "tesGroup",
                        mockLayer("L1", new String[] {}, PublishedType.RASTER),
                        mockLayer("L2", new String[] {}, PublishedType.RASTER));

        GeoServerTileLayerInfo info = defaultVectorInfo;
        info.setId(lg.getId());
        info.setName(GWC.tileLayerName(lg));
        info.getMimeFormats().clear();
        info.getMimeFormats().addAll(defaults.getDefaultOtherCacheFormats());

        GeoServerTileLayerInfo actual;
        actual = TileLayerInfoUtil.loadOrCreate(lg, defaults);

        assertEquals(info, actual);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateAcceptAllRegExParameterFilter() {
        GeoServerTileLayerInfo info = defaultVectorInfo;

        // If createParam is false and there isn't already a filter, don't create one
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(info, "ENV", false);
        assertNull(findParameterFilter("ENV", info.getParameterFilters()));

        // If createParam is true and there isn't already a filter, create one
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(info, "ENV", true);
        ParameterFilter filter = findParameterFilter("ENV", info.getParameterFilters());
        assertTrue(filter instanceof RegexParameterFilter);
        assertEquals(".*", ((RegexParameterFilter) filter).getRegex());

        // If createParam is true and there is already a filter, replace it with a new one
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(info, "ENV", true);
        ParameterFilter filter2 = findParameterFilter("ENV", info.getParameterFilters());
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);

        // If createParam is false and there is already a filter, replace it with a new one
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(info, "ENV", false);
        ParameterFilter filter3 = findParameterFilter("ENV", info.getParameterFilters());
        assertNotSame(filter2, filter3);
        assertEquals(filter, filter3);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateAcceptAllFloatParameterFilter() {
        GeoServerTileLayerInfo info = defaultVectorInfo;

        // If createParam is false and there isn't already a filter, don't create one
        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(info, "ELEVATION", false);
        assertNull(findParameterFilter("ELEVATION", info.getParameterFilters()));

        // If createParam is true and there isn't already a filter, create one
        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(info, "ELEVATION", true);
        ParameterFilter filter = findParameterFilter("ELEVATION", info.getParameterFilters());
        assertTrue(filter instanceof FloatParameterFilter);
        assertEquals(0, ((FloatParameterFilter) filter).getValues().size());

        // If createParam is true and there is already a filter, replace it with a new one
        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(info, "ELEVATION", true);
        ParameterFilter filter2 = findParameterFilter("ELEVATION", info.getParameterFilters());
        assertNotSame(filter, filter2);
        assertEquals(filter, filter2);

        // If createParam is false and there is already a filter, replace it with a new one
        TileLayerInfoUtil.updateAcceptAllFloatParameterFilter(info, "ELEVATION", false);
        ParameterFilter filter3 = findParameterFilter("ELEVATION", info.getParameterFilters());
        assertNotSame(filter2, filter3);
        assertEquals(filter, filter3);
    }

    /** Find a parameter filter by key from a set of filters. */
    private static ParameterFilter findParameterFilter(
            final String paramName, Set<ParameterFilter> parameterFilters) {

        if (parameterFilters == null || parameterFilters.isEmpty()) {
            return null;
        }

        for (ParameterFilter pf : parameterFilters) {
            if (paramName.equalsIgnoreCase(pf.getKey())) {
                return pf;
            }
        }
        return null;
    }
}
