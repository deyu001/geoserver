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

import static org.geogig.geoserver.config.RepositoryManager.isGeogigDirectory;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geogig.geoserver.config.ImportRepositoryFormBean;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.model.DropDownModel;
import org.geogig.geoserver.model.ImportRepositoryFormModel;
import org.geogig.geoserver.util.PostgresConnectionErrorHandler;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Wicket panel that holds the components for importing an existing GeoGig repository. This is
 * very similar to {@link RepositoryEditPanel}, however there are some subtle differences. Since
 * this panel is used for importing an existing repo, the validation checks are different. Also, the
 * {@link DirectoryChooser} is configured to allow repository directories to be selected instead of
 * just the parent directory. As such, the repository name is not separately selected. The
 * differences also called for a slightly different way of wrapping the {@link RepositoryInfo} data
 * bean with a different data model, {@link ImportRepositoryFormModel} and {@link
 * ImportRepositoryFormBean}.
 */
public class RepositoryImportPanel extends FormComponentPanel<RepositoryInfo> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryImportPanel.class);

    private final TextParamPanel repoNamePanel;
    private final DropDownChoiceParamPanel dropdownPanel;
    private final RepoDirectoryComponent repoDirectoryComponent;
    private final PostgresConfigFormPanel pgPanel;
    private final WebMarkupContainer settingsPanel;

    public RepositoryImportPanel(String id, IModel<RepositoryInfo> model) {
        super(id, model);

        setOutputMarkupId(true);

        // build the backing form model
        ImportRepositoryFormModel formModel = new ImportRepositoryFormModel(model);
        // add the dropdown to switch between configurations
        dropdownPanel =
                new DropDownChoiceParamPanel(
                        "configChoicePanel",
                        new DropDownModel(model),
                        new ResourceModel(
                                "RepositoryImportPanel.repositoryType", "Repository Type"),
                        DropDownModel.CONFIG_LIST,
                        true);
        final DropDownChoice<Serializable> dropDownChoice = dropdownPanel.getFormComponent();
        dropDownChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        final String value = dropDownChoice.getModelObject().toString();
                        repoDirectoryComponent.setVisible(
                                DropDownModel.DIRECTORY_CONFIG.equals(value));
                        pgPanel.setVisible(DropDownModel.PG_CONFIG.equals(value));
                        repoNamePanel.setVisible(DropDownModel.PG_CONFIG.equals(value));
                        target.add(settingsPanel);
                    }
                });
        add(dropdownPanel);

        settingsPanel = new WebMarkupContainer("settingsContainer");
        settingsPanel.setOutputMarkupId(true);
        add(settingsPanel);

        repoNamePanel =
                new TextParamPanel(
                        "repositoryNamePanel",
                        new PropertyModel(formModel, "repoName"),
                        new ResourceModel(
                                "RepositoryImportPanel.repositoryName", "Repository Name"),
                        true);
        repoNamePanel.setOutputMarkupId(true);
        repoNamePanel.getFormComponent().setOutputMarkupId(true);
        repoNamePanel.setVisible(
                DropDownModel.PG_CONFIG.equals(dropDownChoice.getModelObject().toString()));
        settingsPanel.add(repoNamePanel);

        pgPanel = new PostgresConfigFormPanel("pgPanel", new PropertyModel<>(formModel, "pgBean"));
        pgPanel.setVisible(
                DropDownModel.PG_CONFIG.equals(dropDownChoice.getModelObject().toString()));
        settingsPanel.add(pgPanel);

        repoDirectoryComponent = new RepoDirectoryComponent("repoDirectoryPanel", formModel);
        repoDirectoryComponent.setOutputMarkupId(true);
        repoDirectoryComponent.setVisible(
                DropDownModel.DIRECTORY_CONFIG.equals(dropDownChoice.getModelObject().toString()));
        settingsPanel.add(repoDirectoryComponent);

        add(
                new IValidator<RepositoryInfo>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void validate(IValidatable<RepositoryInfo> validatable) {
                        ValidationError error = new ValidationError();
                        RepositoryInfo repo = validatable.getValue();
                        // block duplicate names first
                        if (RepositoryManager.get().repoExistsByName(repo.getRepoName())) {
                            error.addKey("errRepositoryNameExists");
                            validatable.error(error);
                            return;
                        }
                        final URI location = repo.getLocation();
                        // look for already configured repos
                        if (RepositoryManager.get().repoExistsByLocation(location)) {
                            error.addKey("errRepositoryAlreadyConfigured");
                            return;
                        }
                        if (error.getKeys().isEmpty()) {
                            final RepositoryResolver resolver = RepositoryResolver.lookup(location);
                            final String scheme = location.getScheme();
                            if ("file".equals(scheme)) {
                                File repoDir = new File(location);
                                if (!repoDir.exists() || !repoDir.isDirectory()) {
                                    error.addKey("errRepositoryDirectoryDoesntExist");
                                }
                                if (!isGeogigDirectory(repoDir)) {
                                    error.addKey("notAGeogigRepository");
                                }
                            } else if ("postgresql".equals(scheme)) {
                                try {
                                    if (!resolver.repoExists(location)) {
                                        error.addKey("errRepositoryDoesntExist");
                                    }
                                } catch (Exception ex) {
                                    // likely failed to connect
                                    LOGGER.error("Failed to connect to PostgreSQL database", ex);
                                    error.addKey("errCannotConnectToDatabase");
                                    // find root cause
                                    error.setVariable(
                                            "message",
                                            PostgresConnectionErrorHandler.getMessage(ex));
                                }
                            }
                        }
                        if (!error.getKeys().isEmpty()) {
                            validatable.error(error);
                        }
                    }
                });
    }

    @Override
    public void convertInput() {
        RepositoryInfo repoInfo = getModelObject();
        final String repoTypeChoice =
                dropdownPanel.getFormComponent().getConvertedInput().toString();
        if (null != repoTypeChoice) {
            switch (repoTypeChoice) {
                case DropDownModel.PG_CONFIG:
                    // PG config used
                    PostgresConfigBean bean = pgPanel.getConvertedInput();
                    // build a URI out of the config
                    URI uri =
                            bean.buildUriForRepo(
                                    repoNamePanel
                                            .getFormComponent()
                                            .getConvertedInput()
                                            .toString()
                                            .trim());
                    repoInfo.setLocation(uri);
                    break;
                case DropDownModel.DIRECTORY_CONFIG:
                    // local directory used
                    ImportRepositoryFormBean formBean = repoDirectoryComponent.getConvertedInput();
                    Path uriPath = Paths.get(formBean.getRepoDirectory());
                    repoInfo.setLocation(uriPath.toUri());
                    break;
                default:
                    throw new IllegalStateException(
                            String.format(
                                    "Unknown repositry type '%s', expected one of %s, %s",
                                    repoTypeChoice,
                                    DropDownModel.PG_CONFIG,
                                    DropDownModel.DIRECTORY_CONFIG));
            }
        }
        setConvertedInput(repoInfo);
    }

    static class RepoDirectoryComponent extends FormComponentPanel<ImportRepositoryFormBean> {

        private static final long serialVersionUID = 1L;

        private final TextField<String> repoDirectoryField;
        private final ModalWindow dialog;

        RepoDirectoryComponent(String id, IModel<ImportRepositoryFormBean> model) {
            super(id, model);

            dialog = new ModalWindow("dialog");
            add(dialog);

            repoDirectoryField =
                    new TextField<>(
                            "repoDirectory", new PropertyModel<String>(model, "repoDirectory"));
            repoDirectoryField.setRequired(true);
            repoDirectoryField.setOutputMarkupId(true);

            IModel<String> labelModel =
                    new ResourceModel(
                            "RepositoryImportPanel.directoryLabel", "Repository Directory") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public String getObject() {
                            String value = super.getObject();
                            return value + " *";
                        }
                    };

            Label directoryLabel = new Label("repoLabel", labelModel.getObject());
            add(directoryLabel);
            repoDirectoryField.setLabel(labelModel);

            FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("wrapper");
            feedback.add(repoDirectoryField);
            feedback.add(createChooserButton());
            add(feedback);
        }

        @Override
        public void convertInput() {
            ImportRepositoryFormBean bean = new ImportRepositoryFormBean();
            String repoDir = repoDirectoryField.getConvertedInput();
            bean.setRepoDirectory(repoDir);
            setConvertedInput(bean);
        }

        private Component createChooserButton() {
            return new AjaxSubmitLink("chooser") {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean getDefaultFormProcessing() {
                    return false;
                }

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    File repoDirFile = null;
                    repoDirectoryField.processInput();
                    String repoDir = repoDirectoryField.getConvertedInput();
                    if (repoDir != null && !repoDir.trim().isEmpty()) {
                        repoDirFile = new File(repoDir);
                    }
                    DirectoryChooser chooser =
                            new DirectoryChooser(
                                    dialog.getContentId(), new Model<>(repoDirFile), true) {

                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void geogigDirectoryClicked(
                                        File file, AjaxRequestTarget target) {
                                    repoDirectoryField.clearInput();
                                    repoDirectoryField.setModelObject(file.getAbsolutePath());
                                    target.add(repoDirectoryField);
                                    dialog.close(target);
                                }

                                @Override
                                protected void directorySelected(
                                        File file, AjaxRequestTarget target) {
                                    repoDirectoryField.clearInput();
                                    repoDirectoryField.setModelObject(file.getAbsolutePath());
                                    target.add(repoDirectoryField);
                                    dialog.close(target);
                                }
                            };
                    chooser.setFileTableHeight(null);
                    dialog.setContent(chooser);
                    dialog.setTitle(
                            new ResourceModel(
                                    "GeoGigDirectoryFormComponent.chooser.chooseParentTile"));
                    dialog.show(target);
                }
            };
        }
    }
}
