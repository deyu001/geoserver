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

package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Category(SystemTest.class)
public class GeoServerCustomFilterTest extends GeoServerSystemTestSupport {

    enum Pos {
        FIRST,
        LAST,
        BEFORE,
        AFTER;
    };

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
    }

    @After
    public void removeCustomFilterConfig() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        if (secMgr.listFilters().contains("custom")) {
            secMgr.removeFilter(secMgr.loadFilterConfig("custom"));
        }
        secMgr.getSecurityConfig().getFilterChain().remove("custom");

        SecurityManagerConfig mgrConfig = secMgr.getSecurityConfig();
        secMgr.saveSecurityConfig(mgrConfig);
    }

    @Test
    public void testInactive() throws Exception {
        HttpServletRequest request = createRequest("/foo");
        ((MockHttpServletRequest) request).setMethod("GET");
        MockHttpServletResponse response = dispatch(request);
        assertNull(response.getHeader("foo"));
    }

    void setupFilterEntry(Pos pos, String relativeTo, boolean assertSecurityContext)
            throws Exception {

        GeoServerSecurityManager secMgr = getSecurityManager();

        FilterConfig config = new FilterConfig();
        config.setName("custom");
        config.setClassName(Filter.class.getName());
        config.setAssertAuth(assertSecurityContext);
        secMgr.saveFilter(config);

        SecurityManagerConfig mgrConfig = secMgr.getSecurityConfig();
        mgrConfig.setConfigPasswordEncrypterName(getPlainTextPasswordEncoder().getName());

        mgrConfig.getFilterChain().remove("custom");
        if (pos == Pos.FIRST) mgrConfig.getFilterChain().insertFirst("/**", "custom");
        if (pos == Pos.LAST) mgrConfig.getFilterChain().insertLast("/**", "custom");
        if (pos == Pos.BEFORE) mgrConfig.getFilterChain().insertBefore("/**", "custom", relativeTo);
        if (pos == Pos.AFTER) mgrConfig.getFilterChain().insertAfter("/**", "custom", relativeTo);

        secMgr.saveSecurityConfig(mgrConfig);
    }

    @Test
    public void testFirst() throws Exception {
        setupFilterEntry(Pos.FIRST, null, false);

        HttpServletRequest request = createRequest("/foo");
        ((MockHttpServletRequest) request).setMethod("GET");
        MockHttpServletResponse response = dispatch(request);
        assertEquals("bar", response.getHeader("foo"));
    }

    @Test
    public void testLast() throws Exception {
        try {
            setupFilterEntry(Pos.LAST, null, true);
            fail("SecurityConfigException missing, anonymous filter must be the last one");
        } catch (SecurityConfigException ex) {

        }
    }

    @Test
    public void testBefore() throws Exception {
        setupFilterEntry(Pos.BEFORE, GeoServerSecurityFilterChain.ANONYMOUS_FILTER, false);

        HttpServletRequest request = createRequest("/foo");
        ((MockHttpServletRequest) request).setMethod("GET");
        MockHttpServletResponse response = dispatch(request);
        assertEquals("bar", response.getHeader("foo"));
    }

    @Test
    public void testAfter() throws Exception {
        setupFilterEntry(Pos.AFTER, GeoServerSecurityFilterChain.BASIC_AUTH_FILTER, true);

        HttpServletRequest request = createRequest("/foo");
        ((MockHttpServletRequest) request).setMethod("GET");
        MockHttpServletResponse response = dispatch(request);
        assertEquals("bar", response.getHeader("foo"));
    }

    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Arrays.asList(applicationContext.getBean(GeoServerSecurityFilterChainProxy.class));
    }

    static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public Class<? extends GeoServerSecurityFilter> getFilterClass() {
            return Filter.class;
        }

        @Override
        public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
            Filter f = new Filter();
            f.setAssertAuth(((FilterConfig) config).isAssertSecurityContext());
            return f;
        }
    }

    static class FilterConfig extends SecurityFilterConfig {
        boolean assertAuth = true;

        public void setAssertAuth(boolean assertAuth) {
            this.assertAuth = assertAuth;
        }

        public boolean isAssertSecurityContext() {
            return assertAuth;
        }
    }

    static class Filter extends GeoServerSecurityFilter implements GeoServerAuthenticationFilter {

        boolean assertAuth = true;

        public Filter() {}

        public void setAssertAuth(boolean assertAuth) {
            this.assertAuth = assertAuth;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            //            Authentication auth =
            // SecurityContextHolder.getContext().getAuthentication();
            //            if (assertAuth) {
            //                assertNotNull(auth);
            //            }
            //            else {
            //                assertNull(auth);
            //            }
            ((HttpServletResponse) response).setHeader("foo", "bar");
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
}
