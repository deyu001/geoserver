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

package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.Cell.ColorMapEntryLegendBuilder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.junit.Before;
import org.junit.Test;

public class RasterLegendBuilderTest {

    GetLegendGraphicRequest request;

    @Before
    public void setup() {
        request = new GetLegendGraphicRequest();
    }

    @Test
    public void testRuleTextRampOneElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null},
                        new double[] {10},
                        new Color[] {Color.RED},
                        ColorMap.TYPE_RAMP);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(1, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("", firstRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextRampTwoElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null},
                        new double[] {10, 100},
                        new Color[] {Color.RED, Color.BLUE},
                        ColorMap.TYPE_RAMP);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("10.0 >= x", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(1);
        assertEquals("100.0 <= x", lastRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextRampThreeElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {10, 50, 100},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_RAMP);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(3, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("10.0 >= x", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals("50.0 = x", midRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(2);
        assertEquals("100.0 <= x", lastRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextIntervalOneElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null},
                        new double[] {10},
                        new Color[] {Color.RED},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(1, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 10.0", firstRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextIntervalsTwoElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null},
                        new double[] {10, 100},
                        new Color[] {Color.RED, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 10.0", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(1);
        assertEquals("10.0 <= x < 100.0", lastRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextIntervalsThreeElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {10, 50, 100},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(3, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 10.0", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals("10.0 <= x < 50.0", midRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(2);
        assertEquals("50.0 <= x < 100.0", lastRow.getRuleManager().text);
    }

    @Test
    public void testInfiniteOnIntervals() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {Double.NEGATIVE_INFINITY, 50, Double.POSITIVE_INFINITY},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 50.0", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals("50.0 <= x", midRow.getRuleManager().text);
    }

    @Test
    public void testLegendBorderColour() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {Double.NEGATIVE_INFINITY, 50, Double.POSITIVE_INFINITY},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        // Check default border colour
        Color colourToTest = LegendUtils.DEFAULT_BORDER_COLOR;

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals(colourToTest, firstRow.getColorManager().borderColor);
        assertEquals(colourToTest, firstRow.getRuleManager().borderColor);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals(colourToTest, midRow.getColorManager().borderColor);
        assertEquals(colourToTest, midRow.getRuleManager().borderColor);

        // Change legend border colour to red
        Map<String, Object> legendOptions = new HashMap<>();

        colourToTest = Color.red;

        legendOptions.put("BORDERCOLOR", SLD.toHTMLColor(colourToTest));

        request.setLegendOptions(legendOptions);
        helper = new RasterLayerLegendHelper(request, style, null);
        rows = new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        firstRow = rows.get(0);
        assertEquals(colourToTest, firstRow.getColorManager().borderColor);
        assertEquals(colourToTest, firstRow.getRuleManager().borderColor);
        midRow = rows.get(1);
        assertEquals(colourToTest, midRow.getColorManager().borderColor);
        assertEquals(colourToTest, midRow.getRuleManager().borderColor);

        // Change legend border colour to blue
        colourToTest = Color.blue;

        legendOptions.clear();
        legendOptions.put("borderColor", SLD.toHTMLColor(colourToTest));

        request.setLegendOptions(legendOptions);
        helper = new RasterLayerLegendHelper(request, style, null);
        rows = new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        firstRow = rows.get(0);
        assertEquals(colourToTest, firstRow.getColorManager().borderColor);
        assertEquals(colourToTest, firstRow.getRuleManager().borderColor);
        midRow = rows.get(1);
        assertEquals(colourToTest, midRow.getColorManager().borderColor);
        assertEquals(colourToTest, midRow.getRuleManager().borderColor);
    }
}
