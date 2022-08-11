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

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.GeoServerApplication;

/**
 * Edit page for specific class of named security service.
 *
 * <p>Most of the work is delegated to {@link SecurityNamedServicePanelInfo} and {@link
 * SecurityNamedServicePanel}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SecurityNamedServiceEditPage<T extends SecurityNamedServiceConfig>
        extends SecurityNamedServicePage<T> {

    SecurityNamedServicePanelInfo panelInfo;

    public SecurityNamedServiceEditPage(IModel<T> config) {
        // create the specific panel
        panelInfo = lookupPanelInfo(config);
        panel = createPanel("dummy", panelInfo, config);

        // set page title and description from the panel title and description
        add(new Label("name", config.getObject().getName()));
        add(new Label("title", createTitleModel(panelInfo)));
        add(new Label("description", createDescriptionModel(panelInfo)));

        if (SecurityNamedServiceTabbedPanel.class.isAssignableFrom(panelInfo.getComponentClass())) {
            // this panel supports tabs, layout in tabbed mode
            add(new TabbedLayoutPanel("panel", config));
        } else {
            // else layout in basic mode
            add(new BasicLayoutPanel("panel", config));
        }
    }

    class ContentPanel extends Panel {

        public ContentPanel(String id, IModel<T> config) {
            super(id, new Model());

            Form<T> form = new Form<>("form", new CompoundPropertyModel<>(config));
            add(form);
            form.add(panel = createPanel("panel", panelInfo, config));

            form.add(
                    new SubmitLink("save", form) {
                        @Override
                        public void onSubmit() {
                            handleSubmit(getForm());
                        }
                    }.setVisible(getSecurityManager().checkAuthenticationForAdminRole()));
            form.add(
                    new Link("cancel") {
                        @Override
                        public void onClick() {
                            doReturn();
                        }
                    });
        }
    }

    /*
     * throws the service panel into a basic form panel
     */
    class BasicLayoutPanel extends Panel {

        public BasicLayoutPanel(String id, IModel<T> config) {
            super(id, new Model());

            add(new ContentPanel("panel", config));
        }
    }

    /*
     * throws the service panel onto the first tab, and then delegates it to create additional
     * tabs.
     */
    class TabbedLayoutPanel extends Panel {

        public TabbedLayoutPanel(String id, final IModel<T> config) {
            super(id, new Model());

            List<ITab> tabs = new ArrayList<>();

            // add the primary panel to the first tab
            tabs.add(
                    new AbstractTab(new StringResourceModel("settings", (IModel<?>) null)) {
                        @Override
                        public Panel getPanel(String panelId) {
                            return new ContentPanel(panelId, config);
                        }
                    });

            // add tabs contributed by the server
            @SuppressWarnings("unchecked")
            SecurityNamedServiceTabbedPanel<T> cast =
                    (SecurityNamedServiceTabbedPanel) SecurityNamedServiceEditPage.this.panel;
            tabs.addAll(cast.createTabs(config));

            // add the error tab that displays any exceptions currently associated with the service
            try {
                SecurityNamedServiceEditPage.this.panel.doLoad(config.getObject());
            } catch (final Exception e) {
                // add the error tab
                tabs.add(
                        new AbstractTab(new StringResourceModel("error", (IModel<?>) null)) {
                            @Override
                            public Panel getPanel(String panelId) {
                                return new ErrorPanel(panelId, e);
                            }
                        });
            }
            add(new TabbedPanel<>("panel", tabs));
        }
    }

    class ErrorPanel extends Panel {

        public ErrorPanel(String id, final Exception error) {
            super(id, new Model());

            add(new Label("message", new PropertyModel<>(error, "message")));
            add(new TextArea<>("stackTrace", new Model<>(handleStackTrace(error))));
            add(
                    new AjaxLink("copy") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            copyToClipBoard(handleStackTrace(error));
                        }
                    });
        }

        public String getLabelKey() {
            return "error";
        };

        String handleStackTrace(Exception error) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out);
            error.printStackTrace(writer);
            writer.flush();

            return new String(out.toByteArray());
        }
    }

    void copyToClipBoard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    SecurityNamedServicePanelInfo lookupPanelInfo(IModel<T> model) {
        T config = model.getObject();
        Class<?> serviceClass = null;
        try {
            serviceClass = Class.forName(config.getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        List<SecurityNamedServicePanelInfo> panelInfos = new ArrayList<>();
        for (SecurityNamedServicePanelInfo pageInfo :
                GeoServerApplication.get().getBeansOfType(SecurityNamedServicePanelInfo.class)) {
            Class<?> psc = pageInfo.getServiceClass();
            if (psc.isAssignableFrom(serviceClass)) {
                panelInfos.add(pageInfo);
            }
        }

        if (panelInfos.isEmpty()) {
            throw new RuntimeException(
                    "Unable to find panel info for service config: "
                            + config
                            + ", service class: "
                            + serviceClass);
        }
        if (panelInfos.size() > 1) {
            // filter by strict equals
            List<SecurityNamedServicePanelInfo> l = new ArrayList<>(panelInfos);
            for (Iterator<SecurityNamedServicePanelInfo> it = l.iterator(); it.hasNext(); ) {
                final SecurityNamedServicePanelInfo targetPanelInfo = it.next();
                if (!targetPanelInfo.getServiceClass().equals(serviceClass)) {
                    it.remove();
                } else if (!targetPanelInfo.getServiceConfigClass().equals(config.getClass())) {
                    it.remove();
                }
            }
            if (l.size() == 1) {
                // filter down to one match
                return l.get(0);
            }
            throw new RuntimeException(
                    "Found multiple panel infos for service config: "
                            + config
                            + ", service class: "
                            + serviceClass);
        }

        // found just one
        return panelInfos.get(0);
    }
}
