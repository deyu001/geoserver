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

package org.geoserver.wms.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

/** These tests are quite brittle, and don't play well with others */
public class StyleEditPageRenameTest extends GeoServerWicketTestSupport {

    StyleInfo buildingsStyle;
    StyleEditPage edit;

    private static final String STYLE_TO_MOVE_NAME = "testStyle";
    private static final String STYLE_TO_MOVE_FILENAME = "testMoveStyle.sld";
    StyleInfo styleInfoToMove;

    @Before
    public void setUp() throws Exception {
        Catalog catalog = getCatalog();
        login();

        buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        if (buildingsStyle == null) {
            // undo the rename performed in one of the test methods
            StyleInfo si = catalog.getStyleByName("BuildingsNew");
            if (si != null) {
                si.setName(MockData.BUILDINGS.getLocalPart());
                catalog.save(si);
            }
            buildingsStyle = catalog.getStyleByName(MockData.BUILDINGS.getLocalPart());
        }

        edit = new StyleEditPage(buildingsStyle);
        tester.startPage(edit);
        styleInfoToMove = catalog.getStyleByName("testStyle");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle(
                STYLE_TO_MOVE_NAME, STYLE_TO_MOVE_FILENAME, this.getClass(), getCatalog());
    }

    // Test that a user can non-destructively move the style out of a workspace.
    @Test
    public void testMoveFromWorkspace() throws Exception {
        // Move into sf
        Catalog catalog = getCatalog();
        StyleInfo si = catalog.getStyleByName(STYLE_TO_MOVE_NAME);
        si.setWorkspace(catalog.getWorkspaceByName("sf"));
        catalog.save(si);

        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        // verify move to workspace was successful
        assertEquals(
                Resource.Type.UNDEFINED, dataDir.get("styles/" + STYLE_TO_MOVE_FILENAME).getType());
        assertEquals(
                Resource.Type.RESOURCE,
                dataDir.get("workspaces/sf/styles/" + STYLE_TO_MOVE_FILENAME).getType());

        // test moving back to default workspace using the UI
        edit = new StyleEditPage(si);
        tester.startPage(edit);

        // Before the edit, the style should have one <FeatureTypeStyle> and be in the sf workspace
        assertEquals(1, si.getStyle().featureTypeStyles().size());
        assertEquals("sf", si.getWorkspace().getName());

        FormTester form = tester.newFormTester("styleForm", false);

        // Update the workspace (select "sf" from the dropdown)
        form.setValue("context:panel:workspace", "");

        // Submit the form and verify that both the new workspace and new rawStyle saved.
        form.submit();

        si = getCatalog().getStyleByName(STYLE_TO_MOVE_NAME);
        assertNotNull(si);
        assertNull(si.getWorkspace());

        // verify move out of the workspace was successful
        assertEquals(
                Resource.Type.RESOURCE, dataDir.get("styles/" + STYLE_TO_MOVE_FILENAME).getType());
        assertEquals(
                Resource.Type.UNDEFINED,
                dataDir.get("workspaces/sf/styles/" + STYLE_TO_MOVE_FILENAME).getType());
    }

    @Test
    public void testGenerateTemplateFrenchLocale() throws Exception {
        final Session session = tester.getSession();
        try {
            session.clear();
            session.setLocale(Locale.FRENCH);

            StyleEditPage edit = new StyleEditPage(buildingsStyle);
            tester.startPage(edit);
            // print(tester.getLastRenderedPage(), true, true);

            // test the copy style link
            tester.newFormTester("styleForm").select("context:panel:templates", 1);
            tester.executeAjaxEvent("styleForm:context:panel:templates", "onchange");
            Component generateLink =
                    tester.getComponentFromLastRenderedPage("styleForm:context:panel:generate");
            tester.executeAjaxEvent(generateLink, "onClick");
            // check single quote in the message has been escaped
            assertTrue(tester.getLastResponseAsString().contains("l\\'éditeur"));
        } finally {
            session.clear();
            session.setLocale(Locale.getDefault());
        }
    }

    @Test
    public void testCopyStyleFrenchLocale() throws Exception {
        final Session session = tester.getSession();
        try {
            session.clear();
            session.setLocale(Locale.FRENCH);

            StyleEditPage edit = new StyleEditPage(buildingsStyle);
            tester.startPage(edit);
            // print(tester.getLastRenderedPage(), true, true);

            // test the copy style link
            tester.newFormTester("styleForm").select("context:panel:existingStyles", 1);
            tester.executeAjaxEvent("styleForm:context:panel:existingStyles", "onchange");
            Component copyLink =
                    tester.getComponentFromLastRenderedPage("styleForm:context:panel:copy");
            tester.executeAjaxEvent(copyLink, "onClick");
            // check single quote in the message has been escaped
            assertTrue(tester.getLastResponseAsString().contains("l\\'éditeur"));
        } finally {
            session.clear();
            session.setLocale(Locale.getDefault());
        }
    }
}
