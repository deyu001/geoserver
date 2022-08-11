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

package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.feature.NameImpl;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.Name;

/**
 * Provides a list of resources for a specific data store
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class NewLayerPageProvider extends GeoServerDataProvider<Resource> {

    public static final Property<Resource> PUBLISHED = new BeanProperty<>("published", "published");
    public static final Property<Resource> NAME = new BeanProperty<>("name", "localName");
    public static final Property<Resource> ACTION = new PropertyPlaceholder<>("action");

    public static final List<Property<Resource>> PROPERTIES =
            Arrays.asList(PUBLISHED, NAME, ACTION);

    boolean showPublished;

    String storeId;

    transient List<Resource> cachedItems;

    @Override
    protected List<Resource> getItems() {
        // return an empty list in case we still don't know about the store
        if (storeId == null) {
            return new ArrayList<>();
        } else if (cachedItems == null) {
            cachedItems = getItemsInternal();
        }
        return cachedItems;
    }

    private List<Resource> getItemsInternal() {
        // else, grab the resource list
        try {
            List<Resource> result;
            StoreInfo store = getCatalog().getStore(storeId, StoreInfo.class);

            Map<String, Resource> resources = new HashMap<>();
            if (store instanceof DataStoreInfo) {
                DataStoreInfo dstore = (DataStoreInfo) store;
                DataStoreInfo expandedStore = getCatalog().getResourcePool().clone(dstore, true);

                // collect all the type names and turn them into resources
                // for the moment we use local names as datastores are not returning
                // namespace qualified NameImpl
                List<Name> names = expandedStore.getDataStore(null).getNames();
                for (Name name : names) {
                    FeatureTypeInfo fti =
                            getCatalog()
                                    .getFeatureTypeByDataStore(expandedStore, name.getLocalPart());
                    // skip views, we cannot have two layers use the same feature type info, as the
                    // underlying definition is attached to the feature type info itself
                    if (fti == null
                            || fti.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE) == null) {
                        resources.put(name.getLocalPart(), new Resource(name));
                    }
                }

            } else if (store instanceof CoverageStoreInfo) {
                CoverageStoreInfo cstore = (CoverageStoreInfo) store;
                CoverageStoreInfo expandedStore =
                        getCatalog().getResourcePool().clone(cstore, true);

                NamespaceInfo ns =
                        getCatalog().getNamespaceByPrefix(expandedStore.getWorkspace().getName());
                GridCoverageReader reader = expandedStore.getGridCoverageReader(null, null);
                try {
                    String[] names = reader.getGridCoverageNames();
                    for (String name : names) {
                        Name qualified = new NameImpl(ns.getURI(), name);
                        Resource resource = new Resource(qualified);
                        resource.setMultiCoverageReader(true);
                        resources.put(name, resource);
                    }
                } catch (UnsupportedOperationException e) {
                    // old code, pre multi-coverage
                    // getting to the coverage name without reading the whole coverage seems to
                    // be hard stuff, let's have the catalog builder to the heavy lifting
                    CatalogBuilder builder = new CatalogBuilder(getCatalog());
                    builder.setStore(store);
                    CoverageInfo ci = builder.buildCoverage();
                    Name name = ci.getQualifiedName();
                    resources.put(name.getLocalPart(), new Resource(name));
                }

            } else if (store instanceof WMTSStoreInfo) {
                WMTSStoreInfo wmsInfo = (WMTSStoreInfo) store;
                WMTSStoreInfo expandedStore = getCatalog().getResourcePool().clone(wmsInfo, true);

                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                WebMapTileServer webMapTileServer = expandedStore.getWebMapTileServer(null);
                WMTSCapabilities capabilities = webMapTileServer.getCapabilities();
                List<WMTSLayer> layers = capabilities.getLayerList();
                for (Layer l : layers) {
                    if (l.getName() == null) {
                        continue;
                    }

                    resources.put(l.getName(), new Resource(new NameImpl(l.getName())));
                }
            } else if (store instanceof WMSStoreInfo) {
                WMSStoreInfo wmsInfo = (WMSStoreInfo) store;
                WMSStoreInfo expandedStore = getCatalog().getResourcePool().clone(wmsInfo, true);

                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(store);
                List<Layer> layers =
                        expandedStore.getWebMapServer(null).getCapabilities().getLayerList();
                for (Layer l : layers) {
                    if (l.getName() == null) {
                        continue;
                    }

                    resources.put(l.getName(), new Resource(new NameImpl(l.getName())));
                }
            }

            // lookup all configured layers, mark them as published in the resources
            List<ResourceInfo> configuredTypes =
                    getCatalog().getResourcesByStore(store, ResourceInfo.class);
            for (ResourceInfo type : configuredTypes) {
                // compare with native name, which is what the DataStore provides through getNames()
                // above
                Resource resource;
                if (type instanceof CoverageInfo) {
                    CoverageInfo ci = (CoverageInfo) type;
                    if (ci.getNativeCoverageName() != null) {
                        resource = resources.get(ci.getNativeCoverageName());
                    } else {
                        resource = resources.get(type.getNativeName());
                    }
                } else {
                    resource = resources.get(type.getNativeName());
                }
                if (resource != null) {
                    resource.setPublished(true);
                }
            }
            result = new ArrayList<>(resources.values());

            // return by natural order
            Collections.sort(result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not list layers for this store, "
                            + "an error occurred retrieving them: "
                            + e.getMessage(),
                    e);
        }
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.cachedItems = null;
        this.storeId = storeId;
    }

    @Override
    protected List<Resource> getFilteredItems() {
        List<Resource> resources = super.getFilteredItems();
        if (showPublished) return resources;

        List<Resource> unconfigured = new ArrayList<>();
        for (Resource resource : resources) {
            if (!resource.isPublished()) unconfigured.add(resource);
        }
        return unconfigured;
    }

    @Override
    protected List<Property<Resource>> getProperties() {
        return PROPERTIES;
    }

    public void setShowPublished(boolean showPublished) {
        this.showPublished = showPublished;
    }
}
