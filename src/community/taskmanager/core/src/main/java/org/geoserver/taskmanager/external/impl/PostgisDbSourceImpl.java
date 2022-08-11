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

import it.geosolutions.geoserver.rest.encoder.GSAbstractStoreEncoder;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.Dialect;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.util.SecuredImpl;
import org.geoserver.taskmanager.util.SqlUtil;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * DbSource for Postgres.
 *
 * @author Niels Charlier
 */
public class PostgisDbSourceImpl extends SecuredImpl implements DbSource {

    private String host;

    private int port = 5432;

    private String db;

    private boolean ssl = false;

    private String schema;

    private String username;

    private String password;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public DataSource getDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        if (schema != null) {
            url += "?currentSchema=" + schema + ",public";
        }
        if (ssl) {
            url += (schema == null ? "?" : "&") + "sslmode=require";
        }
        dataSource.setUrl(url);
        return dataSource;
    }

    @Override
    public GSAbstractStoreEncoder getStoreEncoder(String name, ExternalGS extGs) {
        GSPostGISDatastoreEncoder encoder = new GSPostGISDatastoreEncoder(name);
        encoder.setHost(host);
        encoder.setPort(port);
        encoder.setDatabase(db);
        encoder.setSchema(schema);
        encoder.setUser(username);
        encoder.setPassword(password);
        return encoder;
    }

    @Override
    public Map<String, Serializable> getParameters() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
        params.put(PostgisNGDataStoreFactory.HOST.key, host);
        params.put(PostgisNGDataStoreFactory.PORT.key, port);
        params.put(PostgisNGDataStoreFactory.DATABASE.key, db);
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
        params.put(PostgisNGDataStoreFactory.USER.key, username);
        params.put(PostgisNGDataStoreFactory.PASSWD.key, password);
        return params;
    }

    @Override
    public GSAbstractStoreEncoder postProcess(GSAbstractStoreEncoder encoder, DbTable table) {
        if (table != null) {
            String schema = SqlUtil.schema(table.getTableName());
            if (schema != null) {
                ((GSPostGISDatastoreEncoder) encoder).setSchema(schema);
            }
        }
        return encoder;
    }

    @Override
    public Dialect getDialect() {
        return new PostgisDialectImpl();
    }

    /*
    @Override
    public InputStream dump(String realTableName, String tempTableName) throws IOException {
        String url = "jdbc:postgresql://" + username + ":" + password + "@" + host + ":" + port + "/" + db;
        if (ssl) {
            url +=  "?sslmode=require";
        }
        Process pr = Runtime.getRuntime().exec(
                "pg_dump --dbname=" + url + " --table " + (schema == null ? "" : schema + ".") + realTableName);

        //to do: remove the search_path from the script
        //+ replace all names (table, sequences, indexes, constraints) to temporary names

        return pr.getInputStream();
    }

    @Override
    public OutputStream script() throws IOException {
        String url = "jdbc:postgresql://" + username + ":" + password + "@" + host + ":" + port + "/" + db;
        if (ssl) {
            url +=  "?sslmode=require";
        }
        if (schema != null) {
            url +=  (ssl ? "&" : "?") + "options=--search_path%3D" + schema;
        }
        Process pr = Runtime.getRuntime().exec("psql --dbname=" + url);
        return pr.getOutputStream();
    }*/

}
