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

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.UserLayer;
import org.geotools.util.Version;
import org.opengis.filter.FilterFactory;
import org.xml.sax.EntityResolver;

/** Test style handler based on properties format. */
public class PropertyStyleHandler extends StyleHandler {

    public static final String FORMAT = "psl";
    public static final String MIMETYPE = "application/prs.gs.psl";

    StyleFactory styleFactory;
    FilterFactory filterFactory;

    public PropertyStyleHandler() {
        super("Property", FORMAT);
        styleFactory = CommonFactoryFinder.getStyleFactory();
        filterFactory = CommonFactoryFinder.getFilterFactory();
    }

    @Override
    public String getFileExtension() {
        return "properties";
    }

    @Override
    public String mimeType(Version version) {
        return MIMETYPE;
    }

    @Override
    public StyledLayerDescriptor parse(
            Object input,
            Version version,
            ResourceLocator resourceLocator,
            EntityResolver enityResolver)
            throws IOException {
        Properties p = new Properties();
        try (Reader reader = toReader(input)) {
            p.load(reader);
        }

        Color color = color(p.getProperty("color"), Color.BLACK);
        Symbolizer sym = null;

        String type = p.getProperty("type");
        if ("line".equalsIgnoreCase(type)) {
            LineSymbolizer ls = styleFactory.createLineSymbolizer();
            ls.setStroke(
                    styleFactory.createStroke(
                            filterFactory.literal(color), filterFactory.literal(2)));

            sym = ls;
        } else if ("polygon".equalsIgnoreCase(type)) {
            PolygonSymbolizer ps = styleFactory.createPolygonSymbolizer();
            ps.setFill(styleFactory.createFill(filterFactory.literal(color)));

            sym = ps;
        } else if ("raster".equalsIgnoreCase(type)) {
            RasterSymbolizer rs = styleFactory.createRasterSymbolizer();
            sym = rs;
        } else {
            Mark mark = styleFactory.createMark();
            mark.setFill(styleFactory.createFill(filterFactory.literal(color)));

            PointSymbolizer ps = styleFactory.createPointSymbolizer();
            ps.setGraphic(styleFactory.createDefaultGraphic());
            ps.getGraphic().graphicalSymbols().add(mark);

            sym = ps;
        }

        Rule r = styleFactory.createRule();
        r.symbolizers().add(sym);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(r);

        Style s = styleFactory.createStyle();
        s.featureTypeStyles().add(fts);

        UserLayer l = styleFactory.createUserLayer();
        l.userStyles().add(s);

        StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();
        sld.layers().add(l);
        return sld;
    }

    Color color(String color, Color def) {
        if (color == null) {
            return def;
        }

        return new Color(
                Integer.valueOf(color.substring(0, 2), 16),
                Integer.valueOf(color.substring(2, 4), 16),
                Integer.valueOf(color.substring(4, 6), 16));
    }

    @Override
    public void encode(
            StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        Properties props = new Properties();
        for (Symbolizer sym : SLD.symbolizers(Styles.style(sld))) {
            if (sym instanceof PointSymbolizer) {
                props.put("type", "point");
            } else if (sym instanceof LineSymbolizer) {
                props.put("type", "line");
            } else if (sym instanceof PolygonSymbolizer) {
                props.put("type", "polygon");
            } else if (sym instanceof RasterSymbolizer) {
                props.put("type", "raster");
            }
        }

        props.store(output, null);
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        return Collections.emptyList();
    }

    @Override
    public boolean supportsEncoding(Version version) {
        return true;
    }
}
