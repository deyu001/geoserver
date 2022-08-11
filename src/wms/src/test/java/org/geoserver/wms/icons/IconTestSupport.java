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

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory2;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.junit.BeforeClass;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.style.Fill;
import org.opengis.style.Font;

public class IconTestSupport {

    protected static SimpleFeature fieldIs1;
    protected static SimpleFeature fieldIs2;
    protected static StyleFactory2 styleFactory;
    protected static FilterFactory2 filterFactory;
    protected static SimpleFeatureType featureType;

    @BeforeClass
    public static void classSetup() {
        styleFactory = (StyleFactory2) CommonFactoryFinder.getStyleFactory();
        filterFactory = CommonFactoryFinder.getFilterFactory2();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("example");
        typeBuilder.setNamespaceURI("http://example.com/");
        typeBuilder.setSRS("EPSG:4326");
        typeBuilder.add("field", String.class);
        typeBuilder.add("geom", Point.class, "EPSG:4326");
        featureType = typeBuilder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("field", "1");
        featureBuilder.set("geom", geometryFactory.createPoint(new Coordinate(0, 0)));
        fieldIs1 = featureBuilder.buildFeature(null);
        featureBuilder.set("field", "2");
        featureBuilder.set("geom", geometryFactory.createPoint(new Coordinate(0, 0)));
        fieldIs2 = featureBuilder.buildFeature(null);
    }

    protected String queryString(Map<String, String> params) {
        try {
            StringBuilder buff = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    buff.append("&");
                }
                buff.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return buff.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected final Fill fill(Color color, Double opacity) {
        Expression colorExpr = color == null ? null : filterFactory.literal(color);
        Expression opacityExpr = opacity == null ? null : filterFactory.literal(opacity);
        return styleFactory.fill(null, colorExpr, opacityExpr);
    }

    protected final Font font(String fontFace, String style, String weight, Integer size) {
        List<Expression> fontFaceList =
                fontFace == null
                        ? null
                        : Collections.singletonList(filterFactory.literal(fontFace));
        Expression styleExpr = style == null ? null : filterFactory.literal(style);
        Expression weightExpr = weight == null ? null : filterFactory.literal(weight);
        Expression sizeExpr = size == null ? null : filterFactory.literal(size);
        return styleFactory.font(fontFaceList, styleExpr, weightExpr, sizeExpr);
    }

    protected final TextSymbolizer text(String name, String label, Font font, Fill fill) {
        Expression geometry = filterFactory.property("");
        return styleFactory.textSymbolizer(
                name, geometry, null, null, filterFactory.property(label), font, null, null, fill);
    }

    protected final PointSymbolizer mark(
            String name, Color stroke, Color fill, float opacity, int size) {
        return SLD.pointSymbolizer(SLD.createPointStyle(name, stroke, fill, opacity, size));
    }

    protected final PointSymbolizer externalGraphic(String url, String format) {
        ExternalGraphic exGraphic = styleFactory.createExternalGraphic(url, format);
        Graphic graphic =
                styleFactory.createGraphic(
                        new ExternalGraphic[] {exGraphic}, null, null, null, null, null);
        return styleFactory.createPointSymbolizer(graphic, null);
    }

    protected final PointSymbolizer grayCircle() {
        return mark("circle", Color.BLACK, Color.GRAY, 1f, 16);
    }

    protected final Rule rule(Filter filter, Symbolizer... symbolizer) {
        Rule rule = styleFactory.createRule();
        rule.setFilter(filter);
        for (Symbolizer s : symbolizer) rule.symbolizers().add(s);
        return rule;
    }

    protected final Rule catchAllRule(Symbolizer... symbolizer) {
        Rule rule = styleFactory.createRule();
        for (Symbolizer s : symbolizer) rule.symbolizers().add(s);
        return rule;
    }

    protected final Rule elseRule(Symbolizer... symbolizer) {
        Rule rule = styleFactory.createRule();
        rule.setElseFilter(true);
        for (Symbolizer s : symbolizer) rule.symbolizers().add(s);
        return rule;
    }

    protected final FeatureTypeStyle featureTypeStyle(Rule... rules) {
        FeatureTypeStyle ftStyle = styleFactory.createFeatureTypeStyle();
        for (Rule r : rules) ftStyle.rules().add(r);
        return ftStyle;
    }

    protected final Style styleFromRules(Rule... rules) {
        return style(featureTypeStyle(rules));
    }

    protected final Style style(FeatureTypeStyle... ftStyles) {
        Style style = styleFactory.createStyle();
        for (FeatureTypeStyle f : ftStyles) style.featureTypeStyles().add(f);
        return style;
    }
}
