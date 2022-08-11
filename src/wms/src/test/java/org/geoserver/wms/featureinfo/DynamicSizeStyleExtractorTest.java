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

package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Color;
import java.util.List;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.junit.Before;
import org.junit.Test;

public class DynamicSizeStyleExtractorTest {

    StyleBuilder sb = new StyleBuilder();
    private Rule staticPolygonRule;
    private Rule staticLineRule;
    private DynamicSizeStyleExtractor visitor;

    @Before
    public void setup() {
        staticPolygonRule = sb.createRule(sb.createPolygonSymbolizer(Color.RED));
        staticLineRule = sb.createRule(sb.createLineSymbolizer(Color.BLUE, 1d));

        visitor = new DynamicSizeStyleExtractor();
    }

    @Test
    public void testOneFtsFullyStatic() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        fts.rules().add(staticLineRule);
        style.featureTypeStyles().add(fts);

        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }

    @Test
    public void testTwoFtsFullyStatic() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts1 = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        FeatureTypeStyle fts2 = sb.createFeatureTypeStyle("Feature", staticLineRule);
        style.featureTypeStyles().add(fts1);
        style.featureTypeStyles().add(fts2);

        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }

    @Test
    public void testMixDynamicStroke() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts1 = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setWidth(sb.getFilterFactory().property("myAttribute"));
        FeatureTypeStyle fts2 = sb.createFeatureTypeStyle(ls);
        style.featureTypeStyles().add(fts1);
        style.featureTypeStyles().add(fts2);

        checkSingleSymbolizer(style, ls);
    }

    @Test
    public void testMultipleSymbolizers() {
        Style style = sb.createStyle();
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setWidth(sb.getFilterFactory().property("myAttribute"));
        FeatureTypeStyle fts = sb.createFeatureTypeStyle(sb.createPolygonSymbolizer());
        style.featureTypeStyles().add(fts);
        fts.rules().get(0).symbolizers().add(ls);
        fts.rules().get(0).symbolizers().add(sb.createLineSymbolizer());

        checkSingleSymbolizer(style, ls);
    }

    private void checkSingleSymbolizer(Style style, LineSymbolizer ls) {
        // we should get back only the dynamic one
        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNotNull(copy);
        List<FeatureTypeStyle> featureTypeStyles = copy.featureTypeStyles();
        assertEquals(1, featureTypeStyles.size());
        List<Rule> rules = featureTypeStyles.get(0).rules();
        assertEquals(1, rules.size());
        List<Symbolizer> symbolizers = rules.get(0).symbolizers();
        assertEquals(1, symbolizers.size());
        assertEquals(ls, symbolizers.get(0));
    }

    @Test
    public void testMixDynamicGraphicStroke() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts1 = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        Graphic graphic = sb.createGraphic(null, sb.createMark("square"), null);
        graphic.setSize(sb.getFilterFactory().property("myAttribute"));
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);
        FeatureTypeStyle fts2 = sb.createFeatureTypeStyle(ls);
        style.featureTypeStyles().add(fts1);
        style.featureTypeStyles().add(fts2);

        checkSingleSymbolizer(style, ls);
    }

    @Test
    public void testDynamicSymbolizerStrokeLineSymbolizer() {
        ExternalGraphic dynamicSymbolizer =
                sb.createExternalGraphic("file://./${myAttribute}.jpeg", "image/jpeg");
        Graphic graphic = sb.createGraphic(dynamicSymbolizer, null, null);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);

        Style style = sb.createStyle(ls);

        checkSingleSymbolizer(style, ls);
    }

    @Test
    public void testStaticGraphicLineSymbolizer() {
        ExternalGraphic dynamicSymbolizer =
                sb.createExternalGraphic("file://./hello.jpeg", "image/jpeg");
        Graphic graphic = sb.createGraphic(dynamicSymbolizer, null, null);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);

        Style style = sb.createStyle(ls);

        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }

    @Test
    public void testDynamicStrokeInGraphicMark() {
        Stroke markStroke = sb.createStroke();
        markStroke.setWidth(sb.getFilterFactory().property("myAttribute"));
        Mark mark = sb.createMark("square");
        mark.setStroke(markStroke);
        Graphic graphic = sb.createGraphic(null, mark, null);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);

        Style style = sb.createStyle(ls);

        checkSingleSymbolizer(style, ls);
    }

    @Test // this one should fail now??
    public void testDynamicStrokeInGraphicFill() {
        Stroke markStroke = sb.createStroke();
        markStroke.setWidth(sb.getFilterFactory().property("myAttribute"));
        Mark mark = sb.createMark("square");
        mark.setStroke(markStroke);
        Graphic graphic = sb.createGraphic(null, mark, null);
        PolygonSymbolizer ps = sb.createPolygonSymbolizer();
        ps.getFill().setGraphicFill(graphic);

        Style style = sb.createStyle(ps);
        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }
}
