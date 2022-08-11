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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Authentication Provider based on a successful JDBC Connect
 *
 * @author christian
 */
public class JDBCConnectAuthProvider extends GeoServerAuthenticationProvider {

    protected String connectUrl, driverClassName, userGroupServiceName;

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {

        UsernamePasswordAuthenticationToken token =
                (UsernamePasswordAuthenticationToken) authentication;

        // check for valid user name
        if (token.getPrincipal() == null || token.getPrincipal().toString().isEmpty()) return null;
        String user = token.getPrincipal().toString();
        String password = token.getCredentials() == null ? "" : token.getCredentials().toString();

        UserDetails details = null;

        if (userGroupServiceName != null) {
            try {
                GeoServerUserGroupService service =
                        getSecurityManager().loadUserGroupService(userGroupServiceName);
                details = service.loadUserByUsername(user);
                if (details.isEnabled() == false) {
                    log(new DisabledException("User " + user + " is disabled"));
                    return null;
                }
            } catch (IOException ex) {
                log(new AuthenticationServiceException(ex.getLocalizedMessage(), ex));
                return null;
            } catch (UsernameNotFoundException ex) {
                log(ex);
            } catch (AuthenticationException ex) {
                log(ex);
                return null;
            }
        }

        try (Connection con = DriverManager.getConnection(connectUrl, user, password)) {
        } catch (SQLInvalidAuthorizationSpecException ex) {
            log(new BadCredentialsException("Bad credentials for " + user, ex));
            return null;
        } catch (SQLException ex) {
            log(new AuthenticationServiceException("JDBC connect error", ex));
            return null;
        }
        // do nothing, give up
        UsernamePasswordAuthenticationToken result = null;
        Set<GrantedAuthority> roles = new HashSet<>();
        if (details != null) {
            roles.addAll(details.getAuthorities());
        } else {
            RoleCalculator calc = new RoleCalculator(getSecurityManager().getActiveRoleService());
            try {
                roles.addAll(calc.calculateRoles(new GeoServerUser(user)));
            } catch (IOException e) {
                throw new AuthenticationServiceException(e.getLocalizedMessage(), e);
            }
        }
        roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        result =
                new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), null, roles);
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        JDBCConnectAuthProviderConfig jdbcConfig = (JDBCConnectAuthProviderConfig) config;
        userGroupServiceName = jdbcConfig.getUserGroupServiceName();
        connectUrl = jdbcConfig.getConnectURL();
        driverClassName = jdbcConfig.getDriverClassName();
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
