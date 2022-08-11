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

import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.junit.Before;
import org.junit.Test;

public class EditRolePageTest extends AbstractSecurityWicketTestSupport {

    EditRolePage page;

    @Before
    public void init() throws Exception {
        doInitialize();
        clearServices();

        deactivateRORoleService();
        deactivateROUGService();
    }

    protected void doInitialize() throws Exception {
        initializeForXML();
    }

    @Test
    public void testFill() throws Exception {
        doTestFill();
    }

    @Test
    public void testFill2() throws Exception {
        doTestFill2();
    }

    protected void doTestFill() throws Exception {
        insertValues();

        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRoleServiceName());
        tester.startPage(
                page =
                        (EditRolePage)
                                new EditRolePage(
                                                getRoleServiceName(),
                                                gaService.getRoleByName("ROLE_WFS"))
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditRolePage.class);

        assertFalse(tester.getComponentFromLastRenderedPage("form:name").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:parent").isEnabled());
        tester.assertVisible("form:save");

        tester.assertModelValue("form:name", "ROLE_WFS");
        tester.assertModelValue("form:parent", "ROLE_AUTHENTICATED");

        FormTester form = tester.newFormTester("form");
        form.setValue("parent", null);
        // form.select("parent", index);

        // tester.executeAjaxEvent("form:properties:add", "click");
        // form = tester.newFormTester("form");

        // form.setValue("properties:container:list:0:key", "bbox");
        // form.setValue("properties:container:list:0:value", "10 10 20 20");

        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.assertErrorMessages(new String[0]);

        GeoServerRole role = gaService.getRoleByName("ROLE_WFS");
        assertNotNull(role);
        // assertEquals(1,role.getProperties().size());
        // assertEquals("10 10 20 20",role.getProperties().get("bbox"));
        GeoServerRole parentRole = gaService.getParentRole(role);
        assertNull(parentRole);
    }

    protected void doTestFill2() throws Exception {
        insertValues();

        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRoleServiceName());
        tester.startPage(
                page =
                        (EditRolePage)
                                new EditRolePage(
                                                getRoleServiceName(),
                                                gaService.getRoleByName("ROLE_AUTHENTICATED"))
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditRolePage.class);

        tester.assertModelValue("form:name", "ROLE_AUTHENTICATED");
        tester.assertModelValue("form:parent", null);

        // role params are shown sorted by key
        tester.assertModelValue("form:properties:container:list:0:key", "bbox");
        tester.assertModelValue("form:properties:container:list:0:value", "lookupAtRuntime");
        tester.assertModelValue("form:properties:container:list:1:key", "employee");
        tester.assertModelValue("form:properties:container:list:1:value", "");

        tester.executeAjaxEvent("form:properties:container:list:1:remove", "click");
        FormTester form = tester.newFormTester("form");
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerRole role = gaService.getRoleByName("ROLE_AUTHENTICATED");
        assertNotNull(role);
        assertEquals(1, role.getProperties().size());
        assertEquals("lookupAtRuntime", role.getProperties().get("bbox"));
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        // doInitialize();
        activateRORoleService();

        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRORoleServiceName());
        tester.startPage(
                page =
                        (EditRolePage)
                                new EditRolePage(getRORoleServiceName(), GeoServerRole.ADMIN_ROLE)
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditRolePage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:name").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:parent").isEnabled());
        tester.assertInvisible("form:save");
    }
}
