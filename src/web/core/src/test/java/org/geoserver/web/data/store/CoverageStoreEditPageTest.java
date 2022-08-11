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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class CoverageStoreEditPageTest extends GeoServerWicketTestSupport {

    CoverageStoreInfo coverageStore;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void init() throws IOException {
        login();

        coverageStore =
                getCatalog()
                        .getStoreByName(
                                MockData.TASMANIA_BM.getLocalPart(), CoverageStoreInfo.class);
        if (coverageStore == null) {
            // revert the bluemable modified change
            Catalog cat = getCatalog();
            CoverageStoreInfo c = cat.getCoverageStoreByName("BlueMarbleModified");
            if (c != null) {
                c.setName("BlueMarble");
                cat.save(c);
            }
            coverageStore =
                    getCatalog()
                            .getStoreByName(
                                    MockData.TASMANIA_BM.getLocalPart(), CoverageStoreInfo.class);
        }
        tester.startPage(new CoverageStoreEditPage(coverageStore.getId()));
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(CoverageStoreEditPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("rasterStoreForm:storeType", "GeoTIFF");
        tester.assertModelValue(
                "rasterStoreForm:namePanel:border:border_body:paramValue", "BlueMarble");
    }

    @Test
    public void testChangeName() {
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:border_body:paramValue", "BlueMarbleModified");
        form.submit();
        tester.clickLink("rasterStoreForm:save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(StorePage.class);
        assertNotNull(getCatalog().getStoreByName("BlueMarbleModified", CoverageStoreInfo.class));
    }

    @Test
    public void testChangeNameApply() {
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:border_body:paramValue", "BlueMarbleModified");
        form.submit("apply");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(CoverageStoreEditPage.class);
        assertNotNull(getCatalog().getStoreByName("BlueMarbleModified", CoverageStoreInfo.class));
    }

    @Test
    public void testNameRequired() {
        FormTester form = tester.newFormTester("rasterStoreForm");
        form.setValue("namePanel:border:border_body:paramValue", null);
        form.submit();
        tester.clickLink("rasterStoreForm:save");

        tester.assertRenderedPage(CoverageStoreEditPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Data Source Name' is required."});
    }

    /**
     * Test that changing a datastore's workspace updates the datastore's "namespace" parameter as
     * well as the namespace of its previously configured resources
     */
    @Test
    public void testWorkspaceSyncsUpWithNamespace() {
        final Catalog catalog = getCatalog();

        final FormTester formTester = tester.newFormTester("rasterStoreForm");

        final String wsDropdownPath =
                "rasterStoreForm:workspacePanel:border:border_body:paramValue";

        tester.assertModelValue(wsDropdownPath, catalog.getWorkspaceByName(MockData.WCS_PREFIX));

        // select the fifth item in the drop down, which is the cdf workspace
        formTester.select("workspacePanel:border:border_body:paramValue", 2);

        // weird on this test I need to both call form.submit() and also simulate clicking on the
        // ajax "save" link for the model to be updated. On a running geoserver instance it works ok
        // though
        formTester.submit();

        final boolean isAjax = true;
        tester.clickLink("rasterStoreForm:save", isAjax);

        // did the save finish normally?
        tester.assertRenderedPage(StorePage.class);

        CoverageStoreInfo store = catalog.getCoverageStore(coverageStore.getId());
        WorkspaceInfo workspace = store.getWorkspace();
        assertNotEquals(MockData.WCS_PREFIX, workspace.getName());

        // was the namespace for the datastore resources updated?
        List<CoverageInfo> resourcesByStore;
        resourcesByStore = catalog.getResourcesByStore(store, CoverageInfo.class);

        assertFalse(resourcesByStore.isEmpty());

        for (CoverageInfo cv : resourcesByStore) {
            assertEquals(
                    "Namespace for " + cv.getName() + " was not updated",
                    workspace.getName(),
                    cv.getNamespace().getPrefix());
        }
    }

    @Test
    public void testEditDetached() throws Exception {
        final Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        new CatalogBuilder(catalog).updateCoverageStore(store, coverageStore);
        assertNull(store.getId());

        try {
            tester.startPage(new CoverageStoreEditPage(store));
            tester.assertNoErrorMessage();

            FormTester form = tester.newFormTester("rasterStoreForm");
            form.setValue("namePanel:border:border_body:paramValue", "foo");
            form.submit();
            tester.clickLink("rasterStoreForm:save");
            tester.assertNoErrorMessage();

            assertNotNull(store.getId());
            assertEquals("foo", store.getName());
            assertNotNull(catalog.getStoreByName(coverageStore.getName(), CoverageStoreInfo.class));
            assertNotNull(catalog.getStoreByName("foo", CoverageStoreInfo.class));
        } finally {
            catalog.remove(store);
        }
    }

    @Test
    public void testCoverageStoreEdit() throws Exception {
        final Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        new CatalogBuilder(catalog).updateCoverageStore(store, coverageStore);
        assertNull(store.getId());

        try {
            tester.startPage(new CoverageStoreEditPage(store));
            tester.assertNoErrorMessage();

            FormTester form = tester.newFormTester("rasterStoreForm");
            form.setValue("namePanel:border:border_body:paramValue", "foo");
            form.submit();
            tester.clickLink("rasterStoreForm:save");
            tester.assertNoErrorMessage();

            assertNotNull(store.getId());

            CoverageStoreInfo expandedStore = catalog.getResourcePool().clone(store, true);

            assertNotNull(expandedStore.getId());
            assertNotNull(expandedStore.getCatalog());

            catalog.validate(expandedStore, false).throwIfInvalid();
        } finally {
            catalog.remove(store);
        }
    }
}
