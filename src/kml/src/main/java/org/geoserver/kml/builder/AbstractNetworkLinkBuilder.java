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

package org.geoserver.kml.builder;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LookAt;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class AbstractNetworkLinkBuilder {

    static final Logger LOGGER = Logging.getLogger(AbstractNetworkLinkBuilder.class);

    protected KmlEncodingContext context;

    public AbstractNetworkLinkBuilder(KmlEncodingContext context) {
        this.context = context;
    }

    public Kml buildKMLDocument() {
        // prepare kml, document and folder
        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();
        Map formatOptions = context.getRequest().getFormatOptions();
        String kmltitle = (String) formatOptions.get("kmltitle");
        if (kmltitle == null) {
            kmltitle = context.getMapContent().getTitle();
        }
        document.setName(kmltitle);

        // get the callbacks for the document and let them loose
        List<KmlDecorator> decorators = context.getDecoratorsForClass(Document.class);
        for (KmlDecorator decorator : decorators) {
            document = (Document) decorator.decorate(document, context);
            if (document == null) {
                throw new ServiceException(
                        "Coding error in decorator "
                                + decorator
                                + ", document objects cannot be set to null");
            }
        }

        encodeDocumentContents(document);

        return kml;
    }

    abstract void encodeDocumentContents(Document document);

    /**
     * @return the aggregated bounds for all the requested layers, taking into account whether the
     *     whole layer or filtered bounds is used for each layer
     */
    protected ReferencedEnvelope computePerLayerQueryBounds(
            final WMSMapContent context,
            final List<ReferencedEnvelope> target,
            final LookAt lookAt) {

        // no need to compute queried bounds if request explicitly specified the view area
        final boolean computeQueryBounds = lookAt == null;

        ReferencedEnvelope aggregatedBounds;
        try {
            boolean longitudeFirst = true;
            aggregatedBounds = new ReferencedEnvelope(CRS.decode("EPSG:4326", longitudeFirst));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        aggregatedBounds.setToNull();

        final List<Layer> mapLayers = context.layers();
        final List<MapLayerInfo> layerInfos = context.getRequest().getLayers();
        for (int i = 0; i < mapLayers.size(); i++) {
            final Layer Layer = mapLayers.get(i);
            final MapLayerInfo layerInfo = layerInfos.get(i);

            ReferencedEnvelope layerLatLongBbox;
            layerLatLongBbox = computeLayerBounds(Layer, layerInfo, computeQueryBounds);
            try {
                layerLatLongBbox =
                        layerLatLongBbox.transform(
                                aggregatedBounds.getCoordinateReferenceSystem(), true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            target.add(layerLatLongBbox);
            aggregatedBounds.expandToInclude(layerLatLongBbox);
        }
        return aggregatedBounds;
    }

    @SuppressWarnings("rawtypes")
    protected ReferencedEnvelope computeLayerBounds(
            Layer layer, MapLayerInfo layerInfo, boolean computeQueryBounds) {

        final Query layerQuery = layer.getQuery();
        // make sure if layer is going to be filtered, the resulting bounds are obtained instead of
        // the whole bounds
        final Filter filter = layerQuery.getFilter();
        if (layerQuery.getFilter() == null || Filter.INCLUDE.equals(filter)) {
            computeQueryBounds = false;
        }
        if (!computeQueryBounds && !layerQuery.isMaxFeaturesUnlimited()) {
            computeQueryBounds = true;
        }

        ReferencedEnvelope layerLatLongBbox = null;
        if (computeQueryBounds) {
            FeatureSource featureSource = layer.getFeatureSource();
            try {
                CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
                FeatureCollection features = featureSource.getFeatures(layerQuery);
                layerLatLongBbox = features.getBounds();
                layerLatLongBbox = layerLatLongBbox.transform(targetCRS, true);
            } catch (Exception e) {
                LOGGER.info(
                        "Error computing bounds for "
                                + featureSource.getName()
                                + " with "
                                + layerQuery);
            }
        }
        if (layerLatLongBbox == null) {
            try {
                layerLatLongBbox = layerInfo.getLatLongBoundingBox();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return layerLatLongBbox;
    }
}
