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

package org.geoserver.security.web.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.wicket.Component;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.junit.Before;
import org.junit.Test;

public class UsernamePasswordDetailsPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Override
    protected String getDetailsFormComponentId() {
        return "authenticationProviderPanel:namedConfig";
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new AuthenticationPage();
    }

    @Override
    protected String getBasePanelId() {
        return "form:authProviders";
    }

    @Override
    protected Integer getTabIndex() {
        return 2;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return AuthenticationProviderPanel.class;
    }

    protected void setUGName(String serviceName) {
        formTester.setValue("panel:content:userGroupServiceName", serviceName);
    }

    protected String getUGServiceName() {
        return formTester
                .getForm()
                .get("details:config.userGroupServiceName")
                .getDefaultModelObjectAsString();
    }

    @Before
    public void clearAuthProvider() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        if (secMgr.listAuthenticationProviders().contains("default2")) {
            SecurityAuthProviderConfig config = secMgr.loadAuthenticationProviderConfig("default2");
            secMgr.removeAuthenticationProvider(config);
        }
    }

    @Test
    public void testAddModifyRemove() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName("default2");
        setUGName("default");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setUGName("default");
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        UsernamePasswordAuthenticationProviderConfig authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityNamedServiceConfig("default2");
        assertNotNull(authConfig);
        assertEquals("default2", authConfig.getName());
        assertEquals(
                UsernamePasswordAuthenticationProvider.class.getName(), authConfig.getClassName());
        assertEquals("default", authConfig.getUserGroupServiceName());

        // reload from manager
        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityManager().loadAuthenticationProviderConfig("default2");
        assertNotNull(authConfig);
        assertEquals("default2", authConfig.getName());
        assertEquals(
                UsernamePasswordAuthenticationProvider.class.getName(), authConfig.getClassName());
        assertEquals("default", authConfig.getUserGroupServiceName());

        // test add with name clash
        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setUGName("default");
        clickSave(); // should not work

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash

        // start test modify
        clickNamedServiceConfig("default");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        formTester.setValue("panel:userGroupServiceName", "test");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityNamedServiceConfig("default");
        assertEquals("default", authConfig.getUserGroupServiceName());

        clickNamedServiceConfig("default2");
        newFormTester("panel:panel:form");
        formTester.setValue("panel:userGroupServiceName", "test");
        clickSave();
        tester.assertRenderedPage(basePage.getClass());

        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityNamedServiceConfig("default2");
        assertEquals("test", authConfig.getUserGroupServiceName());

        // reload from manager
        authConfig =
                (UsernamePasswordAuthenticationProviderConfig)
                        getSecurityManager().loadAuthenticationProviderConfig("default2");
        assertEquals("test", authConfig.getUserGroupServiceName());
    }

    @Test
    public void testMultipleAuthProviders() throws Exception {
        initializeForXML();

        activatePanel();

        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test add 1
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName("default_001");
        setUGName("default");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default_001");
        setUGName("default");
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        // Test add 2
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);

        newFormTester();
        setSecurityConfigName("default_002");
        setUGName("default");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(UsernamePasswordAuthProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default_002");
        setUGName("default");
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        // start test modify
        clickNamedServiceConfig("default_001");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        clickNamedServiceConfig("default_002");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        doRemove(null, "default_001");
        doRemove(null, "default_002");
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        UsernamePasswordAuthenticationProviderConfig config =
                new UsernamePasswordAuthenticationProviderConfig();
        config.setName("default2");
        config.setClassName(UsernamePasswordAuthenticationProvider.class.getCanonicalName());
        config.setUserGroupServiceName("default");
        getSecurityManager().saveAuthenticationProvider(config);

        activatePanel();
        doRemove(null, "default2");

        assertNull(getSecurityManager().loadAuthenticationProvider("default2"));
    }
}
