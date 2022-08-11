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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.util.StringUtils;

/**
 * Logout filter
 *
 * @author christian
 */
public class GeoServerLogoutFilter extends GeoServerSecurityFilter {

    public static final String URL_AFTER_LOGOUT = "/web/";
    public static final String LOGOUT_REDIRECT_ATTR = "_logout_redirect";

    private String redirectUrl;

    SecurityContextLogoutHandler logoutHandler;
    SimpleUrlLogoutSuccessHandler logoutSuccessHandler;
    String[] pathInfos;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        logoutHandler = new SecurityContextLogoutHandler();
        redirectUrl = ((LogoutFilterConfig) config).getRedirectURL();
        logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
        if (StringUtils.hasLength(redirectUrl))
            logoutSuccessHandler.setDefaultTargetUrl(redirectUrl);
        String formLogoutChain =
                (((LogoutFilterConfig) config).getFormLogoutChain() != null
                        ? ((LogoutFilterConfig) config).getFormLogoutChain()
                        : GeoServerSecurityFilterChain.FORM_LOGOUT_CHAIN);
        pathInfos = formLogoutChain.split(",");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        boolean doLogout = false;
        for (String pathInfo : pathInfos) {
            if (getRequestPath(request).startsWith(pathInfo)) {
                doLogout = true;
                break;
            }
        }
        if (doLogout) doLogout(request, response);
    }

    public void doLogout(
            HttpServletRequest request, HttpServletResponse response, String... skipHandlerName)
            throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            List<LogoutHandler> logoutHandlers = calculateActiveLogoutHandlers(skipHandlerName);
            for (LogoutHandler h : logoutHandlers) {
                h.logout(request, response, authentication);
            }

            RememberMeServices rms = securityManager.getRememberMeService();
            ((LogoutHandler) rms).logout(request, response, authentication);

            logoutHandler.logout(request, response, authentication);
        }

        String redirectUrl = (String) request.getAttribute(LOGOUT_REDIRECT_ATTR);
        if (StringUtils.hasLength(redirectUrl)) {
            SimpleUrlLogoutSuccessHandler h = new SimpleUrlLogoutSuccessHandler();
            h.setDefaultTargetUrl(redirectUrl);
            h.onLogoutSuccess(request, response, authentication);
            return;
        }

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);
    }

    /**
     * Search for filters implementing {@link LogoutHandler}. If such a filter is on an active
     * filter chain and is not enlisted in the parameter skipHandlerName, add it to the result
     *
     * <p>The skipHandlerName parameter gives other LogoutHandler the chance to trigger a using
     * {@link #doLogout(HttpServletRequest, HttpServletResponse, String...)} without receiving an
     * unnecessary callback.
     */
    List<LogoutHandler> calculateActiveLogoutHandlers(String... skipHandlerName)
            throws IOException {
        List<LogoutHandler> result = new ArrayList<>();
        SortedSet<String> logoutFilterNames = getSecurityManager().listFilters(LogoutHandler.class);
        logoutFilterNames.removeAll(Arrays.asList(skipHandlerName));
        Set<String> handlerNames = new HashSet<>();

        GeoServerSecurityFilterChain chain =
                getSecurityManager().getSecurityConfig().getFilterChain();
        for (RequestFilterChain requestChain : chain.getRequestChains()) {
            for (String filterName : requestChain.getFilterNames()) {
                if (logoutFilterNames.contains(filterName)) handlerNames.add(filterName);
            }
        }

        for (String handlerName : handlerNames) {
            result.add((LogoutHandler) getSecurityManager().loadFilter(handlerName));
        }
        return result;
    }
}
