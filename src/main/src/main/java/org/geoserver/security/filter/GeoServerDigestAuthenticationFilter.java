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
import java.nio.charset.Charset;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.HttpDigestUserDetailsServiceWrapper;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.DigestAuthUtils;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Named Digest Authentication Filter
 *
 * @author mcr
 */
public class GeoServerDigestAuthenticationFilter extends GeoServerCompositeFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter {

    private DigestAuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        DigestAuthenticationFilterConfig authConfig = (DigestAuthenticationFilterConfig) config;

        aep = new DigestAuthenticationEntryPoint();
        aep.setKey(config.getName());
        aep.setNonceValiditySeconds(
                authConfig.getNonceValiditySeconds() <= 0
                        ? 300
                        : authConfig.getNonceValiditySeconds());
        aep.setRealmName(GeoServerSecurityManager.REALM);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        DigestAuthenticationFilter filter = new DigestAuthenticationFilter();

        filter.setCreateAuthenticatedToken(true);
        filter.setPasswordAlreadyEncoded(true);

        filter.setAuthenticationEntryPoint(aep);

        HttpDigestUserDetailsServiceWrapper wrapper =
                new HttpDigestUserDetailsServiceWrapper(
                        getSecurityManager()
                                .loadUserGroupService(authConfig.getUserGroupServiceName()),
                        Charset.defaultCharset());
        filter.setUserDetailsService(wrapper);

        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        Integer validity = aep.getNonceValiditySeconds();
        // upper limits in the cache, makes no sense to cache an expired authentication token
        req.setAttribute(GeoServerCompositeFilter.CACHE_KEY_IDLE_SECS, validity);
        req.setAttribute(GeoServerCompositeFilter.CACHE_KEY_LIVE_SECS, validity);

        super.doFilter(req, res, chain);
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {

        if (request.getSession(false) != null) // no caching if there is an HTTP session
        return null;

        String header = request.getHeader("Authorization");

        if ((header != null) && header.startsWith("Digest ")) {
            String section212response = header.substring(7);

            String[] headerEntries = DigestAuthUtils.splitIgnoringQuotes(section212response, ',');
            Map<String, String> headerMap =
                    DigestAuthUtils.splitEachArrayElementAndCreateMap(headerEntries, "=", "\"");

            String username = headerMap.get("username");
            String realm = headerMap.get("realm");
            String nonce = headerMap.get("nonce");
            String responseDigest = headerMap.get("response");

            if (StringUtils.hasLength(username) == false
                    || StringUtils.hasLength(realm) == false
                    || StringUtils.hasLength(nonce) == false
                    || StringUtils.hasLength(responseDigest) == false) return null;

            if (GeoServerUser.ROOT_USERNAME.equals(username)) return null;

            StringBuffer buff = new StringBuffer();
            buff.append(username).append(":");
            buff.append(realm).append(":");
            buff.append(nonce).append(":");
            buff.append(responseDigest);
            return buff.toString();
        } else {
            return null;
        }
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
