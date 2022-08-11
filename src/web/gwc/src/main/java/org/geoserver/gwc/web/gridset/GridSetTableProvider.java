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

package org.geoserver.gwc.web.gridset;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.grid.GridSet;

public abstract class GridSetTableProvider extends GeoServerDataProvider<GridSet> {

    private static final long serialVersionUID = 399110981279814481L;

    static final Property<GridSet> NAME = new BeanProperty<>("name", "name");

    static final Property<GridSet> EPSG_CODE =
            new AbstractProperty<GridSet>("epsg_code") {
                private static final long serialVersionUID = -4311392731568045337L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    return item.getSrs().toString();
                }
            };

    static final Property<GridSet> TILE_DIMENSION =
            new AbstractProperty<GridSet>("tile_dimension") {
                private static final long serialVersionUID = 7300188694215155063L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    return item.getTileWidth() + " x " + item.getTileHeight();
                }
            };

    static final Property<GridSet> ZOOM_LEVELS =
            new AbstractProperty<GridSet>("zoom_levels") {
                private static final long serialVersionUID = 3155098860179765581L;

                @Override
                public Integer getPropertyValue(GridSet item) {
                    return item.getNumLevels(); // this may fail if item.gridLevels is null
                }
            };

    static final Property<GridSet> QUOTA_USED =
            new AbstractProperty<GridSet>("quota_used") {
                private static final long serialVersionUID = 1152149141759317288L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    String gridSetName = item.getName();
                    Quota usedQuotaByGridSet = GWC.get().getUsedQuotaByGridSet(gridSetName);
                    return usedQuotaByGridSet;
                }
            };

    static final Property<GridSet> ACTION_LINK =
            new AbstractProperty<GridSet>("") {
                private static final long serialVersionUID = -7593097569735264194L;

                @Override
                public Object getPropertyValue(GridSet item) {
                    return item.getName();
                }
            };

    public GridSetTableProvider() {}

    @Override
    public abstract List<GridSet> getItems();

    @Override
    protected List<Property<GridSet>> getProperties() {
        return Arrays.asList(NAME, EPSG_CODE, TILE_DIMENSION, ZOOM_LEVELS, QUOTA_USED, ACTION_LINK);
    }

    @Override
    protected Comparator<GridSet> getComparator(final SortParam<?> sort) {
        return super.getComparator(sort);
    }

    @Override
    public IModel<GridSet> newModel(GridSet gridset) {
        String name = gridset.getName();
        return new GridSetDetachableModel(name);
    }
}
