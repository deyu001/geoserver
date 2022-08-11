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

package org.geoserver.security.keycloak;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Wraps the result of an attempt to authenticate. This is either valid auth, or a challenge in
 * order to obtain them.
 */
class AuthResults implements AuthenticationEntryPoint {

    private final Authentication authentication;
    private final AuthChallenge challenge;

    /** Create a FORBIDDEN result. */
    public AuthResults() {
        this.authentication = null;
        this.challenge = null;
    }

    /**
     * Create a failed result with a potential challenge to obtain credentials and retry.
     *
     * @param challenge instructions to obtain credentials
     */
    public AuthResults(AuthChallenge challenge) {
        this.authentication = null;
        this.challenge = challenge;
    }

    /**
     * Create a successful result.
     *
     * @param authentication valid credentials
     */
    public AuthResults(Authentication authentication) {
        Object username = null;
        Object details = null;
        if (authentication.getDetails() instanceof SimpleKeycloakAccount) {
            details = (SimpleKeycloakAccount) authentication.getDetails();

            assert ((SimpleKeycloakAccount) details).getPrincipal() instanceof KeycloakPrincipal;
            final KeycloakPrincipal principal =
                    (KeycloakPrincipal) ((SimpleKeycloakAccount) details).getPrincipal();

            username = principal.getName();

            if (principal.getKeycloakSecurityContext().getIdToken() != null) {
                username =
                        principal.getKeycloakSecurityContext().getIdToken().getPreferredUsername();
            }
        } else {
            username = authentication.getPrincipal();
            details = authentication.getDetails();
        }

        this.authentication =
                new UsernamePasswordAuthenticationToken(
                        username, authentication.getCredentials(), authentication.getAuthorities());
        ((UsernamePasswordAuthenticationToken) this.authentication).setDetails(details);
        this.challenge = null;
    }

    /**
     * Execute the challenge to modify the response. The response should (upon success) contain
     * instructions on how to obtain valid credentials.
     *
     * @param request incoming request
     * @param response response to modify
     * @return does the response contain auth instructions?
     */
    public boolean challenge(HttpServletRequest request, HttpServletResponse response) {
        // if already authenticated, then there is nothing to do so consider this a success
        if (authentication != null) {
            return true;
        }
        // if no challenge exists and no creds are set, then this is FORBIDDEN
        if (challenge == null) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        // otherwise, defer to the contained challenge
        return challenge.challenge(new SimpleHttpFacade(request, response));
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        challenge(request, response);
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public boolean hasAuthentication() {
        return authentication != null;
    }
}
