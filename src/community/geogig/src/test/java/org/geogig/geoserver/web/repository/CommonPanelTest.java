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

package org.geogig.geoserver.web.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.model.DropDownModel;
import org.geogig.geoserver.model.DropDownTestUtil;
import org.geogig.geoserver.web.RepositoriesPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Parent test class to hold common methods. */
public abstract class CommonPanelTest extends GeoServerWicketTestSupport {

    protected RepositoriesPage repoPage;

    @Rule public TemporaryFolder temp;

    @After
    public void after() {
        if (temp != null) {
            temp.delete();
            temp = null;
        }
    }

    /**
     * Before method that navigates all subclass tests to their respective starting pages.
     *
     * @throws java.io.IOException
     */
    protected void navigateToStartPage() throws IOException {
        // login
        login();
        // create RepositoriesPage
        repoPage = new RepositoriesPage();
        // start on the repos page
        tester.startPage(repoPage);
        // click the appropriate link
        tester.clickLink(getStartPage());
        // verify the page is the correct type
        tester.assertRenderedPage(getStartPageClass());
        temp = new TemporaryFolder();
        temp.create();
    }

    @After
    public void resetDropDownChoices() {
        // restor default backend choices
        DropDownTestUtil.resetAvailableBackends();
    }

    /**
     * Asserts that FeedbackMessages match the expected list of messages.
     *
     * @param actualMsgs List of FeedbackMessages received from submitting the form.
     * @param expectedMsgs List of expected String messages that should appear in the Feedback
     *     Panel.
     */
    protected void assertFeedbackMessages(
            List<FeedbackMessage> actualMsgs, List<String> expectedMsgs) {
        // assert sizes are equal
        assertEquals(
                "Unexpected number of FeedbackMessages", expectedMsgs.size(), actualMsgs.size());
        // loop through expected and assert they are present in the actuals
        final List<String> actuals =
                Lists.transform(
                        actualMsgs,
                        new Function<FeedbackMessage, String>() {
                            @Override
                            public String apply(FeedbackMessage input) {
                                return input.getMessage().toString();
                            }
                        });
        for (String expectedMsg : expectedMsgs) {
            assertTrue(
                    String.format("Missing expected FeedbackMessage: %s", expectedMsg),
                    actuals.contains(expectedMsg));
        }
    }

    /**
     * Simulates a user selecting the repository configuration type from the Add/Edit/Import panels.
     *
     * @param type The config type to select from the DropDownChoice.
     */
    protected void select(final String type) {
        // get the component holding the dropdown
        DropDownChoiceParamPanel panel =
                (DropDownChoiceParamPanel)
                        tester.getComponentFromLastRenderedPage(getChoicePanel());
        // get the dropdown choice component
        DropDownChoice<Serializable> choice = panel.getFormComponent();
        // get the form
        FormTester formTester = tester.newFormTester(getFrom());
        // ensure choice is available
        assertTrue(
                "Choice is not available from DropDown: " + type,
                choice.getChoices().contains(type));
        // make the selection
        formTester.select(getFormChoiceComponent(), choice.getChoices().indexOf(type));
        // fire the ajax event to simulate the selection
        tester.executeAjaxEvent(choice, "change");
    }

    /**
     * Retrieve the String representation of the DropDownChoice component on the form.
     *
     * @return String representation of the path to the Choice component, relative to the containing
     *     form.
     */
    protected abstract String getFormChoiceComponent();

    /**
     * Retrieve the String representation of the Form.
     *
     * @return String representation of the path to the Form component.
     */
    protected abstract String getFrom();

    /**
     * Retrieve the String representation of the DropDownChoice panel.
     *
     * @return String representation of the path to the panel containing the DropDownChoice
     *     component, relative to the start page.
     */
    protected abstract String getChoicePanel();

    /**
     * Retrieve the String representation of the Page from which this test should start.
     *
     * @return String representation of the path to the Page from which subclasses should start
     *     testing.
     */
    protected abstract String getStartPage();

    /**
     * Retrieve the Class of the Page from which this test should start.
     *
     * @return Page subclass Class type of the start page for this test. Used for asserting the
     *     correct start page.
     */
    protected abstract Class<? extends Page> getStartPageClass();

    protected abstract void verifyPostgreSQLBackendComponents();

    protected abstract void verifyDirectoryBackendComponents();

    protected abstract void verifyNoBackendComponents();

    @Test
    public void testNoRocksDBBackend() throws IOException {
        // override the available backends
        ArrayList<String> configs = new ArrayList<>(1);
        configs.add(DropDownModel.PG_CONFIG);
        DropDownTestUtil.setAvailableBackends(configs, configs.get(0));

        navigateToStartPage();
        // try to select Directory from the dropdown
        try {
            select(DropDownModel.DIRECTORY_CONFIG);
            fail(
                    "DropDown config option should not be available: "
                            + DropDownModel.DIRECTORY_CONFIG);
        } catch (AssertionError ae) {
            // AssertionException expected here from the call to select()
        }
        // verify PostgreSQL config components are visible
        verifyPostgreSQLBackendComponents();
    }

    @Test
    public void testNoPostgreSQLBackend() throws IOException {
        // override the available backends
        ArrayList<String> configs = new ArrayList<>(1);
        configs.add(DropDownModel.DIRECTORY_CONFIG);
        DropDownTestUtil.setAvailableBackends(configs, configs.get(0));

        navigateToStartPage();
        // try to select PostgreSQL from the dropdown
        try {
            select(DropDownModel.PG_CONFIG);
            fail("DropDown config option should not be available: " + DropDownModel.PG_CONFIG);
        } catch (AssertionError ae) {
            // AssertionException expected here from the call to select()
        }
        // verify Directory config components are visible
        verifyDirectoryBackendComponents();
    }

    @Test
    public void testNoAvailableBackends() throws IOException {
        // override the available backends
        DropDownTestUtil.setAvailableBackends(new ArrayList<>(0), null);

        navigateToStartPage();
        // try to select Directory from the dropdown
        try {
            select(DropDownModel.DIRECTORY_CONFIG);
            fail(
                    "DropDown config option should not be available: "
                            + DropDownModel.DIRECTORY_CONFIG);
        } catch (AssertionError ae) {
            // AssertionException expected here from the call to select()
        }
        // try to select PostgreSQL from the dropdown
        try {
            select(DropDownModel.PG_CONFIG);
            fail("DropDown config option should not be available: " + DropDownModel.PG_CONFIG);
        } catch (AssertionError ae) {
            // AssertionException expected here from the call to select()
        }

        // verify Directory and PostgreSQL config components are invisible
        verifyNoBackendComponents();
    }
}
