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

package org.geoserver.web.data.layer;

import static org.geoserver.data.test.CiteTestData.BUILDINGS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.wicket.request.mapper.parameter.INamedParameters.Type;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Test;

public class LayerPageTest extends GeoServerWicketTestSupport {

    public static QName GS_BUILDINGS =
            new QName(MockData.DEFAULT_URI, "Buildings", MockData.DEFAULT_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // we don't want any of the defaults
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(BUILDINGS, getCatalog());

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(LayerProperty.STYLE, BUILDINGS.getLocalPart());
        testData.addVectorLayer(GS_BUILDINGS, props, getCatalog());
    }

    @Test
    public void testBasicActions() {
        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two layers
        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.contains("cite"));
        assertTrue(workspaces.contains("gs"));

        // sort on workspace once (top to bottom)
        String wsSortPath = "table:listContainer:sortableLinks:3:header:link";
        tester.clickLink(wsSortPath, true);
        workspaces = getWorkspaces(table);
        assertEquals("cite", workspaces.get(0));
        assertEquals("gs", workspaces.get(1));

        // sort on workspace twice (bottom to top)
        tester.clickLink(wsSortPath, true);
        workspaces = getWorkspaces(table);
        assertEquals("gs", workspaces.get(0));
        assertEquals("cite", workspaces.get(1));

        // select second layer
        table.selectIndex(1);
        assertEquals(1, table.getSelection().size());
        LayerInfo li = (LayerInfo) table.getSelection().get(0);
        assertEquals("cite", li.getResource().getStore().getWorkspace().getName());
    }

    @Test
    public void testFilterState() {
        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two layers
        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.contains("cite"));
        assertTrue(workspaces.contains("gs"));

        // apply filter by only viewing layer from workspace cite
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "cite");
        ft.submit("submit");

        // verify clear button is visible
        tester.assertVisible("table:filterForm:clear");

        // verify the table is only showing 1 layer
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());

        // navigate to a ResourceConfigurationPage
        LayerInfo layerInfo = getCatalog().getLayers().get(0);
        tester.startPage(new ResourceConfigurationPage(layerInfo, false));
        tester.assertRenderedPage(ResourceConfigurationPage.class);
        tester.assertNoErrorMessage();

        // click submit and go back to LayerPage
        ft = tester.newFormTester("publishedinfo");
        ft.submit("save");

        // verify when user navigates back to Layer Page
        // the clear link is visible and filter is populated in text field
        // and table is in filtered state
        tester.assertRenderedPage(LayerPage.class);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "cite");
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());

        // clear the filter by click the Clear button
        tester.clickLink("table:filterForm:clear", true);
        // verify clear button has disappeared and filter is set to empty
        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "");
        // verify table is back to showing all items
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
    }

    @Test
    public void testFilterStateReset() {
        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two layers
        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.contains("cite"));
        assertTrue(workspaces.contains("gs"));

        // apply filter by only viewing layer from workspace cite
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "cite");
        ft.submit("submit");

        // verify clear button is visible
        tester.assertVisible("table:filterForm:clear");

        // verify the table is only showing 1 layer
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());

        // simulate click from left menu which passes
        // show the page with no filter
        PageParameters pageParms = new PageParameters();
        pageParms.set(GeoServerTablePanel.FILTER_PARAM, false, Type.PATH);
        tester.startPage(LayerPage.class, pageParms);
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", null);
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
    }

    private List<String> getWorkspaces(GeoServerTablePanel table) {
        Iterator it = table.getDataProvider().iterator(0, 2);
        List<String> workspaces = new ArrayList<>();
        while (it.hasNext()) {
            LayerInfo li = (LayerInfo) it.next();
            String wsName = li.getResource().getStore().getWorkspace().getName();
            workspaces.add(wsName);
        }
        return workspaces;
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two columns
        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        LayerProvider layerProvider = (LayerProvider) table.getDataProvider();
        assertTrue(layerProvider.getProperties().contains(LayerProvider.CREATED_TIMESTAMP));
        assertTrue(layerProvider.getProperties().contains(LayerProvider.MODIFIED_TIMESTAMP));
    }
}
