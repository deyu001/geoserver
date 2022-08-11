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
import static org.junit.Assert.fail;

import java.util.SortedSet;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.feedback.FeedbackMessage;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.junit.Before;
import org.junit.Test;

public class NewUserPageTest extends AbstractUserPageTest {

    protected void initializeTester() {
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new NewUserPage(getUserGroupServiceName())
                                        .setReturnPage(returnPage));
    }

    @Before
    public void init() throws Exception {
        doInitialize();
        clearServices();
    }

    protected void doInitialize() throws Exception {
        initializeForXML();
    }

    @Test
    public void testFill() throws Exception {
        doTestFill();
    }

    protected void doTestFill() throws Exception {
        insertValues();
        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);

        newFormTester();
        form.setValue("username", "testuser");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");

        assertTrue(((GeoServerUser) page.get("form").getDefaultModelObject()).isEnabled());
        form.setValue("enabled", false);

        // addUserProperty("coord", "10 10");

        assertTrue(page.userGroupPalette.isEnabled());
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        addNewRole("ROLE_NEW");
        tester.assertRenderedPage(NewUserPage.class);
        tester.assertNoErrorMessage();

        assignRole("ROLE_NEW");

        // reopen new role dialog again to ensure that the current state is not lost
        openCloseRolePanel(NewUserPage.class);
        tester.assertNoErrorMessage();

        addNewGroup("testgroup");
        assignGroup("testgroup");
        tester.assertNoErrorMessage();

        openCloseGroupPanel(NewUserPage.class);
        tester.assertNoErrorMessage();

        assertCalculatedRoles(new String[] {"ROLE_NEW"});
        form.submit("save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerUser user = ugService.getUserByUsername("testuser");
        assertNotNull(user);
        assertFalse(user.isEnabled());

        // assertEquals(1,user.getProperties().size());
        // assertEquals("10 10",user.getProperties().get("coord"));
        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(1, groupList.size());
        assertEquals("testgroup", groupList.iterator().next().getGroupname());

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("testuser");
        assertEquals(1, roleList.size());
        assertEquals("ROLE_NEW", roleList.iterator().next().getAuthority());
    }

    @Test
    public void testFill3() throws Exception {
        doTestFill3();
    }

    protected void doTestFill3() throws Exception {
        insertValues();
        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);

        newFormTester();
        form.setValue("username", "testuser");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");

        // TODO: this is a dummy call for the formtester to store
        // the above vaules in the model, otherwise we would
        // lose the values due to the assingRole call
        openCloseGroupPanel(NewUserPage.class);

        assignRole("ROLE_WMS");
        assertCalculatedRoles(new String[] {"ROLE_AUTHENTICATED", "ROLE_WMS"});

        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerUser user = ugService.getUserByUsername("testuser");
        assertNotNull(user);
        assertTrue(user.isEnabled());

        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(0, groupList.size());

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("testuser");
        assertEquals(1, roleList.size());
        assertTrue(roleList.contains(gaService.createRoleObject("ROLE_WMS")));

        user = (GeoServerUser) ugService.loadUserByUsername("testuser");
        assertEquals(2, user.getAuthorities().size());
        assertTrue(
                user.getAuthorities().contains(gaService.createRoleObject("ROLE_AUTHENTICATED")));
        assertTrue(user.getAuthorities().contains(gaService.createRoleObject("ROLE_WMS")));
    }

    @Test
    public void testFill2() throws Exception {
        // initializeForXML();
        doTestFill2();
    }

    protected void doTestFill2() throws Exception {
        insertValues();
        addAdditonalData();
        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);

        newFormTester();
        form.setValue("username", "testuser");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");

        // TODO: this is a dummy call for the formtester to store
        // the above vaules in the model, otherwise we would
        // lose the values due to the assingGroup call
        openCloseGroupPanel(NewUserPage.class);

        assignGroup("group1");
        assertCalculatedRoles(new String[] {"ROLE_AUTHENTICATED", "ROLE_WFS", "ROLE_WMS"});

        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerUser user = ugService.getUserByUsername("testuser");
        assertNotNull(user);
        assertTrue(user.isEnabled());

        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(1, groupList.size());
        assertEquals("group1", groupList.iterator().next().getGroupname());

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("testuser");
        assertEquals(0, roleList.size());

        user = (GeoServerUser) ugService.loadUserByUsername("testuser");
        assertEquals(3, user.getAuthorities().size());
        assertTrue(
                user.getAuthorities().contains(gaService.createRoleObject("ROLE_AUTHENTICATED")));
        assertTrue(user.getAuthorities().contains(gaService.createRoleObject("ROLE_WFS")));
        assertTrue(user.getAuthorities().contains(gaService.createRoleObject("ROLE_WMS")));
    }

    @Test
    public void testUserNameConflict() throws Exception {
        insertValues();

        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);
        newFormTester();
        form.setValue("username", "user1");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");
        form.submit("save");

        assertTrue(testErrorMessagesWithRegExp(".*user1.*"));
        tester.getMessages(FeedbackMessage.ERROR);
        tester.assertRenderedPage(NewUserPage.class);
    }

    @Test
    public void testInvalidWorkflow() throws Exception {

        activateROUGService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        boolean fail = true;
        try {
            tester.startPage(
                    page =
                            (AbstractUserPage)
                                    new NewUserPage(getROUserGroupServiceName())
                                            .setReturnPage(returnPage));
        } catch (RuntimeException ex) {
            fail = false;
        }
        if (fail) fail("No runtime exception for read only UserGroupService");
    }

    @Test
    public void testPasswordsDontMatch() throws Exception {
        super.doTestPasswordsDontMatch(NewUserPage.class);
    }
}
