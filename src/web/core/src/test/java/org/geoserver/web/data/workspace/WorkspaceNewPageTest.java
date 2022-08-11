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

package org.geoserver.web.data.workspace;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.security.AccessDataRuleInfoManager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;

public class WorkspaceNewPageTest extends GeoServerWicketTestSupport {

    @Before
    public void init() {
        login();
        tester.startPage(WorkspaceNewPage.class);
        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertNoErrorMessage();

        tester.assertComponent("form:tabs:panel:name", TextField.class);
        tester.assertComponent("form:tabs:panel:uri", TextField.class);
    }

    @Test
    public void testNameRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }

    @Test
    public void testURIRequired() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "test");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'uri' is required."});
    }

    @Test
    public void testValid() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "abc");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.setValue("tabs:panel:default", "true");
        form.submit("submit");
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        assertEquals("abc", getCatalog().getDefaultWorkspace().getName());
    }

    @Test
    public void testInvalidURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "def");
        form.setValue("tabs:panel:uri", "not a valid uri");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(new String[] {"Invalid URI syntax: not a valid uri"});
    }

    @Test
    public void testInvalidName() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "default");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {"Invalid workspace name: \"default\" is a reserved keyword"});
    }

    @Test
    public void testDuplicateURI() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "def");
        form.setValue("tabs:panel:uri", MockTestData.CITE_URI);
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {
                    "Namespace with URI '" + MockTestData.CITE_URI + "' already exists."
                });

        // Make sure the workspace doesn't get added if the namespace fails
        assertNull(getCatalog().getWorkspaceByName("def"));
        assertNull(getCatalog().getNamespaceByPrefix("def"));
    }

    @Test
    public void testDuplicateName() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", MockTestData.CITE_PREFIX);
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        form.submit("submit");

        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {
                    "Workspace named '" + MockTestData.CITE_PREFIX + "' already exists."
                });
    }

    @Test
    public void addIsolatedWorkspacesWithSameNameSpace() {
        Catalog catalog = getCatalog();
        // create the first workspace
        createWorkspace("test_a", "http://www.test.org", false);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        // check that the correct objects were created in the catalog
        assertThat(catalog.getWorkspaceByName("test_a"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_a").isIsolated(), is(false));
        assertThat(catalog.getNamespaceByPrefix("test_a"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_a").isIsolated(), is(false));
        assertThat(catalog.getNamespaceByURI("http://www.test.org"), notNullValue());
        // try to create non isolated workspace with the same namespace
        createWorkspace("test_b", "http://www.test.org", false);
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertErrorMessages(
                new String[] {"Namespace with URI 'http://www.test.org' already exists."});
        // check that no objects were created in the catalog
        assertThat(catalog.getWorkspaceByName("test_b"), nullValue());
        assertThat(catalog.getNamespaceByPrefix("test_b"), nullValue());
        assertThat(catalog.getNamespaceByURI("http://www.test.org"), notNullValue());
        assertThat(catalog.getNamespaceByURI("http://www.test.org").getPrefix(), is("test_a"));
        // create isolated workspace with the same namespace
        createWorkspace("test_b", "http://www.test.org", true);
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();
        // check that no objects were created in the catalog
        assertThat(catalog.getWorkspaceByName("test_b"), notNullValue());
        assertThat(catalog.getWorkspaceByName("test_b").isIsolated(), is(true));
        assertThat(catalog.getNamespaceByPrefix("test_b"), notNullValue());
        assertThat(catalog.getNamespaceByPrefix("test_b").isIsolated(), is(true));
        assertThat(catalog.getNamespaceByPrefix("test_b").getURI(), is("http://www.test.org"));
        assertThat(catalog.getNamespaceByURI("http://www.test.org").getPrefix(), is("test_a"));
        assertThat(catalog.getNamespaceByURI("http://www.test.org").isIsolated(), is(false));
    }

    /**
     * Helper method that submits a new workspace using the provided parameters.
     *
     * @param name workspace name
     * @param namespace workspace namespace URI
     * @param isolated TRUE if the workspace should be isolated, otherwise false
     */
    private void createWorkspace(String name, String namespace, boolean isolated) {
        // make sure the form is initiated
        init();
        // get the workspace creation form
        FormTester form = tester.newFormTester("form");
        // fill the form with the provided values
        form.setValue("tabs:panel:name", name);
        form.setValue("tabs:panel:uri", namespace);
        form.setValue("tabs:panel:isolated", isolated);
        // submit the form
        form.submit("submit");
    }

    @Test
    public void testSecurityTabLoad() {
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "abc");
        form.setValue("tabs:panel:uri", "http://www.geoserver.org");
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        tester.assertComponent("form:tabs:panel:listContainer", WebMarkupContainer.class);
        tester.assertComponent("form:tabs:panel:listContainer:selectAll", CheckBox.class);
        tester.assertComponent("form:tabs:panel:listContainer:rules", ListView.class);
        tester.assertRenderedPage(WorkspaceNewPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testCreateWsWithAccessRules() throws IOException {
        AccessDataRuleInfoManager manager = new AccessDataRuleInfoManager();
        WorkspaceInfo wsInfo = null;
        FormTester form = tester.newFormTester("form");
        form.setValue("tabs:panel:name", "cba");
        form.setValue("tabs:panel:uri", "http://www.geoserver2.org");
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        form.setValue("tabs:panel:listContainer:rules:0:admin", true);
        form.submit("submit");
        tester.assertNoErrorMessage();
        wsInfo = getCatalog().getWorkspaceByName("cba");
        assertEquals("cba", wsInfo.getName());
        assertEquals(1, manager.getResourceRule(wsInfo.getName(), wsInfo).size());
    }

    @Test
    public void testSecurityTabInactiveWithNoDeafaultAccessManager() {
        TestResourceAccessManager manager = new TestResourceAccessManager();
        SecureCatalogImpl oldSc = (SecureCatalogImpl) GeoServerExtensions.bean("secureCatalog");
        SecureCatalogImpl sc =
                new SecureCatalogImpl(getCatalog(), manager) {

                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        applicationContext.getBeanFactory().destroyBean("secureCatalog");
        GeoServerExtensionsHelper.clear();
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);
        tester.startPage(WorkspaceNewPage.class);
        try {
            tester.newFormTester("form");
            TabbedPanel tabs = (TabbedPanel) tester.getComponentFromLastRenderedPage("form:tabs");
            assertEquals(1, tabs.getTabs().size());
        } finally {
            applicationContext.getBeanFactory().destroyBean("secureCatalog");
            GeoServerExtensionsHelper.clear();
            GeoServerExtensionsHelper.singleton("secureCatalog", oldSc, SecureCatalogImpl.class);
        }
    }
}
