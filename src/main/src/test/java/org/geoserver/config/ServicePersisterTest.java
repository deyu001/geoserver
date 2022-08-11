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

package org.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class ServicePersisterTest extends GeoServerSystemTestSupport {

    GeoServer geoServer;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        GeoServer geoServer = getGeoServer();
        geoServer.addListener(
                new ServicePersister(
                        Arrays.asList(new ServiceLoader(getResourceLoader())), geoServer));
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath*:/org/geoserver/config/ServicePersisterTest-applicationContext.xml");
    }

    @Before
    public void init() {
        geoServer = getGeoServer();
    }

    @Before
    public void removeFooService() throws IOException {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();
        ServiceInfo s = geoServer.getServiceByName(ws, "foo", ServiceInfo.class);
        if (s != null) {
            geoServer.remove(s);
        }

        File serviceFile = getDataDirectory().findFile("service.xml");
        if (serviceFile != null) {
            serviceFile.delete();
        }
    }

    @Test
    public void testAddWorkspaceLocalService() throws Exception {
        File dataDirRoot = getTestData().getDataDirectoryRoot();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();

        ServiceInfo s = geoServer.getFactory().createService();
        s.setName("foo");
        s.setWorkspace(ws);

        File f = new File(dataDirRoot, "workspaces" + "/" + ws.getName() + "/service.xml");
        assertFalse(f.exists());

        geoServer.add(s);
        assertTrue(f.exists());
    }

    @Test
    public void testRemoveWorkspaceLocalService() throws Exception {
        testAddWorkspaceLocalService();

        File dataDirRoot = getTestData().getDataDirectoryRoot();
        WorkspaceInfo ws = getCatalog().getDefaultWorkspace();

        File f = new File(dataDirRoot, "workspaces" + "/" + ws.getName() + "/service.xml");
        assertTrue(f.exists());

        Logger logger = Logging.getLogger(GeoServerImpl.class);
        Level level = logger.getLevel();
        try {
            logger.setLevel(Level.OFF);
            ServiceInfo s = geoServer.getServiceByName(ws, "foo", ServiceInfo.class);
            geoServer.remove(s);
            assertFalse(f.exists());
        } finally {
            logger.setLevel(level);
        }
    }

    @Test
    public void testReloadWithLocalServices() throws Exception {
        // setup a non default workspace
        WorkspaceInfo ws = getCatalog().getFactory().createWorkspace();
        ws.setName("nonDefault");
        NamespaceInfo ni = getCatalog().getFactory().createNamespace();
        ni.setPrefix("nonDefault");
        ni.setURI("http://www.geoserver.org/nonDefault");
        getCatalog().add(ws);
        getCatalog().add(ni);

        // create a ws specific setting
        SettingsInfo s = geoServer.getFactory().createSettings();
        s.setWorkspace(ws);

        geoServer.add(s);

        getGeoServer().reload();
    }

    @Test
    public void testLoadGibberish() throws Exception {
        // we should get a log message, but the startup should continue
        File service =
                new File(getDataDirectory().getResourceLoader().getBaseDirectory(), "service.xml");
        FileUtils.writeStringToFile(service, "duDaDa", "UTF-8");
        getGeoServer().reload();
        assertEquals(0, geoServer.getServices().size());
    }

    public static class ServiceLoader extends XStreamServiceLoader<ServiceInfo> {

        public ServiceLoader(GeoServerResourceLoader resourceLoader) {
            super(resourceLoader, "service");
        }

        @Override
        public Class<ServiceInfo> getServiceClass() {
            return ServiceInfo.class;
        }

        @Override
        protected ServiceInfo createServiceFromScratch(GeoServer gs) {
            return null;
        }
    }
}
