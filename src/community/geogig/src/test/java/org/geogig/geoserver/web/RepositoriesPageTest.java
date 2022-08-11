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

package org.geogig.geoserver.web;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.model.DropDownModel;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.junit.Test;

/** */
public class RepositoriesPageTest extends GeoServerWicketTestSupport {

    private RepositoriesPage repoPage;

    private void navigateToRepositoriesPage() {
        login();
        repoPage = new RepositoriesPage();
        tester.startPage(repoPage);
    }

    @Test
    public void testAddNewPanel() {
        navigateToRepositoriesPage();
        // click the Add New link in the header panel
        tester.clickLink("headerPanel:addNew");
        // verify the page is a RepositoriesEditPage
        tester.assertRenderedPage(RepositoryEditPage.class);
        // verify the type dropdown
        DropDownChoiceParamPanel panel =
                (DropDownChoiceParamPanel)
                        tester.getComponentFromLastRenderedPage(
                                "panel:repoForm:repo:repositoryConfig:configChoicePanel");
        DropDownChoice<Serializable> choice = panel.getFormComponent();
        // make sure Directory is selected
        assertEquals(
                "Expected DropDwon to be set to " + DropDownModel.DIRECTORY_CONFIG,
                DropDownModel.DIRECTORY_CONFIG,
                choice.getModelObject());
        // verify that Directory components are visible
        final String settings = "panel:repoForm:repo:repositoryConfig:settingsContainer:";
        tester.assertVisible(settings + "repositoryNamePanel");
        tester.assertVisible(settings + "parentDirectory");
        tester.assertInvisible(settings + "pgPanel");
        // now select PG config
        FormTester formTester = tester.newFormTester("panel:repoForm");
        formTester.select(
                "repo:repositoryConfig:configChoicePanel:border:border_body:paramValue",
                choice.getChoices().indexOf(DropDownModel.PG_CONFIG));
        tester.executeAjaxEvent(choice, "change");
        // verify the Directory components go away and the PG config is visible
        tester.assertVisible(settings + "repositoryNamePanel");
        tester.assertInvisible(settings + "parentDirectory");
        tester.assertVisible(settings + "pgPanel");
    }

    @Test
    public void testImportPanel() {
        navigateToRepositoriesPage();
        tester.clickLink("headerPanel:importExisting");
        // verify the page is a RepositoryImportPage
        tester.assertRenderedPage(RepositoryImportPage.class);
        // verify the type dropdown
        DropDownChoiceParamPanel panel =
                (DropDownChoiceParamPanel)
                        tester.getComponentFromLastRenderedPage(
                                "panel:repoForm:repo:configChoicePanel");
        DropDownChoice<Serializable> choice = panel.getFormComponent();
        // verify Directory is selected
        assertEquals(
                "Expected DropDwon to be set to " + DropDownModel.DIRECTORY_CONFIG,
                DropDownModel.DIRECTORY_CONFIG,
                choice.getModelObject());
        // verify Parent Directory component
        final String settings = "panel:repoForm:repo:settingsContainer:";
        tester.assertVisible(settings + "repoDirectoryPanel");
        tester.assertInvisible(settings + "repositoryNamePanel");
        tester.assertInvisible(settings + "pgPanel");
        // now select PG config
        FormTester formTester = tester.newFormTester("panel:repoForm");
        formTester.select(
                "repo:configChoicePanel:border:border_body:paramValue",
                choice.getChoices().indexOf(DropDownModel.PG_CONFIG));
        tester.executeAjaxEvent(choice, "change");
        // verify PG config
        tester.assertInvisible(settings + "repoDirectoryPanel");
        tester.assertVisible(settings + "repositoryNamePanel");
        tester.assertVisible(settings + "pgPanel");
    }
}
