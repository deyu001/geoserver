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

package org.geoserver.opensearch.eo.store;

import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess.FF;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.data.Join;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Maps joined simple features up to a complex Collection feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCProductFeatureStore extends AbstractMappingStore {

    static final Logger LOGGER = Logging.getLogger(JDBCProductFeatureStore.class);

    String granuleForeignKey;

    public JDBCProductFeatureStore(
            JDBCOpenSearchAccess openSearchAccess, FeatureType collectionFeatureType)
            throws IOException {
        super(openSearchAccess, collectionFeatureType);

        for (AttributeDescriptor ad :
                getFeatureStoreForTable("granule").getSchema().getAttributeDescriptors()) {
            if (ad.getLocalName().equalsIgnoreCase("product_id")) {
                granuleForeignKey = ad.getLocalName();
            }
        }
        if (granuleForeignKey == null) {
            throw new IllegalStateException(
                    "Could not locate a column named 'product'_id in table 'granule'");
        }
    }

    protected SimpleFeatureSource getDelegateCollectionSource() throws IOException {
        return openSearchAccess.getDelegateStore().getFeatureSource(JDBCOpenSearchAccess.PRODUCT);
    }

    @Override
    protected String getMetadataTable() {
        return "product_metadata";
    }

    @Override
    protected String getLinkTable() {
        return "product_ogclink";
    }

    @Override
    protected String getLinkForeignKey() {
        return "product_id";
    }

    @Override
    protected Query mapToSimpleCollectionQuery(Query query, boolean addJoins) throws IOException {
        Query result = super.mapToSimpleCollectionQuery(query, addJoins);

        // join to quicklook table if necessary
        if (addJoins && hasOutputProperty(query, OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, false)) {
            Filter filter = FF.equal(FF.property("id"), FF.property("quicklook.tid"), true);
            Join join = new Join("product_thumb", filter);
            join.setAlias("quicklook");
            result.getJoins().add(join);
        }

        return result;
    }

    @Override
    protected void mapPropertiesToComplex(ComplexFeatureBuilder builder, SimpleFeature fi) {
        // basic mappings
        super.mapPropertiesToComplex(builder, fi);

        // quicklook extraction
        Object metadataValue = fi.getAttribute("quicklook");
        if (metadataValue instanceof SimpleFeature) {
            SimpleFeature quicklookFeature = (SimpleFeature) metadataValue;
            AttributeBuilder ab = new AttributeBuilder(CommonFactoryFinder.getFeatureFactory(null));
            ab.setDescriptor(
                    (AttributeDescriptor)
                            schema.getDescriptor(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME));
            Attribute attribute = ab.buildSimple(null, quicklookFeature.getAttribute("thumb"));
            builder.append(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, attribute);
        }
    }

    @Override
    protected void removeChildFeatures(List<String> collectionIdentifiers) throws IOException {
        super.removeChildFeatures(collectionIdentifiers);

        // remove thumbnail
        List<Filter> filters =
                collectionIdentifiers
                        .stream()
                        .map(id -> FF.equal(FF.property("tid"), FF.literal(id), false))
                        .collect(Collectors.toList());
        Filter metadataFilter = FF.or(filters);
        SimpleFeatureStore thumbStore = getFeatureStoreForTable("product_thumb");
        thumbStore.setTransaction(getTransaction());
        thumbStore.removeFeatures(metadataFilter);

        // remove granules
        filters =
                collectionIdentifiers
                        .stream()
                        .map(id -> FF.equal(FF.property(granuleForeignKey), FF.literal(id), false))
                        .collect(Collectors.toList());
        Filter granulesFilter = FF.or(filters);
        SimpleFeatureStore granuleStore = getFeatureStoreForTable("granule");
        granuleStore.setTransaction(getTransaction());
        granuleStore.removeFeatures(granulesFilter);
    }

    @Override
    protected boolean modifySecondaryAttribute(Name name, Object value, Filter mappedFilter)
            throws IOException {
        if (OpenSearchAccess.GRANULES.equals(name.getLocalPart())) {
            final String tableName = "granule";
            modifySecondaryTable(
                    mappedFilter,
                    value,
                    tableName,
                    id -> FF.equal(FF.property("product_id"), FF.literal(id), true),
                    (id, granulesStore) -> {
                        SimpleFeatureCollection granules = (SimpleFeatureCollection) value;
                        SimpleFeatureBuilder fb =
                                new SimpleFeatureBuilder(granulesStore.getSchema());
                        ListFeatureCollection mappedGranules =
                                new ListFeatureCollection(granulesStore.getSchema());
                        granules.accepts(
                                f -> {
                                    SimpleFeature sf = (SimpleFeature) f;
                                    for (AttributeDescriptor ad :
                                            granulesStore.getSchema().getAttributeDescriptors()) {
                                        fb.set(
                                                ad.getLocalName(),
                                                sf.getAttribute(ad.getLocalName()));
                                    }
                                    fb.set("the_geom", sf.getDefaultGeometry());
                                    fb.set("product_id", id);
                                    SimpleFeature mappedGranule = fb.buildFeature(null);
                                    mappedGranules.add(mappedGranule);
                                },
                                null);
                        return mappedGranules;
                    });

            // this one has been handled
            return true;
        }

        return false;
    }

    @Override
    protected String getThumbnailTable() {
        return "product_thumb";
    }
}
