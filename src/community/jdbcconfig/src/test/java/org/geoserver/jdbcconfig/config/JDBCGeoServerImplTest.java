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

package org.geoserver.jdbcconfig.config;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServerImplTest;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JDBCGeoServerImplTest extends GeoServerImplTest {

    private JDBCGeoServerFacade facade;

    private JDBCConfigTestSupport testSupport;

    public JDBCGeoServerImplTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        testSupport = new JDBCConfigTestSupport(dbConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return JDBCConfigTestSupport.parameterizedDBConfigs();
    }

    @Override
    public void setUp() throws Exception {
        testSupport.setUp();

        ConfigDatabase configDb = testSupport.getDatabase();
        facade = new JDBCGeoServerFacade(configDb);
        facade.setResourceLoader(testSupport.getResourceLoader());

        super.setUp();
        facade.setLogging(geoServer.getFactory().createLogging());
    }

    @After
    public void tearDown() throws Exception {
        facade.dispose();
        testSupport.tearDown();
    }

    @Override
    protected GeoServerImpl createGeoServer() {
        GeoServerImpl gs = new GeoServerImpl();
        gs.setFacade(facade);
        CatalogImpl catalog = testSupport.getCatalog();
        catalog.setFacade(new JDBCCatalogFacade(testSupport.getDatabase()));
        gs.setCatalog(catalog);
        return gs;
    }

    @Override
    public void testAddService() throws Exception {
        super.testAddService();

        // ensure s.getGeoServer() != null
        ServiceInfo s = geoServer.getServiceByName("foo", ServiceInfo.class);
        assertNotNull(s.getGeoServer());
    }

    @Test
    public void testGlobalSettingsWithId() throws Exception {
        SettingsInfoImpl settings = new SettingsInfoImpl();
        settings.setId("settings");

        GeoServerInfo global = geoServer.getFactory().createGlobal();
        global.setSettings(settings);

        geoServer.setGlobal(global);
        assertEquals(global, geoServer.getGlobal());
    }

    @Override
    @Test
    public void testModifyService() throws Exception {
        ServiceInfo service = geoServer.getFactory().createService();
        ((ServiceInfoImpl) service).setId("id");
        service.setName("foo");
        service.setTitle("bar");
        service.setMaintainer("quux");

        geoServer.add(service);

        ServiceInfo s1 = geoServer.getServiceByName("foo", ServiceInfo.class);
        s1.setMaintainer("quam");

        ServiceInfo s2 = geoServer.getServiceByName("foo", ServiceInfo.class);
        assertEquals("quux", s2.getMaintainer());

        ServiceInfo s3 = geoServer.getService(ServiceInfo.class);
        assertEquals("quux", s3.getMaintainer());

        geoServer.save(s1);
        s2 = geoServer.getServiceByName("foo", ServiceInfo.class);
        assertEquals("quam", s2.getMaintainer());

        s3 = geoServer.getService(ServiceInfo.class);
        assertEquals("quam", s3.getMaintainer());

        geoServer.remove(s1);
        s2 = geoServer.getServiceByName("foo", ServiceInfo.class);
        assertNull(s2);

        s3 = geoServer.getService(ServiceInfo.class);
        assertNull(s3);
    }

    // Would have put this on GeoServerImplTest, but it depends on WMS and WFS InfoImpl classes
    // which would lead to circular dependencies.
    @SuppressWarnings("unchecked")
    @Test
    public void testTypedServicesWithWorkspace() throws Exception {
        // Make a workspace
        WorkspaceInfo ws1 = geoServer.getCatalog().getFactory().createWorkspace();
        ws1.setName("TEST-WORKSPACE-1");
        geoServer.getCatalog().add(ws1);

        // Make a service for that workspace
        ServiceInfo ws1wms = new org.geoserver.wms.WMSInfoImpl();
        ws1wms.setWorkspace(ws1);
        ws1wms.setName("WMS1");
        ws1wms.setTitle("WMS for WS1");
        geoServer.add(ws1wms);

        // Make a second for that workspace
        ServiceInfo ws1wfs = new org.geoserver.wfs.WFSInfoImpl();
        ws1wfs.setWorkspace(ws1);
        ws1wfs.setName("WFS1");
        ws1wfs.setTitle("WFS for WS1");
        geoServer.add(ws1wfs);

        // Make a global service
        ServiceInfo gwms = new org.geoserver.wms.WMSInfoImpl();
        gwms.setName("WMSG");
        gwms.setTitle("Global WMS");
        geoServer.add(gwms);

        // Make a second global service
        ServiceInfo gwfs = new org.geoserver.wfs.WFSInfoImpl();
        gwfs.setName("WFSG");
        gwfs.setTitle("Global WFS");
        geoServer.add(gwfs);

        // Make a workspace
        WorkspaceInfo ws2 = geoServer.getCatalog().getFactory().createWorkspace();
        ws2.setName("TEST-WORKSPACE-2");
        geoServer.getCatalog().add(ws2);

        // Make a service for that workspace
        ServiceInfo ws2wms = new org.geoserver.wms.WMSInfoImpl();
        ws2wms.setWorkspace(ws2);
        ws2wms.setName("WMS2");
        ws2wms.setTitle("WMS for WS2");
        geoServer.add(ws2wms);

        // Make a second for that workspace
        ServiceInfo ws2wfs = new org.geoserver.wfs.WFSInfoImpl();
        ws2wfs.setWorkspace(ws2);
        ws2wfs.setName("WFS2");
        ws2wfs.setTitle("WFS for WS2");
        geoServer.add(ws2wfs);

        // Check that we get the services we expect to
        assertThat(geoServer.getService(org.geoserver.wms.WMSInfo.class), equalTo(gwms));
        assertThat(geoServer.getService(org.geoserver.wfs.WFSInfo.class), equalTo(gwfs));
        assertThat(
                (Collection<ServiceInfo>) geoServer.getServices(),
                allOf(hasItems(gwms, gwfs), not(hasItems(ws1wms, ws1wfs, ws2wms, ws2wfs))));
        assertThat(geoServer.getService(ws1, org.geoserver.wms.WMSInfo.class), equalTo(ws1wms));
        assertThat(geoServer.getService(ws1, org.geoserver.wfs.WFSInfo.class), equalTo(ws1wfs));
        assertThat(
                (Collection<ServiceInfo>) geoServer.getServices(ws1),
                allOf(hasItems(ws1wms, ws1wfs), not(hasItems(gwms, gwfs, ws2wms, ws2wfs))));
        assertThat(geoServer.getService(ws2, org.geoserver.wms.WMSInfo.class), equalTo(ws2wms));
        assertThat(geoServer.getService(ws2, org.geoserver.wfs.WFSInfo.class), equalTo(ws2wfs));
        assertThat(
                (Collection<ServiceInfo>) geoServer.getServices(ws2),
                allOf(hasItems(ws2wms, ws2wfs), not(hasItems(gwms, gwfs, ws1wms, ws1wfs))));
    }

    @Override
    public void testModifyLogging() {
        // TODO: make this work
    }
}
