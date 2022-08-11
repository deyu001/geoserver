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

package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.junit.Before;
import org.junit.Test;

public class JpegOrPngChooserTest {

    private BufferedImage indexed;
    private BufferedImage gray;
    private BufferedImage rgb;
    private BufferedImage rgba;
    private BufferedImage rgba_opaque;
    private BufferedImage rgba_partial;

    @Before
    public void prepareImages() {
        // paletted image
        indexed = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_INDEXED);
        // gray one, no transparency
        gray = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        // opaque rgb
        rgb = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
        // transparent rgba
        rgba = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        // fully opaque rgba
        rgba_opaque = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = rgba_opaque.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 10, 10);
        graphics.dispose();
        // partially transparent rgba
        rgba_partial = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        graphics = rgba_partial.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 10, 5);
        graphics.dispose();
    }

    @Test
    public void testJpegPngImageWriter() {
        assertPng(indexed);
        assertJpeg(gray);
        assertJpeg(rgb);
        assertPng(rgba);
        assertJpeg(rgba_opaque);
        assertPng(rgba_partial);
    }

    private void assertPng(RenderedImage image) {
        JpegOrPngChooser chooser = new JpegOrPngChooser(image);
        assertFalse(chooser.isJpegPreferred());
        assertEquals("image/png", chooser.getMime());
        assertEquals("png", chooser.getExtension());
    }

    private void assertJpeg(RenderedImage image) {
        JpegOrPngChooser chooser = new JpegOrPngChooser(image);
        assertTrue(chooser.isJpegPreferred());
        assertEquals("image/jpeg", chooser.getMime());
        assertEquals("jpeg", chooser.getExtension());
    }
}
