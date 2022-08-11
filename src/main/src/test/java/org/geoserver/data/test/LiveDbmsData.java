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

package org.geoserver.data.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.util.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.jdbc.JDBCUtils;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.logging.Logging;

/**
 * Extends LiveData to deal with the needs of a data directory that uses a DBMS among its data
 * stores. In particular it provides:
 *
 * <ul>
 *   <li>Facilities to replace templates in configuration files, so that the connection parameters
 *       can be replaced with the local version (provided as a key, value map)
 *   <li>Facilities to setup the database structure from a sql script. The sql must have each
 *       command on a single line
 * </ul>
 *
 * @author Andrea Aime - TOPP
 */
@SuppressWarnings({
    "PMD.JUnit4TestShouldUseBeforeAnnotation",
    "PMD.JUnit4TestShouldUseAfterAnnotation"
})
public class LiveDbmsData extends LiveSystemTestData {
    private static final Logger LOGGER = Logging.getLogger(LiveDbmsData.class);

    /**
     * The property file containing the token -> value pairs used for filtering and to grab a JDBC
     * datastore connection.
     */
    protected File fixture;

    /**
     * List of file paths (relative to the source data directory) that will be subjected to token
     * filtering. By default only <code>catalog.xml</code> will be filtered.
     */
    protected List<String> filteredPaths = new ArrayList<>(Arrays.asList("catalog.xml"));

    protected File sqlScript;

    /** Can be used to make sure the datastore used to grab connections is disposed of */
    protected DataStore ds;

    /**
     * The identifier of the fixture, which is also the name of the file (followed by .properties)
     * and the system property used to disable the test (prefixed by gs.)
     */
    protected String fixtureId;

    /**
     * Builds a new LiveDbmsData with the minimal parameter needed:
     *
     * <ul>
     *   <li>the source data directory to be copied
     *   <li>the path to a property file containing the set of parameters required to grab a
     *       connection with a jdbc datastore, which will be also used to filter out the files
     *       during copy (by default, only catalog.xml)
     *   <li>the location of the sql script used to initialize the database (this one can be null if
     *       no initialization is needed). It's advisable to prepare a sql script that first drops
     *       all tables and views and then recreates them, if a statement fails it'll be logged and
     *       skipped anyways. This makes it possible to inspect the database contents
     */
    public LiveDbmsData(File dataDirSourceDirectory, String fixtureId, File sqlScript)
            throws IOException {
        super(dataDirSourceDirectory);
        this.fixture = lookupFixture(fixtureId);
        this.fixtureId = fixtureId;
        this.sqlScript = sqlScript;
    }

    /** Looks up the fixture file in the home directory provided that the */
    private File lookupFixture(String fixtureId) {
        // first of all, make sure the fixture was not disabled using a system
        // variable
        final String property = System.getProperty("gs." + fixtureId);
        if (property != null && "false".equals(property.toLowerCase())) {
            return null;
        }

        // then look in the user home directory
        File base = new File(System.getProperty("user.home"), ".geoserver");
        // create the hidden folder, this is handy especially on windows where
        // a user cannot create a directory starting with . from the UI
        // (works only from the command line)
        if (!base.exists()) base.mkdir();
        File fixtureFile = new File(base, fixtureId + ".properties");
        if (!fixtureFile.exists()) {
            final String warning =
                    "Disabling test based on fixture "
                            + fixtureId
                            + " since the file "
                            + fixtureFile
                            + " could not be found";
            disableTest(warning);
            return null;
        }

        return fixtureFile;
    }

    public boolean isTestDataAvailable() {
        return fixture != null;
    }

    @Override
    @SuppressWarnings({"PMD.CloseResource", "PMD.UseTryWithResources"})
    public void setUp() throws Exception {
        // if the test was disabled we don't need to run the setup
        if (fixture == null) return;

        super.setUp();

        // load the properties from the fixture path and load them into a
        // Map<String, String>
        Properties p = new Properties();
        p.load(new FileInputStream(fixture));
        Map<String, String> filters = new HashMap<>();
        p.forEach((k, v) -> filters.put((String) k, (String) v));

        // replace the keys contained in catalog.xml with the actual values
        if (filteredPaths != null && !filteredPaths.isEmpty()) {
            for (String path : filteredPaths) {
                File from = new File(source, path);
                File to = new File(data, path);
                IOUtils.filteredCopy(from, to, filters);
            }
        }

        // populate the db
        if (sqlScript != null) {
            DataStore ds = null;
            Connection conn = null;
            Statement st = null;
            BufferedReader reader = null;
            try {
                ds = DataStoreFinder.getDataStore(filters);
                if (ds == null) {
                    final String warning =
                            "Disabling online test based on '"
                                    + fixtureId
                                    + "', "
                                    + "could not find a data store compatible "
                                    + "with the following connection properties: "
                                    + filters;
                    disableTest(warning);
                    return;
                }

                conn = getDatabaseConnection(ds);

                if (conn == null) {
                    final String warning =
                            "Disabling online test based on '"
                                    + fixtureId
                                    + "', "
                                    + "could not extract a JDBC connection from the datastore '"
                                    + ds.getClass()
                                    + " obtained using the following "
                                    + "connection properties: "
                                    + filters;
                    disableTest(warning);
                    return;
                }

                // read the script and run the setup commands
                reader = new BufferedReader(new FileReader(sqlScript));
                st = conn.createStatement();
                String command = null;
                while ((command = reader.readLine()) != null) {
                    command = command.trim();
                    // skip comments and empty lines
                    if ("".equals(command) || command.startsWith("--") || command.startsWith("#"))
                        continue;

                    // execute but do not complain, only log the failures
                    try {
                        st.execute(command);
                    } catch (SQLException e) {
                        LOGGER.warning("Error executing \"" + command + "\": " + e.getMessage());
                    }
                }
            } finally {
                JDBCUtils.close(st);
                JDBCUtils.close(conn, null, null);
                // very important, or we'll leak connection pools during
                // execution
                if (ds != null) ds.dispose();
                if (reader != null) reader.close();
            }
        }
    }

    /**
     * Uses the current {@link JDBCDataStore} facilities to grab a connection, subclasses can
     * override to use other methods
     */
    protected Connection getDatabaseConnection(DataStore ds) throws IOException {
        if (ds instanceof JDBCDataStore) {
            return ((JDBCDataStore) ds).getConnection(Transaction.AUTO_COMMIT);
        } else {
            return null;
        }
    }

    /** Returns the filtered paths list as a live list (can be modified directly) */
    public List<String> getFilteredPaths() {
        return filteredPaths;
    }

    /**
     * Permanently disable this test logging the specificed warning message (the reason why the test
     * is being disabled)
     */
    private void disableTest(final String warning) {
        LOGGER.warning(warning);
        fixture = null;
        System.setProperty("gs." + fixtureId, "false");
    }
}
