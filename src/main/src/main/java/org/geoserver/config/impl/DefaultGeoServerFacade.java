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

package org.geoserver.config.impl;

import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;

public class DefaultGeoServerFacade implements GeoServerFacade {

    static final Logger LOGGER = Logging.getLogger(DefaultGeoServerFacade.class);

    GeoServerInfo global;
    List<SettingsInfo> settings = new ArrayList<>();
    LoggingInfo logging;
    List<ServiceInfo> services = new ArrayList<>();

    GeoServer geoServer;

    public DefaultGeoServerFacade(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.global = geoServer.getFactory().createGlobal();
        this.logging = geoServer.getFactory().createLogging();
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public GeoServerInfo getGlobal() {
        if (global == null) {
            return null;
        }

        return ModificationProxy.create(global, GeoServerInfo.class);
    }

    public void setGlobal(GeoServerInfo global) {
        resolve(global);
        setId(global.getSettings());
        this.global = global;
    }

    public void save(GeoServerInfo global) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(global);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireGlobalModified(global, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        for (SettingsInfo s : settings) {
            if (s.getWorkspace().equals(workspace)) {
                return ModificationProxy.create(s, SettingsInfo.class);
            }
        }
        return null;
    }

    @Override
    public void add(SettingsInfo s) {
        s = unwrap(s);
        setId(s);
        settings.add(s);
    }

    @Override
    public void save(SettingsInfo settings) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(settings);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        settings = (SettingsInfo) proxy.getProxyObject();
        geoServer.fireSettingsModified(settings, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    @Override
    public void remove(SettingsInfo s) {
        s = unwrap(s);
        settings.remove(s);
    }

    public LoggingInfo getLogging() {
        if (logging == null) {
            return null;
        }

        return ModificationProxy.create(logging, LoggingInfo.class);
    }

    public void setLogging(LoggingInfo logging) {
        this.logging = logging;
    }

    public void save(LoggingInfo logging) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(logging);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireLoggingModified(logging, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    public void add(ServiceInfo service) {
        // may be adding a proxy, need to unwrap
        service = unwrap(service);
        setId(service);
        service.setGeoServer(geoServer);

        services.add(service);
    }

    public void save(ServiceInfo service) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(service);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireServiceModified(service, propertyNames, oldValues, newValues);

        proxy.commit();
    }

    public void remove(ServiceInfo service) {
        services.remove(service);
    }

    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        return find(clazz, null, services);
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return find(clazz, workspace, services);
    }

    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        for (ServiceInfo si : services) {
            if (id.equals(si.getId())) {
                @SuppressWarnings("unchecked")
                T cast = (T) si;
                return ModificationProxy.create(cast, clazz);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Could not locate service of type "
                            + clazz
                            + " and id '"
                            + id
                            + "', available services were "
                            + services);
        }

        return null;
    }

    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        return findByName(name, null, clazz, services);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return findByName(name, workspace, clazz, services);
    }

    public Collection<? extends ServiceInfo> getServices() {
        return ModificationProxy.createList(filter(services, null), ServiceInfo.class);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return ModificationProxy.createList(filter(services, workspace), ServiceInfo.class);
    }

    public void dispose() {
        if (global != null) global.dispose();
        if (settings != null) settings.clear();
        if (services != null) services.clear();
    }

    public static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    protected void resolve(GeoServerInfo info) {
        GeoServerInfoImpl global = (GeoServerInfoImpl) info;
        if (global.getMetadata() == null) {
            global.setMetadata(new MetadataMap());
        }
        if (global.getClientProperties() == null) {
            global.setClientProperties(new HashMap<>());
        }
        if (global.getCoverageAccess() == null) {
            global.setCoverageAccess(new CoverageAccessInfoImpl());
        }
    }

    <T extends ServiceInfo> T find(
            Class<T> clazz, WorkspaceInfo workspace, List<ServiceInfo> services) {
        for (ServiceInfo si : services) {
            if (clazz.isAssignableFrom(si.getClass()) && wsEquals(workspace, si.getWorkspace())) {
                @SuppressWarnings("unchecked")
                T cast = (T) si;
                return ModificationProxy.create(cast, clazz);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Could not locate service of type "
                            + clazz
                            + " in workspace "
                            + workspace
                            + ", available services were "
                            + services);
        }

        return null;
    }

    <T extends ServiceInfo> T findByName(
            String name, WorkspaceInfo workspace, Class<T> clazz, List<ServiceInfo> services) {
        for (ServiceInfo si : services) {
            if (name.equals(si.getName()) && wsEquals(workspace, si.getWorkspace())) {
                @SuppressWarnings("unchecked")
                T cast = (T) si;
                return ModificationProxy.create(cast, clazz);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Could not locate service of type "
                            + clazz
                            + " in workspace "
                            + workspace
                            + " and name '"
                            + name
                            + "', available services were "
                            + services);
        }

        return null;
    }

    public List<ServiceInfo> filter(List<ServiceInfo> services, WorkspaceInfo workspace) {
        List<ServiceInfo> list = new ArrayList<>();
        for (ServiceInfo si : services) {
            if (wsEquals(workspace, si.getWorkspace())) {
                list.add(si);
            }
        }
        return list;
    }

    boolean wsEquals(WorkspaceInfo ws1, WorkspaceInfo ws2) {
        if (ws1 == null) {
            return ws2 == null;
        }

        return ws1.equals(ws2);
    }

    protected void setId(Object o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            OwsUtils.set(o, "id", o.getClass().getSimpleName() + "-" + uid);
        }
    }
}
