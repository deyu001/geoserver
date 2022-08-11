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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.request.mapper.parameter.INamedParameters.Type;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

public class StylePageTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        testData.addStyle(
                cite, "simplePoint", "simplePoint.sld", StylePageTest.class, getCatalog());
    }

    @Before
    public void clearFilter() {
        // clear persistent table filters from session
        tester.getSession().removeAttribute(GeoServerTablePanel.FILTER_INPUTS);
    }

    @Test
    public void testPageLoad() {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
    }

    @Test
    public void testStyleProvider() {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);

        // Get the StyleProvider

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        Catalog catalog = getCatalog();
        assertEquals(dv.size(), catalog.getStyles().size());
        IDataProvider dataProvider = dv.getDataProvider();

        // Ensure the data provider is an instance of StoreProvider
        assertTrue(dataProvider instanceof StyleProvider);

        // Cast to StoreProvider
        StyleProvider provider = (StyleProvider) dataProvider;

        // Ensure that an unsupportedException is thrown when requesting the Items directly
        boolean catchedException = false;
        try {
            provider.getItems();
        } catch (UnsupportedOperationException e) {
            catchedException = true;
        }

        // Ensure the exception is cacthed
        assertTrue(catchedException);

        StyleInfo actual = provider.iterator(0, 1).next();
        try (CloseableIterator<StyleInfo> list =
                catalog.list(
                        StyleInfo.class, Filter.INCLUDE, 0, 1, Predicates.sortBy("name", true))) {
            assertTrue(list.hasNext());
            StyleInfo expected = list.next();

            // Ensure equality
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testIsDefaultStyle() {
        Catalog cat = getCatalog();
        assertTrue(StylePage.isDefaultStyle(cat.getStyleByName("line")));

        StyleInfo s = cat.getFactory().createStyle();
        s.setName("line");
        s.setFilename("line.sld");
        s.setWorkspace(cat.getDefaultWorkspace());

        assertFalse(StylePage.isDefaultStyle(s));
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();

        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);

        // Get the StyleProvider

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        Catalog catalog = getCatalog();
        assertEquals(dv.size(), catalog.getStyles().size());
        IDataProvider dataProvider = dv.getDataProvider();

        // Ensure the data provider is an instance of StoreProvider
        assertTrue(dataProvider instanceof StyleProvider);

        // Cast to StoreProvider
        StyleProvider provider = (StyleProvider) dataProvider;
        // should have these columns
        assertTrue(provider.getProperties().contains(StyleProvider.CREATED_TIMESTAMP));
        assertTrue(provider.getProperties().contains(StyleProvider.MODIFIED_TIMESTAMP));
    }

    @Test
    public void testFilter() {
        login();
        Catalog catalog = getCatalog();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), catalog.getStyles().size());
        // apply filter by only viewing style with name polygon
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "polygon");
        ft.submit("submit");

        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), 2);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "polygon");
        // navigate to a style page for any style
        tester.startPage(new StyleEditPage(catalog.getStyles().get(0)));
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertNoErrorMessage();

        // click submit and go back to Style page
        tester.executeAjaxEvent("save", "click");

        // verify when user navigates back to Layer Page
        // the clear link is visible and filter is populated in text field
        // and table is in filtered state
        tester.assertRenderedPage(StylePage.class);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "polygon");
        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), 2);

        // clear the filter by click the Clear button
        tester.clickLink("table:filterForm:clear", true);
        //        // verify clear button has disappeared and filter is set to empty
        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "");
        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), catalog.getStyles().size());
    }

    @Test
    public void testWorkspaceFilter() {
        login();
        Catalog catalog = getCatalog();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), catalog.getStyles().size());
        // apply filter by only viewing style with name polygon
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "cite");
        ft.submit("submit");

        print(tester.getLastRenderedPage(), true, true);

        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());
    }

    @Test
    public void testFilterReset() {
        login();
        Catalog catalog = getCatalog();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), catalog.getStyles().size());
        // apply filter by only viewing style with name polygon
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "polygon");
        ft.submit("submit");

        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), 2);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "polygon");

        // simulate click from left menu which passes
        // show the page with no filter
        PageParameters pageParms = new PageParameters();
        pageParms.set(GeoServerTablePanel.FILTER_PARAM, false, Type.PATH);
        tester.startPage(StylePage.class, pageParms);
        tester.assertRenderedPage(StylePage.class);
        tester.assertNoErrorMessage();

        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", null);
        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), catalog.getStyles().size());
    }
}
