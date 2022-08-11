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

package org.geogig.geoserver.web.repository;

import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

/** */
class PostgresConfigFormPanel extends FormComponentPanel<PostgresConfigBean> {

    private static final long serialVersionUID = 1L;

    private final TextParamPanel hostPanel;
    private final TextParamPanel portPanel;
    private final TextParamPanel dbPanel;
    private final TextParamPanel schemaPanel;
    private final TextParamPanel usernamePanel;
    private final PasswordParamPanel passwordPanel;

    public PostgresConfigFormPanel(String id, IModel<PostgresConfigBean> model) {
        super(id, model);

        setOutputMarkupId(true);
        hostPanel =
                new TextParamPanel(
                        "hostPanel",
                        new PropertyModel<>(model, "host"),
                        new ResourceModel("PostgresConfigFormPanel.host", "Host Name"),
                        true);
        hostPanel.getFormComponent().setType(String.class);
        add(hostPanel);
        portPanel =
                new TextParamPanel(
                        "portPanel",
                        new PropertyModel<>(model, "port"),
                        new ResourceModel("PostgresConfigFormPanel.port", "Port"),
                        false);
        // set the type for the port, and validators
        portPanel
                .getFormComponent()
                .setType(Integer.TYPE)
                .add(
                        (IValidator) RangeValidator.minimum(1025),
                        (IValidator) RangeValidator.maximum(65535));
        add(portPanel);
        dbPanel =
                new TextParamPanel(
                        "dbPanel",
                        new PropertyModel<>(model, "database"),
                        new ResourceModel("PostgresConfigFormPanel.database", "Database Name"),
                        true);
        dbPanel.getFormComponent().setType(String.class);
        add(dbPanel);
        schemaPanel =
                new TextParamPanel(
                        "schemaPanel",
                        new PropertyModel<>(model, "schema"),
                        new ResourceModel("PostgresConfigFormPanel.schema", "Schema Name"),
                        false);
        schemaPanel.getFormComponent().setType(String.class);
        add(schemaPanel);
        usernamePanel =
                new TextParamPanel(
                        "usernamePanel",
                        new PropertyModel<>(model, "username"),
                        new ResourceModel("PostgresConfigFormPanel.username", "Username"),
                        true);
        usernamePanel.getFormComponent().setType(String.class);
        add(usernamePanel);
        passwordPanel =
                new PasswordParamPanel(
                        "passwordPanel",
                        new PropertyModel<>(model, "password"),
                        new ResourceModel("PostgresConfigFormPanel.password", "Password"),
                        true);
        passwordPanel.getFormComponent().setType(String.class);
        add(passwordPanel);
    }

    @Override
    public void convertInput() {
        PostgresConfigBean bean = new PostgresConfigBean();
        // populate the bean
        String host = hostPanel.getFormComponent().getConvertedInput().toString().trim();
        Integer port = Integer.class.cast(portPanel.getFormComponent().getConvertedInput());
        String db = dbPanel.getFormComponent().getConvertedInput().toString().trim();
        Object schema = schemaPanel.getFormComponent().getConvertedInput();
        String username = usernamePanel.getFormComponent().getConvertedInput().toString().trim();
        String password = passwordPanel.getFormComponent().getConvertedInput();

        bean.setHost(host);
        bean.setPort(port);
        bean.setDatabase(db);
        bean.setUsername(username);
        bean.setPassword(password);
        if (schema == null || schema.toString().trim().isEmpty()) {
            bean.setSchema(null);
        } else {
            bean.setSchema(schema.toString().trim());
        }

        setConvertedInput(bean);
    }
}
