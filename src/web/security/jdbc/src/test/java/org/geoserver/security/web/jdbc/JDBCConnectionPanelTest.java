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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class JDBCConnectionPanelTest extends AbstractSecurityWicketTestSupport {

    JDBCConnectionPanel<JDBCSecurityServiceConfig> current;

    String relBase = "panel:cxPanelContainer:cxPanel:";
    String base = "form:" + relBase;

    JDBCSecurityServiceConfig config;

    protected void setupPanel(final boolean jndi) {
        config = new JDBCUserGroupServiceConfig();
        config.setJndi(jndi);
        setupPanel(config);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // disable url parameter encoding for these tests
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setEncryptingUrlParams(false);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected void setupPanel(JDBCSecurityServiceConfig theConfig) {
        this.config = theConfig;
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(String id) {
                                return current = new JDBCConnectionPanel<>(id, new Model<>(config));
                            };
                        },
                        new CompoundPropertyModel<>(config)));
    }

    @Test
    public void testJNDI() throws Exception {
        setupPanel(true);
        tester.assertRenderedPage(FormTestPage.class);
        assertTrue(config.isJndi());

        assertVisibility(true);

        FormTester ftester = tester.newFormTester("form");
        ftester.setValue(relBase + "jndiName", "jndiurl");
        ftester.submit();

        tester.assertNoErrorMessage();
        assertEquals("jndiurl", config.getJndiName());
    }

    @Test
    public void testConnectionTestJNDI() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setJndi(true);
        theConfig.setJndiName("jndiurl");

        setupPanel(theConfig);
        tester.assertRenderedPage(FormTestPage.class);
        tester.clickLink("form:panel:cxTest", true);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
    }

    @Test
    public void testBasic() throws Exception {
        setupPanel(false);
        tester.assertRenderedPage(FormTestPage.class);
        assertFalse(config.isJndi());

        assertVisibility(false);

        FormTester ftester = tester.newFormTester("form");
        ftester.setValue(relBase + "userName", "user1");
        ftester.setValue(relBase + "password", "pw");
        ftester.setValue(relBase + "driverClassName", "org.h2.Driver");
        ftester.setValue(relBase + "connectURL", "jdbc:h2");
        ftester.submit();

        tester.assertNoErrorMessage();
        assertEquals("user1", config.getUserName());
        assertEquals("pw", config.getPassword());
        assertEquals("org.h2.Driver", config.getDriverClassName());
        assertEquals("jdbc:h2", config.getConnectURL());
    }

    @Test
    public void testConncetionTestBasic() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setUserName("user1");
        theConfig.setPassword("pw");
        theConfig.setDriverClassName("org.h2.Driver");
        theConfig.setConnectURL("jdbc:foo");

        setupPanel(theConfig);
        tester.assertRenderedPage(FormTestPage.class);

        tester.clickLink("form:panel:cxTest", true);
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
    }

    @Test
    public void testConnectionTestBasicOK() throws Exception {
        JDBCUserGroupServiceConfig theConfig = new JDBCUserGroupServiceConfig();
        theConfig.setUserName("user1");
        theConfig.setPassword("pw");
        theConfig.setDriverClassName("org.h2.Driver");
        theConfig.setConnectURL("jdbc:h2:file:target/db");

        setupPanel(theConfig);
        tester.assertRenderedPage(FormTestPage.class);
        tester.clickLink("form:panel:cxTest", true);
        assertEquals(1, tester.getMessages(FeedbackMessage.INFO).size());
    }

    protected void assertVisibility(boolean jndi) {
        if (jndi) {
            tester.assertComponent(base + "jndiName", TextField.class);
            tester.assertVisible(base + "jndiName");
        } else {
            for (String c :
                    Arrays.asList(
                            new String[] {
                                "driverClassName", "connectURL", "userName", "password"
                            })) {
                tester.assertComponent(base + c, FormComponent.class);
                tester.assertVisible(base + c);
            }
        }
    }
}
