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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.featureinfo.FeatureCollectionDecorator;
import org.geoserver.wms.featureinfo.LayerIdentifier;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;

/**
 * WMS GetFeatureInfo operation
 *
 * @author Gabriel Roldan
 */
public class GetFeatureInfo {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public FeatureCollectionType run(final GetFeatureInfoRequest request) throws ServiceException {
        List<FeatureCollection> results;
        try {
            results = execute(request);
        } catch (ServiceException se) {
            se.printStackTrace();
            throw se;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("Internal error occurred", e);
        }
        return buildResults(results);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private FeatureCollectionType buildResults(List<FeatureCollection> results) {

        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());
        result.getFeature().addAll(results);

        return result;
    }

    @SuppressWarnings("rawtypes")
    private List<FeatureCollection> execute(GetFeatureInfoRequest request) throws Exception {
        final List<MapLayerInfo> requestedLayers = request.getQueryLayers();
        FeatureInfoRequestParameters requestParams = new FeatureInfoRequestParameters(request);

        List<FeatureCollection> results = new ArrayList<>(requestedLayers.size());

        int maxFeatures = request.getFeatureCount();
        List<LayerIdentifier> identifiers = GeoServerExtensions.extensions(LayerIdentifier.class);
        for (final MapLayerInfo layer : requestedLayers) {
            try {
                LayerIdentifier<?> identifier = getLayerIdentifier(layer, identifiers);
                List<FeatureCollection> identifiedCollections =
                        identifier.identify(requestParams, maxFeatures);
                if (identifiedCollections != null) {
                    for (FeatureCollection identifierCollection : identifiedCollections) {
                        FeatureCollection fc =
                                selectProperties(requestParams, identifierCollection);
                        maxFeatures = addToResults(fc, results, layer, request, maxFeatures);
                    }

                    // exit when we have collected enough features
                    if (maxFeatures <= 0) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new ServiceException(
                        "Failed to run GetFeatureInfo on layer " + layer.getName(), e);
            }

            requestParams.nextLayer();
        }
        return results;
    }

    private LayerIdentifier getLayerIdentifier(
            MapLayerInfo layer, List<LayerIdentifier> identifiers) {
        for (LayerIdentifier identifier : identifiers) {
            if (identifier.canHandle(layer)) {
                return identifier;
            }
        }

        throw new ServiceException(
                "Could not find any identifier that can handle layer "
                        + layer.getLayerInfo().prefixedName()
                        + " among these identifiers: "
                        + identifiers);
    }

    private int addToResults(
            FeatureCollection collection,
            List<FeatureCollection> results,
            final MapLayerInfo layer,
            GetFeatureInfoRequest request,
            int maxFeatures) {
        if (collection != null) {
            if (!(collection.getSchema() instanceof SimpleFeatureType)) {
                // put wrapper around it with layer name
                Name name =
                        new NameImpl(
                                layer.getFeature().getNamespace().getName(),
                                layer.getFeature().getName());
                collection = new FeatureCollectionDecorator(name, collection);
            }

            int size = collection.size();
            if (size != 0) {

                // HACK HACK HACK
                // For complex features, we need the targetCrs and version in scenario where we have
                // a top level feature that does not contain a geometry(therefore no crs) and has a
                // nested feature that contains geometry as its property.Furthermore it is possible
                // for each nested feature to have different crs hence we need to reproject on each
                // feature accordingly.
                // This is a Hack, this information should not be passed through feature type
                // appschema will need to remove this information from the feature type again
                if (!(collection instanceof SimpleFeatureCollection)) {
                    collection
                            .getSchema()
                            .getUserData()
                            .put("targetCrs", request.getGetMapRequest().getCrs());
                    collection.getSchema().getUserData().put("targetVersion", "wms:getfeatureinfo");
                }

                results.add(collection);

                // don't return more than FEATURE_COUNT
                maxFeatures -= size;
                if (maxFeatures <= 0) {
                    return 0;
                }
            }
        }
        return maxFeatures;
    }

    protected FeatureCollection selectProperties(
            FeatureInfoRequestParameters params, FeatureCollection collection) throws IOException {
        // no general way to reduce attribute names in complex features yet
        String[] names = params.getPropertyNames();
        if (names != Query.ALL_NAMES && collection instanceof SimpleFeatureCollection) {
            SimpleFeatureCollection sfc = (SimpleFeatureCollection) collection;
            SimpleFeatureType source = sfc.getSchema();
            SimpleFeatureType target = SimpleFeatureTypeBuilder.retype(source, names);
            if (!target.equals(source)) {
                return new RetypingFeatureCollection(sfc, target);
            }
        }

        return collection;
    }
}
