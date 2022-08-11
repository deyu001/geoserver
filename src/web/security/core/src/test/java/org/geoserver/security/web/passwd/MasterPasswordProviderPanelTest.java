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

package org.geoserver.security.web.passwd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.Set;
import org.apache.wicket.Component;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.URLMasterPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.junit.Before;
import org.junit.Test;

public class MasterPasswordProviderPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Before
    public void clearSecurityStuff() throws Exception {
        Set<String> mpProviders = getSecurityManager().listMasterPasswordProviders();
        if (mpProviders.contains("default2")) {
            MasterPasswordProviderConfig default2 =
                    getSecurityManager().loadMasterPassswordProviderConfig("default2");
            getSecurityManager().removeMasterPasswordProvder(default2);
        }
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new PasswordPage();
    }

    @Override
    protected String getBasePanelId() {
        return "form:masterPasswordProviders";
    }

    @Override
    protected Integer getTabIndex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return MasterPasswordProvidersPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Test
    public void testAddModify() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        // detailsPage = (PasswordPolicyPage) tester.getLastRenderedPage();

        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();

        setSecurityConfigName("default2");
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(1, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("default2"));

        clickAddNew();

        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        formTester.setValue("panel:content:uRL", "file:passwd");
        clickSave();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("default2"));

        // test add with name clash
        clickAddNew();
        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        formTester.setValue("panel:content:uRL", "file:passwd");
        clickSave(); // should not work

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash

        // start test modify
        clickNamedServiceConfig("default2");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        formTester.setValue("panel:uRL", "file:passwd2");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        URLMasterPasswordProviderConfig config =
                (URLMasterPasswordProviderConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(new URL("file:passwd"), config.getURL());

        clickNamedServiceConfig("default2");

        newFormTester("panel:panel:form");
        formTester.setValue("panel:uRL", "file:passwd2");
        clickSave();

        tester.assertRenderedPage(basePage.getClass());

        config = (URLMasterPasswordProviderConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(new URL("file:passwd2"), config.getURL());
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        URLMasterPasswordProviderConfig config = new URLMasterPasswordProviderConfig();
        config.setName("default2");
        config.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        config.setURL(new URL("file:passwd"));
        config.setLoginEnabled(true);

        getSecurityManager().saveMasterPasswordProviderConfig(config);
        activatePanel();

        assertEquals(2, countItems());

        doRemove(null, "default2");
        assertNull(getSecurityManager().loadMasterPassswordProviderConfig("default2"));
        assertEquals(1, countItems());
    }
}
