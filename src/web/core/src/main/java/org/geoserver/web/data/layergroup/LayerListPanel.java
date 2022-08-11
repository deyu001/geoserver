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

package org.geoserver.web.data.layergroup;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.data.layer.LayerProvider;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;

/** Base class for a layer listing table with clickable layer names */
public abstract class LayerListPanel extends GeoServerTablePanel<LayerInfo> {

    protected abstract static class LayerListProvider extends LayerProvider {

        private static final long serialVersionUID = -4793382279386643262L;

        @Override
        protected List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(NAME, STORE, WORKSPACE);
        }
    }

    private static final long serialVersionUID = 3638205114048153057L;

    static Property<LayerInfo> NAME = new BeanProperty<>("name", "name");

    static Property<LayerInfo> STORE = new BeanProperty<>("store", "resource.store.name");

    static Property<LayerInfo> WORKSPACE =
            new BeanProperty<>("workspace", "resource.store.workspace.name");

    public LayerListPanel(String id, final WorkspaceInfo workspace) {
        this(
                id,
                new LayerListProvider() {

                    private static final long serialVersionUID = 426375054014475107L;

                    @Override
                    @SuppressWarnings("PMD.UseTryWithResources") // iterator needs to be tested
                    public Iterator<LayerInfo> iterator(final long first, final long count) {
                        Iterator<LayerInfo> iterator = filteredItems((int) first, (int) count);
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

                    @Override
                    protected Filter getFilter() {
                        FilterFactory ff = CommonFactoryFinder.getFilterFactory2();
                        final Filter filter;
                        if (workspace == null) {
                            filter = super.getFilter();
                        } else {
                            filter =
                                    ff.and(
                                            super.getFilter(),
                                            ff.equal(
                                                    ff.property("resource.store.workspace.id"),
                                                    ff.literal(workspace.getId()),
                                                    true));
                        }
                        return filter;
                    }

                    /**
                     * Returns the requested page of layer objects after applying any keyword
                     * filtering set on the page
                     */
                    private Iterator<LayerInfo> filteredItems(Integer first, Integer count) {
                        final Catalog catalog = getCatalog();

                        // global sorting
                        final SortParam<?> sort = getSort();
                        final Property<LayerInfo> property = getProperty(sort);

                        SortBy sortOrder = null;
                        if (sort != null) {
                            if (property instanceof BeanProperty) {
                                final String sortProperty =
                                        ((BeanProperty<LayerInfo>) property).getPropertyPath();
                                sortOrder = sortBy(sortProperty, sort.isAscending());
                            }
                        }

                        final Filter filter = getFilter();
                        // our already filtered and closeable iterator
                        Iterator<LayerInfo> items =
                                catalog.list(LayerInfo.class, filter, first, count, sortOrder);

                        return items;
                    }
                });
    }

    protected LayerListPanel(String id, GeoServerDataProvider<LayerInfo> provider) {
        super(id, provider);
        getTopPager().setVisible(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component getComponentForProperty(
            String id, final IModel<LayerInfo> itemModel, Property<LayerInfo> property) {
        IModel<?> model = property.getModel(itemModel);
        if (NAME == property) {
            return new SimpleAjaxLink<String>(id, (IModel<String>) model) {
                private static final long serialVersionUID = -2968338284881141281L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    LayerInfo layer = itemModel.getObject();
                    handleLayer(layer, target);
                }
            };
        } else {
            return new Label(id, model);
        }
    }

    protected void handleLayer(LayerInfo layer, AjaxRequestTarget target) {}
}
