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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.RuleRolesFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;
import org.opengis.filter.Filter;

/** Abstract page binding a {@link DataAccessRule} */
@SuppressWarnings("serial")
public abstract class AbstractDataAccessRulePage extends AbstractSecurityPage {

    public class RootLabelModel extends LoadableDetachableModel<String> {

        @Override
        protected String load() {
            if (globalGroupRule.getModelObject()) {
                return new ParamResourceModel("globalGroup", AbstractDataAccessRulePage.this)
                        .getString();
            } else {
                return new ParamResourceModel("workspace", AbstractDataAccessRulePage.this)
                        .getString();
            }
        }
    }

    public class RootsModel extends LoadableDetachableModel<List<String>> {

        @Override
        protected List<String> load() {
            if (globalGroupRule.getModelObject()) {
                return getGlobalLayerGroupNames();
            } else {
                return getWorkspaceNames();
            }
        }

        /** Returns a sorted list of global layer group names */
        List<String> getGlobalLayerGroupNames() {
            Stream<String> names =
                    getCatalog()
                            .getLayerGroupsByWorkspace(CatalogFacade.NO_WORKSPACE)
                            .stream()
                            .map(lg -> lg.getName())
                            .sorted();
            return Stream.concat(Stream.of("*"), names).collect(Collectors.toList());
        }

        /** Returns a sorted list of workspace names */
        List<String> getWorkspaceNames() {
            Stream<String> names =
                    getCatalog().getWorkspaces().stream().map(ws -> ws.getName()).sorted();
            return Stream.concat(Stream.of("*"), names).collect(Collectors.toList());
        }
    }

    static List<AccessMode> MODES =
            Arrays.asList(AccessMode.READ, AccessMode.WRITE, AccessMode.ADMIN);

    DropDownChoice<String> rootChoice, layerChoice;
    DropDownChoice<AccessMode> accessModeChoice;
    RuleRolesFormComponent rolesFormComponent;
    CheckBox globalGroupRule;

    WebMarkupContainer layerContainer;

    Label rootLabel;

    WebMarkupContainer layerAndLabel;

    public AbstractDataAccessRulePage(final DataAccessRule rule) {
        // build the form
        Form<DataAccessRule> form = new Form<>("form", new CompoundPropertyModel<>(rule));
        add(form);
        form.add(new EmptyRolesValidator());

        form.add(globalGroupRule = new CheckBox("globalGroupRule"));
        globalGroupRule.setOutputMarkupId(true);
        globalGroupRule.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        rootChoice.getModel().detach();
                        target.add(rootChoice);
                        layerAndLabel.setVisible(!globalGroupRule.getModelObject());
                        target.add(layerContainer);
                        rootLabel.getDefaultModel().detach();
                        target.add(rootLabel);
                    }
                });

        form.add(rootLabel = new Label("rootLabel", new RootLabelModel()));
        rootLabel.setOutputMarkupId(true);
        form.add(rootChoice = new DropDownChoice<>("root", new RootsModel()));
        rootChoice.setRequired(true);
        rootChoice.setOutputMarkupId(true);
        rootChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        layerChoice.setChoices(
                                new Model<>(getLayerNames(rootChoice.getConvertedInput())));
                        layerChoice.modelChanged();
                        target.add(layerChoice);
                    }
                });

        form.add(layerContainer = new WebMarkupContainer("layerContainer"));
        layerContainer.setOutputMarkupId(true);
        layerContainer.add(layerAndLabel = new WebMarkupContainer("layerAndLabel"));
        layerAndLabel.add(
                layerChoice = new DropDownChoice<>("layer", getLayerNames(rule.getRoot())));
        layerAndLabel.setVisible(!rule.isGlobalGroupRule());
        layerChoice.setRequired(true);
        layerChoice.setOutputMarkupId(true);

        form.add(
                accessModeChoice =
                        new DropDownChoice<>("accessMode", MODES, new AccessModeRenderer()));
        accessModeChoice.setRequired(true);

        form.add(
                rolesFormComponent =
                        new RuleRolesFormComponent("roles", new PropertyModel<>(rule, "roles"))
                                .setHasAnyRole(
                                        rule.getRoles()
                                                .contains(GeoServerRole.ANY_ROLE.getAuthority())));

        // build the submit/cancel
        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        DataAccessRule rule = (DataAccessRule) getForm().getModelObject();
                        if (rolesFormComponent.isHasAnyRole()) {
                            rule.getRoles().clear();
                            rule.getRoles().add(GeoServerRole.ANY_ROLE.getAuthority());
                        }
                        if (globalGroupRule.getModelObject()) {
                            // just to be on the safe side
                            rule.setLayer(null);
                        }
                        onFormSubmit(rule);
                    }
                });
        form.add(new BookmarkablePageLink<DataAccessRule>("cancel", DataSecurityPage.class));
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(DataAccessRule rule);

    /**
     * Returns a sorted list of layer names in the specified workspace (or * if the workspace is *)
     */
    ArrayList<String> getLayerNames(String rootName) {
        ArrayList<String> result = new ArrayList<>();
        if (!rootName.equals("*")) {
            Filter wsResources = Predicates.equal("store.workspace.name", rootName);
            try (CloseableIterator<ResourceInfo> it =
                    getCatalog().list(ResourceInfo.class, wsResources)) {
                while (it.hasNext()) {
                    result.add(it.next().getName());
                }
            }
            // collect also layer groups
            getCatalog()
                    .getLayerGroupsByWorkspace(rootName)
                    .stream()
                    .map(lg -> lg.getName())
                    .forEach(
                            name -> {
                                if (!result.contains(name)) {
                                    result.add(name);
                                }
                            });
            Collections.sort(result);
        }
        result.add(0, "*");
        return result;
    }

    /** Makes sure we see translated text, by the raw name is used for the model */
    class AccessModeRenderer extends ChoiceRenderer<AccessMode> {

        public Object getDisplayValue(AccessMode object) {
            return new ParamResourceModel(object.name(), getPage()).getObject();
        }

        public String getIdValue(AccessMode object, int index) {
            return object.name();
        }
    }

    class EmptyRolesValidator extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {
                rootChoice, layerChoice, accessModeChoice, rolesFormComponent
            };
        }

        @Override
        public void validate(Form<?> form) {
            // only validate on final submit
            if (form.findSubmittingButton() != form.get("save")) {
                return;
            }

            updateModels();
            String roleInputString =
                    rolesFormComponent.getPalette().getRecorderComponent().getInput();
            if ((roleInputString == null || roleInputString.trim().isEmpty())
                    && !rolesFormComponent.isHasAnyRole()) {
                form.error(new ParamResourceModel("emptyRoles", getPage()).getString());
            }
        }
    }

    protected void updateModels() {
        rootChoice.updateModel();
        layerChoice.updateModel();
        accessModeChoice.updateModel();
        rolesFormComponent.updateModel();
    }
}
