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

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.model.DropDownModel;
import org.geogig.geoserver.web.RepositoriesPage;
import org.geogig.geoserver.web.RepositoryEditPage;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.geogig.cli.test.functional.CLITestContextBuilder;
import org.locationtech.geogig.repository.impl.GlobalContextBuilder;
import org.locationtech.geogig.test.TestPlatform;

/** */
public class RepositoryEditPanelTest extends CommonPanelTest {

    private static final String FORM_PREFIX = "panel:repoForm:";

    private static final String SETTINGS_PREFIX =
            FORM_PREFIX + "repo:repositoryConfig:settingsContainer:";

    private static final String SAVE_LINK = FORM_PREFIX + "save";

    private static final String FEEDBACK = FORM_PREFIX + "feedback";

    @BeforeClass
    public static void beforeClass() {
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        GlobalContextBuilder.builder(new CLITestContextBuilder(new TestPlatform(tmp, tmp)));
    }

    @Override
    protected String getStartPage() {
        return "headerPanel:addNew";
    }

    @Override
    protected Class<? extends Page> getStartPageClass() {
        return RepositoryEditPage.class;
    }

    @Override
    protected String getFormChoiceComponent() {
        return "repo:repositoryConfig:configChoicePanel:border:border_body:paramValue";
    }

    @Override
    protected String getFrom() {
        return "panel:repoForm";
    }

    @Override
    protected String getChoicePanel() {
        return "panel:repoForm:repo:repositoryConfig:configChoicePanel";
    }

    @Test
    public void testPGAddWithEmptyFields() throws IOException {
        navigateToStartPage();
        // select PG config from the dropdown
        select(DropDownModel.PG_CONFIG);
        // verify the PG config components are visible
        verifyPostgreSQLBackendComponents();
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        tester.assertRenderedPage(getStartPageClass());
        // get the feedback panel
        FeedbackPanel c = (FeedbackPanel) tester.getComponentFromLastRenderedPage(FEEDBACK);
        List<FeedbackMessage> list = c.getFeedbackMessagesModel().getObject();
        // by default, 3 required fields will be emtpy: repo name, database and password
        List<String> expectedMsgs =
                Lists.newArrayList(
                        "Field 'Repository Name' is required.",
                        "Field 'Database Name' is required.",
                        "Field 'Password' is required.");
        assertFeedbackMessages(list, expectedMsgs);
    }

    @Test
    public void testDirectoryAddWithEmptyFields() throws IOException {
        navigateToStartPage();
        // select Directory from the dropdown
        select(DropDownModel.DIRECTORY_CONFIG);
        // verify Directory config components are visible
        verifyDirectoryBackendComponents();
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        tester.assertRenderedPage(getStartPageClass());
        // get the feedback panel
        FeedbackPanel c = (FeedbackPanel) tester.getComponentFromLastRenderedPage(FEEDBACK);
        List<FeedbackMessage> list = c.getFeedbackMessagesModel().getObject();
        // by default, repo parent directory and repo name will be empty
        List<String> expectedMsgs =
                Lists.newArrayList(
                        "Field 'Repository Name' is required.",
                        "Field 'Parent directory' is required.");
        assertFeedbackMessages(list, expectedMsgs);
    }

    @Test
    public void testAddNewRocksDBRepo() throws IOException {
        navigateToStartPage();
        // select Directory from the dropdown
        select(DropDownModel.DIRECTORY_CONFIG);
        // verify Directory config components are visible
        verifyDirectoryBackendComponents();
        // get the form
        FormTester formTester = tester.newFormTester(getFrom());
        // now set a name
        TextParamPanel repoNamePanel =
                (TextParamPanel)
                        tester.getComponentFromLastRenderedPage(
                                SETTINGS_PREFIX + "repositoryNamePanel");
        formTester.setValue(repoNamePanel.getFormComponent(), "temp_repo");
        // and a directory
        TextField parentDirectory =
                (TextField)
                        tester.getComponentFromLastRenderedPage(
                                SETTINGS_PREFIX + "parentDirectory:wrapper:wrapper_body:value");
        formTester.setValue(parentDirectory, temp.getRoot().getCanonicalPath());
        // click the Save button
        tester.executeAjaxEvent(SAVE_LINK, "click");
        // get the page. It should be a RepositoriesPage if the SAVE was successful
        tester.assertRenderedPage(RepositoriesPage.class);
    }

    @Override
    protected void verifyDirectoryBackendComponents() {
        // verify Directory config components are visible
        tester.assertVisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertInvisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(SAVE_LINK);
    }

    @Override
    protected void verifyPostgreSQLBackendComponents() {
        // verify PostgreSQL config components are visible
        tester.assertInvisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertVisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(SAVE_LINK);
    }

    @Override
    protected void verifyNoBackendComponents() {
        // verify Directory and PostgreSQL config components are invisible
        tester.assertInvisible(SETTINGS_PREFIX + "parentDirectory");
        tester.assertVisible(SETTINGS_PREFIX + "repositoryNamePanel");
        tester.assertInvisible(SETTINGS_PREFIX + "pgPanel");
        tester.assertVisible(SAVE_LINK);
    }
}
