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
import java.util.Map;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.CreateLdapServerRule;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.MapModel;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
import org.geoserver.security.ldap.LDAPTestUtils;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

/** @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it" */
@CreateLdapServer(
    transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
    allowAnonymousAccess = true
)
@CreateDS(
    name = "myDS",
    partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)}
)
@ApplyLdifFiles({"data.ldif"})
public class LDAPAuthProviderPanelTest extends AbstractSecurityWicketTestSupport {

    private static final String USER_FORMAT = "uid={0},ou=People,dc=example,dc=com";

    private static final String USER_FILTER = "(telephonenumber=1)";

    private static final String USER_DN_PATTERN = "uid={0},ou=People";

    LDAPAuthProviderPanel current;

    String relBase = "panel:";
    String base = "form:" + relBase;

    LDAPSecurityServiceConfig config;

    FeedbackPanel feedbackPanel = null;

    private static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    private static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;

    @ClassRule public static CreateLdapServerRule serverRule = new CreateLdapServerRule();

    @After
    public void tearDown() throws Exception {}

    protected void setupPanel(
            final String userDnPattern,
            String userFilter,
            String userFormat,
            String userGroupService) {
        config = new LDAPSecurityServiceConfig();
        config.setName("test");
        config.setServerURL(getServerURL());
        config.setUserDnPattern(userDnPattern);
        config.setUserFilter(userFilter);
        config.setUserFormat(userFormat);
        config.setUserGroupServiceName(userGroupService);
        setupPanel(config);
    }

    private String getServerURL() {
        return ldapServerUrl + ":" + serverRule.getLdapServer().getPort() + "/" + basePath;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // disable url parameter encoding for these tests
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setEncryptingUrlParams(false);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected void setupPanel(LDAPSecurityServiceConfig theConfig) {
        this.config = theConfig;
        tester.startPage(
                new LDAPFormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 7319919840443122283L;

                            public Component buildComponent(String id) {

                                return current = new LDAPAuthProviderPanel(id, new Model<>(config));
                            };
                        },
                        new CompoundPropertyModel<>(config)));
    }

    @Test
    public void testTestConnectionWithDnLookup() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(USER_DN_PATTERN, null, null, null);
        testSuccessfulConnection();
    }

    @Test
    public void testTestConnectionWitUserGroupService() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(USER_DN_PATTERN, null, null, "default");
        testSuccessfulConnection();
    }

    @Test
    public void testTestConnectionWithUserFilter() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(null, USER_FILTER, USER_FORMAT, null);
        testSuccessfulConnection();
    }

    @Test
    public void testTestConnectionFailedWithDnLookup() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(USER_DN_PATTERN, null, null, null);
        testFailedConnection();
    }

    @Test
    public void testTestConnectionFailedWithUserFilter() throws Exception {
        serverRule.getDirectoryService().setAllowAnonymousAccess(true);
        setupPanel(null, USER_FILTER, USER_FORMAT, null);
        testFailedConnection();
    }

    private void testSuccessfulConnection() throws Exception {
        authenticate("admin", "admin");

        tester.assertNoErrorMessage();
        String success =
                new StringResourceModel(
                                LDAPAuthProviderPanel.class.getSimpleName()
                                        + ".connectionSuccessful")
                        .getObject();
        tester.assertInfoMessages(new String[] {success});
    }

    private void testFailedConnection() throws Exception {
        authenticate("admin", "wrong");

        tester.assertNoInfoMessage();
        tester.assertContains("AuthenticationException");
    }

    private void authenticate(String username, String password) {
        TextField<?> userField =
                ((TextField<?>) tester.getComponentFromLastRenderedPage(base + "testCx:username"));
        userField.setDefaultModel(new Model<>(username));
        TextField<?> passwordField =
                ((TextField<?>) tester.getComponentFromLastRenderedPage(base + "testCx:password"));
        passwordField.setDefaultModel(new Model<>(password));

        Map<String, String> map = new HashMap<>();
        map.put("username", username);
        map.put("password", password);

        tester.getComponentFromLastRenderedPage("form:panel:testCx")
                .setDefaultModel(new MapModel<>(map));

        tester.clickLink(base + "testCx:test", true);
    }

    private class LDAPFormTestPage extends FormTestPage {
        public LDAPFormTestPage(ComponentBuilder builder, CompoundPropertyModel<Object> model) {
            super(builder, model);
        }

        private static final long serialVersionUID = 3150973967583096118L;

        @Override
        protected void onBeforeRender() {
            feedbackPanel = new FeedbackPanel("topFeedback");
            feedbackPanel.setOutputMarkupId(true);
            addOrReplace(feedbackPanel);
            super.onBeforeRender();
        }
    }
}
