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

package org.geoserver.taskmanager.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.BatchElementsModel;
import org.geoserver.taskmanager.web.panel.DropDownPanel;
import org.geoserver.taskmanager.web.panel.FrequencyPanel;
import org.geoserver.taskmanager.web.panel.PositionPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.UnauthorizedPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;
import org.hibernate.exception.ConstraintViolationException;

public class BatchPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = -5111795911981486778L;

    private static final Logger LOGGER = Logging.getLogger(BatchPage.class);

    private IModel<Batch> batchModel;

    private List<BatchElement> oldElements;

    private Set<BatchElement> removedElements = new HashSet<BatchElement>();

    private Set<Task> addedTasks = new HashSet<Task>();

    private GeoServerDialog dialog;

    private AjaxSubmitLink remove;

    private GeoServerTablePanel<BatchElement> elementsPanel;

    public BatchPage(IModel<Batch> batchModel, Page parentPage) {
        if (batchModel.getObject().getId() != null
                && !TaskManagerBeans.get()
                        .getSecUtil()
                        .isReadable(getSession().getAuthentication(), batchModel.getObject())) {
            throw new RestartResponseException(UnauthorizedPage.class);
        }
        this.batchModel = batchModel;
        oldElements = new ArrayList<>(batchModel.getObject().getElements());
        setReturnPage(parentPage);
    }

    public BatchPage(Batch batch, Page parentPage) {
        this(new Model<Batch>(batch), parentPage);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(dialog = new GeoServerDialog("dialog"));

        add(
                new WebMarkupContainer("notvalidated")
                        .setVisible(
                                batchModel.getObject().getConfiguration() != null
                                        && !batchModel.getObject().getConfiguration().isTemplate()
                                        && !batchModel
                                                .getObject()
                                                .getConfiguration()
                                                .isValidated()));

        Form<Batch> form = new Form<Batch>("batchForm", batchModel);
        add(form);

        AjaxSubmitLink saveButton = saveOrApplyButton("save", true);
        form.add(saveButton);
        AjaxSubmitLink applyButton = saveOrApplyButton("apply", false);
        form.add(applyButton);

        form.add(
                new TextField<String>("name", new PropertyModel<String>(batchModel, "name")) {
                    private static final long serialVersionUID = -3736209422699508894L;

                    @Override
                    public boolean isRequired() {
                        return form.findSubmittingButton() == saveButton
                                || form.findSubmittingButton() == applyButton;
                    }
                });

        SortedSet<String> workspaces = new TreeSet<String>();
        for (WorkspaceInfo wi : GeoServerApplication.get().getCatalog().getWorkspaces()) {
            if (wi.getName().equals(batchModel.getObject().getWorkspace())
                    || TaskManagerBeans.get()
                            .getSecUtil()
                            .isAdminable(getSession().getAuthentication(), wi)) {
                workspaces.add(wi.getName());
            }
        }
        boolean canBeNull =
                (GeoServerApplication.get().getCatalog().getDefaultWorkspace() != null
                        && TaskManagerBeans.get()
                                .getSecUtil()
                                .isAdminable(
                                        getSession().getAuthentication(),
                                        GeoServerApplication.get()
                                                .getCatalog()
                                                .getDefaultWorkspace()));
        form.add(
                new DropDownChoice<String>(
                        "workspace",
                        new PropertyModel<String>(batchModel, "workspace"),
                        new ArrayList<String>(workspaces)) {

                    private static final long serialVersionUID = -9058423608027219299L;

                    @Override
                    public boolean isRequired() {
                        return !canBeNull
                                && (form.findSubmittingButton() == saveButton
                                        || form.findSubmittingButton() == applyButton);
                    }
                }.setNullValid(canBeNull)
                        // theoretically a batch can have a separate workspace from config, but it
                        // is
                        // confusing to users so turning this off by default.
                        .setEnabled(
                                batchModel.getObject().getConfiguration() == null
                                        || batchModel.getObject().getWorkspace() != null));

        form.add(
                new TextField<String>(
                        "description", new PropertyModel<String>(batchModel, "description")));

        form.add(
                new TextField<String>(
                                "configuration",
                                new Model<String>(
                                        batchModel.getObject().getConfiguration() == null
                                                ? ""
                                                : batchModel
                                                        .getObject()
                                                        .getConfiguration()
                                                        .getName()))
                        .setEnabled(false));

        form.add(
                new FrequencyPanel(
                        "frequency", new PropertyModel<String>(batchModel, "frequency")));

        form.add(new CheckBox("enabled", new PropertyModel<Boolean>(batchModel, "enabled")));

        form.add(addButton());

        // the removal button
        form.add(remove = removeButton());
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        // the tasks panel
        form.add(elementsPanel = elementsPanel());
        elementsPanel.setFilterVisible(false);
        elementsPanel.setPageable(false);
        elementsPanel.setSortable(false);
        elementsPanel.setOutputMarkupId(true);

        form.add(
                new AjaxLink<Object>("cancel") {
                    private static final long serialVersionUID = -6892944747517089296L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        // restore elements
                        batchModel.getObject().getElements().clear();
                        batchModel.getObject().getElements().addAll(oldElements);
                        doReturn();
                    }
                });

        if (batchModel.getObject().getId() != null
                && !TaskManagerBeans.get()
                        .getSecUtil()
                        .isAdminable(getSession().getAuthentication(), batchModel.getObject())) {
            form.get("name").setEnabled(false);
            form.get("workspace").setEnabled(false);
            form.get("description").setEnabled(false);
            form.get("configuration").setEnabled(false);
            form.get("frequency").setEnabled(false);
            form.get("enabled").setEnabled(false);
            form.get("addNew").setEnabled(false);
            remove.setEnabled(false);
            saveButton.setEnabled(false);
            elementsPanel.setEnabled(false);
        }
    }

    protected AjaxSubmitLink saveOrApplyButton(final String id, final boolean doReturn) {
        return new AjaxSubmitLink(id) {
            private static final long serialVersionUID = 3735176778941168701L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    Configuration config = batchModel.getObject().getConfiguration();
                    batchModel.setObject(
                            TaskManagerBeans.get()
                                    .getDataUtil()
                                    .saveScheduleAndRemove(
                                            batchModel.getObject(), removedElements));
                    // update the old config (still used on configuration page)
                    if (config != null) {
                        batchModel.getObject().setConfiguration(config);
                        config.getBatches()
                                .put(batchModel.getObject().getName(), batchModel.getObject());
                    }
                    if (doReturn) {
                        doReturn();
                    } else {
                        form.success(new ParamResourceModel("success", getPage()).getString());
                    }
                } catch (Exception e) {
                    if (e.getCause() instanceof ConstraintViolationException) {
                        form.error(new ParamResourceModel("duplicate", getPage()).getString());
                    } else {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                        Throwable rootCause = ExceptionUtils.getRootCause(e);
                        form.error(
                                rootCause == null
                                        ? e.getLocalizedMessage()
                                        : rootCause.getLocalizedMessage());
                    }
                }
                addFeedbackPanels(target);
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                addFeedbackPanels(target);
            }
        };
    }

    protected AjaxSubmitLink addButton() {
        return new AjaxSubmitLink("addNew") {

            private static final long serialVersionUID = 7320342263365531859L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                dialog.setTitle(new ParamResourceModel("newTaskDialog.title", getPage()));
                dialog.setInitialWidth(600);
                dialog.setInitialHeight(100);
                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {

                            private static final long serialVersionUID = 7410393012930249966L;

                            private DropDownPanel panel;
                            private Map<String, Task> tasks;

                            @Override
                            protected Component getContents(String id) {
                                tasks = new TreeMap<String, Task>();
                                for (Task task :
                                        TaskManagerBeans.get()
                                                .getDao()
                                                .getTasksAvailableForBatch(
                                                        batchModel.getObject())) {
                                    if (batchModel.getObject().getConfiguration() != null
                                            && !batchModel
                                                    .getObject()
                                                    .getConfiguration()
                                                    .getTasks()
                                                    .containsKey(task.getName())) {
                                        // deleted in config
                                        continue;
                                    }
                                    if (!addedTasks.contains(task)
                                            && TaskManagerBeans.get()
                                                    .getSecUtil()
                                                    .isWritable(
                                                            BatchPage.this
                                                                    .getSession()
                                                                    .getAuthentication(),
                                                            task.getConfiguration())) {
                                        tasks.put(task.getFullName(), task);
                                    }
                                }
                                for (BatchElement be : removedElements) {
                                    tasks.put(be.getTask().getFullName(), be.getTask());
                                }
                                panel =
                                        new DropDownPanel(
                                                id,
                                                new Model<String>(),
                                                new Model<ArrayList<String>>(
                                                        new ArrayList<String>(tasks.keySet())),
                                                new ParamResourceModel(
                                                        "newTaskDialog.content", getPage()));
                                panel.getDropDownChoice().setNullValid(false).setRequired(true);
                                return panel;
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                Task task = tasks.get(panel.getDropDownChoice().getModelObject());
                                BatchElement be =
                                        TaskManagerBeans.get()
                                                .getDataUtil()
                                                .addBatchElement(batchModel.getObject(), task);
                                if (!removedElements.remove(be)) {
                                    addedTasks.add(task);
                                }

                                // bit of a hack - updates the selected array inside the panel
                                // with the new count
                                elementsPanel.setPageable(false);

                                target.add(elementsPanel);
                                return true;
                            }
                        });
            }
        };
    }

    protected AjaxSubmitLink removeButton() {
        return new AjaxSubmitLink("removeSelected") {
            private static final long serialVersionUID = 3581476968062788921L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                dialog.setTitle(new ParamResourceModel("confirmDeleteDialog.title", getPage()));
                dialog.setInitialWidth(600);
                dialog.setInitialHeight(100);
                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {

                            private static final long serialVersionUID = -5552087037163833563L;

                            @Override
                            protected Component getContents(String id) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(
                                        new ParamResourceModel(
                                                        "confirmDeleteDialog.content", getPage())
                                                .getString());
                                for (BatchElement be : elementsPanel.getSelection()) {
                                    sb.append("\n&nbsp;&nbsp;");
                                    sb.append(
                                            StringEscapeUtils.escapeHtml4(
                                                    be.getTask().getFullName()));
                                }
                                return new MultiLineLabel(id, sb.toString())
                                        .setEscapeModelStrings(false);
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                batchModel
                                        .getObject()
                                        .getElements()
                                        .removeAll(elementsPanel.getSelection());
                                for (BatchElement be : elementsPanel.getSelection()) {
                                    if (!addedTasks.remove(be.getTask())) {
                                        removedElements.add(be);
                                    }
                                }
                                remove.setEnabled(false);
                                target.add(elementsPanel);
                                target.add(remove);
                                return true;
                            }
                        });
            }
        };
    }

    protected GeoServerTablePanel<BatchElement> elementsPanel() {
        return new GeoServerTablePanel<BatchElement>(
                "tasksPanel", new BatchElementsModel(batchModel), true) {

            private static final long serialVersionUID = -8943273843044917552L;

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(elementsPanel.getSelection().size() > 0);
                target.add(remove);
            }

            @Override
            protected Component getComponentForProperty(
                    String id, IModel<BatchElement> itemModel, Property<BatchElement> property) {
                if (property.equals(BatchElementsModel.INDEX)) {
                    return new PositionPanel(id, itemModel, this);
                }
                return null;
            }
        };
    }

    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
