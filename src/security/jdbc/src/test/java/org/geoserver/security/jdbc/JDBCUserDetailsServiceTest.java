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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.AbstractUserDetailsServiceTest;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public abstract class JDBCUserDetailsServiceTest extends AbstractUserDetailsServiceTest {

    protected abstract String getFixtureId();

    @Before
    public void init() {
        Assume.assumeTrue(getTestData().isTestDataAvailable());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {

        return JDBCTestSupport.createUserGroupService(
                getFixtureId(), (LiveDbmsDataSecurity) getTestData(), getSecurityManager());
    }

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return JDBCTestSupport.createRoleService(
                getFixtureId(), (LiveDbmsDataSecurity) getTestData(), getSecurityManager());
    }

    @Override
    public GeoServerRoleStore createStore(GeoServerRoleService service) throws IOException {
        JDBCRoleStore store = (JDBCRoleStore) super.createStore(service);
        try {
            JDBCTestSupport.dropExistingTables(store, store.getConnection());
        } catch (SQLException e) {
            throw new IOException(e);
        }
        store.createTables();
        store.store();

        return store;
    }

    @Override
    public GeoServerUserGroupStore createStore(GeoServerUserGroupService service)
            throws IOException {
        JDBCUserGroupStore store = (JDBCUserGroupStore) super.createStore(service);
        try {
            JDBCTestSupport.dropExistingTables(store, store.getConnection());
        } catch (SQLException e) {
            throw new IOException(e);
        }
        store.createTables();
        store.store();
        return store;
    }

    @After
    public void dropTables() throws Exception {
        if (roleStore != null) {
            JDBCRoleStore jdbcStore1 = (JDBCRoleStore) roleStore;
            JDBCTestSupport.dropExistingTables(jdbcStore1, jdbcStore1.getConnection());
            roleStore.store();
        }

        if (usergroupStore != null) {
            JDBCUserGroupStore jdbcStore2 = (JDBCUserGroupStore) usergroupStore;
            JDBCTestSupport.dropExistingTables(jdbcStore2, jdbcStore2.getConnection());
            usergroupStore.store();
        }
    }

    @Override
    protected void setServices(String serviceName) throws Exception {
        if (getSecurityManager().loadRoleService(getFixtureId()) == null)
            super.setServices(getFixtureId());
        else {
            roleService = getSecurityManager().loadRoleService(getFixtureId());
            roleStore = createStore(roleService);
            usergroupService = getSecurityManager().loadUserGroupService(getFixtureId());
            usergroupStore = createStore(usergroupService);
            getSecurityManager().setActiveRoleService(roleService);
        }
    }

    @Override
    protected boolean isJDBCTest() {
        return true;
    }

    @Override
    protected SystemTestData createTestData() throws Exception {
        if ("h2".equalsIgnoreCase(getFixtureId())) return super.createTestData();
        return new LiveDbmsDataSecurity(getFixtureId());
    }

    @Test
    public void testConfiguration() throws Exception {
        setServices("config");
        assertEquals(roleService, getSecurityManager().getActiveRoleService());
        // assertEquals(usergroupService,getSecurityManager().getActiveUserGroupService());
        assertEquals(
                usergroupService.getName(),
                getSecurityManager().loadUserGroupService(getFixtureId()).getName());
        assertTrue(roleService.canCreateStore());
        assertTrue(usergroupService.canCreateStore());
    }
}
