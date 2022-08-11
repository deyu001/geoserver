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

package org.geoserver.wms;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.dimension.DefaultValueConfiguration;
import org.geoserver.wms.dimension.DefaultValueConfiguration.DefaultValuePolicy;
import org.junit.Before;
import org.junit.Test;

public class DimensionsVectorGetMapTest extends WMSDynamicDimensionTestSupport {

    String baseGetMap;

    @Before
    public void setup() throws Exception {
        baseGetMap =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90"
                        + "&styles=&Format=image/png&width=80&height=40&srs=EPSG:4326&layers="
                        + getLayerId(V_TIME_ELEVATION);
    }

    @Test
    public void testNoDimension() throws Exception {
        BufferedImage image = getAsImage(baseGetMap, "image/png");

        // we should get everything black, all four squares
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.BLACK);
    }

    @Test
    public void testBothDimensionsStaticDefaults() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);

        BufferedImage image = getAsImage(baseGetMap, "image/png");

        // we should get everything white, none of the squares is coming back
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeDynamicRestriction() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=1.0", "image/png");

        // this select the second feature
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeExpressionFull() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN),
                new DefaultValueConfiguration(
                        ResourceInfo.TIME, "Concatenate('2011-05-0', round(elevation + 1))"));

        BufferedImage image = getAsImage(baseGetMap, "image/png");

        // this select the first feature
        assertPixel(image, 20, 10, Color.BLACK);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testTimeExpressionSingleElevation() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.TIME, "Concatenate('2011-05-0', round(elevation + 1))"));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=1.0", "image/png");

        // elevation = 1.0 -> second feature
        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testElevationDynamicRestriction() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&time=2011-05-02", "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testExplicitDefaultTime() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(ResourceInfo.TIME, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=1.0&time=current", "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.BLACK);
        assertPixel(image, 20, 30, Color.WHITE);
        assertPixel(image, 60, 30, Color.WHITE);
    }

    @Test
    public void testExplicitDefaultElevation() throws Exception {
        // setup both dimensions, there is no match records to the static defaults
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);
        setupDynamicDimensions(
                "TimeElevation",
                new DefaultValueConfiguration(
                        ResourceInfo.ELEVATION, DefaultValuePolicy.LIMIT_DOMAIN));

        BufferedImage image = getAsImage(baseGetMap + "&elevation=&time=2011-05-03", "image/png");

        assertPixel(image, 20, 10, Color.WHITE);
        assertPixel(image, 60, 10, Color.WHITE);
        assertPixel(image, 20, 30, Color.BLACK);
        assertPixel(image, 60, 30, Color.WHITE);
    }
}
