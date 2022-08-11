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

import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.AUTO_INDEXING;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.BRANCH;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geogig.geoserver.config.GeoServerGeoGigRepositoryResolver;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.web.repository.RepositoryEditFormPanel;
import org.geogig.geoserver.web.repository.RepositoryImportFormPanel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.locationtech.geogig.repository.RepositoryResolver;

public class GeoGigDataStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 330172801498702374L;

    private final DropDownChoice<String> repository;

    private final BranchSelectionPanel branch;

    private final ModalWindow modalWindow;

    private final IModel<String> repositoryUriModel;

    private final IModel<String> branchNameModel;

    private final String originalRepo, originalBranch;

    private final IModel<Boolean> autoIndexingModel;
    /**
     * @param componentId the wicket component id
     * @param storeEditForm the data store edit form, as provided by {@link DataAccessEditPage} and
     *     {@link DataAccessNewPage}
     */
    @SuppressWarnings("unchecked")
    public GeoGigDataStoreEditPanel(
            final String componentId, final Form<DataStoreInfo> storeEditForm) {
        super(componentId, storeEditForm);
        final IModel<DataStoreInfo> model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, Serializable>> paramsModel =
                new PropertyModel<>(model, "connectionParameters");

        this.repositoryUriModel = new MapModel(paramsModel, REPOSITORY.key);
        this.branchNameModel = new MapModel(paramsModel, BRANCH.key);
        this.autoIndexingModel = new MapModel(paramsModel, AUTO_INDEXING.key);

        this.originalRepo = repositoryUriModel.getObject();
        this.originalBranch = branchNameModel.getObject();

        add(repository = createRepositoryPanel());
        add(importExistingLink(storeEditForm));
        add(addNewLink(storeEditForm));

        add(branch = createBranchNameComponent(storeEditForm));
        add(createAutoIndexingPanel());
        add(modalWindow = new ModalWindow("modalWindow"));
    }

    private CheckBoxParamPanel createAutoIndexingPanel() {
        CheckBoxParamPanel checkBoxParamPanel =
                new CheckBoxParamPanel(
                        "autoIndexing",
                        autoIndexingModel,
                        new ResourceModel("autoIndexing", "Auto-Indexing"));
        checkBoxParamPanel.setOutputMarkupId(true);
        return checkBoxParamPanel;
    }

    private DropDownChoice<String> createRepositoryPanel() {

        IModel<List<String>> choices = new RepositoryListDettachableModel();

        RepoInfoChoiceRenderer choiceRenderer = new RepoInfoChoiceRenderer();
        DropDownChoice<String> choice =
                new DropDownChoice<>(
                        "geogig_repository", repositoryUriModel, choices, choiceRenderer);
        choice.setLabel(new ResourceModel("repository"));
        choice.setNullValid(true);
        choice.setRequired(true);
        choice.setOutputMarkupId(true);

        choice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 6182000388125500580L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        String branchName = null;
                        // do not lose the original branch if the user is moving around the repo
                        // choices
                        if (Objects.equal(originalRepo, repositoryUriModel.getObject())) {
                            branchName = originalBranch;
                        }
                        branchNameModel.setObject(branchName);
                        branch.updateChoices(true, GeoGigDataStoreEditPanel.this.storeEditForm);
                        target.add(branch);
                    }
                });
        return choice;
    }

    private BranchSelectionPanel createBranchNameComponent(Form<DataStoreInfo> form) {

        final String panelId = "branch";
        BranchSelectionPanel selectionPanel;
        selectionPanel =
                new BranchSelectionPanel(panelId, repositoryUriModel, branchNameModel, form);
        selectionPanel.setOutputMarkupId(true);
        return selectionPanel;
    }

    private Component addNewLink(final Form<DataStoreInfo> storeEditForm) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("addNew", storeEditForm) {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                        final RepositoryEditFormPanel panel;
                        panel =
                                new RepositoryEditFormPanel(modalWindow.getContentId()) {
                                    private static final long serialVersionUID =
                                            -2629733074852452891L;

                                    @SuppressWarnings("unchecked")
                                    @Override
                                    protected void saved(
                                            final RepositoryInfo info,
                                            final AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                        updateRepository((Form<DataStoreInfo>) form, target, info);
                                    }

                                    @Override
                                    protected void cancelled(AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                    }
                                };

                        modalWindow.setContent(panel);
                        modalWindow.setTitle(
                                new ResourceModel(
                                        "GeoGigDirectoryFormComponent.chooser.browseTitle"));
                        modalWindow.show(target);
                    }
                };
        return link;
    }

    private Component importExistingLink(Form<DataStoreInfo> storeEditForm) {

        AjaxSubmitLink link =
                new AjaxSubmitLink("importExisting", storeEditForm) {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        final RepositoryImportFormPanel panel;
                        panel =
                                new RepositoryImportFormPanel(modalWindow.getContentId()) {
                                    private static final long serialVersionUID = 1L;

                                    @SuppressWarnings("unchecked")
                                    @Override
                                    protected void saved(
                                            final RepositoryInfo info,
                                            final AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                        updateRepository((Form<DataStoreInfo>) form, target, info);
                                    }

                                    @Override
                                    protected void cancelled(AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                    }
                                };

                        modalWindow.setContent(panel);
                        modalWindow.setTitle(
                                new ResourceModel(
                                        "GeoGigDirectoryFormComponent.chooser.browseTitle"));
                        modalWindow.show(target);
                    }
                };
        return link;
    }

    @SuppressWarnings("unchecked")
    private void updateRepository(
            final Form<DataStoreInfo> form, AjaxRequestTarget target, RepositoryInfo info) {
        repository.setModelObject(GeoServerGeoGigRepositoryResolver.getURI(info.getRepoName()));
        branchNameModel.setObject(null);
        branch.updateChoices(true, form);
        target.add(repository);
        target.add(branch);

        IModel<DataStoreInfo> storeModel = form.getModel();
        String dataStoreName = storeModel.getObject().getName();
        if (Strings.isNullOrEmpty(dataStoreName)) {
            Component namePanel = form.get("dataStoreNamePanel");
            if (namePanel != null && namePanel instanceof TextParamPanel) {
                TextParamPanel paramPanel = (TextParamPanel) namePanel;
                paramPanel.getFormComponent().setModelObject(info.getRepoName());
                target.add(form);
            }
        }
    }

    private static class RepositoryListDettachableModel
            extends LoadableDetachableModel<List<String>> {
        private static final long serialVersionUID = 6664339867388245896L;

        @Override
        protected List<String> load() {
            List<RepositoryInfo> all = RepositoryManager.get().getAll();
            List<String> uris = new ArrayList<>(all.size());
            for (RepositoryInfo info : all) {
                uris.add(GeoServerGeoGigRepositoryResolver.getURI(info.getRepoName()));
            }
            Collections.sort(uris);
            return uris;
        }
    }

    private static class RepoInfoChoiceRenderer extends ChoiceRenderer<String> {
        private static final long serialVersionUID = -7350304450283044479L;

        @Override
        public Object getDisplayValue(String repoUriStr) {
            try {
                URI repoUri = new URI(repoUriStr);
                RepositoryResolver resolver = RepositoryResolver.lookup(repoUri);
                RepositoryInfo info =
                        RepositoryManager.get().getByRepoName(resolver.getName(repoUri));
                return info.getRepoName() + " (" + info.getMaskedLocation() + ")";
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public String getIdValue(String id, int index) {
            return id;
        }
    }
}
