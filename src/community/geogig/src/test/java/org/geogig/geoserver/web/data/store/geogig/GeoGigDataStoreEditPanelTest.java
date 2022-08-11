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

package org.geogig.geoserver.web.data.store.geogig;

import static org.geogig.geoserver.config.GeoServerGeoGigRepositoryResolver.getURI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.locationtech.geogig.model.impl.RevObjectTestSupport.hashString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Suppliers;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.StoreExtensionPoints;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;

public class GeoGigDataStoreEditPanelTest extends GeoServerWicketTestSupport {

    private static final String BASE = "dataStoreForm:parametersPanel";

    private Page page;

    private DataStoreInfo storeInfo;

    private Form<DataStoreInfo> editForm;

    private RepositoryManager mockManager;

    @Before
    public void beforeTest() {
        mockManager = mock(RepositoryManager.class);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        //
    }

    private <T extends Component> T getComponent(String path, Class<T> clazz) {
        return clazz.cast(tester.getComponentFromLastRenderedPage(path));
    }

    private GeoGigDataStoreEditPanel startPanelForNewStore() {
        login();
        page = new DataAccessNewPage(GeoGigDataStoreFactory.DISPLAY_NAME);
        tester.startPage(page);

        editForm = getComponent("dataStoreForm", Form.class);

        editForm.getModelObject()
                .getConnectionParameters()
                .put(GeoGigDataStoreFactory.REPOSITORY.key, null);
        GeoGigDataStoreEditPanel panel = getComponent(BASE, GeoGigDataStoreEditPanel.class);

        return panel;
    }

    private GeoGigDataStoreEditPanel startPanelToEditStore() {
        final Catalog catalog = getCatalog();
        storeInfo = catalog.getFactory().createDataStore();
        storeInfo.setDescription("dummy geogig store");
        storeInfo.setEnabled(true);
        storeInfo.setName("dummy_geogig");
        storeInfo.setType(GeoGigDataStoreFactory.DISPLAY_NAME);
        storeInfo.setWorkspace(catalog.getDefaultWorkspace());
        storeInfo.getConnectionParameters().put(GeoGigDataStoreFactory.BRANCH.key, "alpha");
        storeInfo.getConnectionParameters().put(GeoGigDataStoreFactory.REPOSITORY.key, null);
        catalog.save(storeInfo);
        final String storeId = storeInfo.getId();
        login();
        page = new DataAccessEditPage(storeId);
        tester.startPage(page);
        editForm = getComponent("dataStoreForm", Form.class);
        GeoGigDataStoreEditPanel panel = getComponent(BASE, GeoGigDataStoreEditPanel.class);
        return panel;
    }

    private void assertCommonComponents() {
        tester.assertComponent(BASE + ":geogig_repository", DropDownChoice.class);
        tester.assertComponent(BASE + ":branch", BranchSelectionPanel.class);
        tester.assertComponent(BASE + ":autoIndexing", CheckBoxParamPanel.class);
    }

    @Test
    public void testExtensionPoint() {
        storeInfo = getCatalog().getFactory().createDataStore();
        storeInfo.setType(GeoGigDataStoreFactory.DISPLAY_NAME);
        editForm = new Form("formid");
        editForm.setModel(new Model(storeInfo));
        GeoServerApplication app = getGeoServerApplication();
        StoreEditPanel storeEditPanel =
                StoreExtensionPoints.getStoreEditPanel("id", editForm, storeInfo, app);
        assertNotNull(storeEditPanel);
        assertTrue(storeEditPanel instanceof GeoGigDataStoreEditPanel);
    }

    @Test
    public void testStartupForNew() {
        startPanelForNewStore();
        assertCommonComponents();
    }

    @Test
    public void testStartupForEdit() {
        startPanelToEditStore();
        assertCommonComponents();
        tester.assertModelValue(BASE + ":branch:branchDropDown", "alpha");
    }

    @Test
    public void testRefreshBranchListWithBadConnectionParams() throws Exception {
        startPanelForNewStore();
        editForm.getModelObject()
                .getConnectionParameters()
                .put(GeoGigDataStoreFactory.REPOSITORY.key, getURI("dummy_repo_2"));

        final FormTester formTester = tester.newFormTester("dataStoreForm");
        BranchSelectionPanel branchPanel =
                getComponent(BASE + ":branch", BranchSelectionPanel.class);
        RepositoryInfo repoInfo = mock(RepositoryInfo.class);
        when(repoInfo.getId()).thenReturn(UUID.randomUUID().toString());
        when(mockManager.getByRepoName("dummy_repo_2")).thenReturn(repoInfo);
        when(mockManager.listBranches(anyString())).thenThrow(new IOException("Could not connect"));
        branchPanel.setRepositoryManager(Suppliers.ofInstance(mockManager));
        String submitLink = BASE + ":branch:refresh";
        tester.executeAjaxEvent(submitLink, "click");
        FeedbackMessage feedbackMessage = formTester.getForm().getFeedbackMessages().first();
        assertNotNull(feedbackMessage);
        Serializable message = feedbackMessage.getMessage();
        assertNotNull(message);
        String expectedMessage = "Could not list branches: Could not connect";
        assertEquals(expectedMessage, message.toString());
    }

    @Test
    public void testRefreshBranchList() throws Exception {
        startPanelForNewStore();
        editForm.getModelObject()
                .getConnectionParameters()
                .put(GeoGigDataStoreFactory.REPOSITORY.key, getURI("dummy_repo_3"));

        final FormTester formTester = tester.newFormTester("dataStoreForm");
        BranchSelectionPanel branchPanel =
                getComponent(BASE + ":branch", BranchSelectionPanel.class);
        assertNotNull(branchPanel);
        ObjectId dummyId = hashString("dummy");
        final List<Ref> branches =
                Arrays.asList(
                        new Ref("master", dummyId),
                        new Ref("alpha", dummyId),
                        new Ref("sandbox", dummyId));
        RepositoryInfo repoInfo = mock(RepositoryInfo.class);
        when(repoInfo.getId()).thenReturn(UUID.randomUUID().toString());

        branchPanel.setRepositoryManager(Suppliers.ofInstance(mockManager));
        when(mockManager.getByRepoName("dummy_repo_3")).thenReturn(repoInfo);
        when(mockManager.listBranches(anyString())).thenReturn(branches);
        String dropDownPath = BASE + ":branch:branchDropDown";
        final DropDownChoice choice = getComponent(dropDownPath, DropDownChoice.class);

        assertTrue(choice.getChoices().isEmpty());
        String submitLink = BASE + ":branch:refresh";
        tester.executeAjaxEvent(submitLink, "click");
        FeedbackMessage feedbackMessage = formTester.getForm().getFeedbackMessages().first();
        assertNull(feedbackMessage);
        assertEquals(Arrays.asList("master", "alpha", "sandbox"), choice.getChoices());
    }
}
