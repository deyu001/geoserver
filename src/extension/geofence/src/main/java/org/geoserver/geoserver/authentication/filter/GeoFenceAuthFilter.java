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

package org.geoserver.geoserver.authentication.filter;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider;
import org.geoserver.geoserver.authentication.auth.GeoFenceSecurityProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geotools.util.logging.Logging;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceAuthFilter
        // extends GeoServerSecurityFilter
        extends GeoServerCompositeFilter implements GeoServerAuthenticationFilter {

    static final Logger LOGGER = Logging.getLogger(GeoFenceAuthFilter.class);

    private GeoFenceSecurityProvider geofenceAuth;

    // static final String ROOT_ROLE = "ROLE_ADMINISTRATOR";
    // static final String ANONYMOUS_ROLE = "ROLE_ANONYMOUS";
    static final String USER_ROLE = "ROLE_USER";

    private BasicAuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        // anything to set here? maybe the cache config

        aep = new BasicAuthenticationEntryPoint();
        aep.setRealmName(GeoServerSecurityManager.REALM);

        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        // BasicAuthenticationFilterConfig authConfig = (BasicAuthenticationFilterConfig) config;
        SecurityNamedServiceConfig authCfg =
                securityManager.loadAuthenticationProviderConfig("geofence");
        GeoFenceAuthenticationProvider geofenceAuthProvider =
                geofenceAuth.createAuthenticationProvider(authCfg);
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(geofenceAuthProvider, aep);

        // if (authConfig.isUseRememberMe()) {
        // filter.setRememberMeServices(securityManager.getRememberMeService());
        // GeoServerWebAuthenticationDetailsSource s = new
        // GeoServerWebAuthenticationDetailsSource();
        // filter.setAuthenticationDetailsSource(s);
        // }
        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        super.doFilter(request, response, chain);

        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth == null) {
        // doAuth(request, response);
        // } else {
        // LOGGER.fine("Found existing Authentication in context: " + auth);
        // }
        //
        // chain.doFilter(request, response);
    }

    /** Simple username+password container */
    class BasicUser {
        String name;

        String pw;

        public BasicUser(String name, String pw) {
            this.name = name;
            this.pw = pw;
        }
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    // @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    // @Override
    public boolean applicableForServices() {
        return true;
    }

    public void setGeofenceAuth(GeoFenceSecurityProvider geofenceAuth) {
        this.geofenceAuth = geofenceAuth;
    }
}
