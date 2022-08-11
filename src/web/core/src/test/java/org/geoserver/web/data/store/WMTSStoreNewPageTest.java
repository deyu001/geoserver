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

package org.geoserver.web.data.store;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class WMTSStoreNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = true;

    @Before
    public void init() {
        Logging.getLogger("org.geoserver").setLevel(Level.FINE);
        Logging.getLogger("org.vfny.geoserver").setLevel(Level.FINE);
        Logging.getLogger("org.geotools").setLevel(Level.FINE);
    }

    private WMTSStoreNewPage startPage() {

        login();
        final WMTSStoreNewPage page = new WMTSStoreNewPage();
        tester.startPage(page);

        if (debugMode) {
            print(page, true, true);
        }

        return page;
    }

    /** A kind of smoke test that only asserts the page is rendered when first loaded */
    @Test
    public void testPageRendersOnLoad() {

        startPage();

        tester.assertComponent("form:workspacePanel", WorkspacePanel.class);
    }

    @Test
    public void testInitialModelState() {

        WMTSStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("form:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "form:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
    }

    @Test
    public void testSaveNewStore() {

        WMTSStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        final Catalog catalog = getCatalog();
        WMTSStoreInfo info = catalog.getFactory().createWebMapTileServer();
        info.setName("foo");

        tester.assertNoErrorMessage();

        FormTester form = tester.newFormTester("form");
        form.select("workspacePanel:border:border_body:paramValue", 4);
        Component wsDropDown =
                tester.getComponentFromLastRenderedPage(
                        "form:workspacePanel:border:border_body:paramValue");
        tester.executeAjaxEvent(wsDropDown, "change");
        form.setValue("namePanel:border:border_body:paramValue", "foo");
        form.setValue("capabilitiesURL:border:border_body:paramValue", "http://foo");

        tester.clickLink("form:save", true);
        tester.assertErrorMessages("WMTS Connection test failed: foo");
        catalog.save(info);

        assertNotNull(info.getId());

        WMTSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        assertNotNull(expandedStore.getId());
        assertNotNull(expandedStore.getCatalog());

        catalog.validate(expandedStore, false).throwIfInvalid();
    }

    @Test
    @Ignore
    public void testSaveNewStoreEntityExpansion() throws Exception {

        WMTSStoreNewPage page = startPage();

        assertNull(page.getDefaultModelObject());

        final Catalog catalog = getCatalog();
        WMTSStoreInfo info = getCatalog().getFactory().createWebMapTileServer();
        URL url = getClass().getResource("WMTSGetCapabilities.xml");
        assertNotNull(url);
        info.setName("bar");

        tester.assertNoErrorMessage();

        FormTester form = tester.newFormTester("form");
        form.select("workspacePanel:border:border_body:paramValue", 4);
        Component wsDropDown =
                tester.getComponentFromLastRenderedPage(
                        "form:workspacePanel:border:border_body:paramValue");
        tester.executeAjaxEvent(wsDropDown, "change");
        form.setValue("namePanel:border:border_body:paramValue", "bar");
        form.setValue("capabilitiesURL:border:border_body:paramValue", url.toExternalForm());

        tester.clickLink("form:save", true);
        tester.assertErrorMessages("Connection test failed: Error while parsing XML.");

        // make sure clearing the catalog does not clear the EntityResolver
        getGeoServer().reload();
        tester.clickLink("form:save", true);
        tester.assertErrorMessages("Connection test failed: Error while parsing XML.");

        catalog.save(info);

        assertNotNull(info.getId());

        WMTSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        assertNotNull(expandedStore.getId());
        assertNotNull(expandedStore.getCatalog());

        catalog.validate(expandedStore, false).throwIfInvalid();
    }
}
