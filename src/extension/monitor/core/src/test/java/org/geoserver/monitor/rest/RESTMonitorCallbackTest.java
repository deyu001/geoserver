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

package org.geoserver.monitor.rest;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.monitor.MemoryMonitorDAO;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.MonitorTestData;
import org.geoserver.monitor.RequestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.mock.web.MockHttpServletResponse;

public class RESTMonitorCallbackTest extends GeoServerSystemTestSupport {

    static Monitor monitor;

    RESTMonitorCallback callback;
    RequestData data;
    static Catalog catalog;

    public static Filter parseFilter(String cql) {
        try {
            return CQL.toFilter(cql);
        } catch (CQLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @BeforeClass
    public static void setUpData() throws Exception {
        MonitorDAO dao = new MemoryMonitorDAO();
        new MonitorTestData(dao).setup();

        MonitorConfig mc =
                new MonitorConfig() {

                    @Override
                    public MonitorDAO createDAO() {
                        MonitorDAO dao = new MemoryMonitorDAO();
                        try {
                            new MonitorTestData(dao).setup();
                            return dao;
                        } catch (java.text.ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public BboxMode getBboxMode() {
                        return BboxMode.FULL;
                    }
                };

        GeoServer gs = createMock(GeoServer.class);
        monitor = new Monitor(mc);
        monitor.setServer(gs);

        catalog = new CatalogImpl();

        expect(gs.getCatalog()).andStubReturn(catalog);
        replay(gs);

        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix("acme");
        ns.setURI("http://acme.org");
        catalog.add(ns);
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        FeatureTypeInfo ftFoo = catalog.getFactory().createFeatureType();
        ftFoo.setName("foo");
        ftFoo.setSRS("EPSG:4326");
        ftFoo.setNamespace(ns);
        ftFoo.setStore(ds);
        catalog.add(ftFoo);
        FeatureTypeInfo ftBar = catalog.getFactory().createFeatureType();
        ftBar.setName("bar");
        ftBar.setSRS("EPSG:3348");
        ftBar.setNamespace(ns);
        ftBar.setStore(ds);
        catalog.add(ftBar);
    }

    @Before
    public void setUp() throws Exception {
        callback = new RESTMonitorCallback(monitor);
        data = monitor.start();
    }

    @After
    public void tearDown() throws Exception {
        monitor.complete();
    }

    @Test
    public void testURLEncodedRequestPathInfo() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/layers/foo");
        assertEquals(404, response.getStatus());

        assertEquals("foo", data.getResources().get(1));

        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/layers/acme:foo");
        assertEquals(404, response.getStatus());

        assertEquals("acme:foo", data.getResources().get(2));

        response = getAsServletResponse(RestBaseController.ROOT_PATH + "acme:foo");
        assertEquals(404, response.getStatus());

        assertEquals("acme:foo", data.getResources().get(3));
    }
}
