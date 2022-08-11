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

package org.geoserver.gwc.web.layer;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.gwc.web.GWCIconFactory.CachedLayerType;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.layer.TileLayer;

/**
 * Provides a filtered, sorted view over GWC {@link TileLayer}s for {@link CachedLayersPage} using
 * {@link TileLayer} as data view.
 *
 * @author groldan
 */
class CachedLayerProvider extends GeoServerDataProvider<TileLayer> {

    private static final long serialVersionUID = -8599398086587516574L;
    static final Property<TileLayer> TYPE =
            new AbstractProperty<TileLayer>("type") {

                private static final long serialVersionUID = 3215255763580377079L;

                @Override
                public GWCIconFactory.CachedLayerType getPropertyValue(TileLayer item) {
                    return GWCIconFactory.getCachedLayerType(item);
                }

                @Override
                public Comparator<TileLayer> getComparator() {
                    return new Comparator<TileLayer>() {
                        @Override
                        public int compare(TileLayer o1, TileLayer o2) {
                            CachedLayerType r1 = getPropertyValue(o1);
                            CachedLayerType r2 = getPropertyValue(o2);
                            return r1.compareTo(r2);
                        }
                    };
                }
            };

    static final Property<TileLayer> NAME = new BeanProperty<>("name", "name");

    static final Property<TileLayer> QUOTA_LIMIT =
            new AbstractProperty<TileLayer>("quotaLimit") {
                private static final long serialVersionUID = 5091453765439157623L;

                @Override
                public Object getPropertyValue(TileLayer item) {
                    GWC gwc = GWC.get();

                    return gwc.getQuotaLimit(item.getName());
                }
            };

    static final Property<TileLayer> QUOTA_USAGE =
            new AbstractProperty<TileLayer>("quotaUsed") {
                private static final long serialVersionUID = 3503671083744555325L;

                /** @retun the used quota for the tile layer, may be {@code null} */
                @Override
                public Object getPropertyValue(TileLayer item) {
                    GWC gwc = GWC.get();
                    if (gwc.isDiskQuotaEnabled()) {
                        return gwc.getUsedQuota(item.getName());
                    } else {
                        return null;
                    }
                }
            };

    static final Property<TileLayer> BLOBSTORE = new BeanProperty<>("blobstore", "blobStoreId");

    static final Property<TileLayer> ENABLED = new BeanProperty<>("enabled", "enabled");

    static final Property<TileLayer> PREVIEW_LINKS =
            new AbstractProperty<TileLayer>("preview") {
                private static final long serialVersionUID = 4375670219356088450L;

                @Override
                public Object getPropertyValue(TileLayer item) {
                    return item.getName();
                }

                @Override
                public boolean isSearchable() {
                    return false;
                }

                @Override
                public Comparator<TileLayer> getComparator() {
                    return null;
                }
            };

    static final Property<TileLayer> ACTIONS =
            new AbstractProperty<TileLayer>("actions") {
                private static final long serialVersionUID = 247933970378482802L;

                @Override
                public Object getPropertyValue(TileLayer item) {
                    return item.getName();
                }

                @Override
                public boolean isSearchable() {
                    return false;
                }

                @Override
                public Comparator<TileLayer> getComparator() {
                    return null;
                }
            };

    static final List<Property<TileLayer>> PROPERTIES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            TYPE,
                            NAME,
                            QUOTA_LIMIT,
                            QUOTA_USAGE,
                            BLOBSTORE,
                            ENABLED,
                            PREVIEW_LINKS,
                            ACTIONS));

    /** @see org.geoserver.web.wicket.GeoServerDataProvider#getItems() */
    @Override
    protected List<TileLayer> getItems() {
        final GWC gwc = GWC.get();
        List<String> tileLayerNames = new ArrayList<>(gwc.getTileLayerNames());

        // Filtering String in order to avoid Un-Advertised Layers
        Predicate<? super String> predicate =
                new Predicate<String>() {

                    @Override
                    public boolean apply(@Nullable String input) {
                        if (input != null && !input.isEmpty()) {
                            TileLayer layer = GWC.get().getTileLayerByName(input);
                            return layer.isAdvertised();
                        }
                        return false;
                    }
                };
        tileLayerNames = new ArrayList<>(Collections2.filter(tileLayerNames, predicate));

        return Lists.transform(
                tileLayerNames,
                new Function<String, TileLayer>() {

                    @Override
                    public TileLayer apply(String input) {
                        return GWC.get().getTileLayerByName(input);
                    }
                });
    }

    /** @see org.geoserver.web.wicket.GeoServerDataProvider#getProperties() */
    @Override
    protected List<Property<TileLayer>> getProperties() {
        return PROPERTIES;
    }

    /** @see org.geoserver.web.wicket.GeoServerDataProvider#newModel(java.lang.Object) */
    public IModel<TileLayer> newModel(final TileLayer tileLayer) {
        return new TileLayerDetachableModel(tileLayer.getName());
    }

    /** @see org.geoserver.web.wicket.GeoServerDataProvider#getComparator */
    @Override
    protected Comparator<TileLayer> getComparator(SortParam<?> sort) {
        return super.getComparator(sort);
    }
}
