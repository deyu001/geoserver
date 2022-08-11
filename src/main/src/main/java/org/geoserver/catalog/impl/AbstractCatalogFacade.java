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

package org.geoserver.catalog.impl;

import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;

public abstract class AbstractCatalogFacade implements CatalogFacade {

    private static final Logger LOGGER = Logging.getLogger(AbstractCatalogFacade.class);

    //
    // Utilities
    //
    public static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    protected void beforeSaved(
            CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        CatalogInfo real = ModificationProxy.unwrap(object);

        // TODO: protect this original object, perhaps with another proxy
        getCatalog().fireModified(real, propertyNames, oldValues, newValues);
    }

    protected <T extends CatalogInfo> T commitProxy(T object) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(object);

        // get the real object
        @SuppressWarnings("unchecked")
        T real = (T) h.getProxyObject();

        // commit to the original object
        h.commit();

        return real;
    }

    protected void afterSaved(
            CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        CatalogInfo real = ModificationProxy.unwrap(object);

        // fire the post modify event
        getCatalog().firePostModified(real, propertyNames, oldValues, newValues);
    }

    protected void resolve(LayerInfo layer) {
        setId(layer);

        ResourceInfo resource = ResolvingProxy.resolve(getCatalog(), layer.getResource());
        if (resource != null) {
            resource = unwrap(resource);
            layer.setResource(resource);
        }

        StyleInfo style = ResolvingProxy.resolve(getCatalog(), layer.getDefaultStyle());
        if (style != null) {
            style = unwrap(style);
            layer.setDefaultStyle(style);
        }

        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<>();
        for (StyleInfo s : layer.getStyles()) {
            s = ResolvingProxy.resolve(getCatalog(), s);
            s = unwrap(s);
            styles.add(s);
        }
        ((LayerInfoImpl) layer).setStyles(styles);
    }

    protected void resolve(LayerGroupInfo layerGroup) {
        setId(layerGroup);

        LayerGroupInfoImpl lg = (LayerGroupInfoImpl) layerGroup;

        for (int i = 0; i < lg.getLayers().size(); i++) {
            PublishedInfo l = lg.getLayers().get(i);

            if (l != null) {
                PublishedInfo resolved;
                if (l instanceof LayerGroupInfo) {
                    resolved = unwrap(ResolvingProxy.resolve(getCatalog(), (LayerGroupInfo) l));
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else if (l instanceof LayerInfo) {
                    resolved = unwrap(ResolvingProxy.resolve(getCatalog(), (LayerInfo) l));
                    // special case to handle catalog loading, when nested publishibles might not be
                    // loaded.
                    if (resolved == null) {
                        resolved = l;
                    }
                } else {
                    // Special case for null layer (style group)
                    resolved = unwrap(ResolvingProxy.resolve(getCatalog(), l));
                }
                lg.getLayers().set(i, resolved);
            }
        }

        for (int i = 0; i < lg.getStyles().size(); i++) {
            StyleInfo s = lg.getStyles().get(i);
            if (s != null) {
                StyleInfo resolved = unwrap(ResolvingProxy.resolve(getCatalog(), s));
                lg.getStyles().set(i, resolved);
            }
        }
    }

    protected void resolve(StyleInfo style) {
        setId(style);

        // resolve the workspace
        WorkspaceInfo ws = style.getWorkspace();
        if (ws != null) {
            WorkspaceInfo resolved = ResolvingProxy.resolve(getCatalog(), ws);
            if (resolved != null) {
                resolved = unwrap(resolved);
                style.setWorkspace(resolved);
            } else {
                LOGGER.log(
                        Level.INFO,
                        "Failed to resolve workspace for style \""
                                + style.getName()
                                + "\". This means the workspace has not yet been added to the catalog, keep the proxy around");
            }
        }
    }

    protected void resolve(MapInfo map) {
        setId(map);
    }

    protected void resolve(WorkspaceInfo workspace) {
        setId(workspace);
    }

    protected void resolve(NamespaceInfo namespace) {
        setId(namespace);
    }

    protected void resolve(StoreInfo store) {
        setId(store);
        StoreInfoImpl s = (StoreInfoImpl) store;

        // resolve the workspace
        WorkspaceInfo resolved = ResolvingProxy.resolve(getCatalog(), s.getWorkspace());
        if (resolved != null) {
            resolved = unwrap(resolved);
            s.setWorkspace(resolved);
        } else {
            LOGGER.log(
                    Level.INFO,
                    "Failed to resolve workspace for store \""
                            + store.getName()
                            + "\". This means the workspace has not yet been added to the catalog, keep the proxy around");
        }
    }

    protected void resolve(ResourceInfo resource) {
        setId(resource);
        ResourceInfoImpl r = (ResourceInfoImpl) resource;

        // resolve the store
        StoreInfo store = ResolvingProxy.resolve(getCatalog(), r.getStore());
        if (store != null) {
            store = unwrap(store);
            r.setStore(store);
        }

        // resolve the namespace
        NamespaceInfo namespace = ResolvingProxy.resolve(getCatalog(), r.getNamespace());
        if (namespace != null) {
            namespace = unwrap(namespace);
            r.setNamespace(namespace);
        }
    }

    protected void setId(Object o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            OwsUtils.set(o, "id", o.getClass().getSimpleName() + "-" + uid);
        }
    }
}
