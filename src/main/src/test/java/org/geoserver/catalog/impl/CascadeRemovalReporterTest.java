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

package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.CascadeRemovalReporter;
import org.geoserver.catalog.CascadeRemovalReporter.ModificationType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class CascadeRemovalReporterTest extends CascadeVisitorAbstractTest {

    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(
                new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }

    @Test
    public void testCascadeLayer() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        String name = getLayerId(MockData.LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);
        visitor.visit(layer);
        // layer.accept(visitor);

        // we expect a layer, a resource and two groups
        assertEquals(4, visitor.getObjects(null).size());

        // check the layer and resource have been marked to delete (and
        assertEquals(
                catalog.getLayerByName(name),
                visitor.getObjects(LayerInfo.class, ModificationType.DELETE).get(0));
        assertEquals(
                catalog.getResourceByName(name, ResourceInfo.class),
                visitor.getObjects(ResourceInfo.class, ModificationType.DELETE).get(0));

        // the groups have been marked to update?
        assertTrue(
                visitor.getObjects(LayerGroupInfo.class, ModificationType.GROUP_CHANGED)
                        .contains(catalog.getLayerGroupByName(LAKES_GROUP)));
        assertTrue(
                visitor.getObjects(LayerGroupInfo.class, ModificationType.GROUP_CHANGED)
                        .contains(catalog.getLayerGroupByName(NEST_GROUP)));
    }

    @Test
    public void testCascadeStore() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        String citeStore = MockData.CITE_PREFIX;
        StoreInfo store = catalog.getStoreByName(citeStore, StoreInfo.class);
        String buildings = getLayerId(MockData.BUILDINGS);
        String lakes = getLayerId(MockData.LAKES);
        LayerInfo bl = catalog.getLayerByName(buildings);
        ResourceInfo br = catalog.getResourceByName(buildings, ResourceInfo.class);
        LayerInfo ll = catalog.getLayerByName(lakes);
        ResourceInfo lr = catalog.getResourceByName(lakes, ResourceInfo.class);

        visitor.visit((DataStoreInfo) store);

        assertEquals(store, visitor.getObjects(StoreInfo.class, ModificationType.DELETE).get(0));
        List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, ModificationType.DELETE);
        assertTrue(layers.contains(bl));
        assertTrue(layers.contains(ll));
        List<ResourceInfo> resources =
                visitor.getObjects(ResourceInfo.class, ModificationType.DELETE);
        assertTrue(resources.contains(br));
        assertTrue(resources.contains(lr));
    }

    @Test
    public void testCascadeWorkspace() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        assertNotNull(ws);
        List<StoreInfo> stores = getCatalog().getStoresByWorkspace(ws, StoreInfo.class);
        List<StyleInfo> styles = getCatalog().getStylesByWorkspace(ws);
        List<LayerGroupInfo> layerGroups = getCatalog().getLayerGroupsByWorkspace(ws);
        List<LayerGroupInfo> changedLayerGroups = new ArrayList<>();
        // Added another check for Layergroups which are not in the ws but contain
        // Layers belonging to this ws
        List<LayerGroupInfo> totalLayerGroups = getCatalog().getLayerGroups();
        for (LayerGroupInfo info : totalLayerGroups) {
            List<PublishedInfo> layers = info.getLayers();
            int size = countStores(info, stores);
            if (size == layers.size()) {
                if (!layerGroups.contains(info)) {
                    layerGroups.add(info);
                }
            } else {
                changedLayerGroups.add(info);
            }
        }

        ws.accept(visitor);

        assertTrue(
                stores.containsAll(visitor.getObjects(StoreInfo.class, ModificationType.DELETE)));
        assertTrue(
                styles.containsAll(visitor.getObjects(StyleInfo.class, ModificationType.DELETE)));
        assertTrue(
                layerGroups.containsAll(
                        visitor.getObjects(LayerGroupInfo.class, ModificationType.DELETE)));
        assertTrue(
                changedLayerGroups.containsAll(
                        visitor.getObjects(LayerGroupInfo.class, ModificationType.GROUP_CHANGED)));
    }

    private int countStores(LayerGroupInfo lg, List<StoreInfo> stores) {
        List<PublishedInfo> layers = lg.getLayers();
        int size = 0;
        for (PublishedInfo l : layers) {
            if (l instanceof LayerInfo) {
                if (stores.contains(((LayerInfo) l).getResource().getStore())) {
                    size++;
                }
            } else if (l instanceof LayerGroupInfo) {
                if (countStores((LayerGroupInfo) l, stores)
                        == ((LayerGroupInfo) l).getLayers().size()) {
                    size++;
                }
            }
        }
        return size;
    }

    @Test
    public void testCascadeStyle() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        StyleInfo style = catalog.getStyleByName(MockData.LAKES.getLocalPart());
        LayerInfo buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        visitor.visit(style);

        // test style reset
        assertEquals(style, visitor.getObjects(StyleInfo.class, ModificationType.DELETE).get(0));
        assertEquals(
                lakes, visitor.getObjects(LayerInfo.class, ModificationType.STYLE_RESET).get(0));
        assertEquals(
                buildings,
                visitor.getObjects(LayerInfo.class, ModificationType.EXTRA_STYLE_REMOVED).get(0));
    }
}
