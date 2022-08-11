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

package org.geoserver.wms.style;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.geotools.brewer.styling.builder.FeatureTypeStyleBuilder;
import org.geotools.brewer.styling.builder.StyledLayerDescriptorBuilder;
import org.geotools.brewer.styling.filter.expression.FunctionBuilder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.StyledLayerDescriptor;
import org.opengis.filter.FilterFactory;

/**
 * Support class to parse a palette file and turn it into a dynamic style. The palette file syntax
 * can contain rows with:
 *
 * <ul>
 *   <li>A comment, started with %
 *   <li>A #RRGGBB or 0xRRGGBB color
 *   <li>A #AARRGGBB or 0xAARRGGBB color
 *
 * @author Andrea Aime
 */
public class PaletteParser {

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    /** Number of colors in output, between 1 and 254 (excludes before and after color) */
    public static final String NUMCOLORS = "NUMCOLORBANDS";

    /** If true a logarithmic progression palette is generated */
    public static final String LOGSCALE = "LOGSCALE";

    /** Color after palette */
    public static final String COLOR_AFTER = "ABOVEMAXCOLOR";

    /** Color before palette */
    public static final String COLOR_BEFORE = "BELOWMINCOLOR";

    /** Range max value */
    public static final String RANGE_MAX = "COLORSCALERANGE_MAX";

    /** Range min value */
    public static final String RANGE_MIN = "COLORSCALERANGE_MIN";

    /** Opacity */
    public static final String OPACITY = "OPACITY";

    List<Color> parseColorMap(Reader reader) throws IOException {
        return new BufferedReader(reader)
                .lines()
                .filter(this::isNotEmpty)
                .map(String::trim)
                .map(this::toColor)
                .collect(Collectors.toList());
    }

    StyledLayerDescriptor parseStyle(Reader reader) throws IOException {
        List<Color> colorMap = parseColorMap(reader);
        StyledLayerDescriptor sld = toDynamicColorMapStyle(colorMap);
        return sld;
    }

    StyledLayerDescriptor toDynamicColorMapStyle(List<Color> colorMap) {
        StyledLayerDescriptorBuilder sldBuilder = new StyledLayerDescriptorBuilder();
        final FeatureTypeStyleBuilder fts = sldBuilder.namedLayer().style().featureTypeStyle();
        fts.rule().raster();

        FunctionBuilder dcmFunction = new FunctionBuilder();
        dcmFunction.name("ras:DynamicColorMap");
        dcmFunction.function("parameter").literal("data").end();
        dcmFunction
                .function("parameter")
                .literal("opacity")
                .function("env")
                .literal(OPACITY)
                .literal(1f)
                .end()
                .end();
        FunctionBuilder paramFunction = dcmFunction.function("parameter");
        FunctionBuilder cmFunction = paramFunction.literal("colorRamp").function("colormap");
        cmFunction.literal(toColorExpressions(colorMap));
        cmFunction
                .function("env")
                .literal(RANGE_MIN)
                .function("bandStats")
                .literal(0)
                .literal("minimum")
                .end()
                .end();
        cmFunction
                .function("env")
                .literal(RANGE_MAX)
                .function("bandStats")
                .literal(0)
                .literal("maximum")
                .end()
                .end();
        cmFunction.function("env").literal(COLOR_BEFORE).literal("rgba(255,255,255,0)").end();
        cmFunction.function("env").literal(COLOR_AFTER).literal("rgba(255,255,255,0)").end();
        cmFunction.function("env").literal(LOGSCALE).literal("false").end();
        cmFunction.function("env").literal(NUMCOLORS).literal("254").end();
        cmFunction.end();
        paramFunction.end();

        fts.transformation(dcmFunction.build());

        StyledLayerDescriptor sld = sldBuilder.build();
        return sld;
    }

    private String toColorExpressions(List<Color> colorMap) {
        return colorMap.stream().map(this::toColorSpec).collect(Collectors.joining(";"));
    }

    private String toColorSpec(Color c) {
        if (c.getAlpha() == 255) {
            return String.format("rgb(%d,%d,%d)", c.getRed(), c.getGreen(), c.getBlue());
        } else {
            return String.format(
                    Locale.ENGLISH,
                    "rgba(%d,%d,%d,%.2f)",
                    c.getRed(),
                    c.getGreen(),
                    c.getBlue(),
                    c.getAlpha() / 255.);
        }
    }

    private boolean isNotEmpty(String s) {
        if (s == null) {
            return false;
        }
        s = s.trim();
        return !s.isEmpty() && !"extend".equals(s) && !s.startsWith("%");
    }

    private Color toColor(String s) {
        try {
            final int length = s.length();
            if (length == 7 || length == 8) {
                // #RRGGBB or 0xRRGGBB
                return Color.decode(s);
            } else {
                if (s.startsWith("#")) {
                    s = s.substring(1);
                } else if (s.startsWith("0x")) {
                    s = s.substring(2);
                }
                return new Color(
                        Integer.valueOf(s.substring(2, 4), 16),
                        Integer.valueOf(s.substring(4, 6), 16),
                        Integer.valueOf(s.substring(6, 8), 16),
                        Integer.valueOf(s.substring(0, 2), 16));
            }
        } catch (Exception e) {
            throw new InvalidColorException(s, e);
        }
    }

    @SuppressWarnings("serial")
    public static class InvalidColorException extends RuntimeException {

        public InvalidColorException(String color, Throwable cause) {
            super(
                    "Invalid color '"
                            + color
                            + "', supported syntaxes are #RRGGBB, 0xRRGGBB, #AARRGGBB and 0xAARRGGBB",
                    cause);
        }
    }
}
