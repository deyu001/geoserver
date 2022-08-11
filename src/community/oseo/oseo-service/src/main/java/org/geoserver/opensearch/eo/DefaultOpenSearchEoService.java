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

package org.geoserver.opensearch.eo;

import static org.geoserver.opensearch.eo.ComplexFeatureAccessor.value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Predicates;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.PropertyName;

/**
 * Default implementation of {@link OpenSearchEoService}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultOpenSearchEoService implements OpenSearchEoService {

    static final Logger LOGGER = Logging.getLogger(DefaultOpenSearchEoService.class);

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    /* Some mime types for quicklooks */
    private static String JPEG_MIME = "image/jpeg";

    private static String PNG_MIME = "image/png";

    private static String BINARY_MIME = "application/octet-stream ";

    GeoServer geoServer;

    OpenSearchAccessProvider accessProvider;

    public DefaultOpenSearchEoService(
            GeoServer geoServer, OpenSearchAccessProvider accessProvider) {
        this.geoServer = geoServer;
        this.accessProvider = accessProvider;
    }

    @Override
    public OSEODescription description(OSEODescriptionRequest request) throws IOException {
        final OSEOInfo service = getService();
        final String parentId = request.getParentId();

        List<Parameter<?>> searchParameters;
        if (parentId != null) {
            searchParameters = getProductSearchParameters(parentId);
        } else {
            searchParameters = getCollectionSearchParameters();
        }

        return new OSEODescription(request, service, geoServer.getGlobal(), searchParameters);
    }

    public List<Parameter<?>> getProductSearchParameters(final String parentId) throws IOException {
        OSEOInfo service = getService();
        List<Parameter<?>> searchParameters = new ArrayList<>();
        searchParameters.addAll(OpenSearchParameters.getBasicOpensearch(service));
        searchParameters.addAll(OpenSearchParameters.getGeoTimeOpensearch());

        // product search for a given collection, figure out what parameters apply to it
        Feature match = getCollectionByParentIdentifier(parentId);
        Property sensorTypeProperty =
                match.getProperty(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "sensorType"));
        if (sensorTypeProperty == null || !(sensorTypeProperty.getValue() instanceof String)) {
            throw new OWS20Exception("Unknown or missing sensory type " + sensorTypeProperty);
        }
        // go to the classification
        String sensorType = (String) sensorTypeProperty.getValue();
        ProductClass collectionClass = null;
        try {
            collectionClass = ProductClass.getProductClassFromName(geoServer, sensorType);
        } catch (Exception e) {
            LOGGER.warning(
                    "Could not understand sensor type "
                            + sensorType
                            + ", will only return generic product properties");
        }

        OpenSearchAccess access = getOpenSearchAccess();
        FeatureType productSchema = access.getProductSource().getSchema();
        searchParameters.addAll(getSearchParametersByClass(ProductClass.GENERIC, productSchema));
        if (collectionClass != null) {
            searchParameters.addAll(getSearchParametersByClass(collectionClass, productSchema));
        }

        return searchParameters;
    }

    /** Returns the complex feature representing a collection by parentId */
    private Feature getCollectionByParentIdentifier(final String parentId) throws IOException {
        OpenSearchAccess access = getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> collectionSource = access.getCollectionSource();

        // build the query
        final NameImpl identifier = new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier");
        final PropertyIsEqualTo filter =
                FF.equal(FF.property(identifier), FF.literal(parentId), true);
        Query query = new Query(collectionSource.getName().getLocalPart(), filter);
        FeatureCollection<FeatureType, Feature> features = collectionSource.getFeatures(query);

        // get the expected maching feature
        Feature match = DataUtilities.first(features);
        if (match == null) {
            throw new OWS20Exception(
                    "Unknown parentId '" + parentId + "'", OWSExceptionCode.InvalidParameterValue);
        }
        return match;
    }

    private List<Parameter<?>> getSearchParametersByClass(
            ProductClass pc, FeatureType productSchema) {
        List<Parameter<?>> result = new ArrayList<>();
        final String targetNamespace = pc.getNamespace();
        for (PropertyDescriptor pd : productSchema.getDescriptors()) {
            Name name = pd.getName();
            final String propertyNs = name.getNamespaceURI();
            if (targetNamespace.equals(propertyNs)) {
                Parameter parameter =
                        new ParameterBuilder(name.getLocalPart(), pd.getType().getBinding())
                                .prefix(pc.getPrefix())
                                .build();
                result.add(parameter);
            }
        }

        return result;
    }

    public List<Parameter<?>> getCollectionSearchParameters() throws IOException {
        OSEOInfo service = getService();
        List<Parameter<?>> searchParameters = new ArrayList<>();
        searchParameters.addAll(OpenSearchParameters.getBasicOpensearch(service));
        searchParameters.addAll(OpenSearchParameters.getGeoTimeOpensearch());
        searchParameters.addAll(getCollectionEoSearchParameters());
        return searchParameters;
    }

    private Collection<? extends Parameter<?>> getCollectionEoSearchParameters()
            throws IOException {
        final OpenSearchAccess osAccess = getOpenSearchAccess();
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        List<Parameter<?>> result = new ArrayList<>();
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            final Class<?> type = pd.getType().getBinding();
            if (OpenSearchAccess.EO_NAMESPACE.equals(pd.getName().getNamespaceURI())
                    && !Geometry.class.isAssignableFrom(type)) {
                Parameter<?> parameter =
                        new ParameterBuilder(pd.getName().getLocalPart(), type)
                                .prefix("eo")
                                .build();
                result.add(parameter);
            }
        }
        return result;
    }

    @Override
    public SearchResults search(SearchRequest request) throws IOException {
        // grab the right feature source for the request
        final OpenSearchAccess access = getOpenSearchAccess();
        final FeatureSource<FeatureType, Feature> featureSource;
        Query resultsQuery = request.getQuery();
        final String parentId = request.getParentId();
        if (parentId == null) {
            featureSource = access.getCollectionSource();
        } else {
            featureSource = access.getProductSource();

            // need to determine if the collection is primary or virtual
            Feature collection = getCollectionByParentIdentifier(parentId);
            if (Boolean.FALSE.equals(value(collection, "primary"))) {
                // TODO: parse and integrate virtual collection filter
                throw new OWS20Exception("Virtual collection support not implemented yet");
            } else {
                // adding parent id filter for primary collections
                final PropertyName parentIdProperty =
                        FF.property(
                                new NameImpl(OpenSearchAccess.EO_NAMESPACE, "parentIdentifier"));
                PropertyIsEqualTo parentIdFilter =
                        FF.equal(parentIdProperty, FF.literal(parentId), true);
                resultsQuery = new Query(resultsQuery);
                resultsQuery.setFilter(Predicates.and(resultsQuery.getFilter(), parentIdFilter));
            }
        }

        // count
        Query countQuery = new Query(resultsQuery);
        countQuery.setMaxFeatures(Query.DEFAULT_MAX);
        countQuery.setStartIndex(null);
        int totalResults = featureSource.getCount(countQuery);

        // get actual features
        FeatureCollection<FeatureType, Feature> features;
        if (resultsQuery.getMaxFeatures() == 0) {
            // pure count query
            features =
                    new ListComplexFeatureCollection(
                            featureSource.getSchema(), Collections.emptyList());
        } else {
            features = featureSource.getFeatures(resultsQuery);
        }

        SearchResults results = new SearchResults(request, features, totalResults);

        return results;
    }

    OpenSearchAccess getOpenSearchAccess() throws IOException {
        return accessProvider.getOpenSearchAccess();
    }

    private OSEOInfo getService() {
        return this.geoServer.getService(OSEOInfo.class);
    }

    @Override
    public MetadataResults metadata(MetadataRequest request) throws IOException {
        OpenSearchAccess access = getOpenSearchAccess();

        // build the query
        Query query = queryByIdentifier(request.getId());
        query.setProperties(Arrays.asList(FF.property(OpenSearchAccess.METADATA_PROPERTY_NAME)));

        // run it
        FeatureSource<FeatureType, Feature> source;
        if (request.getParentId() == null) {
            // collection request
            source = access.getCollectionSource();
        } else {
            source = access.getProductSource();
        }
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(query);

        // get the metadata from the feature
        String metadata =
                (String)
                        getPropertyFromFirstFeature(
                                features, OpenSearchAccess.METADATA_PROPERTY_NAME);
        if (metadata == null) {
            throw new OWS20Exception(
                    "Could not locate the requested metadata for uid = "
                            + request.getId()
                            + " and parentId = "
                            + request.getParentId());
        }

        return new MetadataResults(request, metadata);
    }

    @Override
    public QuicklookResults quicklook(QuicklookRequest request) throws IOException {
        OpenSearchAccess access = getOpenSearchAccess();

        // build the query
        Query query = queryByIdentifier(request.getId());
        query.setProperties(Arrays.asList(FF.property(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME)));

        // run it
        FeatureSource<FeatureType, Feature> source;
        if (request.getParentId() == null) {
            // collection request
            source = access.getCollectionSource();
        } else {
            source = access.getProductSource();
        }
        FeatureCollection<FeatureType, Feature> features = source.getFeatures(query);

        byte[] payload =
                (byte[])
                        getPropertyFromFirstFeature(
                                features, OpenSearchAccess.QUICKLOOK_PROPERTY_NAME);
        if (payload == null) {
            throw new OWS20Exception(
                    "Could not locate the quicklook for uid = "
                            + request.getId()
                            + " and parentId = "
                            + request.getParentId());
        }

        return new QuicklookResults(request, payload, guessImageMimeType(payload));
    }

    /** Used to guess the mime type of an encoded image until we start storing the mime in the db */
    public static String guessImageMimeType(byte[] payload) {
        // guesses jpeg and png by the magic number
        if (payload.length >= 4
                && //
                (payload[0] == (byte) 0xFF)
                && //
                (payload[1] == (byte) 0xD8)
                && //
                (payload[2] == (byte) 0xFF)
                && //
                (payload[3] == (byte) 0xE0)) {
            return JPEG_MIME;
        } else if (payload.length >= 8
                && //
                (payload[0] == (byte) 0x89)
                && //
                (payload[1] == (byte) 0x50)
                && //
                (payload[2] == (byte) 0x4E)
                && //
                (payload[3] == (byte) 0x47)
                && //
                (payload[4] == (byte) 0x0D)
                && //
                (payload[5] == (byte) 0x0A)
                && //
                (payload[6] == (byte) 0x1A)
                && //
                (payload[7] == (byte) 0x0A)) {
            return PNG_MIME;
        } else {
            return BINARY_MIME;
        }
    }

    private Object getPropertyFromFirstFeature(
            FeatureCollection<FeatureType, Feature> features, Name propertyName) {
        Feature feature = DataUtilities.first(features);
        Property property;
        Object value;
        if (feature == null
                || //
                ((property = feature.getProperty(propertyName)) == null)
                || //
                ((value = property.getValue()) == null)) {
            return null;
        }

        return value;
    }

    private Query queryByIdentifier(String identifier) {
        Query query = new Query();
        PropertyName idProperty =
                FF.property(new NameImpl(OpenSearchAccess.EO_NAMESPACE, "identifier"));
        final PropertyIsEqualTo idFilter = FF.equal(idProperty, FF.literal(identifier), true);
        query.setFilter(idFilter);
        return query;
    }
}
