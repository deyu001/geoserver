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

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Extension of {@link Filter} for the geoserver security subsystem.
 *
 * <p>Instances of this class are provided by {@link GeoServerSecurityProvider}, or may also be
 * contribute via a spring context. Filters are configured via name through {@link
 * SecurityManagerConfig#getFilterChain()}.The referenced name will be matched to a named security
 * configuration through {@link GeoServerSecurityManager#loadFilter(String)} or matched to a bean
 * name in the application context.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class GeoServerSecurityFilter extends AbstractGeoServerSecurityService
        implements Filter, BeanNameAware {

    /**
     * Geoserver authentication filter should set an {@link AuthenticationEntryPoint} using this
     * servlet attribute name.
     *
     * <p>The {@link GeoServerExceptionTranslationFilter} may use the entry point in case of an
     * {@link AuthenticationException}
     */
    public static final String AUTHENTICATION_ENTRY_POINT_HEADER =
            "_AUTHENTICATION_ENTRY_POINT_HEADER";

    private String beanName;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /** Not used, these filters are not plugged in via web.xml */
    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {}

    /** Does nothing, subclasses may override. */
    @Override
    public void destroy() {}

    /**
     * Tries to authenticate from cache if a key can be derived and the {@link Authentication}
     * object is not in the cache, the key will be returned.
     *
     * <p>A not <code>null</code> return value indicates a missing cache entry
     */
    protected String authenticateFromCache(
            AuthenticationCachingFilter filter, HttpServletRequest request) {

        Authentication authFromCache = null;
        String cacheKey = null;
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            cacheKey = filter.getCacheKey(request);
            if (cacheKey != null) {
                authFromCache =
                        getSecurityManager().getAuthenticationCache().get(getName(), cacheKey);
                if (authFromCache != null)
                    SecurityContextHolder.getContext().setAuthentication(authFromCache);
                else return cacheKey;
            }
        }
        return null;
    }

    protected String getRequestPath(HttpServletRequest request) {
        String url = request.getServletPath();

        if (request.getPathInfo() != null) {
            url += request.getPathInfo();
        }

        url = url.toLowerCase();

        return url;
    }
}
