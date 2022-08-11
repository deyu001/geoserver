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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.wms.WMSMapContent;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.URLs;

public class WatermarkDecoration implements MapDecoration {
    /** A logger for this class. */
    private static final Logger LOGGER = Logger.getLogger("org.geoserver.wms.responses");

    public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

    private String imageURL;

    private float opacity = 1.0f;

    /** Transient cache to avoid reloading the same file over and over */
    private static final Map<URL, LogoCacheEntry> logoCache = new SoftValueHashMap<>();

    public void loadOptions(Map<String, String> options) {
        this.imageURL = options.get("url");

        if (options.containsKey("opacity")) {
            try {
                opacity = Float.valueOf(options.get("opacity")) / 100f;
                opacity = Math.max(Math.min(opacity, 1f), 0f);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Invalid opacity value: " + options.get("opacity"), e);
            }
        }
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContent) {
        try {
            BufferedImage logo = getLogo();
            return new Dimension(logo.getWidth(), logo.getHeight());
        } catch (Exception e) {
            return new Dimension(20, 20);
        }
    }

    /** Print the WaterMarks into the graphic2D. */
    public void paint(Graphics2D g2D, Rectangle paintArea, WMSMapContent mapContent)
            throws MalformedURLException, ClassCastException, IOException {
        BufferedImage logo = getLogo();

        if (logo != null) {
            Composite oldComposite = g2D.getComposite();
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

            AffineTransform tx =
                    AffineTransform.getTranslateInstance(paintArea.getX(), paintArea.getY());

            tx.scale(
                    paintArea.getWidth() / logo.getWidth(),
                    paintArea.getHeight() / logo.getHeight());

            g2D.drawImage(logo, tx, null);

            g2D.setComposite(oldComposite);
        }
    }

    protected BufferedImage getLogo() throws IOException {
        BufferedImage logo = null;

        // fully resolve the url (consider data dir)
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        URL url = null;
        try {
            url = new URL(imageURL);

            if (url.getProtocol() == null || url.getProtocol().equals("file")) {
                File file =
                        Resources.find(
                                Resources.fromURL(
                                        Files.asResource(loader.getBaseDirectory()), imageURL),
                                true);
                if (file.exists()) {
                    url = URLs.fileToUrl(file);
                }
            }
        } catch (MalformedURLException e) {
            // could be a relative reference, check if we can find it in the layouts directory
            Resource layouts = loader.get("layouts");
            if (layouts.getType() == Resource.Type.DIRECTORY) {
                Resource image = layouts.get(imageURL);
                if (image.getType() == Resource.Type.RESOURCE) {
                    url = URLs.fileToUrl(image.file());
                }
            }
            if (url == null) {
                // also check from the root of the data dir (backwards compatibility)
                Resource image = loader.get(imageURL);
                if (image.getType() == Resource.Type.RESOURCE) {
                    url = URLs.fileToUrl(image.file());
                }
            }
        }
        if (url == null) {
            return null;
        }

        LogoCacheEntry entry = logoCache.get(url);
        if (entry == null || entry.isExpired()) {
            logo = ImageIO.read(url);
            if (url.getProtocol().equals("file")) {
                entry = new LogoCacheEntry(logo, new File(url.getFile()));
                logoCache.put(url, entry);
            }
        } else {
            logo = entry.getLogo();
        }

        return logo;
    }

    /**
     * Contains an already loaded logo and the tools to check it's up to date compared to the file
     * system
     *
     * @author Andrea Aime - TOPP
     */
    private static class LogoCacheEntry {
        private BufferedImage logo;

        private long lastModified;

        private File file;

        public LogoCacheEntry(BufferedImage logo, File file) {
            this.logo = logo;
            this.file = file;
            lastModified = file.lastModified();
        }

        public boolean isExpired() {
            return file.lastModified() > lastModified;
        }

        public BufferedImage getLogo() {
            return logo;
        }
    }
}
