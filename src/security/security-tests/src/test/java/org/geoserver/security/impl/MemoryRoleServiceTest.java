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

package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class MemoryRoleServiceTest extends AbstractRoleServiceTest {

    @Override
    public GeoServerRoleService createRoleService(String name) throws IOException {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        service.setSecurityManager(getSecurityManager());
        return service;
    }

    @Before
    public void init() throws IOException {
        service = createRoleService("test");
        store = service.createStore();
    }
    //    @After
    //    public void clearRoleService() throws IOException {
    //        store.clear();
    //    }

    @Test
    public void testInsert() throws Exception {
        super.testInsert();
        for (GeoServerRole role : store.getRoles()) {
            assertSame(role.getClass(), MemoryGeoserverRole.class);
        }
    }

    @Test
    public void testMappedAdminRoles() throws Exception {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName("testAdminRole");
        config.setAdminRoleName("adminRole");
        config.setGroupAdminRoleName("groupAdminRole");
        config.setClassName(MemoryRoleService.class.getName());
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        GeoServerSecurityManager manager = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        service.setSecurityManager(manager);
        manager.setActiveRoleService(service);
        manager.saveRoleService(config);

        GeoServerRoleStore store = service.createStore();
        GeoServerRole adminRole = store.createRoleObject("adminRole");
        GeoServerRole groupAdminRole = store.createRoleObject("groupAdminRole");
        GeoServerRole role1 = store.createRoleObject("role1");
        store.addRole(adminRole);
        store.addRole(groupAdminRole);
        store.addRole(role1);

        store.associateRoleToUser(adminRole, "user1");
        store.associateRoleToUser(groupAdminRole, "user1");
        store.associateRoleToUser(adminRole, "user2");
        store.associateRoleToUser(role1, "user3");
        store.store();

        MemoryUserGroupServiceConfigImpl ugconfig = new MemoryUserGroupServiceConfigImpl();
        ugconfig.setName("testAdminRole");
        ugconfig.setClassName(MemoryUserGroupService.class.getName());
        ugconfig.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        ugconfig.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        GeoServerUserGroupService ugService = new MemoryUserGroupService();
        ugService.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        ugService.initializeFromConfig(ugconfig);

        RoleCalculator calc = new RoleCalculator(ugService, service);
        SortedSet<GeoServerRole> roles;

        roles = calc.calculateRoles(ugService.createUserObject("user1", "abc", true));
        assertEquals(4, roles.size());
        assertTrue(roles.contains(adminRole));
        assertTrue(roles.contains(GeoServerRole.ADMIN_ROLE));
        assertTrue(roles.contains(groupAdminRole));
        assertTrue(roles.contains(GeoServerRole.GROUP_ADMIN_ROLE));

        roles = calc.calculateRoles(ugService.createUserObject("user2", "abc", true));
        assertEquals(2, roles.size());
        assertTrue(roles.contains(adminRole));
        assertTrue(roles.contains(GeoServerRole.ADMIN_ROLE));

        roles = calc.calculateRoles(ugService.createUserObject("user3", "abc", true));
        assertEquals(1, roles.size());
        assertTrue(roles.contains(role1));
    }
}
