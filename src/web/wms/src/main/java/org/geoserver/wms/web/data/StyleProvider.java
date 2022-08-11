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

package org.geoserver.wms.web.data;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/** A {@link GeoServerDataProvider} provider for styles */
@SuppressWarnings("serial")
public class StyleProvider extends GeoServerDataProvider<StyleInfo> {

    public static Property<StyleInfo> NAME = new BeanProperty<>("name", "name");

    public static Property<StyleInfo> WORKSPACE = new BeanProperty<>("workspace", "workspace.name");

    static final Property<StyleInfo> MODIFIED_TIMESTAMP =
            new BeanProperty<>("datemodfied", "dateModified");

    static final Property<StyleInfo> CREATED_TIMESTAMP =
            new BeanProperty<>("datecreated", "dateCreated");

    static List<Property<StyleInfo>> PROPERTIES = Arrays.asList(NAME, WORKSPACE);

    public StyleProvider() {
        setSort(new SortParam<>(NAME.getName(), true));
    }

    @Override
    protected List<StyleInfo> getItems() {
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<StyleInfo>> getProperties() {
        List<Property<StyleInfo>> modifiedPropertiesList =
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

    public IModel<StyleInfo> newModel(StyleInfo object) {
        return new StyleDetachableModel(object);
    }

    @Override
    public long size() {
        Filter filter = getFilter();
        int count = getCatalog().count(StyleInfo.class, filter);
        return count;
    }

    @Override
    public int fullSize() {
        Filter filter = Predicates.acceptAll();
        int count = getCatalog().count(StyleInfo.class, filter);
        return count;
    }

    @Override
    public Iterator<StyleInfo> iterator(final long first, final long count) {
        try (CloseableIterator<StyleInfo> iterator = filteredItems((int) first, (int) count)) {
            // don't know how to force wicket to close the iterator, lets return
            // a copy. Shouldn't be much overhead as we're paging
            return Lists.newArrayList(iterator).iterator();
        }
    }

    /**
     * Returns the requested page of layer objects after applying any keyword filtering set on the
     * page
     */
    private CloseableIterator<StyleInfo> filteredItems(Integer first, Integer count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam sort = getSort();
        final Property<StyleInfo> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if (property instanceof BeanProperty) {
                final String sortProperty = ((BeanProperty<StyleInfo>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            }
        }

        final Filter filter = getFilter();
        // our already filtered and closeable iterator
        return catalog.list(StyleInfo.class, filter, first, count, sortOrder);
    }
}
