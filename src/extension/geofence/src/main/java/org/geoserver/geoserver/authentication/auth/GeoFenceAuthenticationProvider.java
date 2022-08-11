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

package org.geoserver.geoserver.authentication.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authentication provider that delegates to GeoFence
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class GeoFenceAuthenticationProvider extends GeoServerAuthenticationProvider
        implements AuthenticationManager {

    private static final Logger LOGGER =
            Logging.getLogger(GeoFenceAuthenticationProvider.class.getName());
    // protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    private RuleReaderService ruleReaderService;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {

        LOGGER.warning("INIT FROM CONFIG");

        super.initializeFromConfig(config);
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {

        UsernamePasswordAuthenticationToken outTok = null;
        LOGGER.log(Level.FINE, "Auth request with {0}", authentication);

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken inTok =
                    (UsernamePasswordAuthenticationToken) authentication;

            AuthUser authUser = null;
            final String username = SecurityUtils.getUsername(inTok.getPrincipal());
            try {
                authUser = ruleReaderService.authorize(username, inTok.getCredentials().toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in authenticating with GeoFence", e);
                throw new AuthenticationException("Error in GeoFence communication", e) {};
            }

            if (authUser != null) {
                LOGGER.log(
                        Level.FINE,
                        "User {0} authenticated: {1}",
                        new Object[] {username, authUser});

                List<GrantedAuthority> roles = new ArrayList<>();
                roles.addAll(inTok.getAuthorities());
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
                if (authUser.getRole() == AuthUser.Role.ADMIN) {
                    roles.add(GeoServerRole.ADMIN_ROLE);
                    roles.add(new SimpleGrantedAuthority("ADMIN")); // needed for REST?!?
                }

                outTok =
                        new UsernamePasswordAuthenticationToken(
                                username, inTok.getCredentials(), roles);

            } else { // authUser == null
                if ("admin".equals(username) && "geoserver".equals(inTok.getCredentials())) {
                    LOGGER.log(
                            Level.FINE,
                            "Default admin credentials NOT authenticated -- probably a frontend check");
                } else {
                    LOGGER.log(Level.INFO, "User {0} NOT authenticated", username);
                }
            }
            return outTok;

        } else {
            return null;
        }
    }

    public void setRuleReaderService(RuleReaderService ruleReaderService) {
        this.ruleReaderService = ruleReaderService;
    }
}
