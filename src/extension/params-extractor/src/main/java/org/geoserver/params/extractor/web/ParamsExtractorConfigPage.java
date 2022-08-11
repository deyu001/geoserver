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

package org.geoserver.params.extractor.web;

import java.util.Optional;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.params.extractor.UrlTransform;
import org.geoserver.params.extractor.Utils;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;

public class ParamsExtractorConfigPage extends GeoServerSecuredPage {

    GeoServerTablePanel<RuleModel> rulesPanel;

    public ParamsExtractorConfigPage() {
        setHeaderPanel(headerPanel());
        add(
                rulesPanel =
                        new GeoServerTablePanel<RuleModel>("rulesPanel", new RulesModel(), true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<RuleModel> itemModel,
                                    GeoServerDataProvider.Property<RuleModel> property) {
                                if (property == RulesModel.EDIT_BUTTON) {
                                    return new EditButtonPanel(id, itemModel.getObject());
                                }
                                if (property == RulesModel.ACTIVATE_BUTTON) {
                                    return new ActivateButtonPanel(id, itemModel.getObject());
                                }
                                return null;
                            }
                        });
        rulesPanel.setOutputMarkupId(true);
        RuleTestModel ruleTestModel = new RuleTestModel();
        Form<RuleTestModel> form = new Form<>("form", new CompoundPropertyModel<>(ruleTestModel));
        add(form);
        TextArea<String> input =
                new TextArea<>("input", new PropertyModel<>(ruleTestModel, "input"));
        form.add(input);
        input.setDefaultModelObject(
                "/geoserver/tiger/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&CQL_FILTER=CFCC%3D%27H11%27");
        TextArea<String> output =
                new TextArea<>("output", new PropertyModel<>(ruleTestModel, "output"));
        output.setEnabled(false);
        form.add(output);
        form.add(
                new SubmitLink("test") {
                    @Override
                    public void onSubmit() {
                        if (rulesPanel.getSelection().isEmpty()) {
                            output.setModelObject("NO RULES SELECTED !");
                            return;
                        }
                        String outputText;
                        try {
                            RuleTestModel ruleTestModel =
                                    (RuleTestModel) getForm().getModelObject();
                            String[] urlParts = ruleTestModel.getInput().split("\\?");
                            String requestUri = urlParts[0];
                            Optional<String> queryRequest =
                                    urlParts.length > 1
                                            ? Optional.ofNullable(urlParts[1])
                                            : Optional.empty();
                            UrlTransform urlTransform =
                                    new UrlTransform(
                                            requestUri, Utils.parseParameters(queryRequest));
                            rulesPanel
                                    .getSelection()
                                    .stream()
                                    .filter(rule -> !rule.isEchoOnly())
                                    .map(RuleModel::toRule)
                                    .forEach(rule -> rule.apply(urlTransform));
                            if (urlTransform.haveChanged()) {
                                outputText = urlTransform.toString();
                            } else {
                                outputText = "NO RULES APPLIED !";
                            }
                        } catch (Exception exception) {
                            outputText = "Exception: " + exception.getMessage();
                        }
                        output.setModelObject(outputText);
                    }
                });
    }

    private Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        header.add(
                new AjaxLink<Object>("addNew") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(new ParamsExtractorRulePage(Optional.empty()));
                    }
                });
        header.add(
                new AjaxLink<Object>("removeSelected") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        RulesModel.delete(
                                rulesPanel
                                        .getSelection()
                                        .stream()
                                        .map(RuleModel::getId)
                                        .toArray(String[]::new));
                        target.add(rulesPanel);
                    }
                });
        return header;
    }

    private class EditButtonPanel extends Panel {

        public EditButtonPanel(String id, final RuleModel ruleModel) {
            super(id);
            this.setOutputMarkupId(true);
            ImageAjaxLink<Object> editLink =
                    new ImageAjaxLink<Object>(
                            "edit", new PackageResourceReference(getClass(), "img/edit.png")) {
                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            setResponsePage(new ParamsExtractorRulePage(Optional.of(ruleModel)));
                        }
                    };
            editLink.getImage()
                    .add(
                            new AttributeModifier(
                                    "alt",
                                    new ParamResourceModel(
                                            "ParamsExtractorConfigPage.edit", editLink)));
            editLink.setOutputMarkupId(true);
            add(editLink);
        }
    }

    private class ActivateButtonPanel extends Panel {

        public ActivateButtonPanel(String id, final RuleModel ruleModel) {
            super(id);
            this.setOutputMarkupId(true);
            CheckBox activateButton =
                    new CheckBox("activated", new PropertyModel<>(ruleModel, "activated")) {

                        @Override
                        public void onSelectionChanged() {
                            super.onSelectionChanged();
                            RulesModel.saveOrUpdate(ruleModel);
                        }

                        @Override
                        protected boolean wantOnSelectionChangedNotifications() {
                            return true;
                        }
                    };
            add(activateButton);
        }
    }
}
