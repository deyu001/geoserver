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

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geotools.util.logging.Logging;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.UserDetailsServiceLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.NestedLdapAuthoritiesPopulator;

/**
 * LDAP security provider.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPSecurityProvider extends GeoServerSecurityProvider {

    static final Logger LOGGER = Logging.getLogger("org.geoserver.security.ldap");

    GeoServerSecurityManager securityManager;

    public LDAPSecurityProvider(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream().alias("ldap", LDAPSecurityServiceConfig.class);
    }

    @Override
    public Class<LDAPAuthenticationProvider> getAuthenticationProviderClass() {
        return LDAPAuthenticationProvider.class;
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return LDAPUserGroupService.class;
    }

    @Override
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        LDAPSecurityServiceConfig ldapConfig = (LDAPSecurityServiceConfig) config;

        LdapContextSource ldapContext = LDAPUtils.createLdapContext(ldapConfig);

        GeoserverLdapBindAuthenticator authenticator =
                new GeoserverLdapBindAuthenticator(ldapContext);

        // authenticate and extract user using a filter and an optional username
        // format
        authenticator.setUserFilter(ldapConfig.getUserFilter());
        authenticator.setUserFormat(ldapConfig.getUserFormat());

        // authenticate and extract user using a distinguished name
        if (ldapConfig.getUserDnPattern() != null) {
            authenticator.setUserDnPatterns(new String[] {ldapConfig.getUserDnPattern()});
        }

        LdapAuthoritiesPopulator authPopulator = null;
        LdapAuthenticationProvider provider = null;
        String ugServiceName = ldapConfig.getUserGroupServiceName();
        if (ugServiceName != null) {
            // use local user group service for loading authorities
            GeoServerUserGroupService ugService;
            try {
                ugService = securityManager.loadUserGroupService(ugServiceName);
                authPopulator = new UserDetailsServiceLdapAuthoritiesPopulator(ugService);
                provider = new LdapAuthenticationProvider(authenticator, authPopulator);
            } catch (IOException e) {
                LOGGER.log(
                        Level.SEVERE,
                        String.format(
                                "Unable to load user group service '%s', "
                                        + "will use LDAP server for calculating roles",
                                ugServiceName),
                        e);
            }
        }

        if (authPopulator == null) {
            // fall back to looking up roles via LDAP server, choosing
            // between default and binding populator
            if (ldapConfig.isBindBeforeGroupSearch()) {
                authPopulator =
                        new BindingLdapAuthoritiesPopulator(
                                ldapContext, ldapConfig.getGroupSearchBase());
                // set hierarchical configurations
                BindingLdapAuthoritiesPopulator bindPopulator =
                        (BindingLdapAuthoritiesPopulator) authPopulator;
                bindPopulator.setUseNestedParentGroups(ldapConfig.isUseNestedParentGroups());
                bindPopulator.setMaxGroupSearchLevel(ldapConfig.getMaxGroupSearchLevel());
                bindPopulator.setNestedGroupSearchFilter(ldapConfig.getNestedGroupSearchFilter());

                if (ldapConfig.getGroupSearchFilter() != null) {
                    ((BindingLdapAuthoritiesPopulator) authPopulator)
                            .setGroupSearchFilter(ldapConfig.getGroupSearchFilter());
                }
                provider =
                        new LdapAuthenticationProvider(authenticator, authPopulator) {
                            /**
                             * We need to give authoritiesPopulator both username and password, so
                             * it can bind to the LDAP server.
                             */
                            @Override
                            protected Collection<? extends GrantedAuthority> loadUserAuthorities(
                                    DirContextOperations userData,
                                    String username,
                                    String password) {
                                return ((BindingLdapAuthoritiesPopulator) getAuthoritiesPopulator())
                                        .getGrantedAuthorities(userData, username, password);
                            }
                        };
            } else {
                ldapContext.setAnonymousReadOnly(true);
                // is hierarchical nested groups implementation required?
                if (ldapConfig.isUseNestedParentGroups()) {
                    // use nested implementation for nested groups support
                    authPopulator =
                            new NestedLdapAuthoritiesPopulator(
                                    ldapContext, ldapConfig.getGroupSearchBase());
                    ((NestedLdapAuthoritiesPopulator) authPopulator)
                            .setMaxSearchDepth(ldapConfig.getMaxGroupSearchLevel());
                } else {
                    // no hierarchical groups required, use default implementation
                    authPopulator =
                            new DefaultLdapAuthoritiesPopulator(
                                    ldapContext, ldapConfig.getGroupSearchBase());
                }

                if (ldapConfig.getGroupSearchFilter() != null) {
                    ((DefaultLdapAuthoritiesPopulator) authPopulator)
                            .setGroupSearchFilter(ldapConfig.getGroupSearchFilter());
                }
                provider = new LdapAuthenticationProvider(authenticator, authPopulator);
            }
        }

        return new LDAPAuthenticationProvider(
                provider, ldapConfig.getAdminGroup(), ldapConfig.getGroupAdminGroup());
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return LDAPRoleService.class;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new LDAPRoleService();
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return new LDAPUserGroupService(config);
    }
}
