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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.DataStorePanelInfo;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataAccessFactory;

/**
 * Entry point to look up for StoreInfo related extension points
 *
 * <p>At least two {@link DataStorePanelInfo} should be provided by the application context in order
 * to be used as the default ones when no specific panel info is contributed for a given store type,
 * and they shall have the id property set to {@code "defaultVector"} and {@code "defaultRaster"}
 * for {@link DataStoreInfo} and {@link CoverageStoreInfo} defaults respectively. TODO: port the
 * lookup of {@link DataStorePanelInfo} present in {@link CatalogIconFactory} here.
 *
 * @author Gabriel Roldan
 */
public class StoreExtensionPoints {

    private StoreExtensionPoints() {
        // do nothing
    }

    /**
     * Finds out the {@link StoreEditPanel} that provides the edit form components for the given
     * store.
     *
     * @param componentId the id for the returned panel
     * @param editForm the form that's going to contain the components in the returned panel
     * @param storeInfo the store being edited
     * @param app the {@link GeoServerApplication} where to look for registered {@link
     *     DataStorePanelInfo}s
     * @return a custom {@link StoreEditPanel} if there's one declared for the given store type, or
     *     a default one otherwise
     */
    public static StoreEditPanel getStoreEditPanel(
            final String componentId,
            final Form editForm,
            final StoreInfo storeInfo,
            final GeoServerApplication app) {

        if (storeInfo == null) {
            throw new NullPointerException("storeInfo param");
        }
        if (app == null) {
            throw new NullPointerException("GeoServerApplication param");
        }

        DataStorePanelInfo panelInfo = findPanelInfo(storeInfo, app);
        if (panelInfo == null || panelInfo.getComponentClass() == null) {
            // there's either no panel info specific for this kind of store, or it provides no
            // component class
            panelInfo = getDefaultPanelInfo(storeInfo, app);
        }
        final Class<StoreEditPanel> componentClass = panelInfo.getComponentClass();

        final Constructor<StoreEditPanel> constructor;
        try {
            constructor = componentClass.getConstructor(String.class, Form.class);
        } catch (SecurityException e) {
            throw e;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    componentClass.getName() + " does not provide the required constructor");
        }

        final StoreEditPanel storeEditPanel;
        try {
            storeEditPanel = constructor.newInstance(componentId, editForm);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot instantiate extension point contributor " + componentClass.getName(),
                    e);
        }

        return storeEditPanel;
    }

    private static DataStorePanelInfo getDefaultPanelInfo(
            StoreInfo storeInfo, GeoServerApplication app) {

        final List<DataStorePanelInfo> providers = app.getBeansOfType(DataStorePanelInfo.class);

        DataStorePanelInfo panelInfo = null;

        for (DataStorePanelInfo provider : providers) {
            if (storeInfo instanceof DataStoreInfo && "defaultVector".equals(provider.getId())) {
                panelInfo = provider;
                break;
            } else if (storeInfo instanceof CoverageStoreInfo
                    && "defaultRaster".equals(provider.getId())) {
                panelInfo = provider;
                break;
            }
        }

        if (panelInfo == null) {
            if (storeInfo instanceof DataStoreInfo) {
                throw new IllegalStateException(
                        "Bean of type DataStorePanelInfo named "
                                + "'defaultDataStorePanel' not provided by application context");
            } else if (storeInfo instanceof CoverageStoreInfo) {
                throw new IllegalStateException(
                        "Bean of type DataStorePanelInfo named "
                                + "'defaultCoverageStorePanel' not provided by application context");
            } else {
                throw new IllegalArgumentException(
                        "Unknown store type: " + storeInfo.getClass().getName());
            }
        }

        if (panelInfo.getComponentClass() == null) {
            throw new IllegalStateException(
                    "Default DataStorePanelInfo '"
                            + panelInfo.getId()
                            + "' does not define a componentClass property");
        }

        if (panelInfo.getIconBase() == null || panelInfo.getIcon() == null) {
            throw new IllegalStateException(
                    "Default DataStorePanelInfo '"
                            + panelInfo.getId()
                            + "' does not define default icon");
        }

        return panelInfo;
    }

    /**
     * @return the extension point descriptor for the given storeInfo, or {@code null} if there's no
     *     contribution specific for the given storeInfo's type
     */
    private static DataStorePanelInfo findPanelInfo(
            final StoreInfo storeInfo, final GeoServerApplication app) {

        final Catalog catalog = storeInfo.getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();

        Class<?> factoryClass = null;
        if (storeInfo instanceof DataStoreInfo) {
            DataAccessFactory storeFactory;
            try {
                storeFactory = resourcePool.getDataStoreFactory((DataStoreInfo) storeInfo);
            } catch (IOException e) {
                throw new IllegalArgumentException("no factory found for StoreInfo " + storeInfo);
            }
            if (storeFactory != null) {
                factoryClass = storeFactory.getClass();
            }
        } else if (storeInfo instanceof CoverageStoreInfo) {
            AbstractGridFormat gridFormat;
            gridFormat = resourcePool.getGridCoverageFormat((CoverageStoreInfo) storeInfo);
            if (gridFormat != null) {
                factoryClass = gridFormat.getClass();
            }
        } else {
            throw new IllegalArgumentException(
                    "Unknown store type: " + storeInfo.getClass().getName());
        }

        if (factoryClass == null) {
            throw new IllegalArgumentException("Can't locate the factory for the store");
        }

        final List<DataStorePanelInfo> providers = app.getBeansOfType(DataStorePanelInfo.class);

        List<DataStorePanelInfo> fallbacks = new ArrayList<>();
        for (DataStorePanelInfo provider : providers) {
            Class<?> providerFactoryClass = provider.getFactoryClass();
            if (providerFactoryClass == null) {
                continue;
            }
            if (factoryClass.equals(providerFactoryClass)) {
                return provider;
            } else if (providerFactoryClass.isAssignableFrom(factoryClass)) {
                fallbacks.add(provider);
            }
        }

        if (fallbacks.size() == 1) {
            return fallbacks.get(0);
        } else if (fallbacks.size() > 1) {
            // sort by class hierarchy, pick the closest match
            Collections.sort(
                    fallbacks,
                    new Comparator<DataStorePanelInfo>() {
                        public int compare(DataStorePanelInfo o1, DataStorePanelInfo o2) {
                            Class<?> c1 = o1.getFactoryClass();
                            Class<?> c2 = o2.getFactoryClass();

                            if (c1.equals(c2)) {
                                return 0;
                            }

                            if (c1.isAssignableFrom(c2)) {
                                return 1;
                            }

                            return -1;
                        }
                    });
            // check first two and make sure bindings are not equal
            DataStorePanelInfo f1 = fallbacks.get(0);
            DataStorePanelInfo f2 = fallbacks.get(1);

            if (f1.getFactoryClass().equals(f2.getFactoryClass())) {
                String msg =
                        "Multiple editor panels for : ("
                                + f1.getFactoryClass()
                                + "): "
                                + f1
                                + ", "
                                + f2;
                throw new RuntimeException(msg);
            }

            return f1;
        }

        // ok, we don't have a specific one
        return null;
    }
}
