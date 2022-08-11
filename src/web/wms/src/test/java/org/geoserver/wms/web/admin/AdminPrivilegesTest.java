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

package org.geoserver.wms.web.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.web.data.StyleEditPage;
import org.geoserver.wms.web.data.StyleNewPage;
import org.geoserver.wms.web.data.StylePage;
import org.junit.Test;

public class AdminPrivilegesTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite", "cite", null, Arrays.asList("ROLE_CITE_ADMIN"));
        addUser("sf", "sf", null, Arrays.asList("ROLE_SF_ADMIN"));

        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");
        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_CITE_ADMIN");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_SF_ADMIN");

        Catalog cat = getCatalog();

        // add two workspace specific styles
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("sf_style");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("sf.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("cite_style");
        s.setWorkspace(cat.getWorkspaceByName("cite"));
        s.setFilename("cite.sld");
        cat.add(s);
    }

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    @Test
    public void testStyleAllPageAsAdmin() throws Exception {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        tester.debugComponentTrees();
        Catalog cat = getCatalog();

        DataView view =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(cat.getStyles().size(), view.getItemCount());
    }

    @Test
    @SuppressWarnings("PMD.ForLoopCanBeForeach")
    public void testStyleAllPage() throws Exception {
        loginAsCite();

        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);

        @SuppressWarnings("unchecked")
        DataView<Object> view =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        // logged in as CITE, will only see styles in this workspace
        int expected = 1;

        AdminRequest.start(new Object());
        assertEquals(expected, view.getItemCount());

        for (Iterator<Item<Object>> it = view.getItems(); it.hasNext(); ) {
            String name =
                    it.next()
                            .get("itemProperties:0:component:link:label")
                            .getDefaultModelObjectAsString();
            assertNotEquals("sf_style", name);
        }
    }

    @Test
    public void testStyleNewPageAsAdmin() throws Exception {
        login();

        tester.startPage(StyleNewPage.class);
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertModelValue("styleForm:context:panel:workspace", null);

        DropDownChoice choice =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "styleForm:context:panel:workspace");
        assertTrue(choice.isNullValid());
        assertFalse(choice.isRequired());
    }

    @Test
    public void testStyleNewPage() throws Exception {
        loginAsCite();

        tester.startPage(StyleNewPage.class);
        tester.assertRenderedPage(StyleNewPage.class);

        Catalog cat = getCatalog();
        tester.assertModelValue(
                "styleForm:context:panel:workspace", cat.getWorkspaceByName("cite"));

        DropDownChoice choice =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "styleForm:context:panel:workspace");
        assertFalse(choice.isNullValid());
        assertTrue(choice.isRequired());
    }

    @Test
    public void testStyleEditPageGlobal() throws Exception {
        loginAsCite();

        tester.startPage(
                StyleEditPage.class, new PageParameters().add(StyleEditPage.NAME, "point"));
        tester.assertRenderedPage(StyleEditPage.class);

        // assert all form components disabled except for cancel
        assertFalse(
                tester.getComponentFromLastRenderedPage("styleForm:context:panel:name")
                        .isEnabled());
        assertFalse(
                tester.getComponentFromLastRenderedPage("styleForm:context:panel:workspace")
                        .isEnabled());
        assertFalse(
                tester.getComponentFromLastRenderedPage("styleForm:context:panel:copy")
                        .isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("cancel").isEnabled());
    }
}
