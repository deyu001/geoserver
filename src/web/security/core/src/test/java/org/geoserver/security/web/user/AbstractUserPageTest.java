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

package org.geoserver.security.web.user;

import static org.junit.Assert.assertTrue;

import org.apache.wicket.Page;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.group.NewGroupPage;
import org.geoserver.security.web.role.NewRolePage;
import org.junit.Test;

public abstract class AbstractUserPageTest extends AbstractSecurityWicketTestSupport {

    protected AbstractUserPage page;
    protected FormTester form;

    protected abstract void initializeTester();

    @Test
    public void testReadOnlyRoleService() throws Exception {
        doInitialize();
        activateRORoleService();
        initializeTester();
        assertTrue(page.userGroupPalette.isEnabled());
    }

    protected void doInitialize() throws Exception {
        initializeForXML();
    }

    protected void doTestPasswordsDontMatch(Class<? extends Page> pageClass) throws Exception {
        doInitialize();
        initializeTester();
        newFormTester();
        form.setValue("username", "user");
        form.setValue("password", "pwd1");
        form.setValue("confirmPassword", "pwd2");
        form.submit("save");

        assertTrue(testErrorMessagesWithRegExp(".*[Pp]assword.*"));
        tester.assertRenderedPage(pageClass);
    }

    protected void newFormTester() {
        form = tester.newFormTester("form");
    }

    protected void addNewRole(String roleName) {
        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);

        FormTester roleform = tester.newFormTester("form");
        roleform.setValue("name", "ROLE_NEW");
        roleform.submit("save");

        newFormTester();
    }

    protected void addNewGroup(String groupName) {
        // add a role on the fly
        form.submit("groups:addGroup");
        tester.assertRenderedPage(NewGroupPage.class);
        FormTester groupform = tester.newFormTester("form");
        groupform.setValue("groupname", groupName);
        groupform.submit("save");
        newFormTester();
    }

    protected void assignRole(String roleName) throws Exception {
        form.setValue("roles:palette:recorder", gaService.getRoleByName(roleName).getAuthority());
        form.submit();
        tester.executeAjaxEvent("form:roles:palette:recorder", "change");
        newFormTester();
        form.setValue("roles:palette:recorder", gaService.getRoleByName(roleName).getAuthority());
    }

    protected void assignGroup(String groupName) throws Exception {
        String theName = ugService.getGroupByGroupname(groupName).getGroupname();
        form.setValue("groups:palette:recorder", theName);
        form.submit();
        tester.executeAjaxEvent("form:groups:palette:recorder", "change");
        newFormTester();
        form.setValue("groups:palette:recorder", theName);
    }

    protected void openCloseRolePanel(Class<? extends Page> responseClass) {
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(responseClass);
        newFormTester();
    }

    protected void openCloseGroupPanel(Class<? extends Page> responseClass) {
        form.submit("groups:addGroup");
        tester.assertRenderedPage(NewGroupPage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(responseClass);
        newFormTester();
    }

    protected void addUserProperty(String key, String value) {
        tester.executeAjaxEvent("form:properties:add", "click");
        // newFormTester();

        form.setValue("properties:container:list:0:key", key);
        form.setValue("properties:container:list:0:value", value);
    }

    protected void assertCalculatedRoles(String[] roles) throws Exception {
        for (int i = 0; i < roles.length; i++)
            tester.assertModelValue(
                    "form:calculatedRolesContainer:calculatedRoles:" + i,
                    gaService.getRoleByName(roles[i]));
    }
}
