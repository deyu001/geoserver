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

package org.geoserver.test.onlineTest.support;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.junit.Assume;

/**
 * Base class that provides the Wfs test support framework and perform checks on the fixture and the
 * availabilities of the fixture required
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public abstract class AbstractDataReferenceWfsTest extends AbstractAppSchemaTestSupport {
    protected AbstractReferenceDataSetup setup = null;

    protected Properties fixture = null;

    protected boolean available;

    public AbstractDataReferenceWfsTest() throws Exception {
        setup = this.getReferenceDataSetup();
        available = this.checkAvailable();
        Assume.assumeTrue(available);
        if (available) initialiseTest();
    }

    /**
     * The key in the test fixture property file used to set the behaviour of the online test if
     * {@link #connect()} fails.
     */
    public static final String SKIP_ON_FAILURE_KEY = "skip.on.failure";

    /** The default value used for {@link #SKIP_ON_FAILURE_KEY} if it is not present. */
    public static final String SKIP_ON_FAILURE_DEFAULT = "true";

    protected boolean skipOnFailure = true;

    /**
     * A static map which tracks which fixture files can not be found. This prevents continually
     * looking up the file and reporting it not found to the user.
     */
    protected static Map<String, Boolean> found = new HashMap<>();

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        setup.setUp();

        super.setUpTestData(testData);
    }

    public abstract AbstractReferenceDataSetup getReferenceDataSetup() throws Exception;

    public void connect() throws Exception {
        setup.initializeDatabase();
        setup.setUpData();
    }

    /**
     * Loads the test fixture for the test case.
     *
     * <p>The fixture id is obtained via {@link #getFixtureId()}.
     */
    protected final void initialiseTest() throws Exception {
        skipOnFailure =
                Boolean.parseBoolean(
                        fixture.getProperty(SKIP_ON_FAILURE_KEY, SKIP_ON_FAILURE_DEFAULT));
        // call the setUp template method
        try {
            connect();
        } catch (Exception e) {
            if (skipOnFailure) {
                // disable the test
                fixture = null;
                // leave some trace of the swallowed exception
                e.printStackTrace();
            } else {
                // do not swallow the exception
                throw e;
            }
        }
    }

    /**
     * Check whether the fixture is available. This method also loads the configuration if present,
     * and tests the connection using {@link #isOnline()}.
     *
     * @return true if fixture is available for use
     */
    protected boolean checkAvailable() throws Exception {

        setup.configureFixture();
        fixture = setup.getFixture();
        if (fixture == null) {
            return false;
        } else {
            String fixtureId = getFixtureId();
            setup.setFixture(fixture);
            // do an online/offline check
            Map<String, Boolean> online = setup.getOnlineMap();
            Boolean available = online.get(fixtureId);
            if (available == null || available.booleanValue()) {
                // test the connection
                try {
                    available = isOnline();
                } catch (Throwable t) {
                    LOGGER.log(
                            Level.WARNING,
                            "Skipping " + fixtureId + " tests, resources not available.",
                            t);

                    available = Boolean.FALSE;
                }
                online.put(fixtureId, available);
            }
            return available;
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private Boolean isOnline() {
        try {
            DataSource dataSource = setup.getDataSource();
            Connection cx = dataSource.getConnection();
            cx.close();
            return true;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected String getFixtureId() {
        return setup.getDatabaseID();
    }
}
