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

package org.geoserver.taskmanager.external.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.util.logging.Logging;

/**
 * Postgis Dialect.
 *
 * @author Timothy De Bock
 */
public class PostgisDialectImpl extends DefaultDialectImpl {

    private static final Logger LOGGER = Logging.getLogger(PostgisDialectImpl.class);

    @Override
    public String quote(String tableName) {
        return SqlUtil.quote(tableName);
    }

    @Override
    public String createIndex(
            String tableName,
            Set<String> columnNames,
            boolean isSpatialIndex,
            boolean isUniqueIndex) {

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ");
        if (isUniqueIndex) {
            sb.append(" UNIQUE");
        }
        sb.append(" INDEX ");
        // sb.append(indexName);
        sb.append(" ON ");
        sb.append(SqlUtil.quote(tableName));

        if (isSpatialIndex) {
            sb.append(" USING gist ");
        }

        sb.append(" (");
        for (String columnName : columnNames) {
            sb.append(quote(columnName));
            sb.append(",");
        }
        sb.setLength(sb.length() - 1);
        sb.append(" )");

        sb.append(";");
        return sb.toString();
    }

    @Override
    public Set<String> getSpatialColumns(
            Connection sourceConn, String tableName, String defaultSchema) {
        String schema = StringUtils.strip(SqlUtil.schema(tableName), "\"");
        String unqualifiedTableName = StringUtils.strip(SqlUtil.notQualified(tableName), "\"");

        if (schema == null) {
            schema = defaultSchema == null ? "public" : defaultSchema;
        }

        HashSet<String> spatialColumns = new HashSet<>();
        try (Statement stmt = sourceConn.createStatement()) {
            try (ResultSet rs =
                    stmt.executeQuery(
                            "SELECT * FROM geometry_columns "
                                    + " WHERE geometry_columns.f_table_name='"
                                    + unqualifiedTableName
                                    + "' and f_table_schema = '"
                                    + schema
                                    + "' ")) {
                while (rs.next()) {
                    spatialColumns.add(rs.getString("f_geometry_column"));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Could not find the spatial columns:" + e.getMessage());
        }

        return spatialColumns;
    }

    @Override
    public List<Column> getColumns(Connection connection, String tableName, ResultSet rs)
            throws SQLException {
        // this method generates a more precise type description that the default dialect.

        List<Column> result = new ArrayList<Column>();

        Statement stmt = connection.createStatement();

        ResultSet rsMetadata =
                stmt.executeQuery(
                        "select a.attname, a.attnotnull, format_type(a.atttypid, a.atttypmod) "
                                + "from pg_attribute a where attrelid = '"
                                + tableName
                                + "'::regclass and attnum > 0");

        while (rsMetadata.next()) {
            String name = rsMetadata.getString(1);
            Boolean notNull = rsMetadata.getBoolean(2);
            String type = rsMetadata.getString(3);
            result.add(
                    new Column() {

                        @Override
                        public String getName() throws SQLException {
                            return quote(name);
                        }

                        @Override
                        public String getTypeEtc() throws SQLException {
                            return type + (notNull ? " NOT NULL" : " NULL");
                        }
                    });
        }

        return result;
    }

    @Override
    public boolean autoUpdateView() {
        return true;
    }
}
