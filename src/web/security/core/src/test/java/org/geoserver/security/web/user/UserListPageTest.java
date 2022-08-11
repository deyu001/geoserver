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
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.SortedSet;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractTabbedListPageTest;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Test;

public class UserListPageTest extends AbstractTabbedListPageTest<GeoServerUser> {
    protected boolean withRoles = false;

    protected AbstractSecurityPage listPage(String serviceName) {
        AbstractSecurityPage result = initializeForUGServiceNamed(serviceName);
        tester.clickLink(getTabbedPanelPath() + ":tabs-container:tabs:1:link", true);
        return result;
    }

    protected Page newPage(AbstractSecurityPage page, Object... params) {
        if (params.length == 0)
            return new NewUserPage(getUserGroupServiceName()).setReturnPage(page);
        else return new NewUserPage((String) params[0]).setReturnPage(page);
    }

    protected Page editPage(AbstractSecurityPage page, Object... params) {
        if (params.length == 0) {
            return new EditUserPage(getUserGroupServiceName(), new GeoServerUser("dummyuser"))
                    .setReturnPage(page);
        }

        if (params.length == 1)
            return new EditUserPage(getUserGroupServiceName(), (GeoServerUser) params[0])
                    .setReturnPage(page);
        else
            return new EditUserPage((String) params[0], (GeoServerUser) params[1])
                    .setReturnPage(page);
    }

    @Override
    protected String getSearchString() throws Exception {
        GeoServerUser u = ugService.getUserByUsername("user1");
        assertNotNull(u);
        return u.getUsername();
    }

    @Override
    protected Property<GeoServerUser> getEditProperty() {
        return UserListProvider.USERNAME;
    }

    @Override
    protected boolean checkEditForm(String objectString) {
        return objectString.equals(
                tester.getComponentFromLastRenderedPage("form:username").getDefaultModelObject());
    }

    @Test
    public void testReadOnlyService() throws Exception {
        doInitialize();
        tester.startPage(listPage(getUserGroupServiceName()));
        tester.assertVisible(getRemoveLink().getPageRelativePath());
        tester.assertVisible(getRemoveLinkWithRoles().getPageRelativePath());
        tester.assertVisible(getAddLink().getPageRelativePath());

        activateRORoleService();
        tester.startPage(listPage(getUserGroupServiceName()));
        tester.assertVisible(getRemoveLink().getPageRelativePath());
        tester.assertInvisible(getRemoveLinkWithRoles().getPageRelativePath());
        tester.assertVisible(getAddLink().getPageRelativePath());

        activateROUGService();
        tester.startPage(listPage(getROUserGroupServiceName()));
        tester.assertInvisible(getRemoveLink().getPageRelativePath());
        tester.assertInvisible(getAddLink().getPageRelativePath());
        tester.assertInvisible(getRemoveLinkWithRoles().getPageRelativePath());
    }

    @Override
    protected void simulateDeleteSubmit() throws Exception {
        SelectionUserRemovalLink link =
                (SelectionUserRemovalLink) (withRoles ? getRemoveLinkWithRoles() : getRemoveLink());
        Method m =
                link.delegate
                        .getClass()
                        .getDeclaredMethod("onSubmit", AjaxRequestTarget.class, Component.class);
        m.invoke(link.delegate, null, null);

        SortedSet<GeoServerUser> users = ugService.getUsers();
        assertEquals(0, users.size());
        if (withRoles) assertEquals(0, gaService.getRolesForUser("user1").size());
        else assertEquals(2, gaService.getRolesForUser("user1").size());
    }

    @Test
    public void testRemoveWithRoles() throws Exception {
        withRoles = true;
        // initializeForXML();
        // insertValues();
        addAdditonalData();
        doRemove(getTabbedPanelPath() + ":panel:header:removeSelectedWithRoles");
    }

    @Override
    protected String getTabbedPanelPath() {
        // return "UserGroupTabbedPage";
        return "panel:panel";
    }

    @Override
    protected String getServiceName() {
        return getUserGroupServiceName();
    }
}
