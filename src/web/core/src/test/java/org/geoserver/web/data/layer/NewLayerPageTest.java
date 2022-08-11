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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import org.apache.wicket.Component;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.data.DataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.junit.Test;

public class NewLayerPageTest extends GeoServerWicketTestSupport {

    private static final String TABLE_PATH = "selectLayersContainer:selectLayers:layers";

    @Test
    public void testKnownStore() {
        login();
        DataStoreInfo store = getCatalog().getStoreByName(MockData.CDF_PREFIX, DataStoreInfo.class);
        tester.startPage(new NewLayerPage(store.getId()));

        tester.assertRenderedPage(NewLayerPage.class);
        assertNull(tester.getComponentFromLastRenderedPage("selector"));
        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage(TABLE_PATH);
        assertEquals(
                getCatalog().getResourcesByStore(store, FeatureTypeInfo.class).size(),
                table.getDataProvider().size());
    }

    @Test
    public void testAjaxChooser() {
        login();
        tester.startPage(new NewLayerPage());

        tester.assertRenderedPage(NewLayerPage.class);

        // the tester will return null if the component is there, but not visible
        assertNull(tester.getComponentFromLastRenderedPage("selectLayersContainer:selectLayers"));

        // select the first datastore
        tester.newFormTester("selector").select("storesDropDown", 1);
        tester.executeAjaxEvent("selector:storesDropDown", "change");

        // now it should be there
        assertNotNull(
                tester.getComponentFromLastRenderedPage("selectLayersContainer:selectLayers"));

        // select "choose one" item (unselect the form)
        tester.newFormTester("selector").setValue("storesDropDown", "");
        tester.executeAjaxEvent("selector:storesDropDown", "change");

        // now it should be there
        assertNull(tester.getComponentFromLastRenderedPage("selectLayersContainer:selectLayers"));
    }

    @Test
    public void testAddLayer() throws Exception {
        login();
        DataStoreInfo store =
                getCatalog().getStoreByName(MockData.CITE_PREFIX, DataStoreInfo.class);
        NewLayerPage page = new NewLayerPage(store.getId());
        tester.startPage(page);

        // get the name of the first layer in the list
        String[] names = ((DataStore) store.getDataStore(null)).getTypeNames();
        Arrays.sort(names);

        tester.clickLink(
                TABLE_PATH + ":listContainer:items:1:itemProperties:2:component:link", true);
        tester.assertRenderedPage(ResourceConfigurationPage.class);
        assertEquals(
                names[0],
                ((ResourceConfigurationPage) tester.getLastRenderedPage())
                        .getResourceInfo()
                        .getName());
    }

    @Test
    public void testAddLayerFromWFSDataStore() throws Exception {
        login();
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        DataStoreInfo storeInfo = cb.buildDataStore(MockData.CITE_PREFIX);
        ((DataStoreInfoImpl) storeInfo).setId("1");

        getCatalog().add(storeInfo);

        try {
            URL url = getClass().getResource("/WFSCapabilities.xml");

            storeInfo.getConnectionParameters().put(WFSDataStoreFactory.URL.key, url);
            // required or the store won't fetch caps from a file
            storeInfo.getConnectionParameters().put("TESTING", Boolean.TRUE);
            final ResourcePool rp = getCatalog().getResourcePool();
            rp.getDataStore(storeInfo);

            NewLayerPage page = new NewLayerPage(storeInfo.getId());
            tester.startPage(page);
            Component link =
                    tester.getComponentFromLastRenderedPage("selectLayersContainer")
                            .get("createCascadedWFSStoredQueryContainer");

            assertTrue(link.isVisible());
        } finally {
            getCatalog().remove(storeInfo);
        }
    }

    @Test
    public void testAddLayerFromNotWFSDataStore() throws Exception {
        login();
        DataStoreInfo store =
                getCatalog().getStoreByName(MockData.CITE_PREFIX, DataStoreInfo.class);
        NewLayerPage page = new NewLayerPage(store.getId());
        tester.startPage(page);

        Component link =
                tester.getComponentFromLastRenderedPage("selectLayersContainer")
                        .get("createCascadedWFSStoredQueryContainer");

        assertFalse(link.isVisible());
    }
}
