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

package org.geoserver.wfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

/**
 * A feature collection which caches a configurable (and small) amount of features to avoid repeated
 * reads against datastore that cannot optimize count in a count/read cycle typical of WFS requests
 * (count as a FeatureCollection attribute, and then read for feature collection contents)
 *
 * @author Alvaro Huarte
 */
public class FeatureSizeFeatureCollection extends DecoratingSimpleFeatureCollection {

    /** The default feature cache size - disabled by default */
    public static final int DEFAULT_CACHE_SIZE = 0;

    /** The original feature source. */
    protected SimpleFeatureSource featureSource;

    /** The feature cache to manage. */
    private List<SimpleFeature> featureCache;

    /** The original query. */
    private Query query;

    /**
     * Defines the maximum number of feature that will be cached in memory to avoid multiple data
     * reads in case there is no fast {@link FeatureSource#getCount(Query)} implementation for the
     * current query.
     *
     * <p>Useful in particular for stores that do not have any way to perform a fast count against a
     * filtered query, like shapefiles
     */
    private static int FEATURE_CACHE_LIMIT =
            Integer.valueOf(
                    System.getProperty(
                            "org.geoserver.wfs.getfeature.cachelimit",
                            String.valueOf(DEFAULT_CACHE_SIZE)));

    /** Allows to programmatically set the maximum number of cacheable features. */
    public static void setFeatureCacheLimit(int featureCacheLimit) {
        FEATURE_CACHE_LIMIT = featureCacheLimit;
    }

    public FeatureSizeFeatureCollection(
            SimpleFeatureCollection delegate, SimpleFeatureSource source, Query query) {
        super(delegate);
        this.featureSource = source;
        this.query = query;
    }

    /**
     * Wraps the {@link FeatureCollection} into {@link FeatureSizeFeatureCollection} in case the
     * feature caching is enabled and the the features are simple ones
     */
    static FeatureCollection<? extends FeatureType, ? extends Feature> wrap(
            FeatureCollection<? extends FeatureType, ? extends Feature> features,
            FeatureSource<? extends FeatureType, ? extends Feature> source,
            Query gtQuery) {
        if (FEATURE_CACHE_LIMIT > 0 && features.getSchema() instanceof SimpleFeatureType) {
            return new FeatureSizeFeatureCollection(
                    (SimpleFeatureCollection) features, DataUtilities.simple(source), gtQuery);
        } else {
            return features;
        }
    }

    class CachedWrappingFeatureIterator implements SimpleFeatureIterator {

        private List<SimpleFeature> featureCache;
        private int featureIndex = 0;

        public CachedWrappingFeatureIterator(List<SimpleFeature> featureCache) {
            this.featureCache = featureCache;
        }

        @Override
        public boolean hasNext() {
            return featureIndex < featureCache.size();
        }

        @Override
        public SimpleFeature next() {
            return featureCache.get(featureIndex++);
        }

        @Override
        public void close() {
            featureIndex = 0;
        }
    }

    @Override
    public SimpleFeatureIterator features() {
        if (featureCache != null) {
            return new CachedWrappingFeatureIterator(featureCache);
        }
        return super.features();
    }

    @Override
    protected boolean canDelegate(FeatureVisitor visitor) {
        return true;
    }

    @Override
    public int size() {
        if (featureCache != null) {
            return featureCache.size();
        }
        if (FEATURE_CACHE_LIMIT > 0) {
            try {
                // try optimized method, will return -1 if there is no fast way to compute
                int count = featureSource.getCount(query);

                // zero is a legit value, cache and exit
                if (count == 0) {
                    featureCache = new ArrayList<>();
                    return count;
                }
                // fast path, no need to cache
                if (count > 0) {
                    return count;
                }

                // we have to iterate, save to cache to avoid reading data.
                List<SimpleFeature> tempFeatureCache = new ArrayList<>();

                // bean counting like ContentFeatureCollection would do, but with limited
                // size feature caching in the mix
                try (SimpleFeatureIterator it = featureSource.getFeatures(query).features()) {
                    count = 0;
                    while (it.hasNext()) {
                        SimpleFeature feature = it.next();
                        if (tempFeatureCache.size() < FEATURE_CACHE_LIMIT) {
                            tempFeatureCache.add(feature);
                        }
                        count++;
                    }
                    // if the count is below limit, keep the cache, otherwise clear it
                    if (count <= FEATURE_CACHE_LIMIT) {
                        featureCache = tempFeatureCache;
                    } else {
                        tempFeatureCache.clear();
                    }
                    return count;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return super.size();
    }
}
