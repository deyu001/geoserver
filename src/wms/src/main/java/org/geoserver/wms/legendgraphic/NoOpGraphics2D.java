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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import org.apache.batik.ext.awt.g2d.AbstractGraphics2D;
import org.apache.batik.ext.awt.g2d.GraphicContext;

/**
 * Fake Graphics2D, pretends to draw but actually does nothing (Very dishonest!)
 *
 * <p>Implementation note, the base class provides a lot of state management ensuring clients do not
 * go NPE when asking for current clip, color, stroke and the like
 *
 * @author Andrea Aime - GeoSolutions
 */
class NoOpGraphics2D extends AbstractGraphics2D {

    public NoOpGraphics2D() {
        super(true);
        this.gc = new GraphicContext();
    }

    @Override
    public void draw(Shape s) {}

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {}

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {}

    @Override
    public void drawString(String str, float x, float y) {}

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {}

    @Override
    public void fill(Shape s) {}

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {

        return null;
    }

    @Override
    public Graphics create() {

        return this;
    }

    @Override
    public void setXORMode(Color c1) {}

    @Override
    public FontMetrics getFontMetrics(Font f) {
        // works also in headless mode
        return new Canvas().getFontMetrics(f);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {}

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return false;
    }

    @Override
    public boolean drawImage(
            Image img, int x, int y, int width, int height, ImageObserver observer) {
        return false;
    }

    @Override
    public void dispose() {}
}
