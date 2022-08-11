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

package org.geoserver.wms.icons;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class IconPropertyInjectorTest extends IconTestSupport {

    static <T> T assertSingleElement(Iterable<T> elements) {
        Iterator<T> i = elements.iterator();

        assertTrue("Expected one element but got none", i.hasNext());
        T result = i.next();
        assertFalse("Expected one element but got more", i.hasNext());

        return result;
    }

    @SuppressWarnings("unchecked")
    static <T, U extends T> U assertSingleElement(Iterable<T> elements, Class<U> clazz) {
        T result = assertSingleElement(elements);
        assertThat(result, instanceOf(clazz));
        return (U) result;
    }

    static <T> T assumeSingleElement(Iterable<T> elements) {
        Iterator<T> i = elements.iterator();

        assumeTrue("Expected one element but got none", i.hasNext());
        T result = i.next();
        assumeFalse("Expected one element but got more", i.hasNext());

        return result;
    }

    @SuppressWarnings("unchecked")
    static <T, U extends T> U assumeSingleElement(Iterable<T> elements, Class<U> clazz) {
        T result = assertSingleElement(elements);
        assumeThat(result, instanceOf(clazz));
        return (U) result;
    }

    @Test
    public void testSimplePointStyle() throws Exception {
        Style result;
        {
            Symbolizer symb = grayCircle();
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assertSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            assertSingleElement(symb.getGraphic().graphicalSymbols(), Mark.class);
        }
    }

    @Test
    public void testSimplePointStyleOff() throws Exception {
        Style result;
        {
            Symbolizer symb = grayCircle();
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<>();
            // properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            assertThat(rule.symbolizers().size(), is(0));
        }
    }

    @Test
    public void testSimpleGraphicStyle() throws Exception {
        Style result;
        {
            Symbolizer symb = this.externalGraphic("http://example.com/foo.png", "image/png");
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/foo.png"));
        }
    }

    @Test
    public void testSubstitutedGraphicStyle() throws Exception {
        Style result;
        {
            Symbolizer symb =
                    this.externalGraphic("http://example.com/${PROV_ABBR}.png", "image/png");
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<>();
            properties.put("0.0.0", "");
            properties.put("0.0.0.url", "http://example.com/BC.png");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/BC.png"));
        }
    }

    @Test
    public void testUnneccessaryURLInjection() throws Exception {
        Style result;
        {
            Symbolizer symb = this.externalGraphic("http://example.com/NF.png", "image/png");
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<>();
            properties.put("0.0.0", "");
            properties.put("0.0.0.url", "http://example.com/BC.png");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/NF.png"));
        }
    }

    @Test
    public void testRotation() throws Exception {
        Style result;
        {
            PointSymbolizer symb = this.externalGraphic("http://example.com/foo.png", "image/png");
            symb.getGraphic().setRotation(filterFactory.property("heading"));
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<>();
            properties.put("0.0.0", "");
            properties.put("0.0.0.rotation", "45.0");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            Graphic eg = symb.getGraphic();
            assertThat(eg.getRotation().evaluate(null).toString(), is("45.0"));
        }
    }

    @Test
    public void testFilteredRulesPickFirstExternal() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = externalGraphic("http://example.com/foo.png", "image/png");
            PointSymbolizer symb2 = externalGraphic("http://example.com/bar.png", "image/png");
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/foo.png"));
        }
    }

    @Test
    public void testFilteredRulesPickSecondExternal() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = externalGraphic("http://example.com/foo.png", "image/png");
            PointSymbolizer symb2 = externalGraphic("http://example.com/bar.png", "image/png");
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<>();
            properties.put("0.1.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/bar.png"));
        }
    }

    @Test
    public void testFilteredRulesPickFirstMark() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = mark("arrow", Color.BLACK, Color.RED, 1f, 16);
            PointSymbolizer symb2 = mark("arrow", Color.BLACK, Color.BLUE, 1f, 16);
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            Mark mark = assertSingleElement(symb.getGraphic().graphicalSymbols(), Mark.class);
            assertThat(mark.getFill().getColor().evaluate(null, Color.class), is(Color.RED));
        }
    }

    @Test
    public void testFilteredRulesPickSecondMark() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = mark("arrow", Color.BLACK, Color.RED, 1f, 16);
            PointSymbolizer symb2 = mark("arrow", Color.BLACK, Color.BLUE, 1f, 16);
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<>();
            properties.put("0.1.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            Mark mark = assertSingleElement(symb.getGraphic().graphicalSymbols(), Mark.class);
            assertThat(mark.getFill().getColor().evaluate(null, Color.class), is(Color.BLUE));
        }
    }

    @Test
    public void testGraphicFallbacks() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Style style = SLD.createPointStyle("circle", Color.RED, Color.yellow, 0.5f, 10f);
        Graphic g = SLD.graphic(SLD.pointSymbolizer(style));
        g.setRotation(ff.literal(45));
        g.setOpacity(ff.literal(0.5));

        Map<String, String> props = new HashMap<>();
        props.put("0.0.0", "");

        style = IconPropertyInjector.injectProperties(style, props);
        g = SLD.graphic(SLD.pointSymbolizer(style));

        assertEquals(10.0, g.getSize().evaluate(null, Double.class), 0.1);
        assertEquals(45.0, g.getRotation().evaluate(null, Double.class), 0.1);
        assertEquals(0.5, g.getOpacity().evaluate(null, Double.class), 0.1);
    }
}
