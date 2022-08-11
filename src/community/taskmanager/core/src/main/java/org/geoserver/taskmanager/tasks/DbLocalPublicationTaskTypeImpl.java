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

package org.geoserver.taskmanager.tasks;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.feature.NameImpl;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.decorate.Wrapper;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DbLocalPublicationTaskTypeImpl implements TaskType {

    public static final String NAME = "LocalDbPublication";

    public static final String PARAM_WORKSPACE = "workspace";

    public static final String PARAM_LAYER = "layer";

    public static final String PARAM_DB_NAME = "database";

    public static final String PARAM_TABLE_NAME = "table-name";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @Autowired protected ExtTypes extTypes;

    @Autowired protected Catalog catalog;

    @Override
    public String getName() {
        return NAME;
    }

    @PostConstruct
    public void initParamInfo() {
        ParameterInfo dbInfo = new ParameterInfo(PARAM_DB_NAME, extTypes.dbName, true);
        paramInfo.put(PARAM_DB_NAME, dbInfo);
        paramInfo.put(
                PARAM_TABLE_NAME,
                new ParameterInfo(PARAM_TABLE_NAME, extTypes.tableName, true).dependsOn(dbInfo));
        ParameterInfo paramWorkspace =
                new ParameterInfo(PARAM_WORKSPACE, extTypes.workspace, false);
        paramInfo.put(PARAM_WORKSPACE, paramWorkspace);
        paramInfo.put(
                PARAM_LAYER,
                new ParameterInfo(PARAM_LAYER, extTypes.name, true)
                        .dependsOn(false, paramWorkspace));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final Name layerName = (Name) ctx.getParameterValues().get(PARAM_LAYER);
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final DbTable table =
                (DbTable)
                        ctx.getBatchContext()
                                .get(
                                        ctx.getParameterValues().get(PARAM_TABLE_NAME),
                                        new BatchContext.Dependency() {
                                            @Override
                                            public void revert() throws TaskException {
                                                FeatureTypeInfo resource =
                                                        catalog.getResourceByName(
                                                                layerName, FeatureTypeInfo.class);
                                                DbTable table =
                                                        (DbTable)
                                                                ctx.getBatchContext()
                                                                        .get(
                                                                                ctx.getParameterValues()
                                                                                        .get(
                                                                                                PARAM_TABLE_NAME));
                                                resource.setNativeName(
                                                        SqlUtil.notQualified(table.getTableName()));
                                                catalog.save(resource);
                                            }
                                        });

        CatalogFactory catalogFac = new CatalogFactoryImpl(catalog);

        final NamespaceInfo ns = catalog.getNamespaceByURI(layerName.getNamespaceURI());
        final WorkspaceInfo ws = catalog.getWorkspaceByName(ns.getName());

        final boolean createLayer = catalog.getLayerByName(layerName) == null;
        final boolean createStore;
        final boolean createResource;

        final LayerInfo layer;
        final DataStoreInfo store;
        final FeatureTypeInfo resource;

        if (createLayer) {
            String schema = SqlUtil.schema(table.getTableName());
            String dbName = schema == null ? db.getName() : (db.getName() + "_" + schema);
            final DataStoreInfo _store = catalog.getStoreByName(ws, dbName, DataStoreInfo.class);
            final FeatureTypeInfo _resource =
                    catalog.getResourceByName(layerName, FeatureTypeInfo.class);
            createStore = _store == null;
            createResource = _resource == null;

            if (createStore) {
                store = catalogFac.createDataStore();
                store.setWorkspace(ws);
                store.setName(dbName);
                store.getConnectionParameters()
                        .put(JDBCDataStoreFactory.NAMESPACE.getName(), ns.getURI());
                store.getConnectionParameters().putAll(db.getParameters());
                if (schema != null) {
                    store.getConnectionParameters()
                            .put(JDBCDataStoreFactory.SCHEMA.getName(), schema);
                }
                store.setEnabled(true);
                catalog.add(store);
            } else {
                store = unwrap(_store, DataStoreInfo.class);
            }

            CatalogBuilder builder = new CatalogBuilder(catalog);
            if (createResource) {
                builder.setStore(store);
                try {
                    resource =
                            builder.buildFeatureType(
                                    new NameImpl(SqlUtil.notQualified(table.getTableName())));
                    builder.setupBounds(resource);
                } catch (Exception e) {
                    if (createStore) {
                        catalog.remove(store);
                    }
                    throw new TaskException(e);
                }
                resource.setName(layerName.getLocalPart());
                resource.setTitle(layerName.getLocalPart());
                resource.setAdvertised(false);
                catalog.add(resource);
            } else {
                resource = unwrap(_resource, FeatureTypeInfo.class);
            }

            try {
                layer = builder.buildLayer(resource);
                catalog.add(layer);
            } catch (IOException e) {
                if (createStore) {
                    catalog.remove(store);
                }
                if (createResource) {
                    catalog.remove(resource);
                }
                throw new TaskException(e);
            }
        } else {
            layer = null;
            resource = null;
            store = null;
            createStore = false;
            createResource = false;
        }

        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                if (createLayer) {
                    ResourceInfo editResource =
                            catalog.getResource(resource.getId(), ResourceInfo.class);
                    editResource.setAdvertised(true);
                    catalog.save(editResource);
                }
            }

            @Override
            public void rollback() throws TaskException {
                if (createLayer) {
                    catalog.remove(layer);
                    if (createResource) {
                        catalog.remove(resource);
                    }
                    if (createStore) {
                        catalog.remove(store);
                    }
                }
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final Name layerName = (Name) ctx.getParameterValues().get(PARAM_LAYER);
        final String workspace = catalog.getNamespaceByURI(layerName.getNamespaceURI()).getPrefix();

        final DbTable table = (DbTable) ctx.getParameterValues().get(PARAM_TABLE_NAME);
        String schema = SqlUtil.schema(table.getTableName());
        String dbName = schema == null ? db.getName() : (db.getName() + "_" + schema);

        final LayerInfo layer = catalog.getLayerByName(layerName);
        final DataStoreInfo store = catalog.getStoreByName(workspace, dbName, DataStoreInfo.class);
        final FeatureTypeInfo resource =
                catalog.getResourceByName(layerName, FeatureTypeInfo.class);

        catalog.remove(layer);
        catalog.remove(resource);
        if (catalog.getResourcesByStore(store, ResourceInfo.class).isEmpty()) {
            catalog.remove(store);
        }
    }

    private static <T> T unwrap(T o, Class<T> clazz) {
        if (o instanceof Wrapper) {
            return ((Wrapper) o).unwrap(clazz);
        } else {
            return o;
        }
    }
}
