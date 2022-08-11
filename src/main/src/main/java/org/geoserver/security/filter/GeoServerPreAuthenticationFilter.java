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

package org.geoserver.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Abstract base class for pre-authentication filters
 *
 * @author christian
 */
public abstract class GeoServerPreAuthenticationFilter extends GeoServerSecurityFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter {

    private AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>
            authenticationDetailsSource = new WebAuthenticationDetailsSource();
    protected AuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        aep = new Http403ForbiddenEntryPoint();
    }

    /** Try to authenticate if there is no authenticated principal */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String cacheKey = authenticateFromCache(this, (HttpServletRequest) request);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);

            Authentication postAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (postAuthentication != null && cacheKey != null) {
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager()
                            .getAuthenticationCache()
                            .put(getName(), cacheKey, postAuthentication);
                }
            }
        }

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        chain.doFilter(request, response);
    }

    /**
     * subclasses should return the principal, <code>null</code> if no principal was authenticated
     */
    protected abstract String getPreAuthenticatedPrincipal(HttpServletRequest request);

    /**
     * subclasses should return the roles for the principal obtained by {@link
     * #getPreAuthenticatedPrincipal(HttpServletRequest)}
     */
    protected abstract Collection<GeoServerRole> getRoles(
            HttpServletRequest request, String principal) throws IOException;

    /**
     * Try to authenticate and adds {@link GeoServerRole#AUTHENTICATED_ROLE} Takes care of the
     * special user named {@link GeoServerUser#ROOT_USERNAME}
     */
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {

        String principal = getPreAuthenticatedPrincipal(request);
        if (principal == null || principal.trim().length() == 0) {
            return;
        }

        LOGGER.log(
                Level.FINE,
                "preAuthenticatedPrincipal = " + principal + ", trying to authenticate");

        PreAuthenticatedAuthenticationToken result = null;
        if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
            result =
                    new PreAuthenticatedAuthenticationToken(
                            principal, null, Collections.singleton(GeoServerRole.ADMIN_ROLE));
        } else {
            Collection<GeoServerRole> roles = null;
            try {
                roles = new ArrayList<>(getRoles(request, principal));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE) == false)
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            result = new PreAuthenticatedAuthenticationToken(principal, null, roles);
        }

        result.setDetails(authenticationDetailsSource.buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(result);
    }

    public AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>
            getAuthenticationDetailsSource() {
        return authenticationDetailsSource;
    }

    public void setAuthenticationDetailsSource(
            AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails>
                    authenticationDetailsSource) {
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    protected boolean cacheAuthentication(Authentication auth, HttpServletRequest request) {
        // only cache if no HTTP session is available
        return request.getSession(false) == null;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {

        if (request.getSession(false) != null) // no caching if there is an HTTP session
        return null;

        String retval = getPreAuthenticatedPrincipal(request);
        if (GeoServerUser.ROOT_USERNAME.equals(retval)) return null;
        return retval;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    @Override
    public boolean applicableForServices() {
        return true;
    }
}
