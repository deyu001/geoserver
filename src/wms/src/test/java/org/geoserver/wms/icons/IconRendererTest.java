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

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import javax.imageio.ImageIO;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.image.test.ImageAssert;
import org.geotools.styling.StyleFactory;
import org.junit.Test;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.FeatureTypeStyle;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.Mark;
import org.opengis.style.Rule;
import org.opengis.style.Style;
import org.opengis.style.Symbolizer;

public class IconRendererTest {
    /** Upscaled images need a higher threshold for pdiff */
    static final int THRESHOLD = 400;

    @Test
    public void testSimpleCircle() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        Mark m =
                sfact.mark(
                        ffact.literal("circle"),
                        sfact.fill(null, ffact.literal("#FF0000"), null),
                        sfact.stroke(
                                ffact.literal("#000000"),
                                null,
                                ffact.literal(1),
                                null,
                                null,
                                null,
                                null));

        Graphic g =
                sfact.graphic(
                        Arrays.asList(m),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (16 + 1 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("circle-red-16-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testSimpleSquare() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        Mark m =
                sfact.mark(
                        ffact.literal("square"),
                        sfact.fill(null, ffact.literal("#0000FF"), null),
                        sfact.stroke(
                                ffact.literal("#000000"),
                                null,
                                ffact.literal(1),
                                null,
                                null,
                                null,
                                null));

        Graphic g =
                sfact.graphic(
                        Arrays.asList(m),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (16 + 1 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("square-blue-16-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testSquareRotated45() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        Mark m =
                sfact.mark(
                        ffact.literal("square"),
                        sfact.fill(null, ffact.literal("#0000FF"), null),
                        sfact.stroke(
                                ffact.literal("#000000"),
                                null,
                                ffact.literal(1),
                                null,
                                null,
                                null,
                                null));

        Graphic g =
                sfact.graphic(
                        Arrays.asList(m),
                        Expression.NIL,
                        Expression.NIL,
                        ffact.literal(45.0),
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int baseSize = 16;
        final int rotated = (int) Math.ceil(baseSize * Math.sqrt(2));
        final int size = (rotated + 1 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected =
                ImageIO.read(this.getClass().getResource("square-blue-16-x4-45deg.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testExternalImage() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("arrow-16.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (16 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("arrow-16-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testExternalImageRotated45() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("arrow-16.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        ffact.literal(45.0),
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int baseSize = 16;
        final int rotated = (int) Math.ceil(baseSize * Math.sqrt(2));
        final int size = (rotated + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("arrow-16-x4-45deg.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testBigExternalImage() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("planet-42.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (42 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("planet-42-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testBigExternalImageSpecifySize() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("planet-42.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        ffact.literal(42),
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (42 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("planet-42-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }

    @Test
    public void testBigExternalImageNilExpressionSize() throws Exception {
        StyleFactory sfact = CommonFactoryFinder.getStyleFactory();
        FilterFactory ffact = CommonFactoryFinder.getFilterFactory();

        GraphicalSymbol gs =
                sfact.createExternalGraphic(
                        this.getClass().getResource("planet-42.png"), "image/png");

        Graphic g =
                sfact.graphic(
                        Arrays.asList(gs),
                        Expression.NIL,
                        Expression.NIL,
                        Expression.NIL,
                        null,
                        null);
        Symbolizer symb = sfact.pointSymbolizer(null, ffact.property(null), null, null, g);
        Rule r =
                sfact.rule(
                        null,
                        null,
                        null,
                        Float.NEGATIVE_INFINITY,
                        Float.POSITIVE_INFINITY,
                        Arrays.asList(symb),
                        null);
        FeatureTypeStyle fts =
                sfact.featureTypeStyle(
                        null,
                        null,
                        null,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Arrays.asList(r));
        Style s = sfact.style(null, null, true, Arrays.asList(fts), null);

        BufferedImage img = IconRenderer.renderIcon((org.geotools.styling.Style) s);

        // Default mark size, plus border, plus padding, times rendering scale, plus extra padding.
        final int size = (42 + 0 + 1 + 1) * 4;
        assertEquals(size, img.getHeight());
        assertEquals(size, img.getWidth());

        BufferedImage expected = ImageIO.read(this.getClass().getResource("planet-42-x4.png"));

        ImageAssert.assertEquals(expected, img, THRESHOLD);
    }
}
