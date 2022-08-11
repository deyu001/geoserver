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

package org.geoserver.monitor.hib;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

public class MonitoringDataSource extends BasicDataSource implements DisposableBean {

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    MonitorConfig config;
    GeoServerDataDirectory dataDirectory;

    public void setConfig(MonitorConfig config) {
        this.config = config;
    }

    public void setDataDirectory(GeoServerDataDirectory dataDir) {
        this.dataDirectory = dataDir;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            if (getDriverClassName() == null) {
                synchronized (this) {
                    if (getDriverClassName() == null) {
                        initializeDataSource();
                    }
                }
            }
            return super.getConnection();
        } catch (Exception e) {
            // LOGGER.log(Level.WARNING, "Database connection error", e);
            config.setError(e);
            config.setEnabled(false);

            if (e instanceof SQLException) {
                throw (SQLException) e;
            }

            throw (SQLException) new SQLException().initCause(e);
        }
    }

    void initializeDataSource() throws Exception {
        Resource monitoringDir = dataDirectory.get("monitoring");
        Resource dbprops = monitoringDir.get("db.properties");
        if (Resources.exists(dbprops)) {
            LOGGER.info("Configuring monitoring database from: " + dbprops.path());

            // attempt to configure
            try {
                configureDataSource(dbprops, monitoringDir);
            } catch (SQLException e) {
                // configure failed, try db1.properties
                dbprops = monitoringDir.get("db1.properties");
                if (Resources.exists(dbprops)) {
                    try {
                        configureDataSource(dbprops, monitoringDir);

                        // secondary file worked, return
                        return;
                    } catch (SQLException e1) {
                        // secondary file failed as well, try for third
                        dbprops = monitoringDir.get("db2.properties");
                        if (Resources.exists(dbprops)) {
                            try {
                                configureDataSource(dbprops, monitoringDir);

                                // third file worked, return
                                return;
                            } catch (SQLException e2) {
                            }
                        }
                    }
                }

                throw e;
            }
        } else {
            // no db.properties file, use internal default
            configureDataSource(null, monitoringDir);
        }
    }

    void configureDataSource(Resource dbprops, Resource monitoringDir) throws Exception {
        Properties db = new Properties();

        if (dbprops == null) {
            dbprops = monitoringDir.get("db.properties");

            // use a default, and copy the template over
            try (InputStream in = getClass().getResourceAsStream("db.properties");
                    OutputStream out = dbprops.out()) {
                IOUtils.copy(in, out);
            }

            try (InputStream in = getClass().getResourceAsStream("db.properties")) {
                db.load(in);
            }
        } else {
            try (InputStream in = dbprops.in()) {
                db.load(in);
            }
        }

        logDbProperties(db);

        // TODO: check for nulls
        setDriverClassName(db.getProperty("driver"));
        setUrl(getURL(db));

        if (db.containsKey("username")) {
            setUsername(db.getProperty("username"));
        }
        if (db.containsKey("password")) {
            setPassword(db.getProperty("password"));
        }

        setDefaultAutoCommit(Boolean.valueOf(db.getProperty("defaultAutoCommit", "true")));

        // TODO: make other parameters configurable
        setMinIdle(1);
        setMaxActive(4);

        // test the connection
        super.getConnection();
    }

    void logDbProperties(Properties db) {
        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("Monitoring database connection info:\n");
            for (Map.Entry e : db.entrySet()) {
                sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
            LOGGER.fine(sb.toString());
        }
    }

    String getURL(Properties db) {
        return db.getProperty("url")
                .replace("${GEOSERVER_DATA_DIR}", dataDirectory.root().getAbsolutePath());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void destroy() throws Exception {
        super.close();
    }
}
