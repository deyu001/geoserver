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

package org.geoserver.web.data.store;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geotools.data.DataAccessFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.coverage.grid.Format;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;

/** Data providers for the {@link StorePanel} */
@SuppressWarnings("serial")
public class StoreProvider extends GeoServerDataProvider<StoreInfo> {

    static final Property<StoreInfo> DATA_TYPE =
            new AbstractProperty<StoreInfo>("datatype") {

                public IModel<String> getModel(final IModel<StoreInfo> itemModel) {
                    return new AbstractReadOnlyModel<String>() {

                        @Override
                        public String getObject() {
                            StoreInfo si = itemModel.getObject();
                            return (String) getPropertyValue(si);
                        }
                    };
                }

                public Object getPropertyValue(StoreInfo item) {
                    if (item instanceof DataStoreInfo) return "vector";
                    else return "raster";
                }
            };

    static final Property<StoreInfo> WORKSPACE = new BeanProperty<>("workspace", "workspace.name");

    static final Property<StoreInfo> NAME = new BeanProperty<>("name", "name");

    final Property<StoreInfo> TYPE =
            new AbstractProperty<StoreInfo>("type") {

                public Object getPropertyValue(StoreInfo item) {
                    String type = item.getType();
                    if (type != null) {
                        return type;
                    }
                    try {
                        ResourcePool resourcePool = getCatalog().getResourcePool();
                        if (item instanceof DataStoreInfo) {
                            DataStoreInfo dsInfo = (DataStoreInfo) item;
                            DataAccessFactory factory = resourcePool.getDataStoreFactory(dsInfo);
                            if (factory != null) {
                                return factory.getDisplayName();
                            }
                        } else if (item instanceof CoverageStoreInfo) {
                            Format format =
                                    resourcePool.getGridCoverageFormat((CoverageStoreInfo) item);
                            if (format != null) {
                                return format.getName();
                            }
                        }
                    } catch (Exception e) {
                        // fine, we tried
                    }
                    return "?";
                }
            };

    static final Property<StoreInfo> ENABLED = new BeanProperty<>("enabled", "enabled");

    static final Property<StoreInfo> MODIFIED_TIMESTAMP =
            new BeanProperty<>("datemodfied", "dateModified");

    static final Property<StoreInfo> CREATED_TIMESTAMP =
            new BeanProperty<>("datecreated", "dateCreated");

    final List<Property<StoreInfo>> PROPERTIES =
            Arrays.asList(DATA_TYPE, WORKSPACE, NAME, TYPE, ENABLED);

    WorkspaceInfo workspace;

    public StoreProvider() {
        this(null);
    }

    public StoreProvider(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    @Override
    protected List<StoreInfo> getItems() {
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<StoreInfo>> getProperties() {
        List<Property<StoreInfo>> modifiedPropertiesList =
                PROPERTIES.stream().map(c -> c).collect(Collectors.toList());
        // check geoserver properties
        if (GeoServerApplication.get()
                .getGeoServer()
                .getSettings()
                .isShowCreatedTimeColumnsInAdminList())
            modifiedPropertiesList.add(CREATED_TIMESTAMP);
        if (GeoServerApplication.get()
                .getGeoServer()
                .getSettings()
                .isShowModifiedTimeColumnsInAdminList())
            modifiedPropertiesList.add(MODIFIED_TIMESTAMP);
        return modifiedPropertiesList;
    }

    @Override
    protected Comparator<StoreInfo> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }

    public IModel<StoreInfo> newModel(StoreInfo object) {
        return new StoreInfoDetachableModel(object);
    }

    /**
     * A StoreInfo detachable model that holds the store id to retrieve it on demand from the
     * catalog
     */
    static class StoreInfoDetachableModel extends LoadableDetachableModel<StoreInfo> {

        private static final long serialVersionUID = -6829878983583733186L;

        String id;

        public StoreInfoDetachableModel(StoreInfo store) {
            super(store);
            this.id = store.getId();
        }

        @Override
        protected StoreInfo load() {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            return catalog.getStore(id, StoreInfo.class);
        }
    }

    @Override
    public long size() {
        Filter filter = getFilter();
        filter = getWorkspaceFilter(filter);
        int count = getCatalog().count(StoreInfo.class, filter);
        return count;
    }

    @Override
    public int fullSize() {
        Filter filter = Predicates.acceptAll();
        filter = getWorkspaceFilter(filter);
        int count = getCatalog().count(StoreInfo.class, filter);
        return count;
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources") // iterator needs to be tested
    public Iterator<StoreInfo> iterator(final long first, final long count) {
        Iterator<StoreInfo> iterator = filteredItems(first, count);
        if (iterator instanceof CloseableIterator) {
            // don't know how to force wicket to close the iterator, lets return
            // a copy. Shouldn't be much overhead as we're paging
            try {
                return Lists.newArrayList(iterator).iterator();
            } finally {
                CloseableIteratorAdapter.close(iterator);
            }
        } else {
            return iterator;
        }
    }

    /**
     * Returns the requested page of layer objects after applying any keyword filtering set on the
     * page
     */
    private Iterator<StoreInfo> filteredItems(long first, long count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam sort = getSort();
        final Property<StoreInfo> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if (property instanceof BeanProperty) {
                final String sortProperty = ((BeanProperty<StoreInfo>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            } else if (property == ENABLED) {
                sortOrder = sortBy("enabled", sort.isAscending());
            } else if (property == TYPE) {
                sortOrder = sortBy("type", sort.isAscending());
            }
        } else {
            sortOrder = sortBy("name", true);
        }

        final Filter filter = getWorkspaceFilter(getFilter());
        // our already filtered and closeable iterator
        Iterator<StoreInfo> items =
                catalog.list(StoreInfo.class, filter, (int) first, (int) count, sortOrder);

        return items;
    }

    private Filter getWorkspaceFilter(Filter filter) {
        // Filter by workspace if present
        if (workspace != null) {
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            Filter workspaceFilter =
                    ff.equal(ff.property("workspace.id"), ff.literal(workspace.getId()));
            filter = ff.and(filter, workspaceFilter);
        }
        return filter;
    }
}
