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

package org.geoserver.gwc.wmts;

import static org.geoserver.gwc.wmts.MultiDimensionalExtension.ALL_DOMAINS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.testreader.CustomFormat;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.gwc.wmts.dimensions.RasterCustomDimension;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * This class contains tests that check that custom dimensions values are correctly extracted from
 * rasters. Custom dimensions are only supported in rasters.
 */
public class RasterCustomDimensionTest extends TestsSupport {

    @Test
    public void testDisabledDimension() throws Exception {
        // enable a custom dimension
        DimensionInfo dimensionInfo = new DimensionInfoImpl();
        dimensionInfo.setEnabled(true);
        CoverageInfo rasterInfo = getCoverageInfo();
        rasterInfo
                .getMetadata()
                .put(
                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + CustomFormat.CUSTOM_DIMENSION_NAME,
                        dimensionInfo);
        getCatalog().save(rasterInfo);
        // check that we correctly retrieve the custom dimension
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(1));
        // disable the custom dimension
        dimensionInfo.setEnabled(false);
        rasterInfo
                .getMetadata()
                .put(
                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + CustomFormat.CUSTOM_DIMENSION_NAME,
                        dimensionInfo);
        getCatalog().save(rasterInfo);
        // no dimensions should be available
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(0));
    }

    @Test
    public void testGetDefaultValue() {
        testDefaultValueStrategy(Strategy.MINIMUM, "CustomDimValueA");
        testDefaultValueStrategy(Strategy.MAXIMUM, "CustomDimValueC");
    }

    @Test
    public void testGetDomainsValues() throws Exception {
        testDomainsValuesRepresentation(0, "CustomDimValueA--CustomDimValueC");
        testDomainsValuesRepresentation(
                DimensionsUtils.NO_LIMIT, "CustomDimValueA", "CustomDimValueB", "CustomDimValueC");
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        CoverageInfo rasterInfo = getCoverageInfo();
        Dimension dimension =
                new RasterCustomDimension(
                        wms, getLayerInfo(), CustomFormat.CUSTOM_DIMENSION_NAME, dimensionInfo);
        rasterInfo
                .getMetadata()
                .put(
                        ResourceInfo.CUSTOM_DIMENSION_PREFIX + CustomFormat.CUSTOM_DIMENSION_NAME,
                        dimensionInfo);
        getCatalog().save(rasterInfo);
        return dimension;
    }

    @Test
    public void testGetHistogram() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, null);
        assertThat(histogram.first, is("CustomDimValueA,CustomDimValueB,CustomDimValueC"));
        assertThat(histogram.second, containsInAnyOrder(1, 1, 1));
    }

    /** Helper method that just returns the current layer info. */
    private LayerInfo getLayerInfo() {
        return catalog.getLayerByName(RASTER_CUSTOM.getLocalPart());
    }

    /** Helper method that just returns the current coverage info. */
    private CoverageInfo getCoverageInfo() {
        LayerInfo layerInfo = getLayerInfo();
        assertThat(layerInfo.getResource(), instanceOf(CoverageInfo.class));
        return (CoverageInfo) layerInfo.getResource();
    }
}
