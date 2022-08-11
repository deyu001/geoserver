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

package org.geoserver.security.jdbc.config;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Extension of {@link SecurityNamedServiceConfig} in which the underlying config is stored in a
 * database accessible via JDBC.
 *
 * @author christian
 */
public abstract class JDBCSecurityServiceConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;

    private String propertyFileNameDDL;
    private String propertyFileNameDML;
    private String jndiName;
    private boolean jndi;
    private String driverClassName;
    private String connectURL;
    private String userName;
    private String password;
    private boolean creatingTables;

    public JDBCSecurityServiceConfig() {}

    public JDBCSecurityServiceConfig(JDBCSecurityServiceConfig other) {
        super(other);
        propertyFileNameDDL = other.getPropertyFileNameDDL();
        propertyFileNameDML = other.getPropertyFileNameDML();
        jndiName = other.getJndiName();
        jndi = other.isJndi();
        driverClassName = other.getClassName();
        connectURL = other.getConnectURL();
        userName = other.getUserName();
        password = other.getPassword();
    }

    /**
     * Flag controlling whether to connect through JNDI or through creation of a direct connection.
     *
     * <p>If set {@link #getJndiName()} is used to obtain the connection.
     */
    public boolean isJndi() {
        return jndi;
    }

    /**
     * Set flag controlling whether to connect through JNDI or through creation of a direct
     * connection.
     */
    public void setJndi(boolean jndi) {
        this.jndi = jndi;
    }

    /**
     * Name of JNDI resource for database connection.
     *
     * <p>Used if {@link #isJndi()} is set.
     */
    public String getJndiName() {
        return jndiName;
    }

    /** Sets name of JNDI resource for database connection. */
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    /** File name of property file containing DDL statements. */
    public String getPropertyFileNameDDL() {
        return propertyFileNameDDL;
    }

    /** Sets file name of property file containing DDL statements. */
    public void setPropertyFileNameDDL(String propertyFileNameDDL) {
        this.propertyFileNameDDL = propertyFileNameDDL;
    }

    /** File name of property file containing DML statements. */
    public String getPropertyFileNameDML() {
        return propertyFileNameDML;
    }

    /** Sets file name of property file containing DML statements. */
    public void setPropertyFileNameDML(String propertyFileNameDML) {
        this.propertyFileNameDML = propertyFileNameDML;
    }

    /**
     * The JDBC driver class name.
     *
     * <p>Used only if {@link #isJndi()} is false.
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /** Sets the JDBC driver class name. */
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    /**
     * The JDBC url with which to obtain a database connection with.
     *
     * <p>Used only if {@link #isJndi()} is false.
     */
    public String getConnectURL() {
        return connectURL;
    }

    /** The JDBC url with which to obtain a database connection with. */
    public void setConnectURL(String connectURL) {
        this.connectURL = connectURL;
    }

    /**
     * The database user name.
     *
     * <p>Used only if {@link #isJndi()} is false.
     */
    public String getUserName() {
        return userName;
    }

    /** Sets the database user name. */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * /** The database password.
     *
     * <p>Used only if {@link #isJndi()} is false.
     */
    public String getPassword() {
        return password;
    }

    /** Sets the database password. */
    public void setPassword(String password) {
        this.password = password;
    }

    /** Indicates if the tables are created behind the scenes */
    public boolean isCreatingTables() {
        return creatingTables;
    }

    /** set table creation flag */
    public void setCreatingTables(boolean creatingTables) {
        this.creatingTables = creatingTables;
    }

    /** Helper method to determine if the backing database is mysql. */
    protected boolean isMySQL() {
        return "com.mysql.jdbc.Driver".equals(driverClassName);
    }

    /** Initializes the DDL and DML property files based on the database type. */
    public void initBeforeSave() {
        if (propertyFileNameDDL == null) {
            propertyFileNameDDL = isMySQL() ? defaultDDLFilenameMySQL() : defaultDDLFilename();
        }

        if (propertyFileNameDML == null) {
            propertyFileNameDML = isMySQL() ? defaultDMLFilenameMySQL() : defaultDMLFilename();
        }
    }

    /** return the default filename for the DDL file. */
    protected abstract String defaultDDLFilename();

    /** return the default filename for the DDL file on MySQL. */
    protected abstract String defaultDDLFilenameMySQL();

    /** return the default filename for the DML file. */
    protected abstract String defaultDMLFilename();

    /** return the default filename for the DML file on MySQL. */
    protected abstract String defaultDMLFilenameMySQL();
}
