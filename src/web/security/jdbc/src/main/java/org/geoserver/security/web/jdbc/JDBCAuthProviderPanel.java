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

package org.geoserver.security.web.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.jdbc.JDBCConnectAuthProvider;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;

/**
 * Configuration panel for {@link JDBCConnectAuthProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCAuthProviderPanel
        extends AuthenticationProviderPanel<JDBCConnectAuthProviderConfig> {

    private static final long serialVersionUID = 1L;
    FeedbackPanel feedbackPanel;
    String username, password;

    public JDBCAuthProviderPanel(String id, IModel<JDBCConnectAuthProviderConfig> model) {
        super(id, model);

        add(new UserGroupServiceChoice("userGroupServiceName"));
        add(new JDBCDriverChoice("driverClassName"));
        add(new TextField<String>("connectURL"));

        TextField<String> userNameField = new TextField<>("username");
        userNameField.setModel(new PropertyModel<>(this, "username"));
        userNameField.setRequired(false);
        add(userNameField);

        PasswordTextField pwdField = new PasswordTextField("password");
        pwdField.setModel(new PropertyModel<>(this, "password"));
        pwdField.setRequired(false);
        pwdField.setResetPassword(true);
        add(pwdField);

        add(
                new AjaxSubmitLink("cxTest") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            test();
                            info(
                                    new StringResourceModel(
                                                    "connectionSuccessful",
                                                    JDBCAuthProviderPanel.this,
                                                    null)
                                            .getObject());
                        } catch (Exception e) {
                            error(e);
                            LOGGER.log(Level.WARNING, "Connection error", e);
                        } finally {
                            target.add(feedbackPanel);
                        }
                    }
                }.setDefaultFormProcessing(false));

        add(feedbackPanel = new FeedbackPanel("feedback"));
        feedbackPanel.setOutputMarkupId(true);
    }

    public void test() throws Exception {
        // since this wasn't a regular form submission, we need to manually update component
        // models
        ((FormComponent) get("driverClassName")).processInput();
        ((FormComponent) get("connectURL")).processInput();
        ((FormComponent) get("username")).processInput();
        ((FormComponent) get("password")).processInput();

        // do the test
        Class.forName(get("driverClassName").getDefaultModelObjectAsString());
        try (Connection cx =
                DriverManager.getConnection(
                        get("connectURL").getDefaultModelObjectAsString(),
                        get("username").getDefaultModelObjectAsString(),
                        get("password").getDefaultModelObjectAsString())) {}
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
