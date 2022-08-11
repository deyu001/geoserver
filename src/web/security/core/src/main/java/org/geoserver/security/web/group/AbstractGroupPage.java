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

package org.geoserver.security.web.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.AbstractSecurityException;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.RolePaletteFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;

/** Allows creation of a new user in users.properties */
@SuppressWarnings("serial")
public abstract class AbstractGroupPage extends AbstractSecurityPage {

    protected String userGroupServiceName;
    protected RolePaletteFormComponent rolePalette;

    protected AbstractGroupPage(String userGroupServiceName, final GeoServerUserGroup group) {
        this.userGroupServiceName = userGroupServiceName;

        boolean hasUserGroupStore = hasUserGroupStore(userGroupServiceName);
        boolean hasRoleStore = hasRoleStore(getSecurityManager().getActiveRoleService().getName());

        Form<GeoServerUserGroup> form = new Form<>("form", new CompoundPropertyModel<>(group));
        add(form);

        form.add(new TextField<String>("groupname").setEnabled(hasUserGroupStore));
        form.add(new CheckBox("enabled").setEnabled(hasUserGroupStore));

        List<GeoServerRole> roles;
        try {
            roles =
                    new ArrayList<>(
                            getSecurityManager()
                                    .getActiveRoleService()
                                    .getRolesForGroup(group.getGroupname()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        form.add(rolePalette = new RolePaletteFormComponent("roles", new ListModel<>(roles)));
        rolePalette.setEnabled(hasRoleStore);

        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            onFormSubmit(group);
                            setReturnPageDirtyAndReturn(true);
                        } catch (IOException e) {
                            if (e.getCause() instanceof AbstractSecurityException) {
                                error(e.getCause());
                            } else {
                                error(
                                        new ParamResourceModel(
                                                        "saveError", getPage(), e.getMessage())
                                                .getObject());
                            }
                            LOGGER.log(Level.SEVERE, "Error occurred while saving group", e);
                        }
                    }
                }.setEnabled(
                        hasUserGroupStore
                                || hasRoleStore(
                                        getSecurityManager().getActiveRoleService().getName())));

        // build the submit/cancel
        form.add(getCancelLink());
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(GeoServerUserGroup group) throws IOException;
}
