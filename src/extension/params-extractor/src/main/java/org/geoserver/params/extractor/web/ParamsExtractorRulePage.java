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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerSecuredPage;

public class ParamsExtractorRulePage extends GeoServerSecuredPage {

    public ParamsExtractorRulePage(Optional<RuleModel> optionalRuleModel) {
        CompoundPropertyModel<RuleModel> simpleRuleModel =
                new CompoundPropertyModel<>(optionalRuleModel.orElse(new RuleModel()));
        CompoundPropertyModel<RuleModel> complexRuleModel =
                new CompoundPropertyModel<>(optionalRuleModel.orElse(new RuleModel()));
        CompoundPropertyModel<RuleModel> echoParameterModel =
                new CompoundPropertyModel<>(optionalRuleModel.orElse(new RuleModel(true)));
        Form<RuleModel> form = new Form<>("form");
        add(form);
        List<WrappedTab> tabs = new ArrayList<>();
        if (!optionalRuleModel.isPresent() || optionalRuleModel.get().isEchoOnly()) {
            tabs.add(
                    new WrappedTab("Echo Parameter", echoParameterModel) {
                        public Panel getPanel(String panelId) {
                            return new EchoParameterPanel(panelId, echoParameterModel);
                        }
                    });
        }
        if (!optionalRuleModel.isPresent() || optionalRuleModel.get().getPosition() != null) {
            tabs.add(
                    new WrappedTab("Basic Rule", simpleRuleModel) {
                        public Panel getPanel(String panelId) {
                            return new SimpleRulePanel(panelId, simpleRuleModel);
                        }
                    });
        }
        if (!optionalRuleModel.isPresent() || optionalRuleModel.get().getMatch() != null) {
            tabs.add(
                    new WrappedTab("Advanced Rule", complexRuleModel) {
                        public Panel getPanel(String panelId) {
                            return new ComplexRulePanel(panelId, complexRuleModel);
                        }
                    });
        }
        AjaxTabbedPanel tabbedPanel = new AjaxTabbedPanel<>("tabs", tabs);
        form.add(tabbedPanel);
        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            WrappedTab selectedTab = tabs.get(tabbedPanel.getSelectedTab());
                            RuleModel ruleModel = selectedTab.getModel().getObject();
                            RulesModel.saveOrUpdate(ruleModel);
                            doReturn(ParamsExtractorConfigPage.class);
                        } catch (Exception exception) {
                            error(exception);
                        }
                    }
                });
        form.add(new BookmarkablePageLink<>("cancel", ParamsExtractorConfigPage.class));
    }

    public abstract class WrappedTab extends AbstractTab {

        private final IModel<RuleModel> model;

        public WrappedTab(String title, IModel<RuleModel> model) {
            super(new Model<>(title));
            this.model = model;
        }

        public IModel<RuleModel> getModel() {
            return model;
        }
    }

    public class SimpleRulePanel extends Panel {

        public SimpleRulePanel(String panelId, IModel<RuleModel> model) {
            super(panelId, model);
            add(new NumberTextField<Integer>("position").setMinimum(1).setRequired(true));
            add(new TextField<String>("parameter").setRequired(true));
            add(new TextField<String>("transform").setRequired(true));
            add(new CheckBox("echo"));
        }
    }

    public class ComplexRulePanel extends Panel {

        public ComplexRulePanel(String panelId, IModel<RuleModel> model) {
            super(panelId, model);
            add(new TextField<String>("match").setRequired(true));
            add(new TextField<String>("activation"));
            add(new TextField<String>("parameter").setRequired(true));
            add(new TextField<String>("transform").setRequired(true));
            add(new NumberTextField<Integer>("remove").setMinimum(1));
            add(new TextField<String>("combine"));
            add(new CheckBox("repeat"));
            add(new CheckBox("echo"));
        }
    }

    public class EchoParameterPanel extends Panel {

        public EchoParameterPanel(String panelId, IModel<RuleModel> model) {
            super(panelId, model);
            add(new TextField<String>("parameter").setRequired(true));
        }
    }
}
