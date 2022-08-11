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

package org.geoserver.security.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.role.NewRolePage;
import org.geoserver.test.RunTestSetup;
import org.junit.Before;
import org.junit.Test;

public class EditDataAccessRulePageTest extends AbstractSecurityWicketTestSupport {

    EditDataAccessRulePage page;
    String ruleName =
            MockData.CITE_PREFIX
                    + "."
                    + MockData.LAKES.getLocalPart()
                    + "."
                    + AccessMode.WRITE.getAlias();

    @Before
    public void init() throws Exception {
        initializeForXML();
        clearServices();
        DataAccessRuleDAO.get().clear();
    }

    @Test
    public void testFill() throws Exception {

        tester.startPage(page = new EditDataAccessRulePage(getRule(ruleName)));
        tester.assertRenderedPage(EditDataAccessRulePage.class);

        tester.assertModelValue("form:root", MockData.CITE_PREFIX);
        tester.assertModelValue(
                "form:layerContainer:layerAndLabel:layer", MockData.LAKES.getLocalPart());
        tester.assertModelValue("form:accessMode", AccessMode.WRITE);

        // Does not work with Palette
        // tester.assertModelValue("form:roles:roles:recorder", { ROLE_WMS,ROLE_WFS });

        tester.assertModelValue("form:roles:anyRole", Boolean.FALSE);
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("roles:anyRole", true);

        // open new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(EditDataAccessRulePage.class);

        form = tester.newFormTester("form");
        form.setValue("roles:anyRole", true);
        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(DataSecurityPage.class);

        DataAccessRule rule = getRule(ruleName);
        assertNotNull(rule);
        assertEquals(1, rule.getRoles().size());
        assertEquals(GeoServerRole.ANY_ROLE, rule.getRoles().iterator().next());
    }

    @Test
    @RunTestSetup
    public void testEmptyRoles() throws Exception {
        // initializeForXML();
        initializeServiceRules();
        tester.startPage(page = new EditDataAccessRulePage(getRule(ruleName)));

        FormTester form = tester.newFormTester("form");
        form.setValue("roles:palette:recorder", "");

        form.submit("save");
        tester.assertRenderedPage(EditDataAccessRulePage.class);
        // print(tester.getLastRenderedPage(),true,true);
        assertTrue(testErrorMessagesWithRegExp(".*no role.*"));
        tester.assertRenderedPage(EditDataAccessRulePage.class);
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        // initializeForXML();
        activateRORoleService();
        tester.startPage(page = new EditDataAccessRulePage(getRule(ruleName)));
        tester.assertInvisible("form:roles:addRole");
    }

    protected int indexOf(List<? extends String> strings, String searchValue) {
        int index = 0;
        for (String s : strings) {
            if (s.equals(searchValue)) return index;
            index++;
        }
        assertNotEquals(index, -1);
        return -1;
    }

    DataAccessRule getRule(String key) {
        DataAccessRuleDAO.get().reload();
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRules()) {
            if (key.equals(rule.getKey())) {
                return rule;
            }
        }
        return null;
    }
}
