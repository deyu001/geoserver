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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints.Key;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.GetMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.renderer.RenderListener;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.DescriptionImpl;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory2;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.geotools.util.Converters;
import org.geotools.util.SimpleInternationalString;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.style.Description;

/**
 * Alters a legend to add a count of the rules descriptions
 *
 * @author Andrea Aime - GeoSolutions
 */
class FeatureCountProcessor {

    static final StyleFactory2 SF = (StyleFactory2) CommonFactoryFinder.getStyleFactory();
    public static final String WIDTH = "WIDTH";
    public static final String HEIGHT = "HEIGHT";

    /**
     * Updates a rule setting its description's title as the provided targetLabel description)
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static final class TargetLabelUpdater extends DuplicatingStyleVisitor {
        private String targetLabel;

        public TargetLabelUpdater(String targetLabel) {
            this.targetLabel = targetLabel;
        }

        @Override
        public void visit(Rule rule) {
            super.visit(rule);
            Rule copy = (Rule) pages.peek();
            Description description =
                    new DescriptionImpl(
                            new SimpleInternationalString(targetLabel),
                            copy.getDescription() != null
                                    ? copy.getDescription().getAbstract()
                                    : null);
            copy.setDescription(description);
        }
    }

    /**
     * Runs a map generation on an empty graphics object and allows to consume each feature that
     * gets rendered
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static final class FeatureRenderSpyFormat extends RenderedImageMapOutputFormat {
        private Consumer<Feature> consumer;

        private FeatureRenderSpyFormat(WMS wms, Consumer<Feature> consumer) {
            super(wms);
            this.consumer = consumer;
        }

        @Override
        protected RenderedImage prepareImage(
                int width, int height, IndexColorModel palette, boolean transparent) {
            return null;
        }

        @Override
        protected Graphics2D getGraphics(
                boolean transparent,
                Color bgColor,
                RenderedImage preparedImage,
                Map<Key, Object> hintsMap) {
            return new NoOpGraphics2D();
        }

        @Override
        protected void onBeforeRender(StreamingRenderer renderer) {
            super.onBeforeRender(renderer);
            renderer.setGeneralizationDistance(0);
            renderer.addRenderListener(
                    new RenderListener() {

                        @Override
                        public void featureRenderer(SimpleFeature feature) {
                            consumer.accept(feature);
                        }

                        @Override
                        public void errorOccurred(Exception e) {
                            // nothing to do here
                        }
                    });
        }
    }

    /**
     * Checks if there are rules in match first mode
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class MatchFirstVisitor extends AbstractStyleVisitor {
        boolean matchFirst = false;

        @Override
        public void visit(FeatureTypeStyle fts) {
            // yes, it's an approximation, we cannot really work with a mix of FTS that
            // are some evaluate first, others non evaluate first, but the case is so narrow
            // that I'm inclined to wait for dedicated funding before going there
            matchFirst |=
                    FeatureTypeStyle.VALUE_EVALUATION_MODE_FIRST.equals(
                            fts.getOptions().get(FeatureTypeStyle.KEY_EVALUATION_MODE));
        }
    }

    /**
     * Replaces labels with small points for the sake of feature counting
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class LabelReplacer extends DuplicatingStyleVisitor {
        PointSymbolizer ps;

        LabelReplacer() {
            ps = sf.createPointSymbolizer();
            ps.getGraphic().graphicalSymbols().add(sf.createMark());
        }

        @Override
        public void visit(TextSymbolizer text) {
            pages.push(ps);
        }
    }

    private GetLegendGraphicRequest request;

    private GetMapKvpRequestReader getMapReader;

    private boolean hideEmptyRules;

    private boolean countMatched;

    /**
     * Builds a new feature count processor given the legend graphic request. It can be used to
     * alter with feature counts many rule sets.
     */
    public FeatureCountProcessor(GetLegendGraphicRequest request) {
        this.request = request;
        this.getMapReader = new GetMapKvpRequestReader(request.getWms());
        if (Boolean.TRUE.equals(
                request.getLegendOption(
                        GetLegendGraphicRequest.COUNT_MATCHED_KEY, Boolean.class))) {
            countMatched = true;
        }
        if (Boolean.TRUE.equals(
                request.getLegendOption(GetLegendGraphicRequest.HIDE_EMPTY_RULES, Boolean.class))) {
            hideEmptyRules = true;
        }
    }

    /**
     * Pre-processes the legend request and returns a style whose rules have been altered to contain
     * a feature count
     */
    public Rule[] preProcessRules(LegendRequest legend, Rule[] rules) {
        if (rules == null || rules.length == 0) {
            return rules;
        }

        // is the code running in match first mode?
        MatchFirstVisitor matchFirstVisitor = new MatchFirstVisitor();
        legend.getStyle().accept(matchFirstVisitor);
        boolean matchFirst = matchFirstVisitor.matchFirst;

        try {
            GetMapRequest getMapRequest = parseAssociatedGetMap(legend, rules);
            Map<Rule, AtomicInteger> counters =
                    renderAndCountFeatures(rules, getMapRequest, matchFirst);
            Rule[] result = updateRuleTitles(rules, counters);

            return result;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    private Rule[] updateRuleTitles(Rule[] rules, Map<Rule, AtomicInteger> counters) {
        ArrayList<Rule> result = new ArrayList<>();
        for (Rule rule : rules) {
            AtomicInteger counter = counters.get(rule);
            if (counter.get() == 0 && this.hideEmptyRules) {
                continue;
            }
            String label = LegendUtils.getRuleLabel(rule, request);
            if (this.countMatched) {
                if (StringUtils.isEmpty(label)) {
                    label = "(" + counter.get() + ")";
                } else {
                    label = label + " (" + counter.get() + ")";
                }
            }

            TargetLabelUpdater duplicatingVisitor = new TargetLabelUpdater(label);
            rule.accept(duplicatingVisitor);
            Rule clone = (Rule) duplicatingVisitor.getCopy();
            result.add(clone);
        }
        return result.toArray(new Rule[result.size()]);
    }

    private Map<Rule, AtomicInteger> renderAndCountFeatures(
            Rule[] rules, GetMapRequest getMapRequest, boolean matchFirst) {
        final WMS wms = request.getWms();
        // the counters for each rule, all initialized at zero
        Map<Rule, AtomicInteger> counters =
                Arrays.stream(rules)
                        .collect(Collectors.toMap(Function.identity(), r -> new AtomicInteger(0)));

        // run and count
        GetMap getMap =
                new GetMap(wms) {
                    protected org.geoserver.wms.GetMapOutputFormat getDelegate(String outputFormat)
                            throws ServiceException {
                        return new FeatureRenderSpyFormat(
                                wms,
                                f -> {
                                    boolean matched = false;
                                    for (Rule rule : rules) {
                                        if (rule.isElseFilter()) {
                                            if (!matched) {
                                                AtomicInteger counter = counters.get(rule);
                                                counter.incrementAndGet();
                                            }
                                        } else if (rule.getFilter() == null
                                                || rule.getFilter().evaluate(f)) {
                                            AtomicInteger counter = counters.get(rule);
                                            counter.incrementAndGet();
                                            matched = true;
                                            if (matchFirst) {
                                                break;
                                            }
                                        }
                                    }
                                });
                    };
                };
        getMap.run(getMapRequest);

        return counters;
    }

    /** Parse the equivalent GetMap for this layer */
    private GetMapRequest parseAssociatedGetMap(LegendRequest legend, Rule[] rules)
            throws Exception {
        // setup the KVP for the internal, fake GetMap
        Map<String, Object> kvp = new CaseInsensitiveMap<>(request.getKvp());
        Map<String, Object> rawKvp = new CaseInsensitiveMap<>(new HashMap<>());
        request.getRawKvp().forEach((k, v) -> rawKvp.put(k, v));
        // remove width/height, they are part of the GetLegendGraphic request, not GetMap
        kvp.remove("WIDTH");
        kvp.remove("HEIGHT");
        rawKvp.remove("WIDTH");
        rawKvp.remove("HEIGHT");
        // ... the actual layer
        String layerName = getLayerName(legend);
        kvp.put("LAYERS", layerName);
        rawKvp.put("LAYERS", layerName);
        // ... a default style we'll override later
        kvp.put("STYLES", "");
        rawKvp.put("STYLES", "");
        // ... width and height as part of the count related extension
        String srcWidth = (String) rawKvp.get("SRCWIDTH");
        String srcHeight = (String) rawKvp.get("SRCHEIGHT");
        rawKvp.put(WIDTH, srcWidth);
        rawKvp.put(HEIGHT, srcHeight);
        if (srcWidth != null) {
            kvp.put(WIDTH, Converters.convert(srcWidth, Integer.class));
        }
        if (srcHeight != null) {
            kvp.put(HEIGHT, Converters.convert(srcHeight, Integer.class));
        }

        // remove decoration to avoid infinite recursion
        final Map formatOptions = (Map) kvp.get("FORMAT_OPTIONS");
        if (formatOptions != null) {
            formatOptions.remove("layout");
        }

        // parse
        GetMapRequest getMap = getMapReader.read(getMapReader.createRequest(), kvp, rawKvp);
        DefaultWebMapService.autoSetBoundsAndSize(getMap);

        // replace style with the current set of rules
        Style style = buildStyleFromRules(rules);
        getMap.setStyles(Arrays.asList(style));

        return getMap;
    }

    private String getLayerName(LegendRequest legend) {
        if (legend.getLayer() != null) {
            return legend.getLayer();
        } else if (legend.getLayerInfo() != null) {
            return legend.getLayerInfo().prefixedName();
        } else if (legend.getFeatureType() != null) {
            Name name = legend.getFeatureType().getName();
            NamespaceInfo ns =
                    request.getWms().getCatalog().getNamespaceByURI(name.getNamespaceURI());
            final String localName = name.getLocalPart();
            if (ns != null) {
                return ns.getPrefix() + ":" + localName;
            } else {
                return localName;
            }
        } else {
            // should not really happen today, but who knows, may do in the future
            throw new ServiceException("Could not get the layer name out of " + legend);
        }
    }

    private Style buildStyleFromRules(Rule[] rules) {
        // prepare based on rules
        FeatureTypeStyle fts = SF.createFeatureTypeStyle();
        fts.rules().addAll(Arrays.asList(rules));
        Style style = SF.createStyle();
        style.featureTypeStyles().add(fts);

        // replace labels with points (labels report about features that are not
        // really in the viewport only because the lax geometry check loaded them,
        // at the same time we cannot do a true intersection test for a variety or reasons,
        // for example, in place reprojection, rendering transformations, advanced projection
        // handling)
        LabelReplacer replacer = new LabelReplacer();
        style.accept(replacer);

        return (Style) replacer.getCopy();
    }
}
