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

package org.geoserver.web.data.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.data.importer.LayerResource.LayerStatus;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.feature.NameImpl;
import org.geotools.ows.wms.Layer;

/**
 * Provides a list of resources for a specific data store
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class WMSLayerProvider extends GeoServerDataProvider<LayerResource> {

    public static final Property<LayerResource> STATUS = new BeanProperty<>("status", "status");
    public static final Property<LayerResource> NAME = new BeanProperty<>("name", "localName");
    public static final Property<LayerResource> ACTION = new PropertyPlaceholder<>("action");

    public static final List<Property<LayerResource>> PROPERTIES =
            Arrays.asList(NAME, ACTION, STATUS);

    String storeId;
    List<LayerResource> items;

    @Override
    protected List<LayerResource> getItems() {
        if (items == null) {
            // return an empty list in case we still don't know about the store
            if (storeId == null) return new ArrayList<>();

            // else, grab the resource list
            try {
                List<LayerResource> result;
                StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);

                Map<String, LayerResource> resources = new HashMap<>();
                WMSStoreInfo wmsInfo = (WMSStoreInfo) store;

                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                List<Layer> layers = wmsInfo.getWebMapServer(null).getCapabilities().getLayerList();
                for (Layer l : layers) {
                    if (l.getName() == null) {
                        continue;
                    }

                    resources.put(l.getName(), new LayerResource(new NameImpl(l.getName())));
                }

                // lookup all configured layers, mark them as published in the resources
                List<ResourceInfo> configuredTypes =
                        getCatalog().getResourcesByStore(store, ResourceInfo.class);
                for (ResourceInfo type : configuredTypes) {
                    // compare with native name, which is what the DataStore provides through
                    // getNames()
                    // above
                    LayerResource resource = resources.get(type.getNativeName());
                    if (resource != null) resource.setStatus(LayerStatus.PUBLISHED);
                }
                result = new ArrayList<>(resources.values());

                // return by natural order
                Collections.sort(result);
                items = result;
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not list layers for this store, "
                                + "an error occurred retrieving them: "
                                + e.getMessage(),
                        e);
            }
        }
        return items;
    }

    public void updateLayerOrder() {
        Collections.sort(items);
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    @Override
    protected List<Property<LayerResource>> getProperties() {
        return PROPERTIES;
    }
}
