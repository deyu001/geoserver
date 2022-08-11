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

package org.geoserver.web.data.workspace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.AccessDataRuleInfoManager;
import org.geoserver.web.security.AccessDataRulePanel;
import org.geoserver.web.security.DataAccessRuleInfo;
import org.geoserver.web.wicket.URIValidator;
import org.geoserver.web.wicket.XMLNameValidator;

/** Allows creation of a new workspace */
public class WorkspaceNewPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -4355978268880701910L;

    TextField<String> nsUriTextField;
    AccessDataRulePanel accessdataPanel;
    WsNewInfoPanel infoPanel;
    TabbedPanel<ITab> tabbedPanel;

    CompoundPropertyModel<WorkspaceInfo> model;

    public WorkspaceNewPage() {
        WorkspaceInfo ws = getCatalog().getFactory().createWorkspace();
        this.model = new CompoundPropertyModel<>(ws);
        Form form = new Form("form");
        List<ITab> tabs = new ArrayList<>();

        tabs.add(
                new AbstractTab(new Model<>("Basic Info")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        try {
                            infoPanel = new WsNewInfoPanel(panelId, model);
                            return infoPanel;
                        } catch (Exception e) {
                            throw new WicketRuntimeException(e);
                        }
                    }
                });
        if (AccessDataRuleInfoManager.canAccess()) {
            tabs.add(
                    new AbstractTab(new Model<>("Security")) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public WebMarkupContainer getPanel(String panelId) {
                            try {
                                AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
                                ListModel<DataAccessRuleInfo> ownModel =
                                        new ListModel<>(
                                                manager.getDataAccessRuleInfo(model.getObject()));
                                accessdataPanel = new AccessDataRulePanel(panelId, model, ownModel);
                                return accessdataPanel;
                            } catch (Exception e) {
                                throw new WicketRuntimeException(e);
                            }
                        }
                    });
        }

        tabbedPanel =
                new TabbedPanel<ITab>("tabs", tabs) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected WebMarkupContainer newLink(String linkId, final int index) {
                        return new SubmitLink(linkId) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onSubmit() {

                                setSelectedTab(index);
                            }
                        };
                    }
                };
        form.add(tabbedPanel);

        form.add(submitLink());
        // form.setDefaultButton(submitLink);
        form.add(cancelLink());
        add(form);
    }

    private AjaxLink<Void> cancelLink() {
        return new AjaxLink<Void>("cancel") {
            private static final long serialVersionUID = -1731475076965108576L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn(WorkspacePage.class);
            }
        };
    }

    private SubmitLink submitLink() {
        return new SubmitLink("submit") {

            private static final long serialVersionUID = -3462848930497720229L;

            @Override
            public void onSubmit() {
                handleOnSubmit();
            }
        };
    }
    /**
     * Helper method that takes care of storing the user entered workspace information and
     * associated namespace. This method makes sure that or both the workspace and namespace are
     * successfully stored or none is stored.
     */
    private void handleOnSubmit() {
        Catalog catalog = getCatalog();
        // get the workspace information from the form
        WorkspaceInfo workspace = model.getObject();
        NamespaceInfo namespace = catalog.getFactory().createNamespace();
        namespace.setPrefix(workspace.getName());
        namespace.setURI(nsUriTextField.getDefaultModelObjectAsString());
        namespace.setIsolated(workspace.isIsolated());
        // validate the workspace information adn associated namespace
        if (!validateAndReport(() -> catalog.validate(workspace, true))
                || !validateAndReport(() -> catalog.validate(namespace, true))) {
            // at least one validation fail
            return;
        }
        // store the workspace and associated namespace in the catalog
        try {
            catalog.add(workspace);
            catalog.add(namespace);
        } catch (Exception exception) {
            LOGGER.log(Level.INFO, "Error storing workspace related objects.", exception);
            cleanAndReport(exception);
        }
        // let's see if we need to tag this workspace as the default one
        if (infoPanel.defaultWs) {
            catalog.setDefaultWorkspace(workspace);
        }
        try {
            if (accessdataPanel != null) accessdataPanel.save();
            doReturn(WorkspacePage.class);
        } catch (IOException e) {
            LOGGER.log(
                    Level.INFO,
                    "Error saving access rules associated to workspace " + workspace.getName(),
                    e);
            error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    /**
     * Executes a validation and in the case of a failure reports the found errors in the provided
     * form, this method will log the found exception too.
     *
     * @param validation validation to be executed
     * @return TRUE if the validation was successful, otherwise false
     */
    private boolean validateAndReport(Supplier<ValidationResult> validation) {
        // execute the validation
        ValidationResult validationResult;
        try {
            validationResult = validation.get();
        } catch (Exception exception) {
            // the validation it self may fail, for example if the workspace already exists
            LOGGER.log(Level.INFO, "Error validating workspace related objects.", exception);
            error(exception.getMessage());
            return false;
        }
        // if the validation was not successful report the found exceptions
        if (!validationResult.isValid()) {
            String message = validationResult.getErrosAsString(System.lineSeparator());
            LOGGER.log(Level.INFO, message);
            error(message);
            return false;
        }
        // validation was successful
        return true;
    }

    /**
     * Helper method that checks in the case of an exception if both the workspace and namespace
     * where created or removed, if it is not the case then removes the remaining one. The invoker
     * is responsible to log the exception as needed.
     *
     * @param exception exception that happen
     */
    private void cleanAndReport(Exception exception) {
        Catalog catalog = getCatalog();
        WorkspaceInfo workspace = model.getObject();
        // let's see if both the workspace and associated namespace exists
        WorkspaceInfo foundWorkspace = catalog.getWorkspaceByName(workspace.getName());
        if (foundWorkspace != null) {
            NamespaceInfo foundNamespace = catalog.getNamespaceByPrefix(workspace.getName());
            if (foundNamespace == null) {
                // only the workspace was created, let's remove it
                catalog.remove(foundWorkspace);
            }
        }
        // report he exception we got
        error(exception.getMessage());
    }

    class WsNewInfoPanel extends Panel {

        private static final long serialVersionUID = 4286364808180616865L;
        boolean defaultWs;

        public WsNewInfoPanel(String id, IModel<WorkspaceInfo> model) {
            super(id, model);
            TextField<String> nameTextField = new TextField<>("name");
            nameTextField.setRequired(true);
            nameTextField.add(new XMLNameValidator());
            nameTextField.add(
                    new StringValidator() {

                        private static final long serialVersionUID = -5475431734680134780L;

                        @Override
                        public void validate(IValidatable<String> validatable) {
                            if (CatalogImpl.DEFAULT.equals(validatable.getValue())) {
                                validatable.error(
                                        new ValidationError("defaultWsError")
                                                .addKey("defaultWsError"));
                            }
                        }
                    });
            add(nameTextField.setRequired(true));

            nsUriTextField = new TextField<>("uri", new Model<>());
            // maybe a bit too restrictive, but better than not validation at all
            nsUriTextField.setRequired(true);
            nsUriTextField.add(new URIValidator());
            add(nsUriTextField);

            CheckBox defaultChk = new CheckBox("default", new PropertyModel<>(this, "defaultWs"));
            add(defaultChk);

            // add checkbox for isolated workspaces
            CheckBox isolatedChk =
                    new CheckBox("isolated", new PropertyModel<>(model.getObject(), "isolated"));
            if (!getCatalog().getCatalogCapabilities().supportsIsolatedWorkspaces()) {
                // is isolated workspaces are not supported by the current catalog disable them
                isolatedChk.setEnabled(false);
            }
            add(isolatedChk);
        }
    }
}
