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

package org.geoserver.jdbcconfig.config;

import static org.geoserver.catalog.CatalogFacade.ANY_WORKSPACE;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.isNull;

import com.google.common.base.Preconditions;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.logging.LoggingStartupContextListener;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

@ParametersAreNonnullByDefault
public class JDBCGeoServerFacade implements GeoServerFacade {

    static final Logger LOGGER = Logging.getLogger(JDBCGeoServerFacade.class);

    private static final String GLOBAL_ID = "GeoServerInfo.global";

    private static final String GLOBAL_LOGGING_ID = "LoggingInfo.global";

    private GeoServer geoServer;

    public final ConfigDatabase db;

    private ResourceStore ddResourceStore;

    private GeoServerResourceLoader resourceLoader;

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setDdResourceStore(ResourceStore ddResourceStore) {
        this.ddResourceStore = ddResourceStore;
    }

    public JDBCGeoServerFacade(final ConfigDatabase db) {
        this.db = db;
    }

    @SuppressWarnings("deprecation")
    private void reinitializeLogging() {
        try {
            LoggingInfo realLogInfo = this.getLogging();
            if (realLogInfo == null) {
                return;
            }
            LoggingInfo startLogInfo =
                    LoggingStartupContextListener.getLogging(
                            ddResourceStore == null ? resourceLoader : ddResourceStore);

            // Doing this reflectively so that if LoggingInfo gets new properties, this should still
            // work. KS

            ClassProperties properties = OwsUtils.getClassProperties(LoggingInfo.class);

            List<String> propertyNames = new ArrayList<String>(properties.properties().size());
            List<Object> newValues = new ArrayList<Object>(properties.properties().size());
            List<Object> oldValues = new ArrayList<Object>(properties.properties().size());

            final Level propertyTableLevel = Level.FINE;
            LOGGER.log(
                    propertyTableLevel,
                    "Checking Logging configuration in case it neeeds to be reinitialized");
            for (String propName : properties.properties()) {

                // Don't care about the return type
                Method read = properties.getter(propName, null);

                Object newVal = read.invoke(realLogInfo);
                Object oldVal = read.invoke(startLogInfo);

                if ((newVal == null && oldVal == null)
                        || (newVal != null && newVal.equals(oldVal))) {
                    // Values the same
                    LOGGER.log(
                            propertyTableLevel,
                            "=== {0} (logging.xml: {1}, JDBCConfig: {2})",
                            new Object[] {read.getName(), oldVal, newVal});
                } else {
                    // Values different
                    propertyNames.add(propName);
                    newValues.add(newVal);
                    oldValues.add(oldVal);
                    LOGGER.log(
                            propertyTableLevel,
                            "=/= {0} (logging.xml: {1}, JDBCConfig: {2})",
                            new Object[] {read.getName(), oldVal, newVal});
                }
            }
            // If there's a difference other than the ID
            if (!(propertyNames.isEmpty()
                    || (propertyNames.size() == 1 && propertyNames.get(0).equals("Id")))) {
                LOGGER.log(
                        Level.WARNING,
                        "Start up logging config does not match that in JDBCConfig.  Reconfiguring now.  Logs preceding this message may reflect a different configuration.");
                LoggingUtils.initLogging(
                        resourceLoader,
                        realLogInfo.getLevel(),
                        !realLogInfo.isStdOutLogging(),
                        realLogInfo.getLocation());
            }
        } catch (Exception ex) {
            // If something bad happens, log it and keep going with the wrong logging config
            LOGGER.log(
                    Level.SEVERE,
                    "Problem while reinitializing Logging from JDBC Config.  Log configuration may not be correct.",
                    ex);
        }
    }

    @Override
    public GeoServer getGeoServer() {
        return geoServer;
    }

    @Override
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.db.setGeoServer(geoServer);
        reinitializeLogging();
    }

    @Override
    public GeoServerInfo getGlobal() {
        GeoServerInfo global = db.getById(GLOBAL_ID, GeoServerInfo.class);
        return global;
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        OwsUtils.set(global, "id", GLOBAL_ID);
        if (global.getSettings() == null) {
            SettingsInfo defaultSettings = geoServer.getFactory().createSettings();
            add(defaultSettings);
            global.setSettings(defaultSettings);
            // JD: disabling this check, global settings should have an id
            // }else if(null == global.getSettings().getId()){
        } else {
            add(global.getSettings());
        }
        if (null == getGlobal()) {
            db.add(global);
        } else {
            db.save(ModificationProxy.create(global, GeoServerInfo.class));
        }
        GeoServerInfo saved = getGlobal();
        Preconditions.checkNotNull(saved);
    }

    @Override
    public void save(GeoServerInfo global) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(global);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireGlobalModified(global, propertyNames, oldValues, newValues);

        db.save(global);
    }

    @Override
    public LoggingInfo getLogging() {
        LoggingInfo loggingInfo = db.getById(GLOBAL_LOGGING_ID, LoggingInfo.class);
        return loggingInfo;
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        OwsUtils.set(logging, "id", GLOBAL_LOGGING_ID);
        if (null == getLogging()) {
            db.add(logging);
        } else {
            db.save(ModificationProxy.create(logging, LoggingInfo.class));
        }
        LoggingInfo saved = getLogging();
        Preconditions.checkNotNull(saved);
    }

    @Override
    public void save(LoggingInfo logging) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(logging);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireLoggingModified(logging, propertyNames, oldValues, newValues);

        db.save(logging);
    }

    @Override
    public void add(ServiceInfo service) {
        setId(service, ServiceInfo.class);
        service.setGeoServer(geoServer);
        db.add(service);
    }

    @Override
    public void remove(ServiceInfo service) {
        db.remove(service);
    }

    @Override
    public void save(ServiceInfo service) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(service);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireServiceModified(service, propertyNames, oldValues, newValues);

        db.save(service);
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        Filter filter = equal("workspace.id", workspace.getId());

        return db.get(SettingsInfo.class, filter);
    }

    @Override
    public void add(SettingsInfo settings) {
        setId(settings, SettingsInfo.class);
        db.add(settings);
    }

    @Override
    public void save(SettingsInfo settings) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(settings);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireSettingsModified(settings, propertyNames, oldValues, newValues);

        db.save(settings);
    }

    @Override
    public void remove(SettingsInfo settings) {
        db.remove(settings);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices() {
        return getServices((WorkspaceInfo) null);
    }

    private Filter filterForWorkspace(WorkspaceInfo workspace) {
        if (workspace != null && workspace != ANY_WORKSPACE) {
            return equal("workspace.id", workspace.getId());
        } else {
            return filterForGlobal();
        }
    }

    private Filter filterForGlobal() {
        return isNull("workspace.id");
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {

        Filter filter = filterForWorkspace(workspace);
        return db.queryAsList(ServiceInfo.class, filter, null, null, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ServiceInfo> T getService(final Class<T> clazz) {
        return (T) db.getService(null, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ServiceInfo> T getService(
            final WorkspaceInfo workspace, final Class<T> clazz) {
        return (T) db.getService(workspace, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getService(final String id, final Class<T> clazz) {
        return db.getById(id, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(final String name, final Class<T> clazz) {
        return findByName(name, null, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            final String name, final WorkspaceInfo workspace, final Class<T> clazz) {

        return findByName(name, workspace, clazz);
    }

    private <T extends Info> T findByName(
            @Nonnull final String name,
            @Nullable final WorkspaceInfo workspace,
            @Nonnull final Class<T> clazz)
            throws AssertionError {

        Filter filter = equal("name", name);
        if (null != workspace && ANY_WORKSPACE != workspace) {
            final String wsId = workspace.getId();
            Filter wsFilter = equal("workspace.id", wsId);
            filter = and(filter, wsFilter);
        }
        try {
            return db.get(clazz, filter);
        } catch (IllegalArgumentException multipleResults) {
            return null;
        }
    }

    @Override
    public void dispose() {
        db.dispose();
    }

    private void setId(Info info, Class<? extends Info> type) {
        final String curId = info.getId();
        if (null == curId) {
            final String uid = new UID().toString();
            final String id = type.getSimpleName() + "." + uid;
            OwsUtils.set(info, "id", id);
        }
    }
}
