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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.SortedSet;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.web.AbstractSecurityPage;
import org.junit.Before;
import org.junit.Test;

public class EditUserPageTest extends AbstractUserPageTest {

    GeoServerUser current;

    @Before
    public void init() throws Exception {
        doInitialize();
        clearServices();

        deactivateRORoleService();
        deactivateROUGService();
    }

    @Test
    public void testFill() throws Exception {
        doTestFill();
    }

    protected void doTestFill() throws Exception {
        insertValues();
        addAdditonalData();

        current = ugService.getUserByUsername("user1");
        initializeTester();
        tester.assertRenderedPage(EditUserPage.class);

        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());

        tester.assertModelValue("form:username", "user1");
        GeoServerPasswordEncoder encoder =
                (GeoServerPasswordEncoder)
                        GeoServerExtensions.bean(ugService.getPasswordEncoderName());
        String enc =
                (String)
                        tester.getComponentFromLastRenderedPage("form:password")
                                .getDefaultModelObject();
        assertTrue(encoder.isPasswordValid(enc, "11111", null));
        enc =
                (String)
                        tester.getComponentFromLastRenderedPage("form:confirmPassword")
                                .getDefaultModelObject();
        assertTrue(encoder.isPasswordValid(enc, "11111", null));
        tester.assertModelValue("form:enabled", Boolean.TRUE);

        newFormTester();
        form.setValue("enabled", false);
        // addUserProperty("coord", "10 10");

        assertTrue(page.userGroupPalette.isEnabled());
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        tester.debugComponentTrees();
        addNewRole("ROLE_NEW");
        tester.assertRenderedPage(EditUserPage.class);

        assignRole("ROLE_NEW");

        // reopen new role dialog again to ensure that the current state is not lost
        openCloseRolePanel(EditUserPage.class);
        assertCalculatedRoles(
                new String[] {"ROLE_AUTHENTICATED", "ROLE_NEW", "ROLE_WFS", "ROLE_WMS"});

        addNewGroup("testgroup");
        assignGroup("testgroup");

        openCloseGroupPanel(EditUserPage.class);

        assertCalculatedRoles(new String[] {"ROLE_NEW"});
        // print(tester.getLastRenderedPage(),true,true);
        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        // tester.assertRenderedPage(UserGroupTabbedPage.class);

        GeoServerUser user = ugService.getUserByUsername("user1");
        assertNotNull(user);
        assertFalse(user.isEnabled());

        // assertEquals(1,user.getProperties().size());
        // assertEquals("10 10",user.getProperties().get("coord"));
        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(1, groupList.size());
        assertTrue(groupList.contains(ugService.getGroupByGroupname("testgroup")));

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("user1");
        assertEquals(1, roleList.size());
        assertTrue(roleList.contains(gaService.getRoleByName("ROLE_NEW")));
    }

    @Test
    public void testReadOnlyUserGroupService() throws Exception {
        initializeForXML();
        doTestReadOnlyUserGroupService();
    }

    protected void doTestReadOnlyUserGroupService() throws Exception {
        insertValues();
        activateROUGService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        current = ugService.getUserByUsername("user1");
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new EditUserPage(getROUserGroupServiceName(), current)
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditUserPage.class);

        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:password").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:confirmPassword").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:groups").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        tester.assertVisible("form:save");

        newFormTester();
        assignRole(GeoServerRole.ADMIN_ROLE.getAuthority());
        form.submit("save");

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("user1");
        assertEquals(1, roleList.size());
        assertTrue(
                roleList.contains(
                        gaService.getRoleByName(GeoServerRole.ADMIN_ROLE.getAuthority())));
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        doTestReadOnlyRoleService();
    }

    protected void doTestReadOnlyRoleService() throws Exception {
        insertValues();
        activateRORoleService();
        current = ugService.getUserByUsername("user1");
        initializeTester();
        tester.assertRenderedPage(EditUserPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:password").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:confirmPassword").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:groups").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());

        tester.assertVisible("form:save");

        newFormTester();
        form.setValue("enabled", Boolean.FALSE);
        form.submit("save");

        GeoServerUser user = ugService.getUserByUsername("user1");
        assertNotNull(user);
        assertFalse(user.isEnabled());
    }

    @Test
    public void testAllServicesReadOnly() throws Exception {
        insertValues();
        activateROUGService();
        activateRORoleService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        current = ugService.getUserByUsername("user1");
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new EditUserPage(getROUserGroupServiceName(), current)
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditUserPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:password").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:confirmPassword").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:groups").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        // tester.assertInvisible("form:save");
    }

    @Override
    protected void initializeTester() {
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new EditUserPage(getUserGroupServiceName(), current)
                                        .setReturnPage(returnPage));
    }

    @Test
    public void testPasswordsDontMatch() throws Exception {
        insertValues();
        current = ugService.getUserByUsername("user1");
        super.doTestPasswordsDontMatch(EditUserPage.class);
    }
}
