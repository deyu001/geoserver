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

package org.geoserver.security.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.util.URLs;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.Name;

/** Tests {@link LayerGroupContainmentCache} udpates in face of catalog setup and changes */
public class LayerGroupContainmentCacheTest {

    private static final String WS = "ws";

    private static final String ANOTHER_WS = "anotherWs";

    private static final String NATURE_GROUP = "nature";

    private static final String CONTAINER_GROUP = "containerGroup";

    private LayerGroupContainmentCache cc;

    private LayerGroupInfo nature;

    private LayerGroupInfo container;

    private static Catalog catalog;

    @BeforeClass
    public static void setupBaseCatalog() throws Exception {
        catalog = new CatalogImpl();
        catalog.setResourceLoader(new GeoServerResourceLoader());

        // the workspace
        addWorkspaceNamespace(WS);
        addWorkspaceNamespace(ANOTHER_WS);

        // the builder
        CatalogBuilder cb = new CatalogBuilder(catalog);
        final WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
        cb.setWorkspace(defaultWorkspace);

        // setup the store
        String nsURI = catalog.getDefaultNamespace().getURI();
        URL buildings = MockData.class.getResource("Buildings.properties");
        File testData = URLs.urlToFile(buildings).getParentFile();
        DataStoreInfo storeInfo = cb.buildDataStore("store");
        storeInfo.getConnectionParameters().put("directory", testData);
        storeInfo.getConnectionParameters().put("namespace", nsURI);
        catalog.save(storeInfo);

        // setup all the layers
        PropertyDataStore store = new PropertyDataStore(testData);
        store.setNamespaceURI(nsURI);
        cb.setStore(catalog.getDefaultDataStore(defaultWorkspace));
        for (Name name : store.getNames()) {
            FeatureTypeInfo ft = cb.buildFeatureType(name);
            cb.setupBounds(ft);
            catalog.add(ft);
            LayerInfo layer = cb.buildLayer(ft);
            catalog.add(layer);
        }
    }

    private static void addWorkspaceNamespace(String wsName) {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setName(wsName);
        catalog.add(ws);
        NamespaceInfo ns = new NamespaceInfoImpl();
        ns.setPrefix(wsName);
        ns.setURI("http://www.geoserver.org/" + wsName);
        catalog.add(ns);
    }

    @Before
    public void setupLayerGrups() throws Exception {
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        LayerInfo roads = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        WorkspaceInfo ws = catalog.getDefaultWorkspace();

        this.nature = addLayerGroup(NATURE_GROUP, Mode.SINGLE, ws, lakes, forests);
        this.container = addLayerGroup(CONTAINER_GROUP, Mode.CONTAINER, null, nature, roads);

        cc = new LayerGroupContainmentCache(catalog);
    }

    @After
    public void clearLayerGroups() throws Exception {
        CascadeDeleteVisitor remover = new CascadeDeleteVisitor(catalog);
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            if (catalog.getLayerGroup(lg.getId()) != null) {
                remover.visit(lg);
            }
        }
    }

    private LayerGroupInfo addLayerGroup(
            String name, Mode mode, WorkspaceInfo ws, PublishedInfo... layers) throws Exception {
        CatalogBuilder cb = new CatalogBuilder(catalog);

        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.setMode(mode);
        if (ws != null) {
            group.setWorkspace(ws);
        }
        if (layers != null) {
            for (PublishedInfo layer : layers) {
                group.getLayers().add(layer);
                group.getStyles().add(null);
            }
        }
        cb.calculateLayerGroupBounds(group);
        catalog.add(group);
        if (ws != null) {
            return catalog.getLayerGroupByName(ws.getName(), name);
        } else {
            return catalog.getLayerGroupByName(name);
        }
    }

    private Set<String> set(String... names) {
        if (names == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(names));
    }

    private Set<String> containerNamesForGroup(LayerGroupInfo lg) {
        Collection<LayerGroupSummary> summaries = cc.getContainerGroupsFor(lg);
        return summaries.stream().map(gs -> gs.prefixedName()).collect(Collectors.toSet());
    }

    private Set<String> containerNamesForResource(QName name) {
        Collection<LayerGroupSummary> summaries = cc.getContainerGroupsFor(getResource(name));
        return summaries.stream().map(gs -> gs.prefixedName()).collect(Collectors.toSet());
    }

    private FeatureTypeInfo getResource(QName name) {
        return catalog.getResourceByName(getLayerId(name), FeatureTypeInfo.class);
    }

    private String getLayerId(QName name) {
        return "ws:" + name.getLocalPart();
    }

    @Test
    public void testInitialSetup() throws Exception {
        // nature
        Collection<LayerGroupSummary> natureContainers = cc.getContainerGroupsFor(nature);
        assertEquals(1, natureContainers.size());
        assertThat(natureContainers, contains(new LayerGroupSummary(container)));
        LayerGroupSummary summary = natureContainers.iterator().next();
        assertNull(summary.getWorkspace());
        assertEquals(CONTAINER_GROUP, summary.getName());
        assertThat(summary.getContainerGroups(), empty());

        // container has no contaning groups
        assertThat(cc.getContainerGroupsFor(container), empty());

        // now check the groups containing the layers (nature being SINGLE, not a container)
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(
                containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testAddLayerToNature() throws Exception {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        nature.getLayers().add(neatline);
        nature.getStyles().add(null);
        catalog.save(nature);

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testAddLayerToContainer() throws Exception {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        container.getLayers().add(neatline);
        container.getStyles().add(null);
        catalog.save(container);

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testRemoveLayerFromNature() throws Exception {
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        nature.getLayers().remove(lakes);
        nature.getStyles().remove(0);
        catalog.save(nature);

        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(
                containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testRemoveLayerFromContainer() throws Exception {
        LayerInfo roads = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        container.getLayers().remove(roads);
        container.getStyles().remove(0);
        catalog.save(container);

        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), empty());
    }

    @Test
    public void testRemoveNatureFromContainer() throws Exception {
        container.getLayers().remove(nature);
        container.getStyles().remove(0);
        catalog.save(container);

        assertThat(containerNamesForGroup(nature), empty());
        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), empty());
        assertThat(
                containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testRemoveAllGrups() throws Exception {
        catalog.remove(container);
        catalog.remove(nature);

        assertThat(containerNamesForGroup(nature), empty());
        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), empty());
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), empty());
    }

    @Test
    public void testAddRemoveNamed() throws Exception {
        final String NAMED_GROUP = "named";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add and check containment
        LayerGroupInfo named = addLayerGroup(NAMED_GROUP, Mode.NAMED, null, lakes, neatline);
        assertThat(
                containerNamesForResource(MockData.LAKES),
                equalTo(set(CONTAINER_GROUP, NAMED_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(NAMED_GROUP)));
        assertThat(containerNamesForGroup(named), empty());

        // delete and check containment
        catalog.remove(named);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), empty());
        assertThat(containerNamesForGroup(named), empty());
    }

    @Test
    public void testAddRemoveNestedNamed() throws Exception {
        final String NESTED_NAMED = "nestedNamed";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add, nest, and check containment
        LayerGroupInfo nestedNamed = addLayerGroup(NESTED_NAMED, Mode.NAMED, null, lakes, neatline);
        container.getLayers().add(nestedNamed);
        container.getStyles().add(null);
        catalog.save(container);
        assertThat(
                containerNamesForResource(MockData.LAKES),
                equalTo(set(CONTAINER_GROUP, NESTED_NAMED)));
        assertThat(
                containerNamesForResource(MockData.MAP_NEATLINE),
                equalTo(set(CONTAINER_GROUP, NESTED_NAMED)));
        assertThat(containerNamesForGroup(nestedNamed), equalTo(set(CONTAINER_GROUP)));

        // delete and check containment
        new CascadeDeleteVisitor(catalog).visit(nestedNamed);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), empty());
        assertThat(containerNamesForGroup(nestedNamed), empty());
    }

    @Test
    public void testRenameGroup() throws Exception {
        nature.setName("renamed");
        catalog.save(nature);

        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals("renamed", summary.getName());
        assertEquals(WS, summary.getWorkspace());
    }

    @Test
    public void testRenameWorkspace() throws Exception {
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        ws.setName("renamed");
        try {
            catalog.save(ws);

            LayerGroupSummary summary = cc.groupCache.get(nature.getId());
            assertEquals(NATURE_GROUP, summary.getName());
            assertEquals("renamed", summary.getWorkspace());
        } finally {
            ws.setName(WS);
            catalog.save(ws);
        }
    }

    @Test
    public void testChangeWorkspace() throws Exception {
        DataStoreInfo store = catalog.getDataStores().get(0);
        try {
            WorkspaceInfo aws = catalog.getWorkspaceByName(ANOTHER_WS);
            store.setWorkspace(aws);
            catalog.save(store);
            nature.setWorkspace(aws);
            catalog.save(nature);

            LayerGroupSummary summary = cc.groupCache.get(nature.getId());
            assertEquals(NATURE_GROUP, summary.getName());
            assertEquals(ANOTHER_WS, summary.getWorkspace());
        } finally {
            WorkspaceInfo ws = catalog.getWorkspaceByName(WS);
            store.setWorkspace(ws);
            catalog.save(store);
        }
    }

    @Test
    public void testChangeGroupMode() throws Exception {
        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals(Mode.SINGLE, summary.getMode());

        nature.setMode(Mode.OPAQUE_CONTAINER);
        catalog.save(nature);

        summary = cc.groupCache.get(nature.getId());
        assertEquals(Mode.OPAQUE_CONTAINER, summary.getMode());
    }
}
