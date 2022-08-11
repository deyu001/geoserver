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

package org.geoserver.web.data.layergroup;

import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.resource.MetadataLinkEditor;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.publish.PublishedConfigurationPage;
import org.geoserver.web.publish.PublishedEditTabPanel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Handles layer group */
public class LayerGroupEditPage extends PublishedConfigurationPage<LayerGroupInfo> {

    private static final long serialVersionUID = 5659874305843575438L;

    public static final String GROUP = "group";
    public static final String WORKSPACE = "workspace";

    LayerGroupEntryPanel lgEntryPanel;
    private CheckBox queryableCheckBox;

    protected LayerGroupEditPage(boolean isNew) {
        super(isNew);
        this.returnPageClass = LayerGroupPage.class;
    }

    public LayerGroupEditPage() {
        this(true);
        setupPublished(getCatalog().getFactory().createLayerGroup());
        postInit();
    }

    public LayerGroupEditPage(PageParameters parameters) {
        this(false);

        String groupName = parameters.get(GROUP).toString();
        String wsName = parameters.get(WORKSPACE).toOptionalString();

        LayerGroupInfo lg =
                wsName != null
                        ? getCatalog().getLayerGroupByName(wsName, groupName)
                        : getCatalog().getLayerGroupByName(groupName);

        if (lg == null) {
            error(
                    new ParamResourceModel("LayerGroupEditPage.notFound", this, groupName)
                            .getString());
            doReturn(LayerGroupPage.class);
            return;
        }

        setupPublished(lg);
        postInit();
    }

    private void postInit() {
        if (!isAuthenticatedAsAdmin() && !isNew) {
            // global layer groups only editable by full admin
            if (getPublishedInfo().getWorkspace() == null) {
                // disable all form components but cancel
                setInputEnabled(false);

                info(new StringResourceModel("globalLayerGroupReadOnly", this, null).getString());
            }
        }
    }

    public class LayerGroupTab extends PublishedEditTabPanel<LayerGroupInfo> {

        private static final long serialVersionUID = 2192005814142588155L;

        public LayerGroupTab(String id) {
            super(id, myModel);
            initUI();
        }

        private EnvelopePanel envelopePanel;
        protected RootLayerEntryPanel rootLayerPanel;

        @SuppressWarnings("serial")
        private void initUI() {

            final WebMarkupContainer rootLayerPanelContainer =
                    new WebMarkupContainer("rootLayerContainer");
            rootLayerPanelContainer.setOutputMarkupId(true);
            add(rootLayerPanelContainer);

            rootLayerPanel =
                    new RootLayerEntryPanel(
                            "rootLayer", getPublishedInfo().getWorkspace(), myModel);
            rootLayerPanelContainer.add(rootLayerPanel);

            updateRootLayerPanel(getPublishedInfo().getMode());

            TextField<String> name = new TextField<>("name");
            name.setRequired(true);
            // JD: don't need this, this is validated at the catalog level
            // name.add(new GroupNameValidator());
            add(name);
            add(new CheckBox("enabled"));
            add(new CheckBox("advertised"));
            final DropDownChoice<LayerGroupInfo.Mode> modeChoice =
                    new DropDownChoice<>(
                            "mode", new LayerGroupModeModel(), new LayerGroupModeChoiceRenderer());
            modeChoice.setNullValid(false);
            modeChoice.setRequired(true);
            modeChoice.add(
                    new OnChangeAjaxBehavior() {
                        private static final long serialVersionUID = 8819356789334465887L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            LayerGroupInfo.Mode mode = modeChoice.getModelObject();
                            updateRootLayerPanel(mode);
                            target.add(rootLayerPanelContainer);
                        }
                    });

            add(modeChoice);

            queryableCheckBox =
                    new CheckBox("queryable", new Model<>(!getPublishedInfo().isQueryDisabled()));
            add(queryableCheckBox);

            add(new TextField<String>("title"));
            add(new TextArea<String>("abstract"));

            DropDownChoice<WorkspaceInfo> wsChoice =
                    new DropDownChoice<>(
                            "workspace", new WorkspacesModel(), new WorkspaceChoiceRenderer());
            wsChoice.setNullValid(true);
            wsChoice.add(
                    new OnChangeAjaxBehavior() {

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            // nothing to do really, just wanted to get the state back on the server
                            // side
                            // for the chooser dialogs to use
                        }
                    });
            if (!isAuthenticatedAsAdmin()) {
                wsChoice.setNullValid(false);
                wsChoice.setRequired(true);
            }

            add(wsChoice);

            // bounding box
            add(envelopePanel = new EnvelopePanel("bounds") /*.setReadOnly(true)*/);
            envelopePanel.setRequired(true);
            envelopePanel.setCRSFieldVisible(true);
            envelopePanel.setCrsRequired(true);
            envelopePanel.setOutputMarkupId(true);

            add(
                    new GeoServerAjaxFormLink("generateBounds") {
                        private static final long serialVersionUID = -5290731459036222837L;

                        @Override
                        public void onClick(AjaxRequestTarget target, Form<?> form) {
                            // build a layer group with the current contents of the group
                            LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
                            for (LayerGroupEntry entry : lgEntryPanel.getEntries()) {
                                lg.getLayers().add(entry.getLayer());
                                lg.getStyles().add(entry.getStyle());
                            }

                            try {
                                // grab the eventually manually inserted
                                CoordinateReferenceSystem crs =
                                        envelopePanel.getCoordinateReferenceSystem();

                                if (crs != null) {
                                    // ensure the bounds calculated in terms of the user specified
                                    // crs
                                    new CatalogBuilder(getCatalog())
                                            .calculateLayerGroupBounds(lg, crs);
                                } else {
                                    // calculate from scratch
                                    new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg);
                                }

                                envelopePanel.setModelObject(lg.getBounds());
                                target.add(envelopePanel);

                            } catch (Exception e) {
                                throw new WicketRuntimeException(e);
                            }
                        }
                    });

            add(
                    new GeoServerAjaxFormLink("generateBoundsFromCRS") {

                        private static final long serialVersionUID = -7907583302556368270L;

                        @Override
                        protected void onClick(AjaxRequestTarget target, Form<?> form) {
                            LOGGER.log(Level.FINE, "Computing bounds for LG based off CRS");
                            LayerGroupInfo lg = getPublishedInfo();
                            CoordinateReferenceSystem crs =
                                    envelopePanel.getCoordinateReferenceSystem();
                            new CatalogBuilder(getCatalog())
                                    .calculateLayerGroupBoundsFromCRS(lg, crs);

                            envelopePanel.modelChanged();
                            target.add(envelopePanel);
                        }
                    });

            add(
                    lgEntryPanel =
                            new LayerGroupEntryPanel(
                                    "layers", getPublishedInfo(), wsChoice.getModel()));

            add(new MetadataLinkEditor("metadataLinks", myModel));

            // add keywords editor
            add(
                    new KeywordsEditor(
                            "keywords",
                            LiveCollectionModel.list(
                                    new PropertyModel<List<KeywordInfo>>(myModel, "keywords"))));

            if (!isAuthenticatedAsAdmin()) {
                if (isNew) {
                    // default to first available workspace
                    List<WorkspaceInfo> ws = getCatalog().getWorkspaces();
                    if (!ws.isEmpty()) {
                        wsChoice.setModelObject(ws.get(0));
                    }

                } else {
                    // always disable the workspace toggle
                    wsChoice.setEnabled(false);
                }
            }
        }

        private void updateRootLayerPanel(LayerGroupInfo.Mode mode) {
            rootLayerPanel.setEnabled(LayerGroupInfo.Mode.EO.equals(mode));
            rootLayerPanel.setVisible(LayerGroupInfo.Mode.EO.equals(mode));
        }

        class GroupNameValidator implements IValidator<String> {

            private static final long serialVersionUID = -6621372846640620132L;

            @Override
            public void validate(IValidatable<String> validatable) {
                String name = validatable.getValue();
                LayerGroupInfo other = getCatalog().getLayerGroupByName(name);
                if (other != null && (isNew || !other.getId().equals(getPublishedInfo().getId()))) {
                    IValidationError err =
                            new ValidationError("duplicateGroupNameError")
                                    .addKey("duplicateGroupNameError")
                                    .setVariable("name", name);
                    validatable.error(err);
                }
            }
        }
    }

    @Override
    protected PublishedEditTabPanel<LayerGroupInfo> createMainTab(String panelID) {
        return new LayerGroupTab(panelID);
    }

    @Override
    protected void doSaveInternal() {
        // validation
        if (lgEntryPanel.getEntries().size() == 0) {
            error(new ParamResourceModel("oneLayerMinimum", getPage()).getObject());
            return;
        }

        LayerGroupInfo lg = getPublishedInfo();

        if (!LayerGroupInfo.Mode.EO.equals(lg.getMode())) {
            lg.setRootLayer(null);
            lg.setRootLayerStyle(null);
        } else {
            if (lg.getRootLayerStyle() == null && lg.getRootLayer() != null) {
                lg.setRootLayerStyle(lg.getRootLayer().getDefaultStyle());
            }
        }

        // update the layer group entries
        lg.getLayers().clear();
        lg.getStyles().clear();
        for (LayerGroupEntry entry : lgEntryPanel.getEntries()) {
            lg.getLayers().add(entry.getLayer());
            lg.getStyles().add(entry.getStyle());
        }

        // update not queryable flag
        Boolean queryable = queryableCheckBox.getModelObject();
        lg.setQueryDisabled(!queryable);

        try {
            Catalog catalog = getCatalog();
            if (isNew) {
                catalog.add(lg);
            } else {
                catalog.save(lg);
            }
        } catch (Exception e) {
            error(e);
            LOGGER.log(Level.WARNING, "Error adding/modifying layer group.", e);
        }
    }
}
