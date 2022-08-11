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

package org.geoserver.security.rememberme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Category(SystemTest.class)
public class RememberMeTest extends GeoServerSecurityTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        SecurityNamedServiceConfig filterCfg = new BaseSecurityNamedServiceConfig();
        filterCfg.setName("custom");
        filterCfg.setClassName(AuthCapturingFilter.class.getName());

        GeoServerSecurityManager secMgr = getSecurityManager();
        secMgr.saveFilter(filterCfg);

        SecurityManagerConfig cfg = secMgr.getSecurityConfig();
        cfg.getFilterChain()
                .insertAfter(
                        "/web/**",
                        filterCfg.getName(),
                        GeoServerSecurityFilterChain.REMEMBER_ME_FILTER);

        //        cfg.getFilterChain().put("/web/**", Arrays.asList(
        //            new FilterChainEntry(filterCfg.getName(), Position.AFTER,
        //                GeoServerSecurityFilterChain.REMEMBER_ME_FILTER)));

        secMgr.saveSecurityConfig(cfg);
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
    }

    static class AuthCapturingFilter extends GeoServerSecurityFilter
            implements GeoServerAuthenticationFilter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            request.setAttribute("auth", auth);
            chain.doFilter(request, response);
        }

        @Override
        public boolean applicableForHtml() {
            return true;
        }

        @Override
        public boolean applicableForServices() {
            return true;
        }
    }

    static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public Class<? extends GeoServerSecurityFilter> getFilterClass() {
            return AuthCapturingFilter.class;
        }

        @Override
        public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
            return new AuthCapturingFilter();
        }
    }

    @Override
    protected List<Filter> getFilters() {
        return Arrays.asList(applicationContext.getBean(GeoServerSecurityFilterChainProxy.class));
    }

    @Test
    public void testRememberMeLogin() throws Exception {

        MockHttpServletRequest request = createRequest("/login");
        request.addParameter("username", "admin");
        request.addParameter("password", "geoserver");
        request.setMethod("POST");
        MockHttpServletResponse response = dispatch(request);
        assertLoginOk(response);
        assertEquals(0, response.getCookies().length);

        request = createRequest("/login");
        request.addParameter("username", "admin");
        request.addParameter("password", "geoserver");
        request.addParameter("_spring_security_remember_me", "yes");
        request.setMethod("POST");
        response = dispatch(request);
        assertLoginOk(response);
        assertEquals(1, response.getCookies().length);

        Cookie cookie = response.getCookies()[0];

        request = createRequest("/web/");
        request.setMethod("POST");
        response = dispatch(request);
        assertNull(request.getAttribute("auth"));

        request = createRequest("/web/");
        request.setMethod("GET");
        request.setCookies(cookie);
        response = dispatch(request);
        assertTrue(request.getAttribute("auth") instanceof RememberMeAuthenticationToken);
    }

    @Test
    public void testRememberMeOtherUserGroupService() throws Exception {
        // TODO Justin, this should work now

        // need to implement this test, at the moment we don't have a way to mock up new users
        // in a memory user group service...
        /*
        SecurityUserGoupServiceConfig memCfg = new MemoryUserGroupServiceConfigImpl();
        memCfg.setName("memory");
        memCfg.setClassName(MemoryUserGroupService.class.getName());
        memCfg.setPasswordEncoderName(GeoserverPlainTextPasswordEncoder.BeanName);
        memCfg.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);

        GeoServerSecurityManager secMgr = getSecurityManager();
        secMgr.saveUserGroupService(memCfg);

        GeoserverUserGroupService ug = secMgr.loadUserGroupService("memory");
        GeoserverUser user = ug.createUserObject("foo", "bar", true);
        ug.createStore().addUser(user);

        user = ug.getUserByUsername("foo");
        assertNotNull(user);
        */
    }

    void assertLoginOk(MockHttpServletResponse resp) {
        assertEquals("/geoserver/web", resp.getHeader("Location"));
    }

    void assertLoginFailed(MockHttpServletResponse resp) {
        assertTrue(resp.getHeader("Location").endsWith("GeoServerLoginPage&error=true"));
    }
}
