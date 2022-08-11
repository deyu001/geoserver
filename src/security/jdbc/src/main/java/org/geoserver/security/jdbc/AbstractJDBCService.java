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

package org.geoserver.security.jdbc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.util.IOUtils;

/**
 * JDBC base implementation for common used methods
 *
 * @author christian
 */
public abstract class AbstractJDBCService extends AbstractGeoServerSecurityService {
    /** logger */
    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");

    protected Properties ddlProps, dmlProps;
    protected DataSource datasource;

    /** Default isolation level to use */
    static final int DEFAULT_ISOLATION_LEVEL = Connection.TRANSACTION_READ_COMMITTED;

    protected AbstractJDBCService() {}

    /** initialize a {@link DataSource} form a {@link JdbcSecurityServiceConfig} object */
    public void initializeDSFromConfig(SecurityNamedServiceConfig namedConfig) throws IOException {
        JDBCSecurityServiceConfig config = (JDBCSecurityServiceConfig) namedConfig;
        if (config.isJndi()) {
            String jndiName = config.getJndiName();
            try {
                Context initialContext = new InitialContext();
                datasource = (DataSource) initialContext.lookup(jndiName);
            } catch (NamingException e) {
                throw new IOException(e);
            }
        } else {
            BasicDataSource bds = new BasicDataSource();
            bds.setDriverClassName(config.getDriverClassName());
            bds.setUrl(config.getConnectURL());
            bds.setUsername(config.getUserName());
            bds.setPassword(config.getPassword());
            bds.setDefaultAutoCommit(false);
            bds.setDefaultTransactionIsolation(DEFAULT_ISOLATION_LEVEL);
            bds.setMaxActive(10);
            datasource = bds;
        }
    }

    /** simple getter */
    protected DataSource getDataSource() {
        return datasource;
    }

    /**
     * Get a new connection from the datasource, check/set autocommit == false and isolation level
     * according to {@link #DEFAULT_ISOLATION_LEVEL}
     */
    protected Connection getConnection() throws SQLException {
        Connection con = getDataSource().getConnection();
        if (con.getAutoCommit()) con.setAutoCommit(false);
        if (con.getTransactionIsolation() != DEFAULT_ISOLATION_LEVEL)
            con.setTransactionIsolation(DEFAULT_ISOLATION_LEVEL);

        return con;
    }

    /** close a sql connection */
    protected void closeConnection(Connection con) throws SQLException {
        con.close();
    }

    /**
     * helper method, if any of the parametres is not null, try to close it and throw away a
     * possible {@link SQLException}
     */
    protected void closeFinally(Connection con, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException ex) {
        }
        try {
            if (ps != null) ps.close();
        } catch (SQLException ex) {
        }
        try {
            if (con != null) closeConnection(con);
        } catch (SQLException ex) {
        }
    }

    /** get a prepared DML statement for a property key */
    protected PreparedStatement getDMLStatement(String key, Connection con)
            throws IOException, SQLException {
        return getJDBCStatement(key, dmlProps, con);
    }

    /**
     * get a prepared Jdbc Statement by looking into the props for the given key
     *
     * @throws IOException if key does not exist
     */
    protected PreparedStatement getJDBCStatement(String key, Properties props, Connection con)
            throws IOException, SQLException {
        String statementString = props.getProperty(key);
        if (statementString == null || statementString.trim().length() == 0)
            throw new IOException("No sql statement for key : " + key);
        return con.prepareStatement(statementString.trim());
    }

    /** get a prepared DDL statement for a property key */
    protected PreparedStatement getDDLStatement(String key, Connection con)
            throws IOException, SQLException {
        return getJDBCStatement(key, ddlProps, con);
    }

    /**
     * create a boolean from a String
     *
     * <p>"Y" or "y" results in true, all other values result in false
     */
    protected boolean convertFromString(String booleanString) {
        if (booleanString == null) return false;
        return "y".equalsIgnoreCase(booleanString);
    }

    /** convert boolean to string true --> "Y" false --> "N" */
    protected String convertToString(boolean b) {
        return b ? "Y" : "N";
    }

    /** Get ordered property keys for creating tables/indexes */
    protected abstract String[] getOrderedNamesForCreate();

    /** Get ordered property keys for dropping tables/indexes */
    protected abstract String[] getOrderedNamesForDrop();

    public void createTablesIfRequired(JDBCSecurityServiceConfig config) throws IOException {

        if (this.canCreateStore() == false) return;
        if (config.isCreatingTables() == false) return;
        if (tablesAlreadyCreated()) return;

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            for (String stmt : getOrderedNamesForCreate()) {
                ps = getDDLStatement(stmt, con);
                ps.execute();
                ps.close();
            }
            con.commit();
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
    }

    /** create tables and indexes, statement order defined by {@link #getOrderedNamesForCreate()} */
    public void createTables() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            for (String stmt : getOrderedNamesForCreate()) {
                ps = getDDLStatement(stmt, con);
                ps.execute();
                ps.close();
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
    }

    /** drops tables, statement oder defined by {@link #getOrderedNamesForDrop()} */
    public void dropTables() throws IOException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = getConnection();
            for (String stmt : getOrderedNamesForDrop()) {
                ps = getDDLStatement(stmt, con);
                ps.execute();
                ps.close();
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, ps, null);
        }
    }

    /** drops tables, ignore SQLExceptions */
    //    public void dropExistingTables() throws IOException {
    //        Connection con = null;
    //        PreparedStatement ps = null;
    //        try {
    //            con = getConnection();
    //            for (String stmt : getOrderedNamesForDrop()) {
    //                try {
    //                    ps= getDDLStatement(stmt, con);
    //                    ps.execute();
    //                    ps.close();
    //                } catch (SQLException ex) {
    //                    // ignore
    //                }
    //            }
    //        } catch (SQLException ex) {
    //            throw new IOException(ex);
    //        } finally {
    //            closeFinally(con, ps, null);
    //        }
    //    }

    /** Check DML statements using {@link #checkSQLStatements(Properties)} */
    public Map<String, SQLException> checkDMLStatements() throws IOException {
        return checkSQLStatements(dmlProps);
    }

    /** Check DDL statements using {@link #checkSQLStatements(Properties)} */
    public Map<String, SQLException> checkDDLStatements() throws IOException {
        return checkSQLStatements(ddlProps);
    }

    /** Checks if the tables are already created */
    public boolean tablesAlreadyCreated() throws IOException {
        ResultSet rs = null;
        Connection con = null;
        try {
            con = getConnection();
            DatabaseMetaData md = con.getMetaData();
            String schemaName = null;
            String tableName = ddlProps.getProperty("check.table");
            if (tableName.contains(".")) {
                StringTokenizer tok = new StringTokenizer(tableName, ".");
                schemaName = tok.nextToken();
                tableName = tok.nextToken();
            }
            // try exact match
            rs = md.getTables(null, schemaName, tableName, null);
            if (rs.next()) return true;

            // try with upper case letters
            rs.close();
            schemaName = schemaName == null ? null : schemaName.toUpperCase();
            tableName = tableName.toUpperCase();
            rs = md.getTables(null, schemaName, tableName, null);
            if (rs.next()) return true;

            // try with lower case letters
            rs.close();
            schemaName = schemaName == null ? null : schemaName.toLowerCase();
            tableName = tableName.toLowerCase();
            rs = md.getTables(null, schemaName, tableName, null);
            if (rs.next()) return true;

            return false;

        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (con != null) closeConnection(con);
            } catch (SQLException e) {
                // do nothing
            }
        }
    }

    /**
     * Checks if the sql statements contained in props can be prepared against the db
     *
     * @return return error protocol containing key,statement and {@link SQLException}. The key is
     *     created as follows:
     *     <p>property key + "|" + statement string
     */
    protected Map<String, SQLException> checkSQLStatements(Properties props) throws IOException {

        Map<String, SQLException> reportMap = new HashMap<>();
        Connection con = null;
        try {
            con = getConnection();
            for (Object key : props.keySet()) {
                String stmt = props.getProperty(key.toString()).trim();
                try {
                    con.prepareStatement(stmt.trim());
                } catch (SQLException ex) {
                    reportMap.put(key.toString() + "|" + stmt, ex);
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            closeFinally(con, null, null);
        }
        return reportMap;
    }

    @Override
    public String toString() {
        return this.getClass() + " : " + getName();
    }

    /**
     * Check for the existence of the file, if the file exists do nothing.
     *
     * <p>If the file does not exist, check for a template file contained in the jar with the same
     * name, if found, use it.
     *
     * <p>If no template was found, use the default template
     *
     * @param fileName target location
     * @param namedRoot parent dir if fileName is relative
     * @param defaultResource the standard template
     * @return the file to use
     */
    protected Resource checkORCreateJDBCPropertyFile(
            String fileName, Resource namedRoot, String defaultResource) throws IOException {

        Resource resource;
        fileName = fileName != null ? fileName : defaultResource;
        File file = new File(fileName);
        if (file.isAbsolute()) {
            resource = Files.asResource(file);
        } else {
            resource = namedRoot.get(fileName);
        }

        if (Resources.exists(resource)) {
            return resource; // we are happy
        }

        // try to find a template with the same name
        try (InputStream is = this.getClass().getResourceAsStream(fileName)) {
            if (is != null) IOUtils.copy(is, resource.out());
            else // use the default template
            FileUtils.copyURLToFile(getClass().getResource(defaultResource), file);
        }

        return resource;
    }
}
