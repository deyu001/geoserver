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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class JDBCConnectAuthProviderTest extends AbstractAuthenticationProviderTest {

    protected JDBCConnectAuthProviderConfig createAuthConfg(
            String name, String userGroupServiceName) {
        JDBCConnectAuthProviderConfig config = new JDBCConnectAuthProviderConfig();
        config.setName(name);
        config.setClassName(JDBCConnectAuthProvider.class.getName());
        config.setUserGroupServiceName(userGroupServiceName);
        config.setConnectURL("jdbc:h2:target/h2/security");
        config.setDriverClassName("org.h2.Driver");
        return config;
    }

    @Test
    public void testAuthentificationWithoutUserGroupService() throws Exception {
        JDBCConnectAuthProviderConfig config = createAuthConfg("jdbc1", null);
        getSecurityManager().saveAuthenticationProvider(config);
        GeoServerAuthenticationProvider provider =
                getSecurityManager().loadAuthenticationProvider("jdbc1");

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("sa", "");
        token.setDetails("details");
        assertTrue(provider.supports(token.getClass()));
        assertFalse(provider.supports(RememberMeAuthenticationToken.class));

        Authentication auth = provider.authenticate(token);
        assertNotNull(auth);
        assertEquals("sa", auth.getPrincipal());
        assertNull(auth.getCredentials());

        assertEquals("details", auth.getDetails());
        assertEquals(1, auth.getAuthorities().size());
        checkForAuthenticatedRole(auth);

        token = new UsernamePasswordAuthenticationToken("abc", "def");
        boolean fail = false;
        try {
            if (provider.authenticate(token) == null) fail = true;
        } catch (BadCredentialsException ex) {
            fail = true;
        }
        assertTrue(fail);
    }

    @Test
    public void testAuthentificationWithUserGroupService() throws Exception {
        GeoServerRoleService roleService = createRoleService("jdbc2");
        GeoServerUserGroupService ugService = createUserGroupService("jdbc2");
        JDBCConnectAuthProviderConfig config = createAuthConfg("jdbc2", ugService.getName());
        getSecurityManager().saveAuthenticationProvider(config);
        GeoServerAuthenticationProvider provider =
                getSecurityManager().loadAuthenticationProvider("jdbc2");

        GeoServerUserGroupStore ugStore = ugService.createStore();
        GeoServerUser sa = ugStore.createUserObject("sa", "", true);
        ugStore.addUser(sa);
        ugStore.store();

        GeoServerRoleStore roleStore = roleService.createStore();
        roleStore.addRole(GeoServerRole.ADMIN_ROLE);
        roleStore.associateRoleToUser(GeoServerRole.ADMIN_ROLE, sa.getUsername());
        roleStore.store();
        getSecurityManager().setActiveRoleService(roleService);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("sa", "");
        token.setDetails("details");
        assertTrue(provider.supports(token.getClass()));
        assertFalse(provider.supports(RememberMeAuthenticationToken.class));

        Authentication auth = provider.authenticate(token);
        assertNotNull(auth);
        assertEquals("sa", auth.getPrincipal());
        assertNull(auth.getCredentials());
        assertEquals("details", auth.getDetails());
        assertEquals(2, auth.getAuthorities().size());
        checkForAuthenticatedRole(auth);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // Test disabled user
        ugStore = ugService.createStore();
        sa.setEnabled(false);
        ugStore.updateUser(sa);
        ugStore.store();

        assertNull(provider.authenticate(token));

        // test invalid user
        token = new UsernamePasswordAuthenticationToken("abc", "def");
        boolean fail = false;
        try {
            if (provider.authenticate(token) == null) fail = true;
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            fail = true;
        }

        assertTrue(fail);
    }

    @Test
    public void testAuthentificationWithRoleAssociation() throws Exception {
        GeoServerRoleService roleService = createRoleService("jdbc3");
        JDBCConnectAuthProviderConfig config = createAuthConfg("jdbc3", null);
        getSecurityManager().saveAuthenticationProvider(config);
        GeoServerAuthenticationProvider provider =
                getSecurityManager().loadAuthenticationProvider("jdbc3");

        GeoServerRoleStore roleStore = roleService.createStore();
        roleStore.addRole(GeoServerRole.ADMIN_ROLE);
        roleStore.associateRoleToUser(GeoServerRole.ADMIN_ROLE, "sa");
        roleStore.store();
        getSecurityManager().setActiveRoleService(roleService);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("sa", "");
        token.setDetails("details");
        assertTrue(provider.supports(token.getClass()));
        assertFalse(provider.supports(RememberMeAuthenticationToken.class));

        Authentication auth = provider.authenticate(token);
        assertNotNull(auth);
        assertEquals("sa", auth.getPrincipal());
        assertNull(auth.getCredentials());
        assertEquals("details", auth.getDetails());
        assertEquals(2, auth.getAuthorities().size());
        checkForAuthenticatedRole(auth);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // test invalid user
        token = new UsernamePasswordAuthenticationToken("abc", "def");
        boolean fail = false;
        try {
            if (provider.authenticate(token) == null) fail = true;
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            fail = true;
        }

        assertTrue(fail);
    }
}
