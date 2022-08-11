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

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.filter.visitor.IsStaticExpressionVisitor;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer2;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;
import org.opengis.style.GraphicalSymbol;

/**
 * Utility to extract the values of dynamic properties from a style when applied to a particular
 * feature.
 *
 * @see IconPropertyInjector
 * @author David Winslow, OpenGeo
 */
public final class IconPropertyExtractor {

    private static final Logger LOGGER = Logging.getLogger(IconPropertyExtractor.class);

    public static final String NON_POINT_GRAPHIC_KEY = "npg";
    private List<List<MiniRule>> style;

    private IconPropertyExtractor(List<List<MiniRule>> style) {
        this.style = style;
    }

    private IconProperties propertiesFor(SimpleFeature feature) {
        return new FeatureProperties(feature).properties();
    }

    public static IconProperties extractProperties(
            List<List<MiniRule>> style, SimpleFeature feature) {
        return new IconPropertyExtractor(style).propertiesFor(feature);
    }

    public static IconProperties extractProperties(Style style, SimpleFeature feature) {
        return new IconPropertyExtractor(MiniRule.minify(style)).propertiesFor(feature);
    }

    /** Extracts a Graphic object from the given symbolizer */
    public static Graphic getGraphic(Symbolizer symbolizer, boolean includeNonPointGraphics) {
        // try point first
        if (symbolizer instanceof PointSymbolizer) {
            return ((PointSymbolizer) symbolizer).getGraphic();
        }
        if (!includeNonPointGraphics) {
            return null;
        }
        // try in other symbolizers
        if (symbolizer instanceof PolygonSymbolizer) {
            final Fill fill = ((PolygonSymbolizer) symbolizer).getFill();
            if (fill != null && fill.getGraphicFill() != null) {
                return fill.getGraphicFill();
            }
            final Stroke stroke = ((PolygonSymbolizer) symbolizer).getStroke();
            if (stroke != null) {
                return stroke.getGraphicStroke();
            }
        } else if (symbolizer instanceof LineSymbolizer) {
            final Stroke stroke = ((LineSymbolizer) symbolizer).getStroke();
            if (stroke != null) {
                if (stroke.getGraphicStroke() != null) return stroke.getGraphicStroke();
                if (stroke.getGraphicFill() != null) {
                    return stroke.getGraphicFill();
                }
            }
        } else if (symbolizer instanceof TextSymbolizer2) {
            return ((TextSymbolizer2) symbolizer).getGraphic();
        }

        return null;
    }

    private class FeatureProperties {
        private static final String URL = ".url";
        private static final String WIDTH = ".width";
        private static final String LINEJOIN = ".linejoin";
        private static final String LINECAP = ".linecap";
        private static final String DASHOFFSET = ".dashoffset";
        private static final String GRAPHIC = ".graphic";
        private static final String COLOR = ".color";
        private static final String STROKE = ".stroke";
        private static final String FILL = ".fill";
        private static final String NAME = ".name";
        private static final String SIZE = ".size";
        private static final String ROTATION = ".rotation";
        private static final String OPACITY = ".opacity";

        private final SimpleFeature feature;

        public FeatureProperties(SimpleFeature feature) {
            this.feature = feature;
        }

        /**
         * Safe expression execution with default fallback.
         *
         * @return evaluated value or defaultValue if unavailable
         */
        private <T> T evaluate(Expression expression, SimpleFeature feature, T defaultValue) {
            if (expression == null) {
                return defaultValue;
            }
            try {
                @SuppressWarnings("unchecked")
                T value = (T) expression.evaluate(feature, defaultValue.getClass());
                if (value == null || (value instanceof Double && Double.isNaN((Double) value))) {
                    return defaultValue;
                }
                return value;
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "Failed to evaluate " + expression + ", will use default value",
                            e);
                }
                return defaultValue;
            }
        }

        public IconProperties properties() {
            IconProperties singleExternalGraphic = trySingleExternalGraphic();
            if (singleExternalGraphic != null) {
                return singleExternalGraphic;
            } else {
                return embeddedIconProperties();
            }
        }

        public IconProperties trySingleExternalGraphic() {
            MiniRule singleRule = null;
            for (List<MiniRule> rules : style) {
                boolean applied = false;

                for (MiniRule rule : rules) {
                    final boolean applicable =
                            (rule.isElseFilter && !applied)
                                    || (rule.filter == null)
                                    || (rule.filter.evaluate(feature));
                    if (applicable) {
                        if (singleRule == null) {
                            singleRule = rule;
                        } else {
                            return null;
                        }
                    }
                }
            }
            if (singleRule == null) {
                return null;
            }
            return isExternalGraphic(singleRule);
        }

        public IconProperties isExternalGraphic(MiniRule rule) {
            if (rule.symbolizers.size() != 1) {
                return null;
            }
            Graphic g = getGraphic(rule.symbolizers.get(0), true);
            if (g == null) {
                return null;
            }
            if (g.graphicalSymbols().size() != 1) {
                return null;
            }
            GraphicalSymbol gSym = g.graphicalSymbols().get(0);
            if (!(gSym instanceof ExternalGraphic)) {
                return null;
            }
            ExternalGraphic exGraphic = (ExternalGraphic) gSym;
            try {
                Double opacity = evaluate(g.getOpacity(), feature, 1.0);
                Double size = 1d * Icons.getExternalSize(exGraphic, feature);
                if (size != null) size = size / Icons.DEFAULT_SYMBOL_SIZE;
                Double rotation = evaluate(g.getRotation(), feature, 0.0);
                Expression urlExpression =
                        ExpressionExtractor.extractCqlExpressions(
                                exGraphic.getLocation().toExternalForm());
                return IconProperties.externalReference(
                        opacity, size, rotation, urlExpression.evaluate(feature, String.class));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public IconProperties embeddedIconProperties() {
            Map<String, String> props = new TreeMap<>();
            Double size = null;
            boolean allRotated = true;
            boolean nonPointGraphic = false;
            for (int i = 0; i < style.size(); i++) {
                List<MiniRule> rules = style.get(i);
                for (int j = 0; j < rules.size(); j++) {
                    MiniRule rule = rules.get(j);
                    final boolean matches;
                    if (rule.filter == null) {
                        matches = !rule.isElseFilter || props.isEmpty();
                    } else {
                        matches = rule.filter.evaluate(feature);
                    }

                    if (matches) {
                        for (int k = 0; k < rule.symbolizers.size(); k++) {
                            props.put(i + "." + j + "." + k, "");
                            Symbolizer sym = rule.symbolizers.get(k);
                            Graphic graphic = getGraphic(sym, false);
                            if (graphic == null) {
                                graphic = getGraphic(sym, true);
                                nonPointGraphic |= graphic != null;
                            }
                            if (graphic != null) {
                                addGraphicProperties(i + "." + j + "." + k, graphic, props);
                                final Double gRotation = graphicRotation(graphic);
                                allRotated &= gRotation != null;

                                final Double gSize = Icons.graphicSize(graphic, gRotation, feature);
                                if (size == null || (gSize != null && gSize > size)) {
                                    size = gSize;
                                }
                            }
                        }
                    }
                }
            }

            if (nonPointGraphic) {
                props.put(NON_POINT_GRAPHIC_KEY, "true");
            }

            if (size != null) size = size / 16d;
            // If all the symbols in the stack were rotated, force it to be oriented to a bearing.
            final Double rotation = allRotated ? 0d : null;
            return IconProperties.generator(null, size, rotation, props);
        }

        public boolean isStatic(Expression ex) {
            return (Boolean) ex.accept(IsStaticExpressionVisitor.VISITOR, null);
        }

        private Double graphicRotation(Graphic g) {
            if (g.getRotation() != null) {
                return evaluate(g.getRotation(), feature, 0d);
            } else {
                return null;
            }
        }

        public void addGraphicProperties(String prefix, Graphic g, Map<String, String> props) {
            if (g.getOpacity() != null && !isStatic(g.getOpacity())) {
                props.put(prefix + OPACITY, String.valueOf(evaluate(g.getOpacity(), feature, 1d)));
            }
            if (g.getRotation() != null && !isStatic(g.getRotation())) {
                props.put(
                        prefix + ROTATION, String.valueOf(evaluate(g.getRotation(), feature, 0d)));
            }
            if (g.getSize() != null && !isStatic(g.getSize())) {
                props.put(prefix + SIZE, String.valueOf(evaluate(g.getSize(), feature, 16d)));
            }
            if (!g.graphicalSymbols().isEmpty()) {
                if (g.graphicalSymbols().get(0) instanceof Mark) {
                    Mark mark = (Mark) g.graphicalSymbols().get(0);
                    addMarkProperties(prefix, mark, props);
                } else if (g.graphicalSymbols().get(0) instanceof ExternalGraphic) {
                    ExternalGraphic exGraphic = (ExternalGraphic) g.graphicalSymbols().get(0);
                    addExternalGraphicProperties(prefix, exGraphic, props);
                }
            }
        }

        public void addMarkProperties(String prefix, Mark mark, Map<String, String> props) {
            if (mark.getWellKnownName() != null && !isStatic(mark.getWellKnownName())) {
                props.put(prefix + NAME, evaluate(mark.getWellKnownName(), feature, "square"));
            }
            if (mark.getFill() != null) {
                addFillProperties(prefix + FILL, mark.getFill(), props);
            }
            if (mark.getStroke() != null) {
                addStrokeProperties(prefix + STROKE, mark.getStroke(), props);
            }
        }

        public void addFillProperties(String prefix, Fill fill, Map<String, String> props) {
            if (fill.getColor() != null && !isStatic(fill.getColor())) {
                props.put(prefix + COLOR, evaluate(fill.getColor(), feature, "0xAAAAAA"));
            }
            if (fill.getOpacity() != null && !isStatic(fill.getOpacity())) {
                props.put(
                        prefix + OPACITY, String.valueOf(evaluate(fill.getOpacity(), feature, 1d)));
            }
            if (fill.getGraphicFill() != null) {
                addGraphicProperties(prefix + GRAPHIC, fill.getGraphicFill(), props);
            }
        }

        public void addStrokeProperties(String prefix, Stroke stroke, Map<String, String> props) {
            if (stroke.getColor() != null && !isStatic(stroke.getColor())) {
                props.put(prefix + COLOR, evaluate(stroke.getColor(), feature, "0x000000"));
            }
            if (stroke.getDashOffset() != null && !isStatic(stroke.getDashOffset())) {
                props.put(
                        prefix + DASHOFFSET,
                        String.valueOf(evaluate(stroke.getDashOffset(), feature, 0d)));
            }
            if (stroke.getLineCap() != null && !isStatic(stroke.getLineCap())) {
                props.put(prefix + LINECAP, evaluate(stroke.getLineCap(), feature, "butt"));
            }
            if (stroke.getLineJoin() != null && !isStatic(stroke.getLineJoin())) {
                props.put(prefix + LINEJOIN, evaluate(stroke.getLineJoin(), feature, "miter"));
            }
            if (stroke.getOpacity() != null && !isStatic(stroke.getOpacity())) {
                props.put(
                        prefix + OPACITY,
                        String.valueOf(evaluate(stroke.getOpacity(), feature, 1d)));
            }
            if (stroke.getWidth() != null && !isStatic(stroke.getWidth())) {
                props.put(prefix + WIDTH, String.valueOf(evaluate(stroke.getWidth(), feature, 1d)));
            }
            if (stroke.getGraphicStroke() != null) {
                addGraphicProperties(prefix + GRAPHIC, stroke.getGraphicStroke(), props);
            }
            if (stroke.getGraphicFill() != null) {
                addGraphicProperties(prefix + GRAPHIC, stroke.getGraphicFill(), props);
            }
        }

        public void addExternalGraphicProperties(
                String prefix, ExternalGraphic exGraphic, Map<String, String> props) {
            try {
                Expression ex =
                        ExpressionExtractor.extractCqlExpressions(
                                exGraphic.getLocation().toExternalForm());
                if (!isStatic(ex)) {
                    props.put(prefix + URL, ex.evaluate(feature, String.class));
                }
            } catch (MalformedURLException e) {
                // Do nothing, it's just an icon we can't resolve.
                // TODO: Log at FINER or FINEST level?
            }
        }
    }
}
