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

package org.geoserver.web.security.ldap;

import java.util.Optional;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.ldap.LDAPUserGroupServiceConfig;
import org.geoserver.security.web.usergroup.UserGroupServicePanel;

/** @author Niels Charlier */
public class LDAPUserGroupServicePanel extends UserGroupServicePanel<LDAPUserGroupServiceConfig> {
    private static final long serialVersionUID = -5052166946618920800L;

    private static final String USE_NESTED_PARENT_GROUPS = "useNestedParentGroups";
    private static final String MAX_GROUP_SEARCH_LEVEL = "maxGroupSearchLevel";
    private static final String NESTED_GROUP_SEARCH_FILTER = "nestedGroupSearchFilter";
    private static final String NESTED_SEARCH_FIELDS_CONTAINER = "nestedSearchFieldsContainer";

    class LDAPAuthenticationPanel extends WebMarkupContainer {

        private static final long serialVersionUID = 6533128678666053350L;

        public LDAPAuthenticationPanel(String id) {
            super(id);
            add(new TextField<String>("user"));

            PasswordTextField pwdField = new PasswordTextField("password");
            // avoid reseting the password which results in an
            // empty password on saving a modified configuration
            pwdField.setResetPassword(false);
            add(pwdField);
        }

        public void resetModel() {
            get("user").setDefaultModelObject(null);
            get("password").setDefaultModelObject(null);
        }
    }

    public LDAPUserGroupServicePanel(String id, IModel<LDAPUserGroupServiceConfig> model) {
        super(id, model);
        /** LDAP server parameters */
        add(new TextField<String>("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));
        /** group options */
        add(new TextField<String>("groupSearchBase").setRequired(true));
        add(new TextField<String>("groupNameAttribute"));
        add(new TextField<String>("groupFilter"));
        add(new TextField<String>("allGroupsSearchFilter"));
        /** membership options */
        add(new TextField<String>("groupSearchFilter"));
        add(new TextField<String>("groupMembershipAttribute"));
        /** user options */
        add(new TextField<String>("userSearchBase").setRequired(true));
        add(new TextField<String>("userNameAttribute"));
        add(new TextField<String>("userFilter"));
        add(new TextField<String>("allUsersSearchFilter"));
        add(new TextField<String>("populatedAttributes"));
        hierarchicalGroupsInit();

        /** privileged account for querying the LDAP server (if needed) */
        add(
                new AjaxCheckBox("bindBeforeGroupSearch") {
                    private static final long serialVersionUID = -6388847010436939988L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // reset any values that were set
                        LDAPAuthenticationPanel ldapAuthenticationPanel =
                                (LDAPAuthenticationPanel)
                                        LDAPUserGroupServicePanel.this.get("authenticationPanel");
                        ldapAuthenticationPanel.resetModel();
                        ldapAuthenticationPanel.setVisible(getModelObject().booleanValue());
                        target.add(ldapAuthenticationPanel);
                    }
                });
        LDAPAuthenticationPanel authPanel = new LDAPAuthenticationPanel("authenticationPanel");
        authPanel.setVisible(model.getObject().isBindBeforeGroupSearch());
        authPanel.setOutputMarkupPlaceholderTag(true);
        add(authPanel);
    }

    private void hierarchicalGroupsInit() {
        // hierarchical groups configurations
        // create fields objects
        final WebMarkupContainer nestedSearchFieldsContainer =
                new WebMarkupContainer(NESTED_SEARCH_FIELDS_CONTAINER);
        nestedSearchFieldsContainer.setOutputMarkupPlaceholderTag(true);
        nestedSearchFieldsContainer.setOutputMarkupId(true);
        add(nestedSearchFieldsContainer);
        Optional<LDAPUserGroupServiceConfig> useNestedOpt =
                Optional.of(this).map(x -> x.configModel).map(IModel::getObject);
        // get initial value for use_nested checkbox
        boolean useNestedActivated =
                useNestedOpt.map(LDAPUserGroupServiceConfig::isUseNestedParentGroups).orElse(false);
        nestedSearchFieldsContainer.setVisible(useNestedActivated);

        final AjaxCheckBox useNestedCheckbox =
                new AjaxCheckBox(USE_NESTED_PARENT_GROUPS) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        AjaxCheckBox cb =
                                (AjaxCheckBox)
                                        LDAPUserGroupServicePanel.this.get(
                                                USE_NESTED_PARENT_GROUPS);
                        boolean value = cb.getModelObject();
                        nestedSearchFieldsContainer.setVisible(value);
                        target.add(nestedSearchFieldsContainer);
                    }
                };
        add(useNestedCheckbox);
        nestedSearchFieldsContainer.add(new TextField<String>(MAX_GROUP_SEARCH_LEVEL));
        nestedSearchFieldsContainer.add(new TextField<String>(NESTED_GROUP_SEARCH_FILTER));
    }
}
