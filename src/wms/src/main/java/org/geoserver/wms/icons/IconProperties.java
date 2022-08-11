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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.styling.Style;
import org.geotools.util.URLs;

/**
 * Stores the values of dynamic style properties needed to generate an icon for a particular
 * feature.
 *
 * @author David Winslow, OpenGeo
 * @author Kevin Smith, OpenGeo
 */
public abstract class IconProperties {
    private IconProperties() {}

    public abstract Double getOpacity();

    public abstract Double getScale();

    public abstract Double getHeading();

    public abstract String href(String baseURL, String workspace, String styleName);

    public abstract Style inject(Style base);

    public abstract Map<String, String> getProperties();

    public abstract String getIconName(Style style);

    public abstract boolean isExternal();

    public static IconProperties generator(
            final Double opacity,
            final Double scale,
            final Double heading,
            final Map<String, String> styleProperties) {
        return new IconProperties() {
            @Override
            public Double getOpacity() {
                return opacity;
            }

            @Override
            public Double getScale() {
                return scale;
            }

            @Override
            public Double getHeading() {
                return heading;
            }

            @Override
            public boolean isExternal() {
                return false;
            }

            @Override
            public String href(String baseURL, String workspace, String styleName) {
                String stylePathFragment;
                if (workspace != null) {
                    stylePathFragment =
                            ResponseUtils.urlEncode(workspace)
                                    + "/"
                                    + ResponseUtils.urlEncode(styleName);
                } else {
                    stylePathFragment = ResponseUtils.urlEncode(styleName);
                }
                return ResponseUtils.buildURL(
                        baseURL,
                        "kml/icon/" + stylePathFragment,
                        styleProperties,
                        URLType.RESOURCE);
            }

            @Override
            public Style inject(Style base) {
                return IconPropertyInjector.injectProperties(base, styleProperties);
            }

            @Override
            public Map<String, String> getProperties() {
                return styleProperties;
            }

            @Override
            public String getIconName(Style style) {
                try {
                    final MessageDigest digest = MessageDigest.getInstance("MD5");
                    digest.update(style.getName().getBytes("UTF-8"));
                    for (Map.Entry<String, String> property : styleProperties.entrySet()) {
                        digest.update(property.getKey().getBytes("UTF-8"));
                        digest.update(property.getValue().getBytes("UTF-8"));
                    }
                    final byte[] hash = digest.digest();
                    final StringBuilder builder = new StringBuilder();
                    for (byte b : hash) {
                        builder.append(String.format("%02x", b));
                    }
                    return builder.toString();
                } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static IconProperties externalReference(
            final Double opacity, final Double scale, final Double heading, final String url) {
        return new IconProperties() {
            @Override
            public Double getOpacity() {
                return opacity;
            }

            @Override
            public Double getScale() {
                return scale;
            }

            @Override
            public Double getHeading() {
                return heading;
            }

            @Override
            public String href(String baseURL, String workspace, String styleName) {

                try {
                    URL target = new URL(url);
                    String graphicProtocol = target.getProtocol();

                    if ("file".equals(graphicProtocol)) {
                        File file = URLs.urlToFile(target);
                        File styles = null;
                        File graphicFile = null;

                        if (file.isAbsolute()) {
                            GeoServerDataDirectory dataDir =
                                    (GeoServerDataDirectory)
                                            GeoServerExtensions.bean("dataDirectory");
                            // we grab the canonical path to make sure we can compare them, no
                            // relative parts in them and so on
                            styles = dataDir.getStyles().dir().getCanonicalFile();
                            file = graphicFile = file.getCanonicalFile();
                            if (file.getAbsolutePath().startsWith(styles.getAbsolutePath())) {
                                // ok, part of the styles directory, extract only the relative path
                                file =
                                        new File(
                                                file.getAbsolutePath()
                                                        .substring(
                                                                styles.getAbsolutePath().length()
                                                                        + 1));
                            } else {
                                // we wont' transform this, other dirs are not published
                                file = null;
                            }

                            // rebuild the icon href accordingly
                            if (file != null && styles != null) {
                                return ResponseUtils.buildURL(
                                        baseURL,
                                        "styles/" + styles.toURI().relativize(graphicFile.toURI()),
                                        null,
                                        URLType.RESOURCE);
                            } else {
                                // we don't know how to handle this then...
                                return null;
                            }
                        }
                        return ResponseUtils.buildURL(
                                baseURL,
                                "styles/" + target.getPath(),
                                Collections.emptyMap(),
                                URLType.RESOURCE);
                    } else if (!("http".equals(graphicProtocol)
                            || "https".equals(graphicProtocol))) {
                        return null;
                    }

                    // return ResponseUtils.buildURL(baseURL, "rest/render/kml/icon/" + styleName,
                    // styleProperties, URLType.RESOURCE);
                    return url;
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public boolean isExternal() {
                return true;
            }

            @Override
            public Style inject(Style base) {
                return IconPropertyInjector.injectProperties(base, Collections.emptyMap());
            }

            @Override
            public Map<String, String> getProperties() {
                return Collections.emptyMap();
            }

            @Override
            public String getIconName(Style style) {
                throw new RuntimeException("An implementation is missing");
            }
        };
    }
}
