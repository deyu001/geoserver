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

package org.geoserver.web.admin;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;

/**
 * A Font that contains a preview image
 *
 * @author Miles Jordan, Australian Antarctic Division
 */
@SuppressWarnings("serial")
public class PreviewFont implements Serializable {

    /** The width of the preview image */
    public static final int PREVIEW_IMAGE_WIDTH = 450;

    /** The height of the preview image */
    public static final int PREVIEW_IMAGE_HEIGHT = 16;

    /** The preview image */
    private transient BufferedDynamicImageResource previewImage;

    /** The text for the preview image */
    private final String PREVIEW_TEXT =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** The font to be displayed */
    Font font;

    protected PreviewFont(Font font) {
        this.font = font;
    }

    /**
     * Gets the preview image
     *
     * @return a preview image of the font
     */
    public BufferedDynamicImageResource getPreviewImage() {
        if (previewImage == null) {
            previewImage = createPreviewImage();
        }
        return previewImage;
    }

    /**
     * Generates the preview image for this font
     *
     * @return an image resource
     */
    private BufferedDynamicImageResource createPreviewImage() {

        // convert into integer pixels, set the font and turn on antialiasing
        BufferedImage bi =
                new BufferedImage(
                        PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bi.createGraphics();
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics2D.setFont(font);

        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int stringHeight = fontMetrics.getAscent();

        // background/foreground colours
        graphics2D.setBackground(Color.WHITE);
        graphics2D.setPaint(Color.BLACK);

        // write the name of the font to the graphic. Use the same rendering method used by the
        // WMS (more convoluted, but the only one that can be actually centered within a halo)
        GlyphVector gv =
                font.createGlyphVector(
                        graphics2D.getFontRenderContext(), PREVIEW_TEXT.toCharArray());
        final AffineTransform at =
                AffineTransform.getTranslateInstance(
                        2, PREVIEW_IMAGE_HEIGHT / 2 + stringHeight / 4);
        Shape sample = at.createTransformedShape(gv.getOutline());
        graphics2D.fill(sample);

        // create the image
        BufferedDynamicImageResource generatedImage = new BufferedDynamicImageResource("png");
        generatedImage.setImage(bi);
        // generatedImage.setCacheable(true);

        return generatedImage;
    }

    /** Returns the font name */
    public String getFontName() {
        return font.getFontName();
    }
}
