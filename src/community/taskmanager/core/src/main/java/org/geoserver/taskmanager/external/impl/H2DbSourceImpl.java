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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.DbTable;
import org.geoserver.taskmanager.external.Dialect;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.util.SecuredImpl;
import org.h2.tools.RunScript;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * DbSource for Postgres.
 *
 * @author Timothy De Bock
 */
public class H2DbSourceImpl extends SecuredImpl implements DbSource {

    private String path;

    private String db;

    private Resource createDBSqlResource;

    private Resource createDataSqlResource;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public Resource getCreateDBSqlResource() {
        return createDBSqlResource;
    }

    public void setCreateDBSqlResource(Resource createDBSqlResource) {
        this.createDBSqlResource = createDBSqlResource;
    }

    public Resource getCreateDataSqlResource() {
        return createDataSqlResource;
    }

    public void setCreateDataSqlResource(Resource createDataSqlResource) {
        this.createDataSqlResource = createDataSqlResource;
    }

    @Override
    public DataSource getDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        String url = "jdbc:h2:" + path + ":" + db + ";DB_CLOSE_DELAY=-1";
        dataSource.setUrl(url);
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    @Override
    public GSAbstractStoreEncoder getStoreEncoder(String name, ExternalGS extGs) {
        throw new UnsupportedOperationException("Generic datasource cannot be used as a store.");
    }

    @Override
    public Map<String, Serializable> getParameters() {
        throw new UnsupportedOperationException("Generic datasource cannot be used as a store.");
    }

    @Override
    public GSAbstractStoreEncoder postProcess(GSAbstractStoreEncoder encoder, DbTable table) {
        throw new UnsupportedOperationException("Generic datasource cannot be used as a store.");
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        if (createDBSqlResource != null) {
            Connection connection = null;
            connection = getDataSource().getConnection();

            runSql(createDBSqlResource, connection);
        }
    }

    @Override
    public String getSchema() {
        return null;
    }

    // utility method to read a .sql txt input stream
    private void runSql(Resource resource, Connection connection) throws IOException, SQLException {
        InputStream is = null;
        try {
            is = resource.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            RunScript.execute(connection, reader);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Override
    public Dialect getDialect() {
        return new H2DialectImpl();
    }
}
