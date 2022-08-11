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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.util.StringUtils;

/**
 * Named Exception translation filter
 *
 * <p>The {@link AuthenticationEntryPoint} is of type {@link DynamicAuthenticationEntryPoint}
 *
 * <p>if {@link ExceptionTranslationFilterConfig#getAuthenticationEntryPointName()} is not empty,
 * use this name for a lookup of an authentication filter and use the entry point of this filter.
 *
 * <p>if the name is empty, use {@link GeoServerSecurityFilter#AUTHENTICATION_ENTRY_POINT_HEADER} as
 * a servlet attribute name. Previous authentication filter should put an entry point in this
 * attribute.
 *
 * <p>if still no entry point was a found, use {@link Http403ForbiddenEntryPoint} as a default.
 *
 * @author mcr
 */
public class GeoServerExceptionTranslationFilter extends GeoServerCompositeFilter {

    public static class DynamicAuthenticationEntryPoint implements AuthenticationEntryPoint {

        protected AuthenticationEntryPoint defaultEntryPoint = new Http403ForbiddenEntryPoint();
        protected AuthenticationEntryPoint entryEntryPoint = null;

        public AuthenticationEntryPoint getEntryEntryPoint() {
            return entryEntryPoint;
        }

        public void setEntryEntryPoint(AuthenticationEntryPoint entryEntryPoint) {
            this.entryEntryPoint = entryEntryPoint;
        }

        @Override
        public void commence(
                HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException authException)
                throws IOException, ServletException {

            AuthenticationEntryPoint aep =
                    (AuthenticationEntryPoint)
                            request.getAttribute(
                                    GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER);
            if (aep != null) // remove from request
            request.removeAttribute(AUTHENTICATION_ENTRY_POINT_HEADER);

            // entry point specified ?
            if (getEntryEntryPoint() != null) {
                getEntryEntryPoint().commence(request, response, authException);
                return;
            }

            // entry point from request ?
            if (aep != null) {
                aep.commence(request, response, authException);
                return;
            }

            // 403, FORBIDDEN
            defaultEntryPoint.commence(request, response, authException);
        }
    };

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        ExceptionTranslationFilterConfig authConfig = (ExceptionTranslationFilterConfig) config;

        DynamicAuthenticationEntryPoint ep = new DynamicAuthenticationEntryPoint();

        if (StringUtils.hasLength(authConfig.getAuthenticationFilterName())) {
            GeoServerSecurityFilter authFilter =
                    getSecurityManager().loadFilter(authConfig.getAuthenticationFilterName());
            ep.setEntryEntryPoint(authFilter.getAuthenticationEntryPoint());
        }

        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setCreateSessionAllowed(false);
        ExceptionTranslationFilter filter = new ExceptionTranslationFilter(ep, cache);

        AccessDeniedHandlerImpl accessDeniedHandler = new AccessDeniedHandlerImpl();

        if (StringUtils.hasLength(authConfig.getAccessDeniedErrorPage())) {
            // check if page exists
            if (GeoServerExtensions.file(authConfig.getAccessDeniedErrorPage()) != null)
                accessDeniedHandler.setErrorPage(authConfig.getAccessDeniedErrorPage());
            else LOGGER.warning("Cannot find: " + authConfig.getAccessDeniedErrorPage());
        }

        filter.setAccessDeniedHandler(accessDeniedHandler);

        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }
}
