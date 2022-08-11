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


package org.geoserver.security.cas;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Special CAS {@link AuthenticationEntryPoint} implementation. Clients sending requests with an
 * HTTP parameter {@link #CAS_REDIRECT} set to <code>true</code> can avoid the standard CAS
 * redirect. An unsuccessful authentication results in an HTTP 403 error. (Forbidden).
 *
 * <p>The {@link #CAS_REDIRECT} key value pair can also be sent as an HTTP requester header
 * attribute.
 *
 * @author christian
 */
public class GeoServerCasAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String CAS_REDIRECT = "casredirect";

    // private AuthenticationEntryPoint http403 = new Http403ForbiddenEntryPoint();
    private CasAuthenticationFilterConfig authConfig;

    public GeoServerCasAuthenticationEntryPoint(CasAuthenticationFilterConfig config) {
        this.authConfig = config;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        // check for http parameter
        String value = request.getParameter(CAS_REDIRECT);
        if (value != null && "false".equalsIgnoreCase(value)) {
            // http403.commence(request, response, authException);
            sendUnauthorized(response);
            return;
        }

        // check for header attribute
        value = request.getHeader(CAS_REDIRECT);
        if (value != null && "false".equalsIgnoreCase(value)) {
            // http403.commence(request, response, authException);
            sendUnauthorized(response);
            return;
        }

        // standard cas redirect
        ServiceProperties sp = new ServiceProperties();
        sp.setSendRenew(authConfig.isSendRenew());
        sp.setService(getService(request));

        try {
            sp.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        CasAuthenticationEntryPoint aep = new CasAuthenticationEntryPoint();
        aep.setLoginUrl(authConfig.getCasServerUrlPrefix() + GeoServerCasConstants.LOGIN_URI);
        aep.setServiceProperties(sp);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }
        aep.commence(request, response, authException);
    }

    private String getService(HttpServletRequest request) {
        // the callback is what's usually recorded in CAS, the request might be going through
        // http(s)://server/geoserver/j_spring_cas_login, which is not known to CAS
        String service = authConfig.getProxyCallbackUrlPrefix();
        if (service == null) {
            // fallback in case the proxy callback is not configured
            service = GeoServerCasAuthenticationFilter.retrieveService(request);
        }
        return service;
    }

    public void sendUnauthorized(ServletResponse response) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
