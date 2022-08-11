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

package org.geoserver.security.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AuthenticationKeyFilterConfig;
import org.geoserver.security.AuthenticationKeyMapper;
import org.geoserver.security.GeoServerAuthenticationKeyFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerAuthenticationKeyFilter}.
 *
 * @author mcr
 */
public class AuthenticationKeyFilterPanel
        extends AuthenticationFilterPanel<AuthenticationKeyFilterConfig> {

    private static final long serialVersionUID = 1;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    GeoServerDialog dialog;

    IModel<AuthenticationKeyFilterConfig> model;

    public AuthenticationKeyFilterPanel(String id, IModel<AuthenticationKeyFilterConfig> model) {
        super(id, model);

        dialog = (GeoServerDialog) get("dialog");
        this.model = model;

        add(new HelpLink("authKeyParametersHelp", this).setDialog(dialog));

        add(new TextField<String>("authKeyParamName"));

        Map<String, String> parameters = model.getObject().getMapperParameters();
        final ParamsPanel paramsPanel =
                createParamsPanel(
                        "authKeyMapperParamsPanel",
                        model.getObject().getAuthKeyMapperName(),
                        parameters);

        AuthenticationKeyMapperChoice authenticationKeyMapperChoice =
                new AuthenticationKeyMapperChoice("authKeyMapperName");

        authenticationKeyMapperChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    protected void onUpdate(AjaxRequestTarget target) {
                        String newSelection = (String) getFormComponent().getConvertedInput();
                        Map<String, String> parameters = getMapperParameters(newSelection);
                        AuthenticationKeyFilterPanel.this
                                .model
                                .getObject()
                                .setMapperParameters(parameters);
                        paramsPanel.updateParameters(newSelection, parameters);
                        target.add(paramsPanel);
                    }
                });

        add(authenticationKeyMapperChoice);
        add(new UserGroupServiceChoice("userGroupServiceName"));

        add(
                new WebMarkupContainer("authKeyMapperParamsContainer")
                        .add(paramsPanel)
                        .setOutputMarkupId(true));

        add(
                new AjaxSubmitLink("synchronize") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            // AuthenticationKeyFilterPanel.this.updateModel();
                            AuthenticationKeyFilterConfig config =
                                    AuthenticationKeyFilterPanel.this.model.getObject();

                            getSecurityManager().saveFilter(config);
                            AuthenticationKeyMapper mapper =
                                    (AuthenticationKeyMapper)
                                            GeoServerExtensions.bean(config.getAuthKeyMapperName());
                            mapper.setSecurityManager(getSecurityManager());
                            mapper.setUserGroupServiceName(config.getUserGroupServiceName());
                            int numberOfNewKeys = mapper.synchronize();
                            info(
                                    new StringResourceModel(
                                                    "synchronizeSuccessful",
                                                    AuthenticationKeyFilterPanel.this)
                                            .setParameters(numberOfNewKeys)
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, "Authentication key  error ", e);
                        } finally {
                            target.add(getPage().get("topFeedback"));
                        }
                    }
                }.setDefaultFormProcessing(true));
    }

    class ParamsPanel extends FormComponentPanel<Serializable> {

        public ParamsPanel(String id, String authMapperName, Map<String, String> parameters) {
            super(id, new Model<>());
            updateParameters(authMapperName, parameters);
        }

        private void updateParameters(
                final String authMapperName, final Map<String, String> parameters) {

            removeAll();
            add(
                    new ListView<String>(
                            "parametersList", new Model<>(new ArrayList<>(parameters.keySet()))) {
                        @Override
                        protected void populateItem(ListItem<String> item) {
                            item.add(
                                    new Label(
                                            "parameterName",
                                            new StringResourceModel(
                                                    "AuthenticationKeyFilterPanel."
                                                            + authMapperName
                                                            + "."
                                                            + item.getModel().getObject(),
                                                    this,
                                                    null)));
                            item.add(
                                    new TextField<String>(
                                            "parameterField",
                                            new MapModel<>(
                                                    parameters, item.getModel().getObject())));
                        }
                    });
        }

        public void resetModel() {}
    }

    private ParamsPanel createParamsPanel(
            String id, String authKeyMapperName, Map<String, String> parameters) {
        ParamsPanel paramsPanel = new ParamsPanel(id, authKeyMapperName, parameters);
        paramsPanel.setOutputMarkupId(true);
        return paramsPanel;
    }

    private Map<String, String> getMapperParameters(String authKeyMapperName) {
        if (authKeyMapperName != null) {
            AuthenticationKeyMapper mapper =
                    (AuthenticationKeyMapper) GeoServerExtensions.bean(authKeyMapperName);
            if (mapper != null) {
                return mapper.getMapperConfiguration();
            }
        }
        return new HashMap<>();
    }
}
