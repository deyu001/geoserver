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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.DimensionsUtils;
import org.geoserver.gwc.wmts.dimensions.RasterTimeDimension;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.feature.type.DateUtil;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * This class contains tests that check that time dimension values are correctly extracted from
 * rasters. Some domain values are dynamically created based on the current time, this matches what
 * is done in WMS dimensions tests.
 *
 * <p>Note, there is some inconsistency between ISO formatter and GeoTools data serializer string
 * output, this is something that needs to be fixed (or maybe not) at GeoServer and GeoTools level.
 * In the mean time this tests will use the two formatter were needed.
 */
public class RasterTimeDimensionTest extends TestsSupport {

    // sorted time domain values as date objects
    protected static Date[] DATE_VALUES =
            new Date[] {
                DateUtil.deserializeDateTime("2008-10-31T00:00:00Z"),
                DateUtil.deserializeDateTime("2008-11-01T00:00:00Z"),
                getGeneratedMinValue(),
                getGeneratedMiddleValue(),
                getGeneratedMaxValue()
            };

    // sorted time domain values as strings formatted with ISO8601 formatter
    protected static String[] STRING_VALUES =
            new String[] {
                formatDate(DATE_VALUES[0]),
                formatDate(DATE_VALUES[1]),
                formatDate(DATE_VALUES[2]),
                formatDate(DATE_VALUES[3]),
                formatDate(DATE_VALUES[4])
            };

    @Test
    public void testDisabledDimension() throws Exception {
        // enable a time dimension
        DimensionInfo dimensionInfo = new DimensionInfoImpl();
        dimensionInfo.setEnabled(true);
        CoverageInfo rasterInfo = getCoverageInfo();
        rasterInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(rasterInfo);
        // check that we correctly retrieve the time dimension
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(1));
        // disable the time dimension
        dimensionInfo.setEnabled(false);
        rasterInfo.getMetadata().put(ResourceInfo.TIME, dimensionInfo);
        getCatalog().save(rasterInfo);
        // no dimensions should be available
        assertThat(
                DimensionsUtils.extractDimensions(wms, getLayerInfo(), ALL_DOMAINS).size(), is(0));
    }

    @Test
    public void testGetDefaultValue() {
        testDefaultValueStrategy(
                Strategy.MINIMUM, DateUtil.serializeDateTime(DATE_VALUES[0].getTime(), true));
        testDefaultValueStrategy(
                Strategy.MAXIMUM, DateUtil.serializeDateTime(DATE_VALUES[4].getTime(), true));
    }

    @Test
    public void testGetDomainsValues() throws Exception {
        testDomainsValuesRepresentation(DimensionsUtils.NO_LIMIT, STRING_VALUES);
        testDomainsValuesRepresentation(2, STRING_VALUES[0] + "--" + STRING_VALUES[4]);
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        return new RasterTimeDimension(wms, getLayerInfo(), dimensionInfo);
    }

    /** Helper method that simply formats a date using the ISO8601 formatter. */
    private static String formatDate(Date date) {
        ISO8601Formatter formatter = new ISO8601Formatter();
        return formatter.format(date);
    }

    /** Generates the current minimum date. */
    private static Date getGeneratedMinValue() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        return calendar.getTime();
    }

    /**
     * Generates the current middle date, this date is one month later than the current minimum
     * date.
     */
    private static Date getGeneratedMiddleValue() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        return calendar.getTime();
    }

    /**
     * Generates the current maximum date, this date is one year later than the current minimum
     * date.
     */
    private static Date getGeneratedMaxValue() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        // this is the way the original data month is setup, I don't understand this but without
        // this the month may not correspond
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
        return calendar.getTime();
    }

    @Test
    public void testGetHistogram() {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension = buildDimension(dimensionInfo);
        Tuple<String, List<Integer>> histogram = dimension.getHistogram(Filter.INCLUDE, "P1Y");
        assertThat(histogram.first, is("2008-10-31T00:00:00.000Z/" + STRING_VALUES[4] + "/P1Y"));
        // watertemp has 4 files, the test setup adds 3 to them, to a total o f 7 is expected
        assertThat(histogram.second.stream().reduce(0, (total, value) -> total + value), is(7));
    }

    /** Helper method that just returns the current layer info. */
    private LayerInfo getLayerInfo() {
        return catalog.getLayerByName(RASTER_TIME.getLocalPart());
    }

    /** Helper method that just returns the current coverage info. */
    private CoverageInfo getCoverageInfo() {
        LayerInfo layerInfo = getLayerInfo();
        assertThat(layerInfo.getResource(), instanceOf(CoverageInfo.class));
        return (CoverageInfo) layerInfo.getResource();
    }
}
