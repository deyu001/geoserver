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

package org.geoserver.rest;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * An object which contains information about the "page" or "resource" being accessed in a rest
 * request.
 *
 * <p>Equivalent of PageInfo used by the old rest module.
 *
 * <p>An instance of this class can be referenced by any restlet via:
 *
 * <pre>
 * RequestContextHolder.getRequestAttributes().getAttribute( RequestInfo.KEY, RequestAttributes.SCOPE_REQUEST );
 * </pre>
 */
public class RequestInfo {

    /** key to reference this object by */
    public static final String KEY = "RequestInfo";

    String baseURL;
    String servletPath;
    String pagePath;
    String extension;

    private Map<String, String[]> queryMap;

    /** Constructs an empty {@link RequestInfo} object */
    public RequestInfo() {}

    /** Constructs a {@link RequestInfo} object, generating content based on the passed request. */
    public RequestInfo(HttpServletRequest request) {
        // http://host:port/appName
        baseURL =
                request.getRequestURL()
                        .toString()
                        .replace(request.getRequestURI(), request.getContextPath());

        servletPath = request.getServletPath();
        pagePath = request.getServletPath() + request.getPathInfo();
        setQueryMap(request.getParameterMap());
        // strip off the extension
        extension = ResponseUtils.getExtension(pagePath);
        if (extension != null) {
            pagePath = pagePath.substring(0, pagePath.length() - extension.length() - 1);
        }

        // trim leading slash
        if (pagePath.endsWith("/")) {
            pagePath = pagePath.substring(0, pagePath.length() - 1);
        }
    }

    private void setQueryMap(Map<String, String[]> parameterMap) {
        queryMap = parameterMap;
    }

    /** Gets the base URL of the server, e.g. "http://localhost:8080/geoserver" */
    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /** Gets the relative path to the servlet, e.g. "/rest" */
    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    /** Gets the relative path to the current page, e.g. "rest/layers" */
    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    /** Gets the extension for the currnet page, e.g. "xml" */
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String pageURI(String path) {
        return buildURI(pagePath, path);
    }

    public String servletURI(String path) {
        return buildURI(servletPath, path);
    }

    String buildURI(String base, String path) {
        if (path != null) {
            if (path.startsWith(".")) {
                if (base.endsWith("/")) base = base.substring(1);
                path = base + path;
            } else {
                path = ResponseUtils.appendPath(base, path);
            }
        }

        return ResponseUtils.buildURL(baseURL, path, null, URLMangler.URLType.SERVICE);
    }

    /** Returns the RequestInfo from the current {@link RequestContextHolder} */
    public static RequestInfo get() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) return null;
        return (RequestInfo)
                requestAttributes.getAttribute(RequestInfo.KEY, RequestAttributes.SCOPE_REQUEST);
    }

    public Map<String, String[]> getQueryMap() {
        return queryMap;
    }

    /** Sets the provided RequestInfo into the {@link RequestContextHolder} */
    public static void set(RequestInfo requestInfo) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new IllegalStateException("Request attributes are not set");
        }
        requestAttributes.setAttribute(
                RequestInfo.KEY, requestInfo, RequestAttributes.SCOPE_REQUEST);
    }
}
