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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecureCatalogImplFilterTest {
    Authentication anonymous = new TestingAuthenticationToken("anonymous", null);
    ResourceAccessManager manager;

    static <T> List<T> collectAndClose(CloseableIterator<T> it) throws IOException {
        if (it == null) return null;
        try {
            LinkedList<T> list = new LinkedList<>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        } finally {
            it.close();
        }
    }

    static <T> CloseableIterator<T> makeCIterator(List<T> source, Filter f) {
        return CloseableIteratorAdapter.filter(source.iterator(), f);
    }

    FeatureTypeInfo createMockFeatureType(
            String name,
            WorkspaceInfo ws,
            CatalogMode mode,
            Filter mockFilter,
            boolean read,
            boolean write) {
        DataStoreInfo mockStoreInfo = createMock(DataStoreInfo.class);
        FeatureTypeInfo mockFTInfo = createMock(FeatureTypeInfo.class);
        expect(mockFTInfo.getName()).andStubReturn(name);
        expect(mockFTInfo.getStore()).andStubReturn(mockStoreInfo);
        expect(mockStoreInfo.getWorkspace()).andStubReturn(ws);
        replay(mockStoreInfo);
        expect(manager.getAccessLimits(eq(anonymous), eq(mockFTInfo)))
                .andStubReturn(new VectorAccessLimits(mode, null, null, null, null));
        expect(mockFilter.evaluate(mockFTInfo))
                .andStubReturn(read || mode == CatalogMode.CHALLENGE);
        return mockFTInfo;
    }

    static Matcher<FeatureTypeInfo> matchFT(String name, WorkspaceInfo ws) {
        return allOf(
                hasProperty("name", is(name)),
                hasProperty("store", hasProperty("workspace", is(ws))));
    }

    @Test
    @SuppressWarnings("unchecked") // hamcrest varargs method is not marked as @SafeVarArgs
    public void testFeatureTypeList() throws Exception {
        Catalog catalog = createMock(Catalog.class);

        manager = createMock(ResourceAccessManager.class);

        Filter mockFilter = createMock(Filter.class);
        expect(manager.getSecurityFilter(eq(anonymous), eq(FeatureTypeInfo.class)))
                .andStubReturn(mockFilter); // TODO

        final Capture<Filter> filterCapture = Capture.newInstance(CaptureType.LAST);

        final List<FeatureTypeInfo> source = new ArrayList<>();

        WorkspaceInfo mockWSInfo = createMock(WorkspaceInfo.class);
        expect(manager.getAccessLimits(eq(anonymous), eq(mockWSInfo)))
                .andStubReturn(new WorkspaceAccessLimits(CatalogMode.HIDE, true, false, false));

        FeatureTypeInfo mockFTInfo =
                createMockFeatureType("foo", mockWSInfo, CatalogMode.HIDE, mockFilter, true, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        mockFTInfo =
                createMockFeatureType(
                        "bar", mockWSInfo, CatalogMode.HIDE, mockFilter, false, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        mockFTInfo =
                createMockFeatureType(
                        "baz", mockWSInfo, CatalogMode.CHALLENGE, mockFilter, false, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        expect(
                        catalog.list(
                                eq(FeatureTypeInfo.class),
                                capture(filterCapture),
                                isNull(),
                                isNull(),
                                isNull()))
                .andStubAnswer(
                        new IAnswer<CloseableIterator<FeatureTypeInfo>>() {

                            @Override
                            public CloseableIterator<FeatureTypeInfo> answer() throws Throwable {
                                Filter filter = filterCapture.getValue();
                                return CloseableIteratorAdapter.filter(source.iterator(), filter);
                            }
                        });

        replay(catalog, manager, mockFilter);

        @SuppressWarnings("serial")
        SecureCatalogImpl sc =
                new SecureCatalogImpl(catalog, manager) {
                    // Calls static method we can't mock
                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }

                    // Not relevant to the test ad complicates things due to static calls
                    @Override
                    protected <T extends CatalogInfo> T checkAccess(
                            Authentication user, T info, MixedModeBehavior mixedModeBehavior) {
                        return info;
                    }
                };

        // use no user at all
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        List<FeatureTypeInfo> ftResult =
                collectAndClose(sc.list(FeatureTypeInfo.class, Predicates.acceptAll()));
        ftResult.get(0).getStore().getWorkspace();
        assertThat(ftResult, contains(matchFT("foo", mockWSInfo), matchFT("baz", mockWSInfo)));

        verify(catalog, manager);
    }
}
