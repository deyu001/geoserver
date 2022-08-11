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

package org.geoserver.security.web.cas;

import static org.geoserver.security.cas.CasAuthenticationFilterConfig.CasSpecificRoleSource.CustomAttribute;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.cas.CasAuthenticationFilterConfig;
import org.geoserver.security.cas.GeoServerCasAuthenticationFilter;
import org.geoserver.security.cas.GeoServerCasConstants;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.security.web.auth.RoleSourceChoiceRenderer;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerCasAuthenticationFilter}.
 *
 * @author mcr
 */
public class CasAuthFilterPanel
        extends PreAuthenticatedUserNameFilterPanel<CasAuthenticationFilterConfig> {

    private static final long serialVersionUID = 1;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    GeoServerDialog dialog;

    public CasAuthFilterPanel(String id, IModel<CasAuthenticationFilterConfig> model) {
        super(id, model);

        dialog = (GeoServerDialog) get("dialog");

        add(new HelpLink("connectionParametersHelp", this).setDialog(dialog));
        add(new HelpLink("singleSignOnParametersHelp", this).setDialog(dialog));
        add(new HelpLink("singleSignOutParametersHelp", this).setDialog(dialog));
        add(new HelpLink("proxyTicketParametersHelp", this).setDialog(dialog));

        add(new TextField<String>("casServerUrlPrefix"));
        add(new CheckBox("sendRenew"));
        add(new TextField<String>("proxyCallbackUrlPrefix").setRequired(false));

        add(
                new AjaxSubmitLink("casServerTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            testURL("casServerUrlPrefix", GeoServerCasConstants.LOGOUT_URI);
                            info(
                                    new StringResourceModel(
                                                    "casConnectionSuccessful",
                                                    CasAuthFilterPanel.this,
                                                    null)
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            ((GeoServerBasePage) getPage())
                                    .addFeedbackPanels(target); // to display message
                            LOGGER.log(Level.WARNING, "CAS connection error ", e);
                        }
                    }
                }.setDefaultFormProcessing(false));

        add(
                new AjaxSubmitLink("proxyCallbackTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            testURL("proxyCallbackUrlPrefix", null);
                            info(
                                    new StringResourceModel(
                                                    "casProxyCallbackSuccessful",
                                                    CasAuthFilterPanel.this,
                                                    null)
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            ((GeoServerBasePage) getPage())
                                    .addFeedbackPanels(target); // to display message
                            LOGGER.log(Level.WARNING, "CAS proxy callback  error ", e);
                        }
                    }
                }.setDefaultFormProcessing(false));

        CheckBox createSession = new CheckBox("singleSignOut");
        add(createSession);

        add(new TextField<String>("urlInCasLogoutPage"));
        add(
                new AjaxSubmitLink("urlInCasLogoutPageTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            testURL("urlInCasLogoutPage", null);
                            info(
                                    new StringResourceModel(
                                                    "urlInCasLogoutPageSuccessful",
                                                    CasAuthFilterPanel.this,
                                                    null)
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            ((GeoServerBasePage) getPage())
                                    .addFeedbackPanels(target); // to display message
                            LOGGER.log(Level.WARNING, "CAs url in logout page error ", e);
                        }
                    }
                }.setDefaultFormProcessing(false));
    }

    public void testURL(String wicketId, String uri) throws Exception {
        // since this wasn't a regular form submission, we need to manually update component
        // models
        ((FormComponent) get(wicketId)).processInput();
        String urlString = get(wicketId).getDefaultModelObjectAsString();
        if (uri != null) urlString += uri;
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.getInputStream().close();
    }

    @Override
    protected Panel getRoleSourcePanel(RoleSource model) {
        if (CustomAttribute.equals(model)) {
            return new CustomAttributePanel("panel");
        }
        return super.getRoleSourcePanel(model);
    }

    @Override
    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        List<RoleSource> sources =
                new ArrayList<>(Arrays.asList(PreAuthenticatedUserNameRoleSource.values()));
        sources.addAll(Arrays.asList(CasAuthenticationFilterConfig.CasSpecificRoleSource.values()));
        return new DropDownChoice<>("roleSource", sources, new RoleSourceChoiceRenderer());
    }

    static class CustomAttributePanel extends Panel {
        public CustomAttributePanel(String id) {
            super(id, new Model());
            add(new TextField<String>("customAttributeName").setRequired(true));
        }
    }
}
