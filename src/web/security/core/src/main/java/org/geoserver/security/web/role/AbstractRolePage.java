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

package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleHierarchyHelper;
import org.geoserver.security.validation.AbstractSecurityException;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.property.PropertyEditorFormComponent;
import org.springframework.util.StringUtils;

/** Allows creation of a new user in users.properties */
@SuppressWarnings("serial")
public abstract class AbstractRolePage extends AbstractSecurityPage {

    String roleServiceName;

    protected AbstractRolePage(String roleService, GeoServerRole role) {
        this.roleServiceName = roleService;
        boolean hasRoleStore = hasRoleStore(roleServiceName);

        if (role == null) role = new GeoServerRole("");

        Form<GeoServerRole> form = new Form<>("form", new CompoundPropertyModel<>(role));
        add(form);

        StringResourceModel descriptionModel;
        if (role.getUserName() != null) {
            descriptionModel =
                    new StringResourceModel("personalizedRole", getPage())
                            .setParameters(role.getUserName());
        } else {
            descriptionModel = new StringResourceModel("anonymousRole", getPage());
        }
        form.add(new Label("description", descriptionModel));

        form.add(
                new TextField<>("name", new Model<>(role.getAuthority()))
                        .setRequired(true)
                        .setEnabled(hasRoleStore));
        form.add(
                new DropDownChoice<>(
                                "parent", new ParentRoleModel(role), new ParentRolesModel(role))
                        .setNullValid(true)
                        .setEnabled(hasRoleStore));
        form.add(new PropertyEditorFormComponent("properties").setEnabled(hasRoleStore));

        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            onFormSubmit((GeoServerRole) getForm().getModelObject());
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
                            LOGGER.log(Level.SEVERE, "Error occurred while saving role", e);
                        }
                    }
                }.setVisible(hasRoleStore));

        form.add(getCancelLink());
    }

    class ParentRoleModel extends LoadableDetachableModel<String> {
        GeoServerRole role;

        ParentRoleModel(GeoServerRole role) {
            this.role = role;
        }

        @Override
        protected String load() {
            try {
                GeoServerRole parentRole = getRoleService(roleServiceName).getParentRole(role);
                return parentRole != null ? parentRole.getAuthority() : null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ParentRolesModel implements IModel<List<String>> {

        List<String> parentRoles;

        ParentRolesModel(GeoServerRole role) {
            try {
                parentRoles = computeAllowableParentRoles(role);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> computeAllowableParentRoles(GeoServerRole role) throws IOException {
            Map<String, String> parentMappings =
                    getRoleService(roleServiceName).getParentMappings();
            // if no parent mappings, return empty list
            if (parentMappings == null || parentMappings.isEmpty()) return Collections.emptyList();

            if (role != null && StringUtils.hasLength(role.getAuthority())) {
                // filter out roles already used as parents
                RoleHierarchyHelper helper = new RoleHierarchyHelper(parentMappings);

                Set<String> parents = new HashSet<>(parentMappings.keySet());
                parents.removeAll(helper.getDescendants(role.getAuthority()));
                parents.remove(role.getAuthority());
                return new ArrayList<>(parents);

            } else {
                // no rolename given, we are creating a new one
                return new ArrayList<>(parentMappings.keySet());
            }
        }

        @Override
        public List<String> getObject() {
            return parentRoles;
        }

        @Override
        public void setObject(List<String> object) {}

        @Override
        public void detach() {}
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(GeoServerRole role) throws IOException;
}
