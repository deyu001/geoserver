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

package org.geoserver.importer;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.importer.job.ProgressMonitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.vfny.geoserver.util.DataStoreUtils;

public class Database extends ImportData {

    /** Database connection parameters */
    Map<String, Serializable> parameters;

    /** List of tables */
    List<Table> tables = new ArrayList<>();

    public Database(Map<String, Serializable> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    public List<Table> getTables() {
        return tables;
    }

    @Override
    public String getName() {
        String database = (String) parameters.get(JDBCDataStoreFactory.DATABASE.key);
        if (database != null) {
            // file based databases might be a full path to a file (sqlite, h2, etc..) use only
            // the last part
            database = FilenameUtils.getBaseName(database);
        }
        return database;
    }

    /** Loads the available tables from this database. */
    @Override
    public void prepare(ProgressMonitor m) throws IOException {
        tables = new ArrayList<>();
        DataStoreFactorySpi factory =
                (DataStoreFactorySpi) DataStoreUtils.aquireFactory(parameters);
        if (factory == null) {
            throw new IOException("Unable to find data store for specified parameters");
        }

        m.setTask("Loading tables");
        DataStore store = factory.createDataStore(parameters);
        if (store == null) {
            throw new IOException("Unable to create data store from specified parameters");
        }

        format = DataFormat.lookup(parameters);

        try {
            for (String typeName : store.getTypeNames()) {
                Table tbl = new Table(typeName, this);
                tbl.setFormat(format);
                tables.add(tbl);
            }
        } finally {
            // TODO: cache the datastore for subsquent calls
            store.dispose();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (parameters.containsKey(JDBCDataStoreFactory.USER.key)) {
            sb.append(parameters.get(JDBCDataStoreFactory.USER.key)).append("@");
        }
        if (parameters.containsKey(JDBCDataStoreFactory.HOST.key)) {
            sb.append(parameters.get(JDBCDataStoreFactory.HOST.key));
        }
        if (parameters.containsKey(JDBCDataStoreFactory.PORT.key)) {
            sb.append(":").append(parameters.get(JDBCDataStoreFactory.PORT.key));
        }
        if (sb.length() > 0) {
            sb.append("/");
        }
        sb.append(getName());
        return sb.toString();
    }

    @Override
    public void reattach() {
        for (Table t : tables) {
            t.setDatabase(this);
        }
    }

    @Override
    public Table part(final String name) {
        return Iterables.find(
                tables,
                new Predicate<Table>() {
                    @Override
                    public boolean apply(Table input) {
                        return name.equals(input.getName());
                    }
                });
    }

    private Object readResolve() {
        tables = tables != null ? tables : new ArrayList<>();
        return this;
    }
}
