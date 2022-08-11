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

package org.geoserver.feature.retype;

import java.awt.RenderingHints;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Renaming wrapper for a {@link FeatureSource} instance, to be used along with {@link
 * RetypingDataStore}
 */
public class RetypingFeatureSource implements SimpleFeatureSource {

    SimpleFeatureSource wrapped;

    FeatureTypeMap typeMap;

    RetypingDataStore store;

    Map<FeatureListener, FeatureListener> listeners = new HashMap<>();

    /**
     * Builds a retyping wrapper
     *
     * @param targetSchema The target schema can have a different name and less attributes than the
     *     original one
     */
    public static SimpleFeatureSource getRetypingSource(
            SimpleFeatureSource wrapped, SimpleFeatureType targetSchema) throws IOException {
        FeatureTypeMap map = new FeatureTypeMap(wrapped.getSchema(), targetSchema);

        if (wrapped instanceof SimpleFeatureLocking) {
            return new RetypingFeatureLocking((SimpleFeatureLocking) wrapped, map);
        } else if (wrapped instanceof SimpleFeatureStore) {
            return new RetypingFeatureStore((SimpleFeatureStore) wrapped, map);
        } else {
            return new RetypingFeatureSource(wrapped, map);
        }
    }

    RetypingFeatureSource(
            RetypingDataStore ds, SimpleFeatureSource wrapped, FeatureTypeMap typeMap) {
        this.store = ds;
        this.wrapped = wrapped;
        this.typeMap = typeMap;
    }

    RetypingFeatureSource(SimpleFeatureSource wrapped, final FeatureTypeMap typeMap)
            throws IOException {
        this.wrapped = wrapped;
        this.typeMap = typeMap;
        this.store =
                new RetypingDataStore((DataStore) wrapped.getDataStore()) {
                    @Override
                    protected String transformFeatureTypeName(String originalName) {
                        if (typeMap.getOriginalName().equals(originalName)) {
                            // rename
                            return typeMap.getName();
                        } else if (typeMap.getName().equals(originalName)) {
                            // hide
                            return null;
                        } else {
                            return originalName;
                        }
                    }

                    @Override
                    protected SimpleFeatureType transformFeatureType(SimpleFeatureType original)
                            throws IOException {
                        if (typeMap.getOriginalName().equals(original.getTypeName())) {
                            return typeMap.featureType;
                        } else {
                            return super.transformFeatureType(original);
                        }
                    }

                    @Override
                    public String[] getTypeNames() throws IOException {
                        // Populate local hashmaps with new values.
                        Map<String, FeatureTypeMap> forwardMapLocal = new ConcurrentHashMap<>();
                        Map<String, FeatureTypeMap> backwardsMapLocal = new ConcurrentHashMap<>();

                        forwardMapLocal.put(typeMap.getOriginalName(), typeMap);
                        backwardsMapLocal.put(typeMap.getName(), typeMap);

                        // Replace the member variables.
                        forwardMap = forwardMapLocal;
                        backwardsMap = backwardsMapLocal;

                        return new String[] {typeMap.getName()};
                    }
                };
    }

    /**
     * Returns the same name than the feature type (ie, {@code getSchema().getName()} to honor the
     * simple feature land common practice of calling the same both the Features produces and their
     * types
     *
     * @since 1.7
     * @see FeatureSource#getName()
     */
    public Name getName() {
        return getSchema().getName();
    }

    public void addFeatureListener(FeatureListener listener) {
        FeatureListener wrapper = new WrappingFeatureListener(this, listener);
        listeners.put(listener, wrapper);
        wrapped.addFeatureListener(wrapper);
    }

    public void removeFeatureListener(FeatureListener listener) {
        FeatureListener wrapper = listeners.get(listener);
        if (wrapper != null) {
            wrapped.removeFeatureListener(wrapper);
            listeners.remove(listener);
        }
    }

    public ReferencedEnvelope getBounds() throws IOException {
        // not fully correct if we use this to shave attributes too, but this is
        // not in the scope now
        return wrapped.getBounds();
    }

    public ReferencedEnvelope getBounds(Query query) throws IOException {
        // not fully correct if we use this to shave attributes too, but this is
        // not in the scope now
        return wrapped.getBounds(store.retypeQuery(query, typeMap));
    }

    public int getCount(Query query) throws IOException {
        return wrapped.getCount(store.retypeQuery(query, typeMap));
    }

    public DataStore getDataStore() {
        return store;
    }

    public SimpleFeatureCollection getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        if (query.getTypeName() == null) {
            query = new Query(query);
            query.setTypeName(typeMap.getName());
        } else if (!typeMap.getName().equals(query.getTypeName())) {
            throw new IOException(
                    "Cannot query this feature source with "
                            + query.getTypeName()
                            + " since it serves only "
                            + typeMap.getName());
        }

        // GEOS-3210, if the query specifies a subset of property names we need to take that into
        // account
        SimpleFeatureType target = typeMap.getFeatureType(query);
        return new RetypingFeatureCollection(
                wrapped.getFeatures(store.retypeQuery(query, typeMap)), target);
    }

    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return getFeatures(new Query(typeMap.getName(), filter));
    }

    public SimpleFeatureType getSchema() {
        return typeMap.getFeatureType();
    }

    public Set<RenderingHints.Key> getSupportedHints() {
        return wrapped.getSupportedHints();
    }

    public ResourceInfo getInfo() {
        return wrapped.getInfo();
    }

    public QueryCapabilities getQueryCapabilities() {
        return wrapped.getQueryCapabilities();
    }
}
