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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import org.geotools.data.jdbc.datasource.DataSourceFinder;
import org.geotools.data.jdbc.datasource.UnWrapper;
import org.junit.Before;
import org.junit.Test;

/**
 * Uses the known target org.apache.commons.dbcp.DelegatingStatement to test out GenericUnWrapper in
 * isolation.
 *
 * @author Jody Garnett (Boundless)
 */
public class GenericUnWrapperTest {
    GenericUnWrapper unwrapper = new GenericUnWrapper();

    @Before
    public void reset() {
        // reset generic wrapper state
        GenericUnWrapper.CONNECTION_METHODS.remove(WrapperConnection.class);
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    public void testUnwrapConnection()
            throws SQLException, NoSuchMethodException, SecurityException {
        Connection connection = new TestConnection();
        Connection wrapper = new WrapperConnection(connection);
        assertTrue(wrapper.isWrapperFor(Connection.class));
        Connection unwrap = wrapper.unwrap(Connection.class);

        assertSame(connection, unwrap);

        UnWrapper unwrapper = new GenericUnWrapper();

        assertFalse(unwrapper.canUnwrap(wrapper));
        try {
            assertNull(unwrapper.unwrap(wrapper));
            fail("Cannot unwrap yet");
        } catch (Exception expected) {
        }
        GenericUnWrapper.CONNECTION_METHODS.put(
                WrapperConnection.class,
                WrapperConnection.class.getMethod("getUnderlyingConnection", null));

        assertTrue(unwrapper.canUnwrap(wrapper));
        assertSame(connection, unwrapper.unwrap(wrapper));
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    public void testSPIRegistration() throws Exception {
        Connection connection = new TestConnection();
        Connection wrapper = new WrapperConnection(connection);

        GenericUnWrapper.CONNECTION_METHODS.put(
                WrapperConnection.class,
                WrapperConnection.class.getMethod("getUnderlyingConnection", null));

        UnWrapper uw = DataSourceFinder.getUnWrapper(wrapper);
        assertNotNull("registed and canUnwrap", uw);
        if (uw instanceof GenericUnWrapper) {
            assertSame("Generic unwrapper is working", connection, uw.unwrap(wrapper));
        }
    }
}
