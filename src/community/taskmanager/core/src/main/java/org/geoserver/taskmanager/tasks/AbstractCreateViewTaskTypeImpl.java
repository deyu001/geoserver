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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCreateViewTaskTypeImpl implements TaskType {

    public static final String PARAM_DB_NAME = "database";

    public static final String PARAM_VIEW_NAME = "view-name";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    protected static final Logger LOGGER = Logging.getLogger(AbstractCreateViewTaskTypeImpl.class);

    @Autowired protected ExtTypes extTypes;

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_DB_NAME, new ParameterInfo(PARAM_DB_NAME, extTypes.dbName, true));
        paramInfo.put(
                PARAM_VIEW_NAME, new ParameterInfo(PARAM_VIEW_NAME, ParameterType.STRING, true));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final String viewName = (String) ctx.getParameterValues().get(PARAM_VIEW_NAME);
        final String tempViewName =
                SqlUtil.qualified(
                        SqlUtil.schema(viewName),
                        "_temp_" + UUID.randomUUID().toString().replace('-', '_'));
        ctx.getBatchContext().put(new DbTableImpl(db, viewName), new DbTableImpl(db, tempViewName));

        final String definition =
                buildQueryDefinition(
                        ctx,
                        db.getDialect().autoUpdateView()
                                ? null
                                : new BatchContext.Dependency() {
                                    @Override
                                    public void revert() throws TaskException {
                                        final String definition = buildQueryDefinition(ctx, null);
                                        try (Connection conn = db.getDataSource().getConnection()) {
                                            try (Statement stmt = conn.createStatement()) {
                                                StringBuilder sb =
                                                        new StringBuilder("DROP VIEW ")
                                                                .append(viewName)
                                                                .append("; CREATE VIEW ")
                                                                .append(viewName)
                                                                .append(" AS ")
                                                                .append(definition);
                                                LOGGER.log(
                                                        Level.FINE,
                                                        "replacing temporary View: "
                                                                + sb.toString());
                                                stmt.executeUpdate(sb.toString());
                                            }
                                        } catch (SQLException e) {
                                            throw new TaskException(e);
                                        }
                                    }
                                });

        try (Connection conn = db.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {

                String sqlCreateSchemaIfNotExists =
                        db.getDialect().createSchema(conn, SqlUtil.schema(tempViewName));

                StringBuilder sb = new StringBuilder(sqlCreateSchemaIfNotExists);
                sb.append("CREATE VIEW ").append(tempViewName).append(" AS ").append(definition);
                LOGGER.log(Level.FINE, "creating temporary View: " + sb.toString());
                stmt.executeUpdate(sb.toString());
            }
        } catch (SQLException e) {
            throw new TaskException(e);
        }

        return new TaskResult() {
            @Override
            public void commit() throws TaskException {
                try (Connection conn = db.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        LOGGER.log(Level.FINE, "committing view: " + viewName);
                        String viewNameQuoted = db.getDialect().quote(viewName);
                        stmt.executeUpdate("DROP VIEW IF EXISTS " + viewNameQuoted);

                        stmt.executeUpdate(
                                db.getDialect()
                                        .sqlRenameView(
                                                tempViewName,
                                                db.getDialect()
                                                        .quote(SqlUtil.notQualified(viewName))));

                        ctx.getBatchContext().delete(new DbTableImpl(db, viewName));

                        LOGGER.log(Level.FINE, "committed view: " + viewName);
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try (Connection conn = db.getDataSource().getConnection()) {
                    try (Statement stmt = conn.createStatement()) {
                        LOGGER.log(Level.FINE, "rolling back view: " + viewName);
                        stmt.executeUpdate("DROP VIEW " + tempViewName);
                        LOGGER.log(Level.FINE, "rolled back view: " + viewName);
                    }
                } catch (SQLException e) {
                    throw new TaskException(e);
                }
            }
        };
    }

    public abstract String buildQueryDefinition(TaskContext ctx, BatchContext.Dependency dependency)
            throws TaskException;

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB_NAME);
        final String viewName = (String) ctx.getParameterValues().get(PARAM_VIEW_NAME);
        try (Connection conn = db.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP VIEW IF EXISTS " + db.getDialect().quote(viewName));
            }
        } catch (SQLException e) {
            throw new TaskException(e);
        }
    }
}
