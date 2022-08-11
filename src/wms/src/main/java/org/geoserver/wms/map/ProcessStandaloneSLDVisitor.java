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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.GeoServerSLDVisitor;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.opengis.filter.Filter;

/**
 * Processes a standalone SLD document for use in a WMS GetMap request
 *
 * <p>Replacement for org.geoserver.wms.map.GetMapKvpRequestReader#processStandaloneSld, using
 * {@link GeoServerSLDVisitor}
 */
public class ProcessStandaloneSLDVisitor extends GeoServerSLDVisitor {

    final WMS wms;
    final GetMapRequest request;
    final List<MapLayerInfo> layers;
    final List<Style> styles;
    MapLayerInfo currLayer = null;

    public ProcessStandaloneSLDVisitor(final WMS wms, final GetMapRequest request) {
        super(wms.getCatalog(), request.getCrs());
        this.wms = wms;
        this.request = request;

        layers = new ArrayList<>();
        styles = new ArrayList<>();
    }

    @Override
    public void visit(StyledLayerDescriptor sld) {
        try {
            super.visit(sld);
            request.setLayers(layers);
            request.setStyles(styles);
            // Convert various more specific exceptions into service exceptions
        } catch (IllegalStateException | UncheckedIOException | UnsupportedOperationException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public PublishedInfo visitNamedLayerInternal(StyledLayer sl) {
        currLayer = null;
        String layerName = sl.getName();

        LayerGroupInfo groupInfo = wms.getLayerGroupByName(layerName);
        if (groupInfo == null) {
            LayerInfo layerInfo = wms.getLayerByName(layerName);

            if (layerInfo == null) {
                throw new ServiceException("Unknown layer: " + layerName);
            }

            currLayer = new MapLayerInfo(layerInfo);
            if (sl instanceof NamedLayer) {
                NamedLayer namedLayer = ((NamedLayer) sl);
                currLayer.setLayerFeatureConstraints(namedLayer.getLayerFeatureConstraints());
            }
            return layerInfo;
        }
        return groupInfo;
    }

    @Override
    public void visitUserLayerRemoteOWS(UserLayer ul) {
        currLayer = null;
        final FeatureTypeConstraint[] featureConstraints = ul.getLayerFeatureConstraints();
        if (request.getFilter() == null) {
            request.setFilter(new ArrayList<>());
        }
        for (FeatureTypeConstraint featureConstraint : featureConstraints) {
            // grab the filter
            Filter filter = featureConstraint.getFilter();
            if (filter == null) {
                filter = Filter.INCLUDE;
            }
            request.getFilter().add(filter);
        }
    }

    @Override
    public void visitUserLayerInlineFeature(UserLayer ul) {
        currLayer = new MapLayerInfo((LayerInfo) info);
    }

    @Override
    public StyleInfo visitNamedStyleInternal(NamedStyle namedStyle) {
        StyleInfo s;
        s = catalog.getStyleByName(namedStyle.getName());
        if (s == null) {
            String failMessage = "couldn't find style named '" + namedStyle.getName() + "'";
            if (currLayer.getType() == MapLayerInfo.TYPE_RASTER) {
                // hmm, well, the style they specified in the wms request wasn't found.
                // Let's try the default raster style named 'raster'
                s = catalog.getStyleByName("raster");
                if (s == null) {
                    // nope, no default raster style either. Give up.
                    throw new ServiceException(
                            failMessage
                                    + "  Also tried to use "
                                    + "the generic raster style 'raster', but it wasn't available.");
                }
            } else {
                throw new ServiceException(failMessage);
            }
        }

        if (currLayer != null) {
            try {
                layers.add(currLayer);
                styles.add(s.getStyle());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return s;
    }

    @Override
    public void visitUserStyleInternal(Style userStyle) {
        if (currLayer != null) {
            layers.add(currLayer);
            styles.add(userStyle);
        } else if (info != null && info instanceof LayerInfo) {
            layers.add(new MapLayerInfo((LayerInfo) info));
            styles.add(userStyle);
        }
    }
}
