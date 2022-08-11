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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.gce.geotiff.GeoTiffFormatFactorySpi;
import org.geotools.geopkg.mosaic.GeoPackageFormat;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.Format;

public class CoverageStoreNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = false;

    String formatType;

    String formatDescription;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void init() {
        Format format = new GeoTiffFormatFactorySpi().createFormat();
        formatType = format.getName();
        formatDescription = format.getDescription();
    }

    private CoverageStoreNewPage startPage() {

        login();
        final CoverageStoreNewPage page = new CoverageStoreNewPage(formatType);
        tester.startPage(page);

        if (debugMode) {
            print(page, true, true);
        }

        return page;
    }

    @Test
    public void testInitCreateNewCoverageStoreInvalidDataStoreFactoryName() {

        final String formatName = "_invalid_";
        try {
            login();
            new CoverageStoreNewPage(formatName);
            fail("Expected IAE on invalid format name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Can't obtain the factory"));
        }
    }

    /** A kind of smoke test that only asserts the page is rendered when first loaded */
    @Test
    public void testPageRendersOnLoad() {

        startPage();

        tester.assertLabel("rasterStoreForm:storeType", formatType);
        tester.assertLabel("rasterStoreForm:storeTypeDescription", formatDescription);

        tester.assertComponent("rasterStoreForm:workspacePanel", WorkspacePanel.class);
    }

    @Test
    public void testInitialModelState() {

        CoverageStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("rasterStoreForm:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "rasterStoreForm:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
        tester.assertModelValue(
                "rasterStoreForm:parametersPanel:url", "file:data/example.extension");
    }

    @Test
    public void testMultipleResources() {

        CoverageStoreNewPage page = startPage();

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("rasterStoreForm:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "rasterStoreForm:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
        tester.assertModelValue(
                "rasterStoreForm:parametersPanel:url", "file:data/example.extension");
    }

    @Test
    public void testGeoPackageRaster() {
        login();
        formatType = new GeoPackageFormat().getName();
        final CoverageStoreNewPage page = new CoverageStoreNewPage(formatType);
        tester.startPage(page);

        tester.debugComponentTrees();
        Component urlComponent =
                tester.getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel:url");
        assertThat(urlComponent, instanceOf(FileParamPanel.class));
    }

    @Test
    public void testNewCoverageSave() {
        startPage();
        FormTester ft = tester.newFormTester("rasterStoreForm");
        ft.setValue(
                "parametersPanel:url:fileInput:border:border_body:paramValue",
                "BlueMarble/tazbm.tiff");
        ft.setValue("namePanel:border:border_body:paramValue", "tazbm2");
        ft.submit("save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(NewLayerPage.class);
        CoverageStoreInfo store = getCatalog().getCoverageStoreByName("tazbm2");
        assertNotNull(store);
        assertEquals("BlueMarble/tazbm.tiff", store.getURL());
    }

    @Test
    public void testNewCoverageApply() {
        startPage();
        FormTester ft = tester.newFormTester("rasterStoreForm");
        ft.setValue(
                "parametersPanel:url:fileInput:border:border_body:paramValue",
                "BlueMarble/tazbm.tiff");
        ft.setValue("namePanel:border:border_body:paramValue", "tazbm3");
        ft.submit("apply");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(CoverageStoreEditPage.class);
        CoverageStoreInfo store = getCatalog().getCoverageStoreByName("tazbm3");
        assertNotNull(store);
        assertEquals("BlueMarble/tazbm.tiff", store.getURL());
    }
}
