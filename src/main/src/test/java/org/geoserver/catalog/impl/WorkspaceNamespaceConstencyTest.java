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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.HashMap;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.NamespaceWorkspaceConsistencyListener;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class WorkspaceNamespaceConstencyTest {

    @Test
    public void testChangeWorkspace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener(anyObject());
        expectLastCall();

        NamespaceInfo ns = createMock(NamespaceInfo.class);
        ns.setPrefix("abcd");
        expectLastCall();

        expect(cat.getNamespaceByPrefix("gs")).andReturn(ns);

        cat.save(ns);
        expectLastCall();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(ws).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("name"));
        expect(e.getOldValues()).andReturn(Arrays.asList("gs"));
        expect(e.getNewValues()).andReturn(Arrays.asList("abcd"));

        replay(e, ws, ns, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);
        verify(ns, cat);
    }

    @Test
    public void testChangeNamespace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener(anyObject());
        expectLastCall();

        WorkspaceInfo ws = createMock(WorkspaceInfo.class);
        ws.setName("abcd");
        expectLastCall();

        expect(cat.getWorkspaceByName("gs")).andReturn(ws);

        cat.save(ws);
        expectLastCall();

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(ns).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("prefix"));
        expect(e.getOldValues()).andReturn(Arrays.asList("gs"));
        expect(e.getNewValues()).andReturn(Arrays.asList("abcd"));

        replay(e, ws, ns, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);
        verify(ws, cat);
    }

    @Test
    public void testChangeDefaultWorkspace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener(anyObject());
        expectLastCall();

        NamespaceInfo def = createNiceMock(NamespaceInfo.class);
        expect(cat.getDefaultNamespace()).andReturn(def);

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        expect(cat.getNamespaceByPrefix("abcd")).andReturn(ns);

        cat.setDefaultNamespace(ns);
        expectLastCall();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        expect(ws.getName()).andReturn("abcd");

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(cat).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("defaultWorkspace"));
        expect(e.getNewValues()).andReturn(Arrays.asList(ws));

        replay(ns, ws, e, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);

        verify(ns, ws, cat);
    }

    @Test
    public void testChangeDefaultNamespace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener(anyObject());
        expectLastCall();

        WorkspaceInfo def = createNiceMock(WorkspaceInfo.class);
        expect(cat.getDefaultWorkspace()).andReturn(def);

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        expect(cat.getWorkspaceByName("abcd")).andReturn(ws);

        cat.setDefaultWorkspace(ws);
        expectLastCall();

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        expect(ns.getPrefix()).andReturn("abcd");

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(cat).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("defaultNamespace"));
        expect(e.getNewValues()).andReturn(Arrays.asList(ns));

        replay(ns, ws, e, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);

        verify(ns, ws, cat);
    }

    @Test
    public void testChangeNamespaceURI() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener(anyObject());
        expectLastCall();

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        expect(ns.getPrefix()).andReturn("foo");
        expect(ns.getURI()).andReturn("http://foo.org");

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        expect(cat.getWorkspaceByName("foo")).andReturn(ws);

        DataStoreInfo ds = createNiceMock(DataStoreInfo.class);

        expect(cat.getDataStoresByWorkspace(ws)).andReturn(Arrays.asList(ds));

        HashMap params = new HashMap();
        params.put("namespace", "http://bar.org");
        expect(ds.getConnectionParameters()).andReturn(params).anyTimes();

        cat.save(hasNamespace("http://foo.org"));
        expectLastCall();

        CatalogPostModifyEvent e = createNiceMock(CatalogPostModifyEvent.class);
        expect(e.getSource()).andReturn(ns).anyTimes();
        expect(ns.getPrefix()).andReturn("foo");
        expect(cat.getWorkspaceByName("foo")).andReturn(ws);

        replay(ds, ws, ns, e, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handlePostModifyEvent(e);
        verify(cat);
    }

    protected StoreInfo hasNamespace(final String namespace) {
        EasyMock.reportMatcher(
                new IArgumentMatcher() {
                    @Override
                    public boolean matches(Object argument) {
                        return namespace.equals(
                                ((StoreInfo) argument).getConnectionParameters().get("namespace"));
                    }

                    @Override
                    public void appendTo(StringBuffer buffer) {
                        buffer.append("hasNamespace '").append(namespace).append("'");
                    }
                });
        return null;
    }
}
