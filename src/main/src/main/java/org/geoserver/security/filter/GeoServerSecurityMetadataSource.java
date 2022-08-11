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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Justin, nasty hack to get rid of the spring bean "filterSecurityInterceptor"; I think, there is a
 * better was to solve this.
 *
 * @author mcr
 */
public class GeoServerSecurityMetadataSource extends DefaultFilterInvocationSecurityMetadataSource {

    /**
     * Should match
     *
     * <p>/web/?wicket:bookmarkablePage=:org.geoserver.web.GeoServerLoginPage&error=false
     *
     * @author christian
     */
    static class LoginPageRequestMatcher implements RequestMatcher {

        RequestMatcher webChainMatcher1 =
                new AntPathRequestMatcher("/" + GeoServerSecurityFilterChain.WEB_CHAIN_NAME);

        RequestMatcher webChainMatcher2 =
                new AntPathRequestMatcher("/" + GeoServerSecurityFilterChain.WEB_CHAIN_NAME + "/");

        @Override
        public boolean matches(HttpServletRequest request) {

            // check if we are on the "web" chain
            boolean isOnWebChain =
                    webChainMatcher1.matches(request) || webChainMatcher2.matches(request);
            if (isOnWebChain == false) return false;

            Map params = request.getParameterMap();
            if (params.size() != 2) return false;

            String[] pageClass = (String[]) params.get("wicket:bookmarkablePage");
            if (pageClass == null || pageClass.length != 1) return false;

            if (":org.geoserver.web.GeoServerLoginPage".equals(pageClass[0]) == false) return false;

            String error[] = (String[]) params.get("error");
            if (error == null || error.length != 1) return false;

            return true;
        }
    };

    static LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap;

    static {
        requestMap = new LinkedHashMap<>();

        // the login page is a public resource
        requestMap.put(new LoginPageRequestMatcher(), new ArrayList<>());
        // images,java script,... are public resources
        requestMap.put(new AntPathRequestMatcher("/web/resources/**"), new ArrayList<>());

        RequestMatcher matcher = new AntPathRequestMatcher("/config/**");
        List<ConfigAttribute> list = new ArrayList<>();
        list.add(new SecurityConfig(GeoServerRole.ADMIN_ROLE.getAuthority()));
        requestMap.put(matcher, list);

        matcher = new AntPathRequestMatcher("/**");
        list = new ArrayList<>();
        list.add(new SecurityConfig("IS_AUTHENTICATED_ANONYMOUSLY"));
        requestMap.put(matcher, list);
    };

    public GeoServerSecurityMetadataSource() {
        super(requestMap);
        /*
        <sec:intercept-url pattern="/config/**" access="ROLE_ADMINISTRATOR"/>
        <sec:intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/>
        */

    }
}
