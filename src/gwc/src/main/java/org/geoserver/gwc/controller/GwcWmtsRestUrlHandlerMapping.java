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

package org.geoserver.gwc.controller;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.ows.LocalWorkspace;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Specific URL mapping handler for GWC WMTS REST API. The main goal of this handler id to handle
 * virtual services, it makes sure URLs with an workspace are correctly mapped and that a local
 * workspace is set and removed when needed.
 */
public final class GwcWmtsRestUrlHandlerMapping extends RequestMappingHandlerMapping
        implements HandlerInterceptor {

    private final Catalog catalog;

    public GwcWmtsRestUrlHandlerMapping(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    protected void registerHandlerMethod(
            Object handler, Method method, RequestMappingInfo mapping) {
        // this handler is only interested on GWC WMTS REST API URLs
        for (String pattern : mapping.getPatternsCondition().getPatterns()) {
            if (pattern.contains("/gwc/rest/wmts")) {
                // this is an handler for GWC WMTS REST API
                super.registerHandlerMethod(handler, method, mapping);
                break;
            }
        }
    }

    @Override
    protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request)
            throws Exception {
        int gwcRestBaseIndex = lookupPath.indexOf("/gwc/rest/wmts");
        if (gwcRestBaseIndex == -1 || gwcRestBaseIndex == 0) {
            // not a GWC REST URL or not in the context of a virtual service
            return null;
        }
        int startIndex = lookupPath.charAt(0) == '/' ? 1 : 0;
        String workspaceName = lookupPath.substring(startIndex, gwcRestBaseIndex);
        WorkspaceInfo workspace = catalog.getWorkspaceByName(workspaceName);
        if (workspace == null) {
            // not a valid workspace,we are done
            return null;
        }
        // we are in the context of a virtual service
        HandlerMethod handler =
                super.lookupHandlerMethod(
                        lookupPath.substring(gwcRestBaseIndex),
                        new Wrapper(request, catalog, workspaceName));
        if (handler == null) {
            // no handler found
            return null;
        }
        // setup the thread local workspace
        LocalWorkspace.set(workspace);
        return handler;
    }

    /**
     * Utility wrapper aground the HTTP servlet request that allow us to replace the original
     * virtual service URL with the global URL, i.e. no workspace in the URL. Note, this is only
     * used by Spring to match against the correct handler, GeoServer will use \ see the original
     * request.
     */
    private static final class Wrapper extends HttpServletRequestWrapper {

        // regex expression that matches a layer name in a WMTS REST URI
        private final Pattern URI_LAYER_PATTERN = Pattern.compile("rest/wmts/([^/]+)/]");

        private final String requestUri;

        Wrapper(HttpServletRequest request, Catalog catalog, String workspaceName) {
            super(request);
            // remove the virtual service workspace from the URL
            requestUri = request.getRequestURI().replace(workspaceName + "/", "");
        }

        @Override
        public String getRequestURI() {
            // return the global request URL, i.e. no workspace on it
            return requestUri;
        }

        private String adaptQueryUri(Catalog catalog, String queryUri, String workspaceName) {
            // remove the virtual service workspace from the URL
            queryUri = queryUri.replace(workspaceName + "/", "");
            // prefix layers in the URI with the local workspace prefix
            Matcher matcher = URI_LAYER_PATTERN.matcher(requestUri);
            if (!matcher.find()) {
                // no layers in the URI, we are done
                return queryUri;
            }
            // we have layers in the URI let's adapt them
            String layerName = matcher.group(0);
            layerName = CatalogConfiguration.removeWorkspacePrefix(layerName, catalog);
            return matcher.replaceFirst("rest/wmts/" + workspaceName + ":" + layerName);
        }
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        // nothing to do here
        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {
        // nothing to do here
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        // make sure that local workspace is properly cleaned
        LocalWorkspace.remove();
    }
}
