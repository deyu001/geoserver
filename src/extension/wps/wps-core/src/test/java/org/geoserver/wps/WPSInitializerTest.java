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

package org.geoserver.wps;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.easymock.IArgumentMatcher;
import org.easymock.internal.LastControl;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.executor.DefaultProcessManager;
import org.geoserver.wps.executor.WPSExecutionManager;
import org.geoserver.wps.resource.ProcessArtifactsStore;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.Before;
import org.junit.Test;

public class WPSInitializerTest {

    WPSInitializer initer;

    @Before
    public void mockUp() {
        WPSExecutionManager execMgr = createNiceMock(WPSExecutionManager.class);
        DefaultProcessManager procMgr = createNiceMock(DefaultProcessManager.class);
        WPSStorageCleaner cleaner = createNiceMock(WPSStorageCleaner.class);
        WPSResourceManager resources = createNiceMock(WPSResourceManager.class);
        expect(resources.getArtifactsStore())
                .andReturn(createNiceMock(ProcessArtifactsStore.class))
                .anyTimes();
        replay(resources);
        GeoServerResourceLoader loader = createNiceMock(GeoServerResourceLoader.class);

        replay(execMgr, procMgr, cleaner);

        initer = new WPSInitializer(execMgr, procMgr, cleaner, resources, loader);
    }

    @Test
    public void testNoSave() throws Exception {
        GeoServer gs = createMock(GeoServer.class);

        List<ConfigurationListener> listeners = new ArrayList<>();
        gs.addListener(capture(listeners));
        expectLastCall().atLeastOnce();

        // load all process groups so there is no call to save
        List<ProcessGroupInfo> procGroups = WPSInitializer.lookupProcessGroups();

        WPSInfo wps = createNiceMock(WPSInfo.class);
        expect(wps.getProcessGroups()).andReturn(procGroups).anyTimes();
        replay(wps);

        expect(gs.getService(WPSInfo.class)).andReturn(wps).anyTimes();
        replay(gs);

        initer.initialize(gs);

        assertEquals(1, listeners.size());

        ConfigurationListener l = listeners.get(0);
        l.handleGlobalChange(null, null, null, null);
        l.handlePostGlobalChange(null);

        verify(gs);
    }

    @Test
    public void testSingleSave() throws Exception {

        GeoServer gs = createMock(GeoServer.class);

        List<ConfigurationListener> listeners = new ArrayList<>();
        gs.addListener(capture(listeners));
        expectLastCall().atLeastOnce();

        // empty list should cause save
        List<ProcessGroupInfo> procGroups = new ArrayList<>();

        WPSInfo wps = createNiceMock(WPSInfo.class);
        expect(wps.getProcessGroups()).andReturn(procGroups).anyTimes();
        replay(wps);

        expect(gs.getService(WPSInfo.class)).andReturn(wps).anyTimes();
        gs.save(wps);
        expectLastCall().once();
        replay(gs);

        initer.initialize(gs);

        assertEquals(1, listeners.size());

        ConfigurationListener l = listeners.get(0);
        l.handleGlobalChange(null, null, null, null);
        l.handlePostGlobalChange(null);

        verify(gs);
    }

    ConfigurationListener capture(List<ConfigurationListener> listeners) {
        LastControl.reportMatcher(new ListenerCapture(listeners));
        return null;
    }

    static class ListenerCapture implements IArgumentMatcher {

        List<ConfigurationListener> listeners;

        public ListenerCapture(List<ConfigurationListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public boolean matches(Object argument) {
            if (argument instanceof ConfigurationListener) {
                listeners.add((ConfigurationListener) argument);
                return true;
            }
            return false;
        }

        @Override
        public void appendTo(StringBuffer buffer) {
            buffer.append("ListenerCapture");
        }
    }
}
