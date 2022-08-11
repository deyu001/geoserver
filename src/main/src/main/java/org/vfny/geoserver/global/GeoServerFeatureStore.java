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

package org.vfny.geoserver.global;

import java.io.IOException;
import java.util.List;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

/**
 * GeoServer wrapper for backend Geotools2 DataStore.
 *
 * <p>Support FeatureSource decorator for FeatureTypeInfo that takes care of mapping the
 * FeatureTypeInfo's FeatureSource with the schema and definition query configured for it.
 *
 * <p>Because GeoServer requires that attributes always be returned in the same order we need a way
 * to smoothly inforce this. Could we use this class to do so? It would need to support writing and
 * locking though.
 *
 * @author Gabriel Rold?n
 * @version $Id$
 */
public class GeoServerFeatureStore extends GeoServerFeatureSource implements SimpleFeatureStore {
    /**
     * Creates a new DEFQueryFeatureLocking object.
     *
     * @param store GeoTools2 FeatureSource
     * @param settings Settings for this store
     */
    GeoServerFeatureStore(FeatureStore<SimpleFeatureType, SimpleFeature> store, Settings settings) {
        super(store, settings);
    }

    /** FeatureStore access (to save casting) */
    SimpleFeatureStore store() {
        return (SimpleFeatureStore) source;
    }

    /** see interface for details. */
    public List<FeatureId> addFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> fc)
            throws IOException {
        FeatureStore<SimpleFeatureType, SimpleFeature> store = store();

        // check if the feature collection needs to be retyped
        if (!store.getSchema().equals(fc.getSchema())) {
            fc = new RetypingFeatureCollection(DataUtilities.simple(fc), store.getSchema());
        }

        return store().addFeatures(fc);
    }

    /** */
    public void removeFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        store().removeFeatures(filter);
    }

    /** */
    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader)
            throws IOException {
        FeatureStore<SimpleFeatureType, SimpleFeature> store = store();

        // check if the feature reader needs to be retyped
        if (!store.getSchema().equals(reader.getFeatureType())) {
            reader = new RetypingFeatureCollection.RetypingFeatureReader(reader, store.getSchema());
        }

        store().setFeatures(reader);
    }

    /** */
    public void setTransaction(Transaction transaction) {
        store().setTransaction(transaction);
    }

    /** */
    public Transaction getTransaction() {
        return store().getTransaction();
    }

    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(name, attributeValue, filter);
    }

    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(names, attributeValues, filter);
    }

    public void modifyFeatures(Name[] attributeNames, Object[] attributeValues, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(attributeNames, attributeValues, filter);
    }

    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(attributeName, attributeValue, filter);
    }
}
