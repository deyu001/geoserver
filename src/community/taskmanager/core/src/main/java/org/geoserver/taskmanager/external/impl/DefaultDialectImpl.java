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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.geoserver.taskmanager.external.Dialect;
import org.geoserver.taskmanager.util.SqlUtil;

/**
 * Default implementation for the Dialect interface.
 *
 * <p>This should work with most databases, but it also limits the functionality of the task
 * manager.
 *
 * @author Timothy De Bock
 */
public class DefaultDialectImpl implements Dialect {

    @Override
    public String quote(String tableName) {
        return SqlUtil.quote(tableName);
    }

    @Override
    public String sqlRenameView(String currentViewName, String newViewName) {
        return "ALTER VIEW " + currentViewName + " RENAME TO " + newViewName;
    }

    @Override
    public String createIndex(
            String tableName,
            Set<String> columnNames,
            boolean isSpatialIndex,
            boolean isUniqueIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE INDEX ");
        sb.append(" ON ");
        sb.append(tableName);
        // regular index
        sb.append(" (");
        for (String columnName : columnNames) {
            sb.append(quote(columnName));
            sb.append(",");
        }
        sb.setLength(sb.length() - 1);
        sb.append(" );");
        return sb.toString();
    }

    @Override
    public Set<String> getSpatialColumns(
            Connection connection, String tableName, String defaultSchema) {
        return Collections.emptySet();
    }

    @Override
    public int isNullable(int nullable) {
        return nullable;
    }

    @Override
    public String createSchema(Connection connection, String schema) {
        StringBuilder sb =
                new StringBuilder("CREATE SCHEMA IF NOT EXISTS ").append(schema).append(" ;");
        return sb.toString();
    }

    @Override
    public boolean autoUpdateView() {
        return false;
    }

    @Override
    public List<Column> getColumns(Connection connection, String tableName, ResultSet rs)
            throws SQLException {
        List<Column> result = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();

        int columnCount = rsmd.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            int col = i;
            result.add(
                    new Column() {

                        @Override
                        public String getName() throws SQLException {
                            return rsmd.getColumnLabel(col);
                        }

                        @Override
                        public String getTypeEtc() throws SQLException {
                            String typeName = rsmd.getColumnTypeName(col);
                            StringBuffer sb = new StringBuffer(typeName);
                            if (("char".equals(typeName) || "varchar".equals(typeName))
                                    && rsmd.getColumnDisplaySize(col) > 0
                                    && rsmd.getColumnDisplaySize(col) < Integer.MAX_VALUE) {
                                typeName += " (" + rsmd.getColumnDisplaySize(col) + " ) ";
                            }
                            switch (isNullable(rsmd.isNullable(col))) {
                                case ResultSetMetaData.columnNoNulls:
                                    sb.append(" NOT NULL");
                                    break;
                                case ResultSetMetaData.columnNullable:
                                    sb.append(" NULL");
                                    break;
                            }
                            return sb.toString();
                        }
                    });
        }

        return result;
    }
}
