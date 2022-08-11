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

package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.util.NearestMatchFinder;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DimensionsRasterGetFeatureInfoTest extends WMSDimensionsTestSupport {

    static final String BASE_URL_NO_COUNT =
            "wms?service=WMS&version=1.1.0&request=GetFeatureInfo"
                    + "&layers=watertemp&styles=&bbox=0.237,40.562,14.593,44.558&width=200&height=80"
                    + "&srs=EPSG:4326&format=image/png"
                    + "&query_layers=watertemp";

    static final String BASE_URL = BASE_URL_NO_COUNT + "&feature_count=50";
    static final String BASE_URL_ONE = BASE_URL_NO_COUNT + "&feature_count=1";

    static final double EPS = 1e-03;

    private XpathEngine xpath;

    @Before
    public void setXpathEngine() throws Exception {
        xpath = XMLUnit.newXpathEngine();
    };

    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     *
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     * @param layerName TODO
     */
    Double getFeatureAt(String baseFeatureInfo, int x, int y, String layerName) throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        baseFeatureInfo
                                + "&info_format=application/vnd.ogc.gml&x="
                                + x
                                + "&y="
                                + y);
        assertEquals("application/vnd.ogc.gml", response.getContentType());
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        String sCount = xpath.evaluate("count(//" + layerName + ")", doc);
        int count = Integer.valueOf(sCount);

        if (count == 0) {
            return null;
        } else if (count == 1) {
            return Double.valueOf(xpath.evaluate("//" + layerName + "/sf:GRAY_INDEX", doc));
        } else {
            fail("Found more than one feature: " + count);
            return null; // just to make the compiler happy, fail throws an unchecked exception
        }
    }

    @Test
    public void testDefaultValues() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        // this one should be medium
        assertEquals(14.51, getFeatureAt(BASE_URL, 36, 31, "sf:watertemp"), EPS);
        // this one hot
        assertEquals(19.15, getFeatureAt(BASE_URL, 68, 72, "sf:watertemp"), EPS);
    }

    @Test
    public void testSortTime() throws Exception {
        // do not setup time, only elevation, and sort by time
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);

        // this one should be medium
        assertEquals(
                14.51,
                getFeatureAt(BASE_URL_ONE + "&sortBy=ingestion D", 36, 31, "sf:watertemp"),
                EPS);
        // this one hot
        assertEquals(
                19.15,
                getFeatureAt(BASE_URL_ONE + "&sortBy=ingestion D", 68, 72, "sf:watertemp"),
                EPS);
    }

    @Test
    public void testSortTimeElevation() throws Exception {
        // do not setup anything, only sort

        // this one should be medium
        assertEquals(
                14.51,
                getFeatureAt(
                        BASE_URL_ONE + "&sortBy=ingestion D,elevation", 36, 31, "sf:watertemp"),
                EPS);
        // this one hot
        assertEquals(
                19.15,
                getFeatureAt(
                        BASE_URL_ONE + "&sortBy=ingestion D,elevation", 68, 72, "sf:watertemp"),
                EPS);
    }

    @Test
    public void testElevation() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        // this one should be the no-data
        String url = BASE_URL + "&elevation=100";
        assertEquals(-30000, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        // and this one should be medium
        assertEquals(14.492, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }

    @Test
    public void testTime() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z";

        // should be similar to the default, but with different shades of color
        assertEquals(14.592, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        assertEquals(19.371, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }

    @Test
    public void testTimeNoNearestClose() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        String url = BASE_URL + "&time=2008-10-31T08:00:00.000Z";

        // no match without nearest match support
        assertNull(getFeatureAt(url, 36, 31, "sf:watertemp"));
        assertNull(getFeatureAt(url, 68, 72, "sf:watertemp"));
    }

    @Test
    public void testTimeNearestClose() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(WATTEMP, ResourceInfo.TIME, true);

        String url = BASE_URL + "&time=2008-10-31T08:00:00.000Z";

        // same as testTime
        assertEquals(14.592, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");
        assertEquals(19.371, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");
    }

    @Test
    public void testTimeNearestBefore() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(WATTEMP, ResourceInfo.TIME, true);

        String url = BASE_URL + "&time=1990-10-31";

        // same as testTime
        assertEquals(14.592, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");
        assertEquals(19.371, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");
    }

    @Test
    public void testTimeNearestAfter() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(WATTEMP, ResourceInfo.TIME, true);

        String url = BASE_URL + "&time=2018-10-31";

        // same as testDefaultValues
        assertEquals(14.51, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-11-01T00:00:00.000Z");
        assertEquals(19.15, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-11-01T00:00:00.000Z");
    }

    @Test
    public void testTimeNearestCloseNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeNearestClose();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeNearestBeforeNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeNearestBefore();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeNearestAfterNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeNearestAfter();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeElevation() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z&elevation=100";
        // this one should be the no-data
        assertEquals(-30000, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        // and this one should be medium
        assertEquals(14.134, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }

    @Test
    public void testTimeRange() throws Exception {
        setupRasterDimension(
                TIMERANGES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIMERANGES, "wavelength", DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", DimensionPresentation.LIST, null, null, null);

        String layer = getLayerId(TIMERANGES);
        String baseUrl =
                "wms?LAYERS="
                        + layer
                        + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&SRS=EPSG:4326"
                        + "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&query_layers="
                        + layer;

        // last range
        String url = baseUrl + "&TIME=2008-11-05T00:00:00.000Z/2008-11-06T12:00:00.000Z";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertEquals(14.782, getFeatureAt(url, 68, 72, layer), EPS);

        // in the middle hole, no data
        url = baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z";
        assertNull(getFeatureAt(url, 36, 31, layer));

        // first range
        url = baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-10-31T16:00:00.000Z";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertEquals(20.027, getFeatureAt(url, 68, 72, layer), EPS);
    }

    @Test
    public void testTimeRangeNearestMatch() throws Exception {
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIMERANGES, "wavelength", DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", DimensionPresentation.LIST, null, null, null);
        setupNearestMatch(TIMERANGES, ResourceInfo.TIME, true);

        String layer = getLayerId(TIMERANGES);
        String baseUrl =
                "wms?LAYERS="
                        + layer
                        + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&SRS=EPSG:4326"
                        + "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&query_layers="
                        + layer;

        // after last range, as a range
        String url = baseUrl + "&TIME=2018-11-05/2018-11-06";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-11-07T00:00:00.000Z");
        assertEquals(14.782, getFeatureAt(url, 68, 72, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-11-07T00:00:00.000Z");

        // after last range, point in time
        url = baseUrl + "&TIME=2018-11-05";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-11-07T00:00:00.000Z");
        assertEquals(14.782, getFeatureAt(url, 68, 72, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-11-07T00:00:00.000Z");

        // in the middle hole, close to latest value
        url = baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-11-05T00:00:00.000Z");
        assertEquals(14.782, getFeatureAt(url, 68, 72, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-11-05T00:00:00.000Z");

        // before first range, as a range
        url = baseUrl + "&TIME=2005-10-30/2005-10-31";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-10-31T00:00:00.000Z");
        assertEquals(20.027, getFeatureAt(url, 68, 72, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-10-31T00:00:00.000Z");

        // before first range, as a point
        url = baseUrl + "&TIME=2005-10-30";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-10-31T00:00:00.000Z");
        assertEquals(20.027, getFeatureAt(url, 68, 72, layer), EPS);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-10-31T00:00:00.000Z");
    }

    @Test
    public void testTimeRangeNearestMatchNonStructured() throws Exception {
        NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = false;
        try {
            testTimeRangeNearestMatch();
        } finally {
            NearestMatchFinder.ENABLE_STRUCTURED_READER_SUPPORT = true;
        }
    }

    @Test
    public void testTimeDefaultAsRange() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2008-10-30T23:00:00.000Z/2008-10-31T01:00:00.000Z");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.TIME, defaultValueSetting);

        // use the default time range, specify elevation
        String url = BASE_URL + "&elevation=100";
        // this one should be the no-data
        assertEquals(-30000, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        // and this one should be medium
        assertEquals(14.134, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }

    @Test
    public void testElevationDefaultAsRange() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        // setup a default
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("99/101");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.ELEVATION, defaultValueSetting);

        // default elevation, specific time
        String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z";
        // this one should be the no-data
        assertEquals(-30000, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        // and this one should be medium
        assertEquals(14.134, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }

    @Test
    public void testTimeElevationDefaultAsRange() throws Exception {
        // setup a range default for time
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("2008-10-30T23:00:00.000Z/2008-10-31T01:00:00.000Z");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.TIME, defaultValueSetting);
        // setup a range default for elevation
        defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("99/101");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.ELEVATION, defaultValueSetting);

        // default elevation, default time
        String url = BASE_URL;
        // this one should be the no-data
        assertEquals(-30000, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        // and this one should be medium
        assertEquals(14.134, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }

    @Test
    public void testNearestMatchTwoLayers() throws Exception {
        // setup time ranges
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupRasterDimension(
                TIMERANGES,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                TIMERANGES, "wavelength", DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", DimensionPresentation.LIST, null, null, null);
        setupNearestMatch(TIMERANGES, ResourceInfo.TIME, true);

        // setup water temp
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.LIST,
                null,
                ResourceInfo.TIME_UNIT,
                null);
        setupNearestMatch(WATTEMP, ResourceInfo.TIME, true);

        String layers = getLayerId(TIMERANGES) + "," + getLayerId(WATTEMP);
        String baseUrl =
                "wms?LAYERS="
                        + layers
                        + "&STYLES=,&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo"
                        + "&SRS=EPSG:4326&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941"
                        + "&WIDTH=200&HEIGHT=80&query_layers="
                        + layers
                        + "&FEATURE_COUNT=50";

        // run time before both (don't care about results, just check the headers)
        String url = baseUrl + "&TIME=2000-01-01";
        getFeatureAt(url, 68, 72, getLayerId(TIMERANGES));
        assertWarningCount(2);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-10-31T00:00:00.000Z");
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-10-31T00:00:00.000Z");

        // after both
        url = baseUrl + "&TIME=2100-01-01";
        getFeatureAt(url, 68, 72, getLayerId(TIMERANGES));
        assertWarningCount(2);
        assertNearestTimeWarning(getLayerId(TIMERANGES), "2008-11-07T00:00:00.000Z");
        assertNearestTimeWarning(getLayerId(WATTEMP), "2008-11-01T00:00:00.000Z");
    }
}
