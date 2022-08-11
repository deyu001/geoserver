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

import java.io.IOException;
import java.sql.SQLException;
import org.geoserver.data.test.LiveSystemTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.GroupAdminServiceTest;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.junit.After;

public class JDBCGroupAdminServiceTest extends GroupAdminServiceTest {

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new LiveSystemTestData(unpackTestDataDir());
    }

    //    @Before
    //    public void init() throws Exception {
    //        super.init();
    //        ugStore.store();
    //        roleStore.store();
    //    }

    @After
    public void rollback() throws Exception {
        if (ugStore != null) ugStore.load();
        if (roleStore != null) roleStore.load();
    }

    //    @AfterClass
    //    public void dropTables() throws Exception {
    //
    //        JDBCRoleStore rs = (JDBCRoleStore) roleStore;
    //        JDBCTestSupport.dropExistingTables(rs, rs.getConnection());
    //        roleStore.store();
    //
    //        JDBCUserGroupStore ugs = (JDBCUserGroupStore) ugStore;
    //        JDBCTestSupport.dropExistingTables(ugs, ugs.getConnection());
    //        ugStore.store();
    //    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        JDBCUserGroupService service =
                (JDBCUserGroupService)
                        JDBCTestSupport.createH2UserGroupService(name, getSecurityManager());
        if (!service.tablesAlreadyCreated()) {
            service.createTables();
        }

        return service;
    }

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        JDBCRoleService service =
                (JDBCRoleService) JDBCTestSupport.createH2RoleService(name, getSecurityManager());
        if (!service.tablesAlreadyCreated()) {
            service.createTables();
        }
        JDBCRoleServiceConfig gaConfig =
                (JDBCRoleServiceConfig) getSecurityManager().loadRoleServiceConfig(name);
        gaConfig.setAdminRoleName("adminRole");
        gaConfig.setGroupAdminRoleName("groupAdminRole");
        getSecurityManager().saveRoleService(gaConfig);
        return getSecurityManager().loadRoleService(name);
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
}
