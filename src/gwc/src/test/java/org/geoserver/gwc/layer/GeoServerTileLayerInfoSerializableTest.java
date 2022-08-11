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

import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.geoserver.gwc.layer.TileLayerInfoUtil.loadOrCreate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.beans.Introspector;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GeoServerTileLayerInfoSerializableTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private GeoServerTileLayerInfo info;

    private GWCConfig defaults;

    private GeoServerTileLayerInfo defaultVectorInfo;

    @Before
    public void setUp() throws Exception {
        info = new GeoServerTileLayerInfoImpl();
        defaults = GWCConfig.getOldDefaults();
        defaultVectorInfo = TileLayerInfoUtil.create(defaults);
        defaultVectorInfo.getMimeFormats().clear();
        defaultVectorInfo.getMimeFormats().addAll(defaults.getDefaultVectorCacheFormats());
    }

    <T> Matcher<T> sameProperty(T expected, String property) throws Exception {
        return sameProperty(expected, property, Matchers::is);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    <T> Matcher<T> sameProperty(T expected, String property, Function<?, Matcher<?>> valueMatcher)
            throws Exception {
        Object value =
                Arrays.stream(
                                Introspector.getBeanInfo(expected.getClass())
                                        .getPropertyDescriptors())
                        .filter(p -> p.getName().equals(property))
                        .findAny()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "bean expected lacks the property " + property))
                        .getReadMethod()
                        .invoke(expected);
        return hasProperty(property, (Matcher<?>) ((Function) valueMatcher).apply(value));
    }

    private GeoServerTileLayerInfo testMarshaling(GeoServerTileLayerInfo info) throws Exception {

        File f = temp.newFile();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            out.writeObject(info);
        }
        GeoServerTileLayerInfo unmarshalled;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f)); ) {
            unmarshalled = (GeoServerTileLayerInfo) in.readObject();
        }

        assertThat(unmarshalled, notNullValue());

        assertThat(unmarshalled, sameProperty(info, "enabled"));
        assertThat(unmarshalled, sameProperty(info, "autoCacheStyles"));

        assertThat(unmarshalled, sameProperty(info, "gutter"));
        assertThat(unmarshalled, sameProperty(info, "metaTilingX"));
        assertThat(unmarshalled, sameProperty(info, "metaTilingY"));
        assertThat(unmarshalled, sameProperty(info, "gridSubsets"));
        assertThat(unmarshalled, sameProperty(info, "mimeFormats"));
        assertThat(unmarshalled, sameProperty(info, "parameterFilters"));
        assertThat(unmarshalled, equalTo(info));

        assertThat("cachedStyles", unmarshalled.cachedStyles(), equalTo(info.cachedStyles()));

        return unmarshalled;
    }

    @Test
    public void testMarshallingDefaults() throws Exception {
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        LayerInfo layerInfo = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        info = loadOrCreate(layerInfo, oldDefaults);
        testMarshaling(info);
    }

    @Test
    public void testMarshallingBlobStoreId() throws Exception {
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        LayerInfo layerInfo = mockLayer("testLayer", new String[] {}, PublishedType.RASTER);
        info = loadOrCreate(layerInfo, oldDefaults);
        info.setBlobStoreId("myBlobStore");
        GeoServerTileLayerInfo unmarshalled = testMarshaling(info);
        assertThat(unmarshalled, hasProperty("blobStoreId", is("myBlobStore")));
    }

    @Test
    public void testMarshallingGridSubsets() throws Exception {
        List<XMLGridSubset> subsets = new ArrayList<>();
        XMLGridSubset subset;
        subset = new XMLGridSubset();
        subset.setGridSetName("EPSG:4326");
        subset.setZoomStart(1);
        subset.setZoomStop(10);
        subset.setExtent(new BoundingBox(0, 0, 180, 90));
        subsets.add(subset);

        subset = new XMLGridSubset();
        subset.setGridSetName("EPSG:900913");
        subsets.add(subset);

        subset = new XMLGridSubset();
        subset.setGridSetName("GlobalCRS84Scale");
        subset.setZoomStart(4);
        subset.setExtent(new BoundingBox(-100, -40, 100, 40));
        subsets.add(subset);

        info.getGridSubsets().add(subsets.get(0));
        testMarshaling(info);

        info.getGridSubsets().clear();
        info.getGridSubsets().add(subsets.get(1));
        testMarshaling(info);

        info.getGridSubsets().clear();
        info.getGridSubsets().add(subsets.get(2));
        testMarshaling(info);

        info.getGridSubsets().addAll(subsets);
        testMarshaling(info);
    }

    @Test
    public void testMarshallingParameterFilters() throws Exception {
        StringParameterFilter strParam = new StringParameterFilter();
        strParam.setKey("TIME");
        strParam.setDefaultValue("now");
        List<String> strValues = new ArrayList<>(strParam.getValues());
        strValues.addAll(Arrays.asList("today", "yesterday", "tomorrow"));
        strParam.setValues(strValues);

        RegexParameterFilter regExParam = new RegexParameterFilter();
        regExParam.setKey("CQL_FILTER");
        regExParam.setDefaultValue("INCLUDE");
        regExParam.setRegex(".*");

        FloatParameterFilter floatParam = new FloatParameterFilter();
        floatParam.setKey("ENV");
        floatParam.setThreshold(Float.valueOf(1E-4F));
        List<Float> floatValues = new ArrayList<>(floatParam.getValues());
        floatValues.addAll(Arrays.asList(1f, 1.5f, 2f, 2.5f));
        floatParam.setValues(floatValues);

        info.getParameterFilters().clear();
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(strParam);
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(regExParam);
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(floatParam);
        testMarshaling(info);

        info.getParameterFilters().clear();
        info.getParameterFilters().add(strParam);
        info.getParameterFilters().add(regExParam);
        info.getParameterFilters().add(floatParam);
        testMarshaling(info);

        StringParameterFilter strParam2 = new StringParameterFilter();
        strParam2.setKey("ELEVATION");
        strParam2.setDefaultValue("1");
        List<String> strValues2 = new ArrayList<>(strParam2.getValues());
        strValues2.addAll(Arrays.asList("1", "2", "3"));
        strParam2.setValues(strValues2);
        info.getParameterFilters().add(strParam2);
        testMarshaling(info);
    }
}
