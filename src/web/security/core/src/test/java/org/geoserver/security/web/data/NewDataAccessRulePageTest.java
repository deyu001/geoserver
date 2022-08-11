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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.role.NewRolePage;
import org.junit.Before;
import org.junit.Test;

public class NewDataAccessRulePageTest extends AbstractSecurityWicketTestSupport {

    NewDataAccessRulePage page;

    @Before
    public void init() throws Exception {
        initializeForXML();
        clearServices();
        // clear the rules
        DataAccessRuleDAO.get().clear();
        // ensure cleared rules survive reload
        DataAccessRuleDAO.get().storeRules();
    }

    @Test
    public void testFillAndSwitchToNewRolePage() throws Exception {
        testFill(true);
    }

    @Test
    public void testFill() throws Exception {
        testFill(false);
    }

    private void testFill(boolean testSwitchToNewRole) throws IOException {
        // insertValues();
        tester.startPage(page = new NewDataAccessRulePage());
        tester.assertRenderedPage(NewDataAccessRulePage.class);

        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.rootChoice.getChoices(), MockData.CITE_PREFIX);
        form.select("root", index);
        tester.executeAjaxEvent("form:root", "change");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(), MockData.STREAMS.getLocalPart());
        form.select("layerContainer:layerAndLabel:layer", index);

        index = page.accessModeChoice.getChoices().indexOf(AccessMode.READ);
        form.select("accessMode", index);

        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        form = tester.newFormTester("form");
        form.setValue("name", "ROLE_NEW");
        form.submit("save");

        // assign the new role to the method
        form = tester.newFormTester("form");
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());

        // reopen new role dialog again to ensure that the current state is not lost
        if (testSwitchToNewRole) {
            form.submit("roles:addRole");
            tester.assertRenderedPage(NewRolePage.class);
            tester.clickLink("form:cancel");
            tester.assertRenderedPage(NewDataAccessRulePage.class);
            form = tester.newFormTester("form", false);
        }

        // now save
        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(DataSecurityPage.class);

        DataAccessRule foundRule = null;
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRules()) {
            if (MockData.CITE_PREFIX.equals(rule.getRoot())
                    && MockData.STREAMS.getLocalPart().equals(rule.getLayer())
                    && AccessMode.READ.equals(rule.getAccessMode())) {
                foundRule = rule;
                break;
            }
        }
        assertNotNull(foundRule);
        assertEquals(1, foundRule.getRoles().size());
        assertEquals("ROLE_NEW", foundRule.getRoles().iterator().next());
    }

    @Test
    public void testDuplicateRule() throws Exception {
        initializeServiceRules();

        addRule();
        tester.assertNoErrorMessage();
        addRule();
        assertTrue(
                testErrorMessagesWithRegExp(
                        ".*"
                                + MockData.CITE_PREFIX
                                + "\\."
                                + MockData.BRIDGES.getLocalPart()
                                + ".*"));
        tester.assertRenderedPage(NewDataAccessRulePage.class);
    }

    private void addRule() {
        tester.startPage(page = new NewDataAccessRulePage());

        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.rootChoice.getChoices(), MockData.CITE_PREFIX);
        form.select("root", index);
        tester.executeAjaxEvent("form:root", "change");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(), MockData.BRIDGES.getLocalPart());
        form.select("layerContainer:layerAndLabel:layer", index);

        index = page.accessModeChoice.getChoices().indexOf(AccessMode.WRITE);
        form.select("accessMode", index);

        form.setValue("roles:palette:recorder", "ROLE_WMS");

        form.submit("save");
    }

    @Test
    public void testEmptyRoles() throws Exception {
        initializeServiceRules();
        tester.startPage(page = new NewDataAccessRulePage());

        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.rootChoice.getChoices(), MockData.CITE_PREFIX);
        form.select("root", index);
        tester.executeAjaxEvent("form:root", "change");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(), MockData.STREAMS.getLocalPart());
        form.select("layerContainer:layerAndLabel:layer", index);

        index = page.accessModeChoice.getChoices().indexOf(AccessMode.READ);
        form.select("accessMode", index);

        form.submit("save");
        assertTrue(testErrorMessagesWithRegExp(".*no role.*"));
        tester.assertRenderedPage(NewDataAccessRulePage.class);
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        activateRORoleService();
        tester.startPage(page = new NewDataAccessRulePage());
        tester.assertInvisible("form:roles:addRole");
    }

    @Test
    public void testAddAdminRule() throws Exception {
        tester.startPage(page = new NewDataAccessRulePage());
        tester.assertRenderedPage(NewDataAccessRulePage.class);

        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.rootChoice.getChoices(), MockData.CITE_PREFIX);
        form.select("root", index);
        tester.executeAjaxEvent("form:root", "change");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(), DataAccessRule.ANY);
        form.select("layerContainer:layerAndLabel:layer", index);

        index = page.accessModeChoice.getChoices().indexOf(AccessMode.ADMIN);
        form.select("accessMode", index);

        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        form = tester.newFormTester("form");
        form.setValue("name", "ROLE_NEW");
        form.submit("save");
        tester.assertNoErrorMessage();
        // assign the new role to the method
        form = tester.newFormTester("form");
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());

        // reopen new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(NewDataAccessRulePage.class);

        DataAccessRuleDAO dao = DataAccessRuleDAO.get();

        DataAccessRule rule =
                new DataAccessRule(MockData.CITE_PREFIX, DataAccessRule.ANY, AccessMode.ADMIN);
        assertFalse(dao.getRules().contains(rule));

        // now save
        form = tester.newFormTester("form");
        form.submit("save");

        dao.reload();
        assertTrue(dao.getRules().contains(rule));
    }

    @Test
    public void testAddGlobalLayerGroupRule() throws Exception {
        tester.startPage(page = new NewDataAccessRulePage());
        tester.assertRenderedPage(NewDataAccessRulePage.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("globalGroupRule", true);
        tester.executeAjaxEvent("form:globalGroupRule", "change");
        // need to set it again, the ajax event apparently resets the form...
        form.setValue("globalGroupRule", true);
        int index = indexOf(page.rootChoice.getChoices(), NATURE_GROUP);
        form.select("root", index);

        // this one should have been made invisible
        tester.assertInvisible("form:layerContainer:layerAndLabel");

        // setup access mode
        index = page.accessModeChoice.getChoices().indexOf(AccessMode.READ);
        form.select("accessMode", index);

        // allow all roles for simplicity
        form.setValue("roles:anyRole", true);
        // tester.debugComponentTrees();
        form.submit("save");
        tester.assertNoErrorMessage();

        // check the global group rule has been setup
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        DataAccessRule rule = new DataAccessRule(NATURE_GROUP, null, AccessMode.READ);
        final List<DataAccessRule> rules = dao.getRules();
        assertTrue(rules.contains(rule));
    }

    @Test
    public void testWorkspaceGlobalLayerGroupRule() throws Exception {
        tester.startPage(page = new NewDataAccessRulePage());
        tester.assertRenderedPage(NewDataAccessRulePage.class);

        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.rootChoice.getChoices(), MockData.CITE_PREFIX);
        form.select("root", index);
        tester.executeAjaxEvent("form:root", "change");
        form.setValue("roles:anyRole", true);
        tester.executeAjaxEvent("form:roles:anyRole", "click");

        // start again, the ajax event voided the previous selection...
        form.select("root", index);

        // select workspace specific group
        index = indexOf(page.layerChoice.getChoices(), CITE_NATURE_GROUP);
        assertNotEquals(-1, index);
        form.select("layerContainer:layerAndLabel:layer", index);

        // setup access mode
        index = page.accessModeChoice.getChoices().indexOf(AccessMode.READ);
        form.select("accessMode", index);

        // allow all roles for simplicity
        form.setValue("roles:anyRole", true);
        form.submit("save");
        tester.assertNoErrorMessage();

        // check the global group rule has been setup
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        DataAccessRule rule =
                new DataAccessRule(MockData.CITE_PREFIX, CITE_NATURE_GROUP, AccessMode.READ);
        final List<DataAccessRule> rules = dao.getRules();
        assertTrue(rules.contains(rule));
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
}
