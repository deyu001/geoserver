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

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskRunnable;
import org.geoserver.taskmanager.util.SqlUtil;
import org.springframework.stereotype.Component;

@Component
public class DbRemotePublicationTaskTypeImpl extends AbstractRemotePublicationTaskTypeImpl {

    public static final String NAME = "RemoteDbPublication";

    public static final String PARAM_DB_NAME = "database";

    public static final String PARAM_TABLE_NAME = "table-name";

    @PostConstruct
    @Override
    public void initParamInfo() {
        super.initParamInfo();
        ParameterInfo dbInfo = new ParameterInfo(PARAM_DB_NAME, extTypes.dbName, true);
        paramInfo.put(PARAM_DB_NAME, dbInfo);
        paramInfo.put(
                PARAM_TABLE_NAME,
                new ParameterInfo(PARAM_TABLE_NAME, extTypes.tableName, false).dependsOn(dbInfo));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected boolean createStore(
            ExternalGS extGS,
            GeoServerRESTManager restManager,
            StoreInfo store,
            TaskContext ctx,
            String name)
            throws IOException, TaskException {
        try {
            final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
            final DbTable table = (DbTable) ctx.getParameterValues().get(PARAM_TABLE_NAME);
            final ExternalGS gs = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
            return restManager
                    .getStoreManager()
                    .create(
                            store.getWorkspace().getName(),
                            db.postProcess(db.getStoreEncoder(name, gs), table));
        } catch (UnsupportedOperationException e) {
            throw new TaskException(
                    "Failed to create store "
                            + store.getWorkspace().getName()
                            + ":"
                            + store.getName(),
                    e);
        }
    }

    @Override
    protected String getStoreName(StoreInfo store, TaskContext ctx) throws TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final DbTable table = (DbTable) ctx.getParameterValues().get(PARAM_TABLE_NAME);
        final String schema = table == null ? null : SqlUtil.schema(table.getTableName());
        return schema == null ? db.getName() : (db.getName() + "_" + schema);
    }

    @Override
    protected boolean neverReuseStore() {
        return false;
    }

    @Override
    protected void postProcess(
            StoreType storeType,
            ResourceInfo resource,
            GSResourceEncoder re,
            TaskContext ctx,
            TaskRunnable<GSResourceEncoder> update)
            throws TaskException {
        final DbTable table =
                (DbTable)
                        ctx.getBatchContext()
                                .get(
                                        ctx.getParameterValues().get(PARAM_TABLE_NAME),
                                        new BatchContext.Dependency() {
                                            @Override
                                            public void revert() throws TaskException {
                                                GSFeatureTypeEncoder re =
                                                        new GSFeatureTypeEncoder(false);

                                                if (resource.getMetadata()
                                                        .containsKey(
                                                                FeatureTypeInfo
                                                                        .JDBC_VIRTUAL_TABLE)) {
                                                    // virtual table, resource must be attached to
                                                    // SQL query
                                                    // in metadata, rather than just table name
                                                    re.setNativeName(resource.getNativeName());
                                                } else {
                                                    DbTable table =
                                                            (DbTable)
                                                                    ctx.getBatchContext()
                                                                            .get(
                                                                                    ctx.getParameterValues()
                                                                                            .get(
                                                                                                    PARAM_TABLE_NAME));
                                                    re.setNativeName(
                                                            SqlUtil.notQualified(
                                                                    table.getTableName()));
                                                }
                                                update.run(re);
                                            }
                                        });
        if (table != null) {
            ((GSFeatureTypeEncoder) re).setNativeName(SqlUtil.notQualified(table.getTableName()));
        }
    }
}
