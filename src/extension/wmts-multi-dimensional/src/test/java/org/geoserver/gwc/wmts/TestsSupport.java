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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.dimension.RasterTimeDimensionDefaultValueTest;
import org.geoserver.wms.dimension.VectorElevationDimensionDefaultValueTest;
import org.geotools.data.Query;
import org.junit.Before;

public abstract class TestsSupport extends WMSTestSupport {

    protected static final QName RASTER_ELEVATION_TIME =
            new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static final QName RASTER_ELEVATION =
            new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static final QName RASTER_TIME =
            new QName(MockData.SF_URI, "watertemp_future_generated", MockData.SF_PREFIX);
    protected static final QName RASTER_CUSTOM =
            new QName(MockData.SF_URI, "watertemp_custom", MockData.SF_PREFIX);

    protected static final QName VECTOR_ELEVATION_TIME =
            new QName(MockData.SF_URI, "ElevationWithStartEnd", MockData.SF_PREFIX);
    protected static final QName VECTOR_ELEVATION =
            new QName(MockData.SF_URI, "ElevationWithStartEnd", MockData.SF_PREFIX);
    protected static final QName VECTOR_TIME =
            new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);
    protected static final QName VECTOR_CUSTOM =
            new QName(MockData.SF_URI, "TimeElevationCustom", MockData.SF_PREFIX);

    protected WMS wms;
    protected Catalog catalog;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do no setup common layers
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // raster with elevation dimension
        testData.addRasterLayer(
                RASTER_ELEVATION,
                "/org/geoserver/wms/dimension/watertemp.zip",
                null,
                Collections.emptyMap(),
                getClass(),
                getCatalog());
        // raster with time dimension
        RasterTimeDimensionDefaultValueTest.prepareFutureCoverageData(
                RASTER_TIME, this.getDataDirectory(), this.getCatalog());
        // raster with custom dimension
        testData.addRasterLayer(
                RASTER_CUSTOM,
                "/org/geoserver/wms/dimension/custwatertemp.zip",
                null,
                Collections.emptyMap(),
                getClass(),
                getCatalog());
        // vector with elevation dimension
        testData.addVectorLayer(
                VECTOR_ELEVATION,
                Collections.emptyMap(),
                "/TimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        // vector with time dimension
        testData.addVectorLayer(
                VECTOR_TIME,
                Collections.emptyMap(),
                "/TimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        // vector with custom dimension
        testData.addVectorLayer(
                VECTOR_CUSTOM,
                Collections.emptyMap(),
                "TimeElevationCustom.properties",
                VectorElevationDimensionDefaultValueTest.class,
                getCatalog());
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
        // invoke after setup callback
        afterSetup(testData);
    }

    protected void afterSetup(SystemTestData testData) {}

    @Before
    public void setup() throws Exception {
        wms = getWMS();
        catalog = getCatalog();
    }

    protected abstract Dimension buildDimension(DimensionInfo dimensionInfo);

    protected void testDomainsValuesRepresentation(
            int expandLimit, String... expectedDomainValues) {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        List<String> valuesAsStrings =
                dimension.getDomainValuesAsStrings(Query.ALL, expandLimit).second;
        assertThat(valuesAsStrings.size(), is(expectedDomainValues.length));
        assertThat(valuesAsStrings, containsInAnyOrder(expectedDomainValues));
    }

    protected void testDefaultValueStrategy(
            DimensionDefaultValueSetting.Strategy strategy, String expectedDefaultValue) {
        DimensionDefaultValueSetting defaultValueStrategy = new DimensionDefaultValueSetting();
        defaultValueStrategy.setStrategyType(strategy);
        testDefaultValueStrategy(defaultValueStrategy, expectedDefaultValue);
    }

    protected void testDefaultValueStrategy(
            DimensionDefaultValueSetting defaultValueStrategy, String expectedDefaultValue) {
        DimensionInfo dimensionInfo = createDimension(true, defaultValueStrategy);
        Dimension dimension = buildDimension(dimensionInfo);
        String defaultValue = dimension.getDefaultValueAsString();
        assertThat(defaultValue, is(expectedDefaultValue));
    }

    protected static DimensionInfo createDimension(
            boolean enable, DimensionDefaultValueSetting defaultValueStrategy) {
        DimensionInfo dimension = new DimensionInfoImpl();
        dimension.setEnabled(enable);
        dimension.setPresentation(DimensionPresentation.LIST);
        dimension.setDefaultValue(defaultValueStrategy);
        return dimension;
    }
}
