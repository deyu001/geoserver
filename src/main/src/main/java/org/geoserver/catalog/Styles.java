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

package org.geoserver.catalog;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

/**
 * Provides methods to parse/encode style documents.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class Styles {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.wms");

    static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    /**
     * Encodes the specified SLD as a string.
     *
     * @param sld The sld to encode.
     * @param handler The handler to use to encode.
     * @param ver Version of the style to encode, may be <code>null</code>.
     * @param pretty Whether to format the style.
     * @return The encoded style.
     */
    public static String string(
            StyledLayerDescriptor sld, SLDHandler handler, Version ver, boolean pretty)
            throws IOException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        handler.encode(sld, ver, pretty, bout);

        return new String(bout.toByteArray());
    }

    /**
     * Convenience method to pull a UserSyle from a StyledLayerDescriptor.
     *
     * <p>This method will return the first UserStyle it encounters in the StyledLayerDescriptor
     * tree.
     *
     * @param sld The StyledLayerDescriptor object.
     * @return The UserStyle, or <code>null</code> if no such style could be found.
     */
    public static Style style(StyledLayerDescriptor sld) {
        for (int i = 0; i < sld.getStyledLayers().length; i++) {
            Style[] styles = null;

            if (sld.getStyledLayers()[i] instanceof NamedLayer) {
                NamedLayer layer = (NamedLayer) sld.getStyledLayers()[i];
                styles = layer.getStyles();
            } else if (sld.getStyledLayers()[i] instanceof UserLayer) {
                UserLayer layer = (UserLayer) sld.getStyledLayers()[i];
                styles = layer.getUserStyles();
            }

            if (styles != null) {
                for (Style style : styles) {
                    if (!(style instanceof NamedStyle)) {
                        return style;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Convenience method to wrap a UserStyle in a StyledLayerDescriptor object.
     *
     * <p>This method wraps the UserStyle in a NamedLayer, and wraps the result in a
     * StyledLayerDescriptor.
     *
     * @param style The UserStyle.
     * @return The StyledLayerDescriptor.
     */
    public static StyledLayerDescriptor sld(Style style) {
        StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();

        NamedLayer layer = styleFactory.createNamedLayer();
        layer.setName(style.getName());
        sld.addStyledLayer(layer);

        layer.addStyle(style);

        return sld;
    }

    /**
     * Looks up a style handler by format, file extension, or mime type.
     *
     * @param format The format, file extension, or mime type.
     * @see StyleHandler#getFormat()
     * @see StyleHandler#getFileExtension()
     * @see StyleHandler#mimeType(org.geotools.util.Version)
     */
    public static StyleHandler handler(String format) {
        if (format == null) {
            throw new IllegalArgumentException("Style format must not be null");
        }

        List<StyleHandler> allHandlers = handlers();
        List<StyleHandler> matches = new ArrayList<>();

        // look by format
        for (StyleHandler h : allHandlers) {
            if (format.equalsIgnoreCase(h.getFormat())) {
                matches.add(h);
            }
        }

        if (matches.isEmpty()) {
            // look by mime type
            for (StyleHandler h : allHandlers) {
                for (Version ver : h.getVersions()) {
                    if (h.mimeType(ver).equals(format)) {
                        matches.add(h);
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            // look by file extension
            for (StyleHandler h : allHandlers) {
                if (format.equalsIgnoreCase(h.getFileExtension())) {
                    matches.add(h);
                }
            }
        }

        if (matches.isEmpty()) {
            throw new RuntimeException("No such style handler: format = " + format);
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        List<String> handlerNames =
                Lists.transform(
                        matches,
                        new Function<StyleHandler, String>() {
                            @Nullable
                            @Override
                            public String apply(@Nullable StyleHandler styleHandler) {
                                if (styleHandler == null) {
                                    throw new RuntimeException(
                                            "Got a null style handler, unexpected");
                                }
                                return styleHandler.getName();
                            }
                        });
        throw new IllegalArgumentException(
                "Multiple style handlers: " + handlerNames + " found for format: " + format);
    }

    /** Returns all registered style handlers. */
    public static List<StyleHandler> handlers() {
        return GeoServerExtensions.extensions(StyleHandler.class);
    }
}
