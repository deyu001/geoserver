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

package org.geoserver.security.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.MemoryRoleService;
import org.geoserver.security.impl.MemoryRoleStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it" */
public class LDAPAuthenticationProviderTest extends LDAPBaseTest {

    protected LDAPAuthenticationProvider authProvider;

    @Override
    protected void createConfig() {
        config = new LDAPSecurityServiceConfig();
    }

    protected void createAuthenticationProvider() {
        authProvider =
                (LDAPAuthenticationProvider) securityProvider.createAuthenticationProvider(config);
    }

    @RunWith(FrameworkRunner.class)
    @CreateLdapServer(
        transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
        allowAnonymousAccess = true
    )
    @CreateDS(
        name = "myDS",
        partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)}
    )
    @ApplyLdifFiles({"data.ldif"})
    public static class LDAPAuthenticationProviderDataTest extends LDAPAuthenticationProviderTest {

        /**
         * LdapTestUtils Test that bindBeforeGroupSearch correctly enables roles fetching on a
         * server without anonymous access enabled.
         */
        @Test
        public void testBindBeforeGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(false);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authentication);
            assertNotNull(result);
            assertEquals("admin", result.getName());
            assertEquals(3, result.getAuthorities().size());
        }

        /**
         * Test that without bindBeforeGroupSearch we get an exception during roles fetching on a
         * server without anonymous access enabled.
         */
        @Test
        public void testBindBeforeGroupSearchRequiredIfAnonymousDisabled() throws Exception {
            // no anonymous access
            try {
                getService().setAllowAnonymousAccess(false);
                ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
                // we don't bind
                config.setBindBeforeGroupSearch(false);
                createAuthenticationProvider();
                boolean error = false;
                try {
                    authProvider.authenticate(authentication);
                } catch (Exception e) {
                    error = true;
                }
                assertTrue(error);
            } finally {
                getService().setAllowAnonymousAccess(true);
            }
        }

        /**
         * Test that authentication can be done using the couple userFilter and userFormat instead
         * of userDnPattern.
         */
        @Test
        public void testUserFilterAndFormat() throws Exception {
            getService().setAllowAnonymousAccess(true);
            // filter to extract user data
            config.setUserFilter("(telephonenumber=1)");
            // username to bind to
            ((LDAPSecurityServiceConfig) config)
                    .setUserFormat("uid={0},ou=People,dc=example,dc=com");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authentication);
            assertEquals(3, result.getAuthorities().size());
        }

        /**
         * Test that authentication can be done using the couple userFilter and userFormat instead
         * of userDnPattern, using placemarks in userFilter.
         */
        @Test
        public void testUserFilterPlacemarks() throws Exception {
            getService().setAllowAnonymousAccess(true);
            // filter to extract user data
            config.setUserFilter("(givenName={1})");
            // username to bind to
            ((LDAPSecurityServiceConfig) config)
                    .setUserFormat("uid={0},ou=People,dc=example,dc=com");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authentication);
            assertEquals(3, result.getAuthorities().size());

            // filter to extract user data
            config.setUserFilter("(cn={0})");
            // username to bind to
            ((LDAPSecurityServiceConfig) config)
                    .setUserFormat("uid={0},ou=People,dc=example,dc=com");

            createAuthenticationProvider();

            result = authProvider.authenticate(authentication);
            assertEquals(3, result.getAuthorities().size());
        }

        /** Test that if and adminGroup is defined, the roles contain ROLE_ADMINISTRATOR */
        @Test
        public void testAdminGroup() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setAdminGroup("other");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRATOR")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }

        /** Test that if and groupAdminGroup is defined, the roles contain ROLE_GROUP_ADMIN */
        @Test
        public void testGroupAdminGroup() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setGroupAdminGroup("other");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_GROUP_ADMIN")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }

        /** Test that active role service is applied in the LDAPAuthenticationProvider */
        @Test
        public void testRoleService() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");

            createAuthenticationProvider();

            authProvider.setSecurityManager(securityManager);
            securityManager.setProviders(Collections.singletonList(authProvider));
            MemoryRoleStore roleService = new MemoryRoleStore();
            roleService.initializeFromService(new MemoryRoleService());
            roleService.setSecurityManager(securityManager);
            GeoServerRole role = roleService.createRoleObject("MyRole");
            roleService.addRole(role);
            roleService.associateRoleToUser(role, "other");
            securityManager.setActiveRoleService(roleService);

            Authentication result = authProvider.authenticate(authenticationOther);
            assertTrue(result.getAuthorities().contains(role));
            assertEquals(3, result.getAuthorities().size());
        }

        /** Tests LDAP hierarchical nested groups search. */
        @Test
        public void testHierarchicalGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(true);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(false);
            // activate hierarchical group search
            config.setUseNestedParentGroups(true);
            config.setNestedGroupSearchFilter("member=cn={1}");
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationNested);
            assertNotNull(result);
            assertEquals("nestedUser", result.getName());
            assertEquals(3, result.getAuthorities().size());
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_NESTED".equals(x.getAuthority())));
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_EXTRA".equals(x.getAuthority())));
        }

        /** Tests LDAP hierarchical nested groups search. */
        @Test
        public void testBindBeforeHierarchicalGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(false);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            // activate hierarchical group search
            config.setUseNestedParentGroups(true);
            config.setNestedGroupSearchFilter("member=cn={1}");
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationNested);
            assertNotNull(result);
            assertEquals("nestedUser", result.getName());
            assertEquals(3, result.getAuthorities().size());
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_NESTED".equals(x.getAuthority())));
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_EXTRA".equals(x.getAuthority())));
        }

        /** Tests LDAP hierarchical nested groups search disabled. */
        @Test
        public void testBindBeforeHierarchicalDisabledGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(false);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            // activate hierarchical group search
            config.setUseNestedParentGroups(false);
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationNested);
            assertNotNull(result);
            assertEquals("nestedUser", result.getName());
            assertEquals(2, result.getAuthorities().size());
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_NESTED".equals(x.getAuthority())));
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .noneMatch(x -> "ROLE_EXTRA".equals(x.getAuthority())));
        }
    }

    @RunWith(FrameworkRunner.class)
    @CreateLdapServer(
        transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
        allowAnonymousAccess = true
    )
    @CreateDS(
        name = "myDS",
        partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)}
    )
    @ApplyLdifFiles({"data3.ldif"})
    public static class LDAPAuthenticationProviderData3Test extends LDAPAuthenticationProviderTest {

        /**
         * Test that LDAPAuthenticationProvider finds roles even if there is a colon in the password
         */
        @Test
        public void testColonPassword() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");

            createAuthenticationProvider();

            authentication = new UsernamePasswordAuthenticationToken("colon", "da:da");

            Authentication result = authProvider.authenticate(authentication);
            assertEquals(2, result.getAuthorities().size());
        }
    }
}
