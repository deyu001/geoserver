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

package org.geoserver.web.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Assert;
import org.junit.Test;

public class PreviewLayerProviderTest extends GeoServerWicketTestSupport {

    @Test
    public void testNonAdvertisedLayer() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        try {
            // now you see me
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, layerId);
            assertNotNull(pl);

            // now you don't!
            layer.setAdvertised(false);
            getCatalog().save(layer);
            pl = getPreviewLayer(provider, layerId);
            assertNull(pl);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }

    @Test
    public void testWmsLinkParametersOfLayer() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);

        PreviewLayerProvider provider = new PreviewLayerProvider();
        PreviewLayer pl = getPreviewLayer(provider, layerId);
        assertNotNull(pl);

        String wmsLink = pl.getWmsLink();
        String[] wmsParams = wmsLink.substring(wmsLink.indexOf("?") + 1).split("&");
        Set<String> wmsKeys = new HashSet<>();

        for (String param : wmsParams) {
            String[] wmsParam = param.split("=");

            if (wmsParam.length > 0) {
                wmsKeys.add(wmsParam[0]);
            }
        }

        List<String> keysToCheck =
                Arrays.asList(
                        "service", "version", "request", "layers", "bbox", "width", "height", "srs",
                        "styles");

        for (String key : keysToCheck) {
            if (!wmsKeys.contains(key)) {
                Assert.fail(
                        String.format(
                                "Parameter '%s' not specified in WmsLink URL of Layer.", key));
            }
        }
    }

    @Test
    public void testSingleLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testSingleLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNotNull(pl);
            assertEquals("This is the title", pl.getTitle());
            assertEquals("This is the abstract", pl.getAbstract());
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testDisabledLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testSingleLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        group.setEnabled(false);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNull(pl);
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testNotAdvertisedLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testSingleLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        group.setAdvertised(false);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNull(pl);
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testOpaqueContainerLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testOpaqueContainerLayerGroup");
        group.setMode(LayerGroupInfo.Mode.OPAQUE_CONTAINER);
        group.getLayers().add(layer);
        group.setTitle("This is the title");
        group.setAbstract("This is the abstract");
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNotNull(pl);
            assertEquals("This is the title", pl.getTitle());
            assertEquals("This is the abstract", pl.getAbstract());
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testWorkspacedLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("cite");

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testWorkspacedLayerGroup");
        group.setMode(LayerGroupInfo.Mode.SINGLE);
        group.setWorkspace(ws);
        group.getLayers().add(layer);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNotNull(pl);
            assertEquals("cite:testWorkspacedLayerGroup", pl.getName());
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testContainerLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testContainerLayerGroup");
        group.setMode(LayerGroupInfo.Mode.CONTAINER);
        group.getLayers().add(layer);
        getCatalog().add(group);
        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, group.prefixedName());
            assertNull(pl);
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testNestedContainerLayerGroup() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);

        LayerGroupInfo containerGroup = getCatalog().getFactory().createLayerGroup();
        containerGroup.setName("testContainerLayerGroup");
        containerGroup.setMode(LayerGroupInfo.Mode.SINGLE);
        containerGroup.getLayers().add(layer);
        getCatalog().add(containerGroup);

        LayerGroupInfo singleGroup = getCatalog().getFactory().createLayerGroup();
        singleGroup.setName("testSingleLayerGroup");
        singleGroup.setMode(LayerGroupInfo.Mode.SINGLE);
        singleGroup.getLayers().add(containerGroup);
        getCatalog().add(singleGroup);

        try {
            PreviewLayerProvider provider = new PreviewLayerProvider();
            assertNotNull(getPreviewLayer(provider, singleGroup.prefixedName()));
            assertNotNull(getPreviewLayer(provider, layer.prefixedName()));
        } finally {
            getCatalog().remove(singleGroup);
            getCatalog().remove(containerGroup);
        }
    }

    @Test
    public void testKewordsFilterSizeCache() {
        PreviewLayerProvider provider = new PreviewLayerProvider();
        assertEquals(29, provider.size());

        provider.setKeywords(new String[] {"cite"});
        assertEquals(12, provider.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetItems() throws Exception {
        // Ensure that the method getItems is no more called
        PreviewLayerProvider provider = new PreviewLayerProvider();
        provider.getItems();
    }

    private PreviewLayer getPreviewLayer(PreviewLayerProvider provider, String prefixedName) {
        for (PreviewLayer pl : Lists.newArrayList(provider.iterator(0, Integer.MAX_VALUE))) {
            if (pl.getName().equals(prefixedName)) {
                return pl;
            }
        }
        return null;
    }
}
