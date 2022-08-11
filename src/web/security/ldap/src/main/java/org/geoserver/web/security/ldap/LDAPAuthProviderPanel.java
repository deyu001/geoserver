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

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;
import javax.naming.AuthenticationException;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.ldap.LDAPAuthenticationProvider;
import org.geoserver.security.ldap.LDAPSecurityProvider;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanel;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.util.MapModel;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Configuration panel for {@link LDAPAuthenticationProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPAuthProviderPanel extends AuthenticationProviderPanel<LDAPSecurityServiceConfig> {

    private static final long serialVersionUID = 4772173006888418298L;

    public LDAPAuthProviderPanel(String id, IModel<LDAPSecurityServiceConfig> model) {
        super(id, model);

        add(new TextField<String>("serverURL").setRequired(true));
        add(new CheckBox("useTLS"));
        add(new TextField<String>("userDnPattern"));
        add(new TextField<String>("userFilter"));
        add(new TextField<String>("userFormat"));

        boolean useLdapAuth = model.getObject().getUserGroupServiceName() == null;
        add(
                new AjaxCheckBox("useLdapAuthorization", new Model<>(useLdapAuth)) {

                    private static final long serialVersionUID = 2060279075143716273L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        WebMarkupContainer c =
                                (WebMarkupContainer)
                                        LDAPAuthProviderPanel.this.get(
                                                "authorizationPanelContainer");

                        // reset any values that were set
                        ((AuthorizationPanel) c.get("authorizationPanel")).resetModel();

                        // remove the old panel
                        c.remove("authorizationPanel");

                        // add the new panel
                        c.add(createAuthorizationPanel("authorizationPanel", getModelObject()));

                        target.add(c);
                    }
                });
        add(
                new WebMarkupContainer("authorizationPanelContainer")
                        .add(createAuthorizationPanel("authorizationPanel", useLdapAuth))
                        .setOutputMarkupId(true));

        add(new TestLDAPConnectionPanel("testCx"));
    }

    AuthorizationPanel createAuthorizationPanel(String id, boolean useLDAP) {
        return useLDAP ? new LDAPAuthorizationPanel(id) : new UserGroupAuthorizationPanel(id);
    }

    abstract class AuthorizationPanel extends FormComponentPanel<HashMap<String, Object>> {

        private static final long serialVersionUID = -2021795762927385164L;

        public AuthorizationPanel(String id) {
            super(id, new Model<>());
        }

        public abstract void resetModel();
    }

    class UserGroupAuthorizationPanel extends AuthorizationPanel {

        private static final long serialVersionUID = 2464048864034610244L;

        public UserGroupAuthorizationPanel(String id) {
            super(id);

            add(new UserGroupServiceChoice("userGroupServiceName"));
        }

        @Override
        public void resetModel() {
            get("userGroupServiceName").setDefaultModelObject(null);
        }
    }

    class LDAPAuthorizationPanel extends AuthorizationPanel {

        private static final long serialVersionUID = 7541432269535150812L;
        private static final String USE_NESTED_PARENT_GROUPS = "useNestedParentGroups";
        private static final String MAX_GROUP_SEARCH_LEVEL = "maxGroupSearchLevel";
        private static final String NESTED_GROUP_SEARCH_FILTER = "nestedGroupSearchFilter";
        private static final String NESTED_SEARCH_FIELDS_CONTAINER = "nestedSearchFieldsContainer";

        public LDAPAuthorizationPanel(String id) {
            super(id);
            setOutputMarkupId(true);
            add(new CheckBox("bindBeforeGroupSearch"));
            add(new TextField<String>("adminGroup"));
            add(new TextField<String>("groupAdminGroup"));
            add(new TextField<String>("groupSearchBase"));
            add(new TextField<String>("groupSearchFilter"));
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            hierarchicalGroupsinit();
        }

        private void hierarchicalGroupsinit() {
            // hierarchical groups configurations
            Optional<LDAPSecurityServiceConfig> useNestedOpt =
                    Optional.of(this)
                            .map(
                                    x -> {
                                        try {
                                            return x.getForm();
                                        } catch (WicketRuntimeException ex) {
                                            // no form
                                        }
                                        return null;
                                    })
                            .map(Form::getModel)
                            .map(IModel::getObject)
                            .filter(x -> x instanceof LDAPSecurityServiceConfig)
                            .map(x -> (LDAPSecurityServiceConfig) x);
            // get initial value for use_nested checkbox
            boolean useNestedActivated =
                    useNestedOpt
                            .map(LDAPSecurityServiceConfig::isUseNestedParentGroups)
                            .orElse(false);
            // create fields objects
            final WebMarkupContainer nestedSearchFieldsContainer =
                    new WebMarkupContainer(NESTED_SEARCH_FIELDS_CONTAINER);
            nestedSearchFieldsContainer.setOutputMarkupPlaceholderTag(true);
            nestedSearchFieldsContainer.setOutputMarkupId(true);
            nestedSearchFieldsContainer.setVisible(useNestedActivated);
            add(nestedSearchFieldsContainer);
            final TextField<String> maxLevelField = new TextField<>(MAX_GROUP_SEARCH_LEVEL);
            final TextField<String> nestedGroupSearchFilterField =
                    new TextField<>(NESTED_GROUP_SEARCH_FILTER);
            final AjaxCheckBox useNestedCheckbox =
                    new AjaxCheckBox(USE_NESTED_PARENT_GROUPS) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            // get the checkbox boolean value and set visibility for fields
                            AjaxCheckBox cb =
                                    (AjaxCheckBox)
                                            LDAPAuthorizationPanel.this.get(
                                                    USE_NESTED_PARENT_GROUPS);
                            boolean value = cb.getModelObject();
                            nestedSearchFieldsContainer.setVisible(value);
                            target.add(nestedSearchFieldsContainer);
                        }
                    };
            add(useNestedCheckbox);
            nestedSearchFieldsContainer.add(maxLevelField);
            nestedSearchFieldsContainer.add(nestedGroupSearchFilterField);
        }

        @Override
        public void resetModel() {
            get("bindBeforeGroupSearch").setDefaultModelObject(null);
            get("adminGroup").setDefaultModelObject(null);
            get("groupAdminGroup").setDefaultModelObject(null);
            get("groupSearchBase").setDefaultModelObject(null);
            get("groupSearchFilter").setDefaultModelObject(null);
            // hierarchical groups reset
            get(USE_NESTED_PARENT_GROUPS).setDefaultModelObject(false);
        }
    }

    class TestLDAPConnectionPanel extends FormComponentPanel<HashMap<String, Object>> {

        private static final long serialVersionUID = 5433983389877706266L;

        public TestLDAPConnectionPanel(String id) {
            super(id, new Model<>(new HashMap<>()));

            add(new TextField<>("username", new MapModel<>(getModel().getObject(), "username")));
            add(
                    new PasswordTextField(
                                    "password", new MapModel<>(getModel().getObject(), "password"))
                            .setRequired(false));
            add(
                    new AjaxSubmitLink("test") {

                        private static final long serialVersionUID = 2373404292655355758L;

                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            // since this is not a regular form submit we have to manually update
                            // models
                            // of form components we care about
                            ((FormComponent<?>) TestLDAPConnectionPanel.this.get("username"))
                                    .processInput();
                            ((FormComponent<?>) TestLDAPConnectionPanel.this.get("password"))
                                    .processInput();

                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("serverURL"))
                                    .processInput();
                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("useTLS"))
                                    .processInput();

                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("userDnPattern"))
                                    .processInput();
                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("userFilter"))
                                    .processInput();
                            ((FormComponent<?>) LDAPAuthProviderPanel.this.get("userFormat"))
                                    .processInput();

                            String username =
                                    (String)
                                            ((FormComponent<?>)
                                                            TestLDAPConnectionPanel.this.get(
                                                                    "username"))
                                                    .getConvertedInput();
                            String password =
                                    (String)
                                            ((FormComponent<?>)
                                                            TestLDAPConnectionPanel.this.get(
                                                                    "password"))
                                                    .getConvertedInput();

                            LDAPSecurityServiceConfig ldapConfig =
                                    (LDAPSecurityServiceConfig) getForm().getModelObject();
                            doTest(ldapConfig, username, password);

                            target.add(getPage().get("topFeedback"));
                        }

                        void doTest(
                                LDAPSecurityServiceConfig ldapConfig,
                                String username,
                                String password) {

                            try {

                                if (ldapConfig.getUserDnPattern() == null
                                        && ldapConfig.getUserFilter() == null) {
                                    error("Neither user dn pattern or user filter specified");
                                    return;
                                }

                                LDAPSecurityProvider provider =
                                        new LDAPSecurityProvider(getSecurityManager());
                                LDAPAuthenticationProvider authProvider =
                                        (LDAPAuthenticationProvider)
                                                provider.createAuthenticationProvider(ldapConfig);
                                Authentication authentication =
                                        authProvider.authenticate(
                                                new UsernamePasswordAuthenticationToken(
                                                        username, password));
                                if (authentication == null || !authentication.isAuthenticated()) {
                                    throw new AuthenticationException(
                                            "Cannot authenticate " + username);
                                }

                                provider.destroy(null);
                                info(
                                        new StringResourceModel(
                                                        LDAPAuthProviderPanel.class.getSimpleName()
                                                                + ".connectionSuccessful")
                                                .getObject());
                            } catch (Exception e) {
                                error(e);
                                LOGGER.log(Level.WARNING, e.getMessage(), e);
                            }
                        }
                    }.setDefaultFormProcessing(false));
        }
    }
}
