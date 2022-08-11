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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.data.test.Security_2_2_TestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.config.SSLFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.util.StringUtils;

/**
 * Tests migration from 2.2.x to 2.3.x
 *
 * @author mcr
 */
public class MigrateFrom_2_2_Test extends GeoServerSystemTestSupport {

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new Security_2_2_TestData();
    }

    @Test
    public void testMigration() throws Exception {

        File logoutFilterDir =
                new File(
                        getSecurityManager().get("security/filter").dir(),
                        GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        File oldLogoutFilterConfig = new File(logoutFilterDir, "config.xml.2.2.x");
        assertTrue(oldLogoutFilterConfig.exists());

        File oldSecManagerConfig =
                new File(getSecurityManager().get("security").dir(), "config.xml.2.2.x");
        assertTrue(oldSecManagerConfig.exists());

        RoleFilterConfig rfConfig =
                (RoleFilterConfig)
                        getSecurityManager()
                                .loadFilterConfig(GeoServerSecurityFilterChain.ROLE_FILTER);

        assertNotNull(rfConfig);

        SSLFilterConfig sslConfig =
                (SSLFilterConfig)
                        getSecurityManager()
                                .loadFilterConfig(GeoServerSecurityFilterChain.SSL_FILTER);

        assertNotNull(sslConfig);

        assertNull(
                getSecurityManager()
                        .loadFilterConfig(
                                GeoServerSecurityFilterChain.GUI_EXCEPTION_TRANSLATION_FILTER));

        SecurityManagerConfig config = getSecurityManager().loadSecurityConfig();
        for (RequestFilterChain chain : config.getFilterChain().getRequestChains()) {
            assertFalse(
                    chain.getFilterNames()
                            .contains(
                                    GeoServerSecurityFilterChain
                                            .DYNAMIC_EXCEPTION_TRANSLATION_FILTER));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER));
            assertFalse(
                    chain.getFilterNames()
                            .remove(GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER));

            assertFalse(chain.isDisabled());
            assertFalse(chain.isRequireSSL());
            assertFalse(StringUtils.hasLength(chain.getRoleFilterName()));

            if (GeoServerSecurityFilterChain.WEB_CHAIN_NAME.equals(chain.getName())
                    || GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME.equals(chain.getName())
                    || GeoServerSecurityFilterChain.WEB_LOGOUT_CHAIN_NAME.equals(chain.getName()))
                assertTrue(chain.isAllowSessionCreation());
            else assertFalse(chain.isAllowSessionCreation());

            if (chain instanceof VariableFilterChain) {
                VariableFilterChain vchain = (VariableFilterChain) chain;

                assertEquals(
                        GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
                        vchain.getExceptionTranslationName());

                if (GeoServerSecurityFilterChain.REST_CHAIN_NAME.equals(vchain.getName())
                        || GeoServerSecurityFilterChain.GWC_CHAIN_NAME.equals(vchain.getName()))
                    assertEquals(
                            GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR,
                            vchain.getInterceptorName());
                else
                    assertEquals(
                            GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR,
                            vchain.getInterceptorName());
            }
        }
    }

    @Test
    public void testWebLoginChainSessionCreation() throws Exception {
        // GEOS-6077
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        RequestFilterChain chain =
                config.getFilterChain()
                        .getRequestChainByName(GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME);
        assertTrue(chain.isAllowSessionCreation());
    }
}
