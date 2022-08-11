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

package org.geoserver.data.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/** Placeholder connection for testing */
class TestConnection implements Connection {
    private int timeout;

    private String schema;

    private Properties clientInfo;

    private int holdability;

    private Map<String, Class<?>> typeMap;

    private int transactionIsolation;

    private String catalog;

    private boolean readOnly = false;

    private boolean closed = false;

    private boolean autoCommit = true;

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Not a wrapper for " + iface.getSimpleName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public Statement createStatement() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return sql;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return autoCommit;
    }

    @Override
    public void commit() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void rollback() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void close() throws SQLException {
        this.closed = true;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return readOnly;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        this.catalog = catalog;
    }

    @Override
    public String getCatalog() throws SQLException {
        return catalog;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        this.transactionIsolation = level;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return transactionIsolation;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {}

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return typeMap;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.typeMap = map;
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        this.holdability = holdability;
    }

    @Override
    public int getHoldability() throws SQLException {
        return holdability;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public Statement createStatement(
            int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public CallableStatement prepareCall(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (clientInfo == null) {
            clientInfo = new Properties();
        }
        this.clientInfo.put(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        this.clientInfo = properties;
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return clientInfo != null ? clientInfo.getProperty(name) : null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return clientInfo;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        this.schema = schema;
    }

    @Override
    public String getSchema() throws SQLException {
        return schema;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        this.timeout = milliseconds;
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return timeout;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
