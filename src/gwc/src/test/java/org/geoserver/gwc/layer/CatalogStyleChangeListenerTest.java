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

package org.geoserver.gwc.layer;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.gwc.GWC;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.junit.Before;
import org.junit.Test;

public class CatalogStyleChangeListenerTest {

    private final String STYLE_NAME = "highways";

    private String STYLE_NAME_MODIFIED = STYLE_NAME + "_modified";

    private final String PREFIXED_RESOURCE_NAME = "mock:Layer";

    private GWC mockMediator;

    private ResourceInfo mockResourceInfo;

    private LayerInfo mockLayerInfo;

    private StyleInfo mockStyle;

    private GeoServerTileLayer mockTileLayer;

    private GeoServerTileLayerInfoImpl mockTileLayerInfo;

    private CatalogModifyEventImpl styleNameModifyEvent;

    private CatalogStyleChangeListener listener;

    @Before
    public void setUp() throws Exception {
        mockMediator = mock(GWC.class);
        mockStyle = mock(StyleInfo.class);
        when(mockStyle.prefixedName()).thenReturn(STYLE_NAME);

        mockResourceInfo = mock(FeatureTypeInfo.class);
        when(mockResourceInfo.prefixedName()).thenReturn(PREFIXED_RESOURCE_NAME);

        mockLayerInfo = mock(LayerInfo.class);
        when(mockLayerInfo.getResource()).thenReturn(mockResourceInfo);

        mockTileLayer = mock(GeoServerTileLayer.class);

        mockTileLayerInfo = mock(GeoServerTileLayerInfoImpl.class);
        ImmutableSet<String> empty = ImmutableSet.of();
        when(mockTileLayerInfo.cachedStyles()).thenReturn(empty);

        when(mockTileLayer.getPublishedInfo()).thenReturn(mockLayerInfo);
        when(mockTileLayer.getInfo()).thenReturn(mockTileLayerInfo);
        when(mockTileLayer.getName()).thenReturn(PREFIXED_RESOURCE_NAME);
        when(mockMediator.getTileLayersForStyle(eq(STYLE_NAME)))
                .thenReturn(Collections.singletonList(mockTileLayer));

        Catalog mockCatalog = mock(Catalog.class);
        listener = new CatalogStyleChangeListener(mockMediator, mockCatalog);

        styleNameModifyEvent = new CatalogModifyEventImpl();
        styleNameModifyEvent.setSource(mockStyle);
        styleNameModifyEvent.setPropertyNames(Arrays.asList("name"));
        styleNameModifyEvent.setOldValues(Arrays.asList(STYLE_NAME));
        styleNameModifyEvent.setNewValues(Arrays.asList(STYLE_NAME_MODIFIED));
    }

    @Test
    public void testIgnorableChange() throws Exception {

        // not a name change
        styleNameModifyEvent.setPropertyNames(Arrays.asList("fileName"));
        listener.handleModifyEvent(styleNameModifyEvent);

        // name didn't change at all
        styleNameModifyEvent.setPropertyNames(Arrays.asList("name"));
        styleNameModifyEvent.setOldValues(Arrays.asList(STYLE_NAME));
        styleNameModifyEvent.setNewValues(Arrays.asList(STYLE_NAME));
        listener.handleModifyEvent(styleNameModifyEvent);

        // not a style change
        styleNameModifyEvent.setSource(mock(LayerInfo.class));
        listener.handleModifyEvent(styleNameModifyEvent);

        // a change in the name of the default style should not cause a truncate
        verify(mockMediator, never()).truncateByLayerAndStyle(anyString(), anyString());
        // nor a save, as the default style name is dynamic
        verify(mockMediator, never()).save(any());

        verify(mockTileLayer, never()).getInfo();
        verify(mockTileLayerInfo, never()).cachedStyles();
    }

    @Test
    public void testRenameDefaultStyle() throws Exception {
        // this is another case of an ignorable change. Renaming the default style shall have no
        // impact.
        listener.handleModifyEvent(styleNameModifyEvent);
        // a change in the name of the default style should not cause a truncate
        verify(mockMediator, never()).truncateByLayerAndStyle(anyString(), anyString());
        // nor a save, as the default style name is dynamic
        verify(mockMediator, never()).save(any());

        verify(mockTileLayer, atLeastOnce()).getInfo();
        verify(mockTileLayerInfo, atLeastOnce()).cachedStyles();
    }

    @Test
    public void testRenameAlternateStyle() throws Exception {

        Set<ParameterFilter> params = new HashSet<>();
        StyleParameterFilter newStyleFilter = new StyleParameterFilter();
        newStyleFilter.setStyles(ImmutableSet.of(STYLE_NAME));
        params.add(newStyleFilter);

        TileLayerInfoUtil.setCachedStyles(mockTileLayerInfo, null, ImmutableSet.of(STYLE_NAME));

        verify(mockTileLayerInfo)
                .addParameterFilter(
                        argThat(
                                allOf(
                                        hasProperty("key", is("STYLES")),
                                        hasProperty("styles", is(ImmutableSet.of(STYLE_NAME))))));

        ImmutableSet<String> styles = ImmutableSet.of(STYLE_NAME);
        when(mockTileLayerInfo.cachedStyles()).thenReturn(styles);

        listener.handleModifyEvent(styleNameModifyEvent);

        verify(mockTileLayerInfo)
                .addParameterFilter(
                        argThat(
                                allOf(
                                        hasProperty("key", is("STYLES")),
                                        hasProperty(
                                                "styles",
                                                is(ImmutableSet.of(STYLE_NAME_MODIFIED))))));

        verify(mockTileLayer, times(1)).resetParameterFilters();
        verify(mockMediator, times(1))
                .truncateByLayerAndStyle(eq(PREFIXED_RESOURCE_NAME), eq(STYLE_NAME));
        verify(mockMediator, times(1)).save(same(mockTileLayer));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLayerInfoDefaultOrAlternateStyleChanged() throws Exception {
        when(mockMediator.getLayerInfosFor(same(mockStyle)))
                .thenReturn(Collections.singleton(mockLayerInfo));
        when(mockMediator.getLayerGroupsFor(same(mockStyle))).thenReturn(Collections.emptyList());

        CatalogPostModifyEventImpl postModifyEvent = new CatalogPostModifyEventImpl();
        postModifyEvent.setSource(mockStyle);
        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockMediator, times(1))
                .truncateByLayerAndStyle(eq(PREFIXED_RESOURCE_NAME), eq(STYLE_NAME));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLayerGroupInfoImplicitOrExplicitStyleChanged() throws Exception {
        LayerGroupInfo mockGroup = mock(LayerGroupInfo.class);
        when(GWC.tileLayerName(mockGroup)).thenReturn("mockGroup");

        when(mockMediator.getLayerInfosFor(same(mockStyle))).thenReturn(Collections.emptyList());
        when(mockMediator.getLayerGroupsFor(same(mockStyle)))
                .thenReturn(Collections.singleton(mockGroup));

        CatalogPostModifyEventImpl postModifyEvent = new CatalogPostModifyEventImpl();
        postModifyEvent.setSource(mockStyle);
        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockMediator, times(1)).truncate(eq("mockGroup"));
    }

    @Test
    public void testChangeWorkspaceWithoutName() {
        CatalogModifyEventImpl modifyEvent = new CatalogModifyEventImpl();
        modifyEvent.setSource(mockStyle);
        modifyEvent.setPropertyNames(Collections.singletonList("workspace"));
        modifyEvent.setOldValues(Collections.singletonList(""));
        modifyEvent.setNewValues(Collections.singletonList("test"));

        // should occur without exception
        listener.handleModifyEvent(modifyEvent);
    }
}
