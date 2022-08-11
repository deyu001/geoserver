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

package org.geoserver.security.web.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.Component;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;
import org.geoserver.security.web.UserGroupRoleServicesPage;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.junit.Before;
import org.junit.Test;

public class XMLRoleConfigDetailsPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Override
    protected String getDetailsFormComponentId() {
        return "RoleTabbedPage:panel:namedConfig";
    }

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new UserGroupRoleServicesPage();
    }

    @Override
    protected String getBasePanelId() {
        return "panel:panel:roleServices";
    }

    @Override
    protected Integer getTabIndex() {
        return 1;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return RoleServicesPanel.class;
    }

    protected void setAdminRoleName(String roleName) {
        formTester.setValue("panel:content:adminRoleName", roleName);
    }

    protected String getAdminRoleName() {
        return formTester
                .getForm()
                .get("details:config.adminRoleName")
                .getDefaultModelObjectAsString();
    }

    protected void setFileName(String fileName) {
        formTester.setValue("panel:content:fileName", fileName);
    }

    protected String getFileName() {
        return formTester.getForm().get("details:config.fileName").getDefaultModelObjectAsString();
    }

    protected void setCheckInterval(Integer interval) {
        formTester.setValue("panel:content:checkInterval", interval.toString());
    }

    protected Integer getCheckInterval() {
        String temp =
                formTester
                        .getForm()
                        .get("details:config.checkInterval")
                        .getDefaultModelObjectAsString();
        if (temp == null || temp.length() == 0) return 0;
        return Integer.valueOf(temp);
    }

    protected void setValidating(Boolean flag) {
        formTester.setValue("panel:content:validating", flag);
    }

    protected Boolean getValidating() {
        String temp =
                formTester
                        .getForm()
                        .get("details:config.validating")
                        .getDefaultModelObjectAsString();
        return Boolean.valueOf(temp);
    }

    @Before
    public void removeRoleService2() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        if (secMgr.listRoleServices().contains("default2")) {
            SecurityRoleServiceConfig roleService = secMgr.loadRoleServiceConfig("default2");
            secMgr.removeRoleService(roleService);
        }
    }

    @Test
    public void testAddModifyRemove() throws Exception {
        initializeForXML();

        activatePanel();

        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("test"));
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));

        // Test simple add
        clickAddNew();

        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester();
        setSecurityConfigClassName(XMLRoleServicePanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setFileName("abc.xml");
        setCheckInterval(5000);
        setValidating(true);
        clickCancel();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        clickAddNew();
        newFormTester();
        setSecurityConfigClassName(XMLRoleServicePanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        setFileName("abc.xml");
        setCheckInterval(5000);
        setValidating(true);
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        clickSave();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(3, countItems());
        assertNotNull(getSecurityNamedServiceConfig("default"));

        XMLRoleServiceConfig xmlConfig =
                (XMLRoleServiceConfig) getSecurityNamedServiceConfig("default2");
        assertNotNull(xmlConfig);
        assertEquals("default2", xmlConfig.getName());
        assertEquals(XMLRoleService.class.getName(), xmlConfig.getClassName());
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5000, xmlConfig.getCheckInterval());
        assertTrue(xmlConfig.isValidating());

        // reload from manager
        xmlConfig = (XMLRoleServiceConfig) getSecurityManager().loadRoleServiceConfig("default2");
        assertNotNull(xmlConfig);
        assertEquals("default2", xmlConfig.getName());
        assertEquals(XMLRoleService.class.getName(), xmlConfig.getClassName());
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5000, xmlConfig.getCheckInterval());
        assertTrue(xmlConfig.isValidating());

        // test add with name clash
        clickAddNew();
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester();
        setSecurityConfigClassName(XMLRoleServicePanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        clickSave(); // should not work
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash

        // start test modify
        clickNamedServiceConfig("default");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester("panel:panel:panel:form");
        tester.debugComponentTrees();
        formTester.setValue("panel:adminRoleName", "ROLE_ADMINISTRATOR");

        // setFileName("abcd.xml");
        formTester.setValue("panel:checkInterval", "5001");
        // setCheckInterval(5001);
        formTester.setValue("panel:validating", true);
        // setValidating(true);
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        xmlConfig = (XMLRoleServiceConfig) getSecurityNamedServiceConfig("default");
        assertEquals(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE, xmlConfig.getAdminRoleName());
        assertEquals(
                XMLRoleService.DEFAULT_LOCAL_GROUP_ADMIN_ROLE, xmlConfig.getGroupAdminRoleName());
        assertEquals("roles.xml", xmlConfig.getFileName());
        assertEquals(10000, xmlConfig.getCheckInterval());
        assertTrue(xmlConfig.isValidating());

        clickNamedServiceConfig("default2");
        // detailsPage = (RoleTabbedPage) tester.getLastRenderedPage();
        newFormTester("panel:panel:panel:form");
        formTester.setValue("panel:adminRoleName", null);

        // setFileName("abcd.xml");
        formTester.setValue("panel:checkInterval", "5001");
        // setCheckInterval(5001);
        formTester.setValue("panel:validating", false);

        clickSave();
        tester.assertRenderedPage(basePage.getClass());

        xmlConfig = (XMLRoleServiceConfig) getSecurityNamedServiceConfig("default2");
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5001, xmlConfig.getCheckInterval());
        assertFalse(xmlConfig.isValidating());

        // reload from manager
        xmlConfig = (XMLRoleServiceConfig) getSecurityManager().loadRoleServiceConfig("default2");
        assertNull(xmlConfig.getAdminRoleName());
        assertEquals("abc.xml", xmlConfig.getFileName());
        assertEquals(5001, xmlConfig.getCheckInterval());
        assertFalse(xmlConfig.isValidating());

        // doRemove("tabbedPanel:panel:removeSelected");
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();

        XMLRoleServiceConfig config = new XMLRoleServiceConfig();
        config.setName("default2");
        config.setClassName(XMLRoleService.class.getCanonicalName());
        config.setFileName("foo.xml");
        getSecurityManager().saveRoleService(config);
        activatePanel();

        doRemove(null, "default2");
        assertNull(getSecurityManager().loadRoleService("default2"));
    }
}
