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

package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geoserver.platform.ServiceException;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Wraps the large number of information normally extracted from a feature info request into a
 * single object, provides facilities to access extra information about the current layer (view
 * parameters, styles)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureInfoRequestParameters {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    int x;

    int y;

    int buffer;

    List<Map<String, String>> viewParams;

    GetMapRequest getMapReq;

    CoordinateReferenceSystem requestedCRS;

    int width;

    int height;

    ReferencedEnvelope bbox;

    double scaleDenominator;

    List<Filter> filters;

    List<SortBy[]> sorts;

    List<MapLayerInfo> layers;

    List<Style> styles;

    List<Object> elevations;

    List<Object> times;

    FilterFactory2 ff;

    private List<List<String>> propertyNames;

    int currentLayer = 0;

    int maxFeatures;

    boolean excludeNodataResults;

    public FeatureInfoRequestParameters(GetFeatureInfoRequest request) {
        // use the layer of the QUERY_LAYERS parameter, not the LAYERS one
        this.layers = request.getQueryLayers();

        this.filters = request.getGetMapRequest().getFilter();
        this.sorts = request.getGetMapRequest().getSortByArrays();
        this.styles = getStyles(request, layers);
        this.x = request.getXPixel();
        this.y = request.getYPixel();
        this.buffer = request.getGetMapRequest().getBuffer();
        this.viewParams = request.getGetMapRequest().getViewParams();
        this.getMapReq = request.getGetMapRequest();
        this.requestedCRS = getMapReq.getCrs(); // optional, may be null
        this.maxFeatures = request.getFeatureCount();
        this.excludeNodataResults = request.isExcludeNodataResults();

        // basic information about the request
        this.width = getMapReq.getWidth();
        this.height = getMapReq.getHeight();
        this.bbox = new ReferencedEnvelope(getMapReq.getBbox(), getMapReq.getCrs());
        this.scaleDenominator = getScaleDenominator(request.getGetMapRequest());
        this.elevations = request.getGetMapRequest().getElevation();
        this.times = request.getGetMapRequest().getTime();
        this.ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

        this.propertyNames = request.getPropertyNames();
    }

    private double getScaleDenominator(GetMapRequest request) {
        final Envelope envelope = request.getBbox();
        final CoordinateReferenceSystem mapcrs = request.getCrs();
        WMSMapContent mapContent = new WMSMapContent(request);
        if (mapcrs != null) {
            mapContent.getViewport().setBounds(new ReferencedEnvelope(envelope, mapcrs));
        } else {
            mapContent
                    .getViewport()
                    .setBounds(new ReferencedEnvelope(envelope, DefaultGeographicCRS.WGS84));
        }
        mapContent.setMapWidth(request.getWidth());
        mapContent.setMapHeight(request.getHeight());
        mapContent.setAngle(request.getAngle());

        return mapContent.getScaleDenominator(true);
    }

    /**
     * Grab the list of styles for each query layer, we'll use them to auto-evaluate the
     * GetFeatureInfo radius if the user did not specify one
     */
    private List<Style> getStyles(final GetFeatureInfoRequest request, List<MapLayerInfo> layers) {
        List<Style> getMapStyles = request.getGetMapRequest().getStyles();
        List<Style> styles = new ArrayList<>();
        List<MapLayerInfo> getMapLayers = request.getGetMapRequest().getLayers();
        for (MapLayerInfo layer : layers) {
            final String targetLayer = layer.getName();
            Style s = null;
            for (int j = 0; j < getMapLayers.size(); j++) {
                if (getMapLayers.get(j).getName().equals(targetLayer)) {
                    if (getMapStyles != null && getMapStyles.size() > j) {
                        s = getMapStyles.get(j);
                    } else {
                        s = getMapLayers.get(j).getDefaultStyle();
                    }
                    break;
                }
            }
            if (s != null) {
                styles.add(s);
            } else {
                throw new ServiceException("Failed to locate style for layer " + targetLayer);
            }
        }
        return styles;
    }

    /** Moves to the next requested layer */
    void nextLayer() {
        currentLayer++;
    }

    /** Returns the current layer */
    public MapLayerInfo getLayer() {
        return layers.get(currentLayer);
    }

    public Style getStyle() {
        return styles.get(currentLayer);
    }

    public Filter getFilter() {
        if (filters == null || filters.size() <= currentLayer) {
            return null;
        } else {
            return filters.get(currentLayer);
        }
    }

    public SortBy[] getSort() {
        if (sorts == null || sorts.size() <= currentLayer) {
            return null;
        } else {
            SortBy[] layerSort = sorts.get(currentLayer);
            final MapLayerInfo layer = layers.get(currentLayer);
            if (layer.getType() == MapLayerInfo.TYPE_VECTOR
                    || layer.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
                // for visual consistency, we must return the information that is on top of the
                // map first, to get this we just need to invert the sort (the code returns the
                // features it encounters first, until FEATURE_COUNT is reached).
                // Failing to do so will result in the user seeing one feature but getting
                // information
                // on those below it (given the sort has been specified, it should be considered
                // as particularly important for the user, unlike a normal info request in which
                // performance and backwards compatibility are favored).
                SortBy[] result = new SortBy[layerSort.length];
                for (int i = 0; i < layerSort.length; i++) {
                    SortBy sb = layerSort[i];
                    SortOrder order = sb.getSortOrder();
                    SortBy reverse =
                            FF.sort(
                                    sb.getPropertyName().getPropertyName(),
                                    order == SortOrder.ASCENDING || order == null
                                            ? SortOrder.DESCENDING
                                            : SortOrder.ASCENDING);
                    result[i] = reverse;
                }
                return result;
            } else {
                // for rasters no need to invert
                return layerSort;
            }
        }
    }

    /** The property names for the specified layer (if any, null otherwise) */
    public String[] getPropertyNames() {
        if (propertyNames == null
                || propertyNames.isEmpty()
                || propertyNames.get(currentLayer) == null) {
            return Query.ALL_NAMES;
        } else {
            List<String> layerPropNames = propertyNames.get(currentLayer);
            return layerPropNames.toArray(new String[layerPropNames.size()]);
        }
    }

    /** The view parameters for the current layer */
    public Map<String, String> getViewParams() {
        if (viewParams == null || viewParams.size() < currentLayer) {
            return null;
        } else {
            return viewParams.get(currentLayer);
        }
    }

    /** The x of the clicked pixel on the raster output map */
    public int getX() {
        return x;
    }

    /** The y of the clicked pixel on the raster output map */
    public int getY() {
        return y;
    }

    /** The buffer, as specified by the user */
    public int getBuffer() {
        return buffer;
    }

    /** The requested coordinate reference system */
    public CoordinateReferenceSystem getRequestedCRS() {
        return requestedCRS;
    }

    /** Pixel width of the requested map */
    public int getWidth() {
        return width;
    }

    /** Pixel width of the requested map */
    public int getHeight() {
        return height;
    }

    /** Real world bounds of the requested map */
    public ReferencedEnvelope getRequestedBounds() {
        return bbox;
    }

    /** The scale denominator of the requested map */
    public double getScaleDenominator() {
        return scaleDenominator;
    }

    /** The elevations in the request (if any) */
    public List<Object> getElevations() {
        return elevations;
    }

    /** The elevations in the request (if any) */
    public List<Object> getTimes() {
        return times;
    }

    /** A filter factory suitable to build filters */
    public FilterFactory2 getFilterFactory() {
        return ff;
    }

    /** The GetMap request wrapped by the GetFeatureInfo one */
    public GetMapRequest getGetMapRequest() {
        return getMapReq;
    }

    /** Excluding nodata from results */
    public boolean isExcludeNodataResults() {
        return excludeNodataResults;
    }
}
