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

package org.geoserver.wms.decoration;

import static org.geoserver.template.TemplateUtils.FM_VERSION;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.StringModel;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.WMSMapContent;
import org.geotools.renderer.style.FontCache;
import org.geotools.util.Converters;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

/**
 * A map decoration showing a text message driven by a Freemarker template
 *
 * @author Andrea Aime - GeoSolutions
 */
public class TextDecoration implements MapDecoration {

    /** A logger for this class. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.wms.responses");

    private static Font DEFAULT_FONT = new java.awt.Font("Serif", java.awt.Font.PLAIN, 12);

    String fontFamily;

    boolean fontBold;

    boolean fontItalic;

    float fontSize;

    float haloRadius;

    Color haloColor;

    String messageTemplate;

    Color fontColor;

    @Override
    public void loadOptions(Map<String, String> options) throws Exception {
        // message
        this.messageTemplate = options.get("message");
        if (messageTemplate == null) {
            messageTemplate = "You forgot to set the 'message' option";
        }
        // font
        this.fontFamily = options.get("font-family");
        if (options.get("font-italic") != null) {
            this.fontItalic = Boolean.parseBoolean(options.get("font-italic"));
        }
        if (options.get("font-bold") != null) {
            this.fontBold = Boolean.parseBoolean(options.get("font-bold"));
        }
        if (options.get("font-size") != null) {
            try {
                this.fontSize = Float.parseFloat(options.get("font-size"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'font-size' must be a float.", e);
            }
        }
        if (options.get("font-color") != null) {
            try {
                this.fontColor = MapDecorationLayout.parseColor(options.get("font-color"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'font-color' must be a color in #RRGGBB[AA] format.", e);
            }
        }
        if (fontColor == null) {
            fontColor = Color.BLACK;
        }
        // halo
        if (options.get("halo-radius") != null) {
            try {
                this.haloRadius = Float.parseFloat(options.get("halo-radius"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'halo-radius' must be a float.", e);
            }
        }
        if (options.get("halo-color") != null) {
            try {
                this.haloColor = MapDecorationLayout.parseColor(options.get("halo-color"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "'halo-color' must be a color in #RRGGBB[AA] format.", e);
            }
        }
        if (haloRadius > 0 && haloColor == null) {
            haloColor = Color.WHITE;
        }
    }

    Font getFont() {
        Font font = DEFAULT_FONT;
        if (fontFamily != null) {
            font = FontCache.getDefaultInstance().getFont(fontFamily);
            if (font == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Font " + fontFamily + " not found, falling back on the default");
                font = DEFAULT_FONT;
            }
        }
        if (fontSize > 0) {
            font = font.deriveFont(fontSize);
        }
        if (fontItalic) {
            font = font.deriveFont(Font.ITALIC);
        }
        if (fontBold) {
            font = font.deriveFont(Font.BOLD);
        }
        return font;
    }

    String evaluateMessage(WMSMapContent content) throws IOException, TemplateException {
        final Map env = content.getRequest().getEnv();
        Template t =
                new Template(
                        "name",
                        new StringReader(messageTemplate),
                        TemplateUtils.getSafeConfiguration());
        final BeansWrapper bw = new BeansWrapper(FM_VERSION);
        return FreeMarkerTemplateUtils.processTemplateIntoString(
                t,
                new TemplateHashModel() {

                    @Override
                    public boolean isEmpty() throws TemplateModelException {
                        return env.isEmpty();
                    }

                    @Override
                    public TemplateModel get(String key) throws TemplateModelException {
                        String value = (String) env.get(key);
                        // also allow using the other request parameters
                        if (value == null) {
                            Request request = Dispatcher.REQUEST.get();
                            if (request != null && request.getRawKvp() != null) {
                                Object obj = request.getRawKvp().get(key);
                                value = Converters.convert(obj, String.class);
                                if (obj != null && value == null) {
                                    // try also Spring converters as a fallback, can do some thing
                                    // GT converters
                                    // cannot do (e.g array -> string). Did not create a bridge to
                                    // GT converters as it might
                                    // have global consequences
                                    DefaultConversionService.getSharedInstance()
                                            .convert(obj, String.class);
                                }
                            }
                        }
                        if (value != null) {
                            return new StringModel(value, bw);
                        } else {
                            return null;
                        }
                    }
                });
    }

    @Override
    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) throws Exception {
        Font font = getFont();
        String message = evaluateMessage(mapContent);
        GlyphVector gv = font.createGlyphVector(g2d.getFontRenderContext(), message.toCharArray());
        Shape outline = gv.getOutline();
        Rectangle2D bounds = outline.getBounds2D();
        double width = bounds.getWidth() + haloRadius * 2;
        double height = bounds.getHeight() + haloRadius * 2;
        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }

    @Override
    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContent)
            throws Exception {
        Font font = getFont();
        String message = evaluateMessage(mapContent);
        Font oldFont = g2d.getFont();
        Color oldColor = g2d.getColor();
        Stroke oldStroke = g2d.getStroke();
        try {
            // extract the glyph vector outline (paint like the labelling system does)
            GlyphVector gv =
                    font.createGlyphVector(g2d.getFontRenderContext(), message.toCharArray());
            AffineTransform at =
                    AffineTransform.getTranslateInstance(
                            paintArea.x + haloRadius, paintArea.y + paintArea.height - haloRadius);
            Shape outline = gv.getOutline();
            outline = at.createTransformedShape(outline);

            // draw the halo if necessary
            if (haloRadius > 0) {
                g2d.setColor(haloColor);
                g2d.setStroke(
                        new BasicStroke(
                                2 * haloRadius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.draw(outline);
            }

            // draw the string
            g2d.setFont(font);
            g2d.setColor(fontColor);
            g2d.fill(outline);
        } finally {
            g2d.setColor(oldColor);
            g2d.setFont(oldFont);
            g2d.setStroke(oldStroke);
        }
    }
}
