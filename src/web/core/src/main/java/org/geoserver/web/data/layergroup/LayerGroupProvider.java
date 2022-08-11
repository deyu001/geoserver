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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/** Provides a table model for listing layer groups */
public class LayerGroupProvider extends GeoServerDataProvider<LayerGroupInfo> {

    private static final long serialVersionUID = 4806818198949114395L;

    public static Property<LayerGroupInfo> NAME = new BeanProperty<>("name", "name");

    public static Property<LayerGroupInfo> WORKSPACE =
            new BeanProperty<>("workspace", "workspace.name");

    static final Property<LayerGroupInfo> MODIFIED_TIMESTAMP =
            new BeanProperty<>("datemodfied", "dateModified");

    static final Property<LayerGroupInfo> CREATED_TIMESTAMP =
            new BeanProperty<>("datecreated", "dateCreated");

    public static Property<LayerGroupInfo> ENABLED =
            new AbstractProperty<LayerGroupInfo>("Enabled") {

                public Boolean getPropertyValue(LayerGroupInfo item) {
                    return Boolean.valueOf(item.isEnabled());
                }
            };

    static List<Property<LayerGroupInfo>> PROPERTIES = Arrays.asList(NAME, WORKSPACE, ENABLED);

    protected LayerGroupProviderFilter groupFilter = null;

    public LayerGroupProvider() {}

    public LayerGroupProvider(LayerGroupProviderFilter groupFilter) {
        this.groupFilter = groupFilter;
    }

    @Override
    protected List<LayerGroupInfo> getItems() {
        List<LayerGroupInfo> groups = getCatalog().getLayerGroups();
        if (groupFilter != null) {
            List<LayerGroupInfo> filtered = new ArrayList<>(groups.size());
            for (LayerGroupInfo group : groups) {
                if (groupFilter.accept(group)) {
                    filtered.add(group);
                }
            }
            groups = filtered;
        }
        return groups;
    }

    @Override
    protected List<Property<LayerGroupInfo>> getProperties() {
        List<Property<LayerGroupInfo>> modifiedPropertiesList =
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

    public IModel<LayerGroupInfo> newModel(LayerGroupInfo object) {
        return new LayerGroupDetachableModel(object);
    }
}
