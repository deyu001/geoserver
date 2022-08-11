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

package org.geoserver.security.web.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.SortedSet;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.role.NewRolePage;
import org.junit.Before;
import org.junit.Test;

public class NewGroupPageTest extends AbstractSecurityWicketTestSupport {

    NewGroupPage page;

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
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (NewGroupPage)
                                new NewGroupPage(getUserGroupServiceName())
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(NewGroupPage.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("groupname", "testgroup");

        assertTrue(((GeoServerUserGroup) page.get("form").getDefaultModelObject()).isEnabled());
        form.setValue("enabled", false);

        assertTrue(page.rolePalette.isEnabled());
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        form = tester.newFormTester("form");
        form.setValue("name", "ROLE_NEW");
        form.submit("save");

        // assign the new role to the new group
        form = tester.newFormTester("form");
        tester.assertRenderedPage(NewGroupPage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());

        // reopen new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(NewGroupPage.class);

        // now save
        form = tester.newFormTester("form");
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        tester.assertErrorMessages(new String[0]);

        GeoServerUserGroup group = ugService.getGroupByGroupname("testgroup");
        assertNotNull(group);
        assertFalse(group.isEnabled());
        SortedSet<GeoServerRole> roleList = gaService.getRolesForGroup("testgroup");
        assertEquals(1, roleList.size());
        assertEquals("ROLE_NEW", roleList.iterator().next().getAuthority());
    }

    @Test
    public void testGroupNameConflict() throws Exception {
        insertValues();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (NewGroupPage)
                                new NewGroupPage(getUserGroupServiceName())
                                        .setReturnPage(returnPage));

        FormTester form = tester.newFormTester("form");
        form.setValue("groupname", "group1");
        form.submit("save");

        assertTrue(testErrorMessagesWithRegExp(".*group1.*"));
        tester.getMessages(FeedbackMessage.ERROR);
        tester.assertRenderedPage(NewGroupPage.class);
    }

    @Test
    public void testInvalidWorkflow() throws Exception {
        activateROUGService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        boolean fail = true;
        try {
            tester.startPage(
                    page =
                            (NewGroupPage)
                                    new NewGroupPage(getROUserGroupServiceName())
                                            .setReturnPage(returnPage));
        } catch (RuntimeException ex) {
            fail = false;
        }
        if (fail) fail("No runtime exception for read only UserGroupService");
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        activateRORoleService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (NewGroupPage)
                                new NewGroupPage(getUserGroupServiceName())
                                        .setReturnPage(returnPage));
        assertFalse(page.rolePalette.isEnabled());

        FormTester form = tester.newFormTester("form");
        form.setValue("groupname", "testgroup");
        form.submit("save");

        GeoServerUserGroup group = ugService.getGroupByGroupname("testgroup");
        assertNotNull(group);
        assertTrue(group.isEnabled());
        SortedSet<GeoServerRole> roleList = gaService.getRolesForGroup("testgroup");
        assertEquals(0, roleList.size());
    }
}
