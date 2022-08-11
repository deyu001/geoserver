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

import java.util.logging.Logger;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.util.logging.Logging;

/**
 * Base class for configuration panels of a specific class of named security service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class SecurityNamedServicePanel<T extends SecurityNamedServiceConfig>
        extends FormComponentPanel<T> {

    /** logger */
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.web.security");

    /** model for underlying config */
    protected IModel<T> configModel;

    /** pop-up dialog */
    protected GeoServerDialog dialog;

    public SecurityNamedServicePanel(String id, IModel<T> model) {
        super(id, new Model<>());
        this.configModel = model;

        // check for administrator, if not disable the panel and emit warning message
        boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole();
        setEnabled(isAdmin);

        add(
                new Label(
                        "message",
                        isAdmin ? new Model() : new StringResourceModel("notAdmin", this, null)));
        if (!isAdmin) {
            get("message").add(new AttributeAppender("class", new Model<>("info-link"), " "));
        }

        setOutputMarkupId(true);
        add(new TextField("name").setRequired(true).setEnabled(model.getObject().getId() == null));

        add(dialog = new GeoServerDialog("dialog"));
    }

    protected GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    /**
     * Determines if the configuration object represents a new configuration, or an existing one.
     */
    protected boolean isNew() {
        return configModel.getObject().getId() == null;
    }

    public abstract void doSave(T config) throws Exception;

    public abstract void doLoad(T config) throws Exception;
}
