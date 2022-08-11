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

package org.geoserver.importer.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.PropertyModel;

/**
 * Panel for the basic dbms parameters
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
class BasicDbParamPanel extends Panel {

    String host;
    int port;
    String username;
    String password;
    String database;
    String schema;

    ConnectionPoolParamPanel connPoolPanel;
    WebMarkupContainer connPoolPanelContainer;
    Component connPoolLink;

    public BasicDbParamPanel(String id, String host, int port, boolean databaseRequired) {
        this(id, host, port, null, null, null, databaseRequired);
    }

    public BasicDbParamPanel(
            String id,
            String host,
            int port,
            String database,
            String schema,
            String username,
            boolean databaseRequired) {
        super(id);

        this.host = host;
        this.port = port;
        this.database = database;
        this.schema = schema;
        this.username = username;

        add(new TextField<>("host", new PropertyModel<>(this, "host")).setRequired(true));
        add(new TextField<>("port", new PropertyModel<>(this, "port")).setRequired(true));
        add(new TextField<>("username", new PropertyModel<>(this, "username")).setRequired(true));
        add(
                new PasswordTextField("password", new PropertyModel<>(this, "password"))
                        .setResetPassword(false)
                        .setRequired(false));
        add(
                new TextField<>("database", new PropertyModel<>(this, "database"))
                        .setRequired(databaseRequired));
        add(new TextField<>("schema", new PropertyModel<>(this, "schema")));

        connPoolLink = toggleConnectionPoolLink();
        add(connPoolLink);

        connPoolPanelContainer = new WebMarkupContainer("connPoolPanelContainer");
        connPoolPanelContainer.setOutputMarkupId(true);
        connPoolPanel = new ConnectionPoolParamPanel("connPoolPanel", true);
        connPoolPanel.setVisible(false);
        connPoolPanelContainer.add(connPoolPanel);
        add(connPoolPanelContainer);
    }

    /** Toggles the connection pool param panel */
    Component toggleConnectionPoolLink() {
        AjaxLink connPoolLink =
                new AjaxLink("connectionPoolLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        connPoolPanel.setVisible(!connPoolPanel.isVisible());
                        target.add(connPoolPanelContainer);
                        target.add(this);
                    }
                };
        connPoolLink.add(
                new AttributeModifier(
                        "class",
                        new AbstractReadOnlyModel() {

                            @Override
                            public Object getObject() {
                                return connPoolPanel.isVisible() ? "expanded" : "collapsed";
                            }
                        }));
        connPoolLink.setOutputMarkupId(true);
        return connPoolLink;
    }
}
