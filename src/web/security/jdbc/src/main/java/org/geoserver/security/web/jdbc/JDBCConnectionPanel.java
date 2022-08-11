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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geotools.util.logging.Logging;

/**
 * Reusable form component for jdbc connect configurations
 *
 * @author Chrisitian Mueller
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCConnectionPanel<T extends JDBCSecurityServiceConfig>
        extends FormComponentPanel<T> {

    private static final long serialVersionUID = 1L;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    FeedbackPanel feedbackPanel;

    public JDBCConnectionPanel(String id, IModel<T> model) {
        super(id, new Model<>());

        add(
                new AjaxCheckBox("jndi") {
                    @Override
                    @SuppressWarnings("unchecked")
                    protected void onUpdate(AjaxRequestTarget target) {
                        WebMarkupContainer c =
                                (WebMarkupContainer)
                                        JDBCConnectionPanel.this.get("cxPanelContainer");

                        // reset any values that were set
                        ((ConnectionPanel) c.get("cxPanel")).resetModel();

                        // replace old panel
                        c.addOrReplace(createCxPanel("cxPanel", getModelObject()));

                        target.add(c);
                    }
                });

        boolean useJNDI = model.getObject().isJndi();
        add(
                new WebMarkupContainer("cxPanelContainer")
                        .add(createCxPanel("cxPanel", useJNDI))
                        .setOutputMarkupId(true));

        add(
                new AjaxSubmitLink("cxTest") {
                    @Override
                    @SuppressWarnings("unchecked")
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            ((ConnectionPanel)
                                            JDBCConnectionPanel.this.get(
                                                    "cxPanelContainer:cxPanel"))
                                    .test();
                            info(
                                    new StringResourceModel(
                                                    "connectionSuccessful",
                                                    JDBCConnectionPanel.this,
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

    ConnectionPanel createCxPanel(String id, boolean useJNDI) {
        return useJNDI ? new JNDIConnectionPanel(id) : new BasicConnectionPanel(id);
    }

    abstract class ConnectionPanel extends FormComponentPanel<Serializable> {

        public ConnectionPanel(String id) {
            super(id, new Model<>());
        }

        public abstract void resetModel();

        public abstract void test() throws Exception;
    }

    class BasicConnectionPanel extends ConnectionPanel {

        public BasicConnectionPanel(String id) {
            super(id);

            add(new JDBCDriverChoice("driverClassName").setRequired(true));
            add(new TextField("connectURL").setRequired(true));
            add(new TextField("userName").setRequired(true));

            PasswordTextField pwdField = new PasswordTextField("password");
            pwdField.setRequired(false);

            // avoid reseting the password which results in an
            // empty password on saving a modified configuration
            pwdField.setResetPassword(false);

            add(pwdField);
        }

        @Override
        public void resetModel() {
            // get("userGroupServiceName").setDefaultModelObject(null);
        }

        @Override
        public void test() throws Exception {
            // since this wasn't a regular form submission, we need to manually update component
            // models
            ((FormComponent) get("driverClassName")).processInput();
            ((FormComponent) get("connectURL")).processInput();
            ((FormComponent) get("userName")).processInput();
            ((FormComponent) get("password")).processInput();

            // do the test
            Class.forName(get("driverClassName").getDefaultModelObjectAsString());
            try (Connection cx =
                    DriverManager.getConnection(
                            get("connectURL").getDefaultModelObjectAsString(),
                            get("userName").getDefaultModelObjectAsString(),
                            get("password").getDefaultModelObjectAsString())) {}
        }
    }

    class JNDIConnectionPanel extends ConnectionPanel {

        public JNDIConnectionPanel(String id) {
            super(id);

            add(new TextField("jndiName").setRequired(true));
        }

        @Override
        public void resetModel() {
            // get("groupSearchBase").setDefaultModelObject(null);
            // get("groupSearchFilter").setDefaultModelObject(null);
        }

        @Override
        public void test() throws Exception {
            // since this wasn't a regular form submission, we need to manually update component
            // models
            ((FormComponent) get("jndiName")).processInput();

            Context initialContext = new InitialContext();
            try {
                DataSource datasource =
                        (DataSource)
                                initialContext.lookup(
                                        get("jndiName").getDefaultModelObjectAsString());
                try (Connection con = datasource.getConnection()) {}
            } finally {
                initialContext.close();
            }
        }
    }
}
