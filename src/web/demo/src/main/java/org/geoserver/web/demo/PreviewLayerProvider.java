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

package org.geoserver.web.demo;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Provides a filtered, sorted view over the catalog layers.
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class PreviewLayerProvider extends GeoServerDataProvider<PreviewLayer> {

    public static final long DEFAULT_CACHE_TIME = 1;

    public static final String KEY_SIZE = "key.size";

    public static final String KEY_FULL_SIZE = "key.fullsize";

    private final Cache<String, Integer> cache;

    private SizeCallable sizeCaller;

    private FullSizeCallable fullSizeCaller;

    public PreviewLayerProvider() {
        super();
        // Initialization of an inner cache in order to avoid to calculate two times
        // the size() method in a time minor than a second
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

        cache = builder.expireAfterWrite(DEFAULT_CACHE_TIME, TimeUnit.SECONDS).build();
        // Callable which internally calls the size method
        sizeCaller = new SizeCallable();
        // Callable which internally calls the fullSize() method
        fullSizeCaller = new FullSizeCallable();
    }

    public static final Property<PreviewLayer> TYPE = new BeanProperty<>("type", "type");

    public static final AbstractProperty<PreviewLayer> NAME =
            new AbstractProperty<PreviewLayer>("name") {
                @Override
                public Object getPropertyValue(PreviewLayer item) {
                    if (item.layerInfo != null) {
                        return item.layerInfo.prefixedName();
                    }
                    if (item.groupInfo != null) {
                        return item.groupInfo.prefixedName();
                    }
                    return null;
                }
            };

    public static final Property<PreviewLayer> TITLE = new BeanProperty<>("title", "title");

    public static final Property<PreviewLayer> ABSTRACT =
            new BeanProperty<>("abstract", "abstract", false);

    public static final Property<PreviewLayer> KEYWORDS =
            new BeanProperty<>("keywords", "keywords", false);

    public static final Property<PreviewLayer> COMMON = new PropertyPlaceholder<>("commonFormats");

    public static final Property<PreviewLayer> ALL = new PropertyPlaceholder<>("allFormats");

    public static final List<Property<PreviewLayer>> PROPERTIES =
            Arrays.asList(TYPE, TITLE, NAME, ABSTRACT, KEYWORDS, COMMON, ALL);

    @Override
    protected List<PreviewLayer> getItems() {
        // forced to implement this method as its abstract in the super class
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<PreviewLayer>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected IModel<PreviewLayer> newModel(PreviewLayer object) {
        return new PreviewLayerModel(object);
    }

    @Override
    public long size() {
        try {
            if (getKeywords() != null && getKeywords().length > 0) {
                // Use a unique key for different queries
                return cache.get(KEY_SIZE + "." + String.join(",", getKeywords()), sizeCaller);
            }
            return cache.get(KEY_SIZE, sizeCaller);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private int sizeInternal() {
        Filter filter = getFilter();
        int result = getCatalog().count(PublishedInfo.class, filter);
        return result;
    }

    @Override
    public int fullSize() {
        try {
            return cache.get(KEY_FULL_SIZE, fullSizeCaller);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private int fullSizeInternal() {
        Filter filter = Predicates.acceptAll();
        return getCatalog().count(PublishedInfo.class, filter);
    }

    @Override
    public Iterator<PreviewLayer> iterator(final long first, final long count) {
        try (CloseableIterator<PreviewLayer> iterator = filteredItems(first, count)) {
            // don't know how to force wicket to close the iterator, lets return
            // a copy. Shouldn't be much overhead as we're paging
            return Lists.newArrayList(iterator).iterator();
        }
    }

    /**
     * Returns the requested page of layer objects after applying any keyword filtering set on the
     * page
     */
    @SuppressWarnings("resource")
    private CloseableIterator<PreviewLayer> filteredItems(long first, long count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam sort = getSort();
        final Property<PreviewLayer> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if (property instanceof BeanProperty) {
                final String sortProperty =
                        ((BeanProperty<PreviewLayer>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            } else if (property == NAME) {
                sortOrder = sortBy("prefixedName", sort.isAscending());
            }
        }

        Filter filter = getFilter();
        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        CloseableIterator<PublishedInfo> pi =
                catalog.list(PublishedInfo.class, filter, (int) first, (int) count, sortOrder);
        return CloseableIteratorAdapter.transform(
                pi,
                new Function<PublishedInfo, PreviewLayer>() {

                    @Override
                    public PreviewLayer apply(PublishedInfo input) {
                        if (input instanceof LayerInfo) {
                            return new PreviewLayer((LayerInfo) input);
                        } else if (input instanceof LayerGroupInfo) {
                            return new PreviewLayer((LayerGroupInfo) input);
                        }
                        return null;
                    }
                });
    }

    @Override
    protected Filter getFilter() {
        Filter filter = super.getFilter();

        // need to get only advertised and enabled layers
        Filter isLayerInfo = Predicates.isInstanceOf(LayerInfo.class);
        Filter isLayerGroupInfo = Predicates.isInstanceOf(LayerGroupInfo.class);

        Filter enabledFilter = Predicates.equal("resource.enabled", true);
        Filter storeEnabledFilter = Predicates.equal("resource.store.enabled", true);
        Filter advertisedFilter = Predicates.equal("resource.advertised", true);
        Filter enabledLayerGroup = Predicates.equal("enabled", true);
        Filter advertisedLayerGroup = Predicates.equal("advertised", true);
        // return only layer groups that are not containers
        Filter nonContainerGroup =
                Predicates.or(
                        Predicates.equal("mode", LayerGroupInfo.Mode.EO),
                        Predicates.equal("mode", LayerGroupInfo.Mode.NAMED),
                        Predicates.equal("mode", LayerGroupInfo.Mode.OPAQUE_CONTAINER),
                        Predicates.equal("mode", LayerGroupInfo.Mode.SINGLE));

        // Filter for the Layers
        Filter layerFilter =
                Predicates.and(isLayerInfo, enabledFilter, storeEnabledFilter, advertisedFilter);
        // Filter for the LayerGroups
        Filter layerGroupFilter =
                Predicates.and(
                        isLayerGroupInfo,
                        nonContainerGroup,
                        enabledLayerGroup,
                        advertisedLayerGroup);
        // Or filter for merging them
        Filter orFilter = Predicates.or(layerFilter, layerGroupFilter);
        // And between the new filter and the initial filter
        return Predicates.and(filter, orFilter);
    }

    /**
     * Inner class which calls the sizeInternal() method
     *
     * @author Nicpla Lagomarsini geosolutions
     */
    class SizeCallable implements Callable<Integer>, Serializable {
        @Override
        public Integer call() throws Exception {
            return sizeInternal();
        }
    }

    /**
     * Inner class which calls the fullsizeInternal() method
     *
     * @author Nicpla Lagomarsini geosolutions
     */
    class FullSizeCallable implements Callable<Integer>, Serializable {
        @Override
        public Integer call() throws Exception {
            return fullSizeInternal();
        }
    }
}
