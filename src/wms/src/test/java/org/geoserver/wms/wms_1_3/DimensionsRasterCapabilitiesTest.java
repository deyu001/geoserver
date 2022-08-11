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

package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class DimensionsRasterCapabilitiesTest extends WMSDimensionsTestSupport {

    @Test
    public void testNoDimension() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Name='sf:watertemp'])", dom);
        assertXpathEvaluatesTo("0", "count(//wms:Layer/wms:Dimension)", dom);
    }

    @Test
    public void testDefaultElevationUnits() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, null, null);
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);

        assertXpathEvaluatesTo(
                DimensionInfo.ELEVATION_UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(
                DimensionInfo.ELEVATION_UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
    }

    @Test
    public void testElevationList() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0,100.0", "//wms:Layer/wms:Dimension", dom);
    }

    @Test
    public void testElevationContinuous() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/100.0/0", "//wms:Layer/wms:Dimension", dom);
    }

    @Test
    public void testElevationDiscreteNoResolution() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.DISCRETE_INTERVAL,
                null,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/100.0/100.0", "//wms:Layer/wms:Dimension", dom);
    }

    @Test
    public void testElevationDiscreteManualResolution() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.DISCRETE_INTERVAL,
                10.0,
                UNITS,
                UNIT_SYMBOL);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/100.0/10.0", "//wms:Layer/wms:Dimension", dom);
    }

    @Test
    public void testTimeList() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("", "//wms:Layer/wms:Dimension/@nearestValue", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT,
                "//wms:Layer/wms:Dimension/@default",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z",
                "//wms:Layer/wms:Dimension",
                dom);
    }

    @Test
    public void testTimeNearestMatch() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        setupNearestMatch(WATTEMP, ResourceInfo.TIME, true);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("1", "//wms:Layer/wms:Dimension/@nearestValue", dom);
    }

    @Test
    public void testTimeContinuous() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null,
                null,
                null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT,
                "//wms:Layer/wms:Dimension/@default",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z/PT1S",
                "//wms:Layer/wms:Dimension",
                dom);
    }

    @Test
    public void testTimeResolution() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.DISCRETE_INTERVAL,
                Double.valueOf(1000 * 60 * 60 * 12),
                null,
                null);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);

        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the wms:Dimension
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo(
                DimensionDefaultValueSetting.TIME_CURRENT,
                "//wms:Layer/wms:Dimension/@default",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z/PT12H",
                "//wms:Layer/wms:Dimension",
                dom);
    }

    @Test
    public void testDefaultTimeRangeFixed() throws Exception {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("P1M/PRESENT");
        setupResourceDimensionDefaultValue(WATTEMP, ResourceInfo.TIME, defaultValueSetting);

        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        print(dom);

        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("P1M/PRESENT", "//wms:Layer/wms:Dimension/@default", dom);
    }
}
