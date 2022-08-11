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

package org.geoserver.wms.describelayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test suite for {@link DescribeLayerKvpRequestReader}
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class DescribeLayerKvpRequestReaderTest {

    private GeoServerImpl geoServerImpl;

    private WMS wms;

    private Map<String, String> params;

    @Before
    public void setUp() throws Exception {
        geoServerImpl = new GeoServerImpl();
        geoServerImpl.add(new WMSInfoImpl());
        wms = new WMS(geoServerImpl);
        params = new HashMap<>();
    }

    @After
    public void tearDown() throws Exception {
        wms = null;
        params = null;
    }

    private DescribeLayerRequest getRequest(Map<String, String> rawKvp) throws Exception {
        return getRequest(rawKvp, new HashMap<>(rawKvp));
    }

    private DescribeLayerRequest getRequest(Map<String, String> rawKvp, Map<String, Object> kvp)
            throws Exception {

        DescribeLayerKvpRequestReader reader = new DescribeLayerKvpRequestReader(wms);
        DescribeLayerRequest req = (DescribeLayerRequest) reader.createRequest();
        return (DescribeLayerRequest) reader.read(req, kvp, rawKvp);
    }

    @Test
    public void testGetRequestNoVersion() throws Exception {
        params.put("LAYERS", "topp:states");
        try {
            getRequest(params);
            fail("expected ServiceException if version is not provided");
        } catch (ServiceException e) {
            assertEquals("NoVersionInfo", e.getCode());
        }
    }

    @Test
    public void testGetRequestInvalidVersion() throws Exception {
        params.put("LAYERS", "topp:states");
        params.put("VERSION", "fakeVersion");
        try {
            getRequest(params);
            fail("expected ServiceException if the wrong version is requested");
        } catch (ServiceException e) {
            assertEquals("InvalidVersion", e.getCode());
        }
    }

    @Test
    public void testGetRequestNoLayerRequested() throws Exception {
        params.put("VERSION", "1.1.1");
        try {
            getRequest(params);
            fail("expected ServiceException if no layer is requested");
        } catch (ServiceException e) {
            assertEquals("NoLayerRequested", e.getCode());
        }
    }

    @Test
    public void testGetRequest() throws Exception {
        CatalogImpl catalog = new CatalogImpl();
        geoServerImpl.setCatalog(catalog);
        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setPrefix("topp");
        ns.setURI("http//www.geoserver.org");

        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("fakeWs");
        workspace.setName("fakeWs");

        DataStoreInfoImpl dataStoreInfo = new DataStoreInfoImpl(catalog);
        dataStoreInfo.setName("fakeDs");
        dataStoreInfo.setId("fakeDs");
        dataStoreInfo.setWorkspace(workspace);

        FeatureTypeInfoImpl featureTypeInfo = new FeatureTypeInfoImpl(catalog);
        featureTypeInfo.setNamespace(ns);
        featureTypeInfo.setName("states");
        featureTypeInfo.setStore(dataStoreInfo);

        final LayerInfoImpl layerInfo = new LayerInfoImpl();
        layerInfo.setResource(featureTypeInfo);
        layerInfo.setId("states");
        layerInfo.setName("states");

        catalog.add(ns);
        catalog.add(workspace);
        catalog.add(dataStoreInfo);
        catalog.add(featureTypeInfo);
        catalog.add(layerInfo);

        params.put("VERSION", "1.1.1");

        CoverageStoreInfoImpl coverageStoreInfo = new CoverageStoreInfoImpl(catalog);
        coverageStoreInfo.setId("coverageStore");
        coverageStoreInfo.setName("coverageStore");
        coverageStoreInfo.setWorkspace(workspace);

        CoverageInfoImpl coverageInfo = new CoverageInfoImpl(catalog);
        coverageInfo.setNamespace(ns);
        coverageInfo.setName("fakeCoverage");
        coverageInfo.setStore(coverageStoreInfo);

        LayerInfoImpl layerInfo2 = new LayerInfoImpl();
        layerInfo2.setResource(coverageInfo);
        layerInfo2.setId("fakeCoverage");
        layerInfo2.setName("fakeCoverage");

        catalog.add(coverageStoreInfo);
        catalog.add(coverageInfo);
        catalog.add(layerInfo2);

        params.put("LAYERS", "topp:states,topp:fakeCoverage");
        Map<String, Object> kvp = new HashMap<>(params);
        kvp.put("LAYERS", Arrays.asList(new MapLayerInfo(layerInfo), new MapLayerInfo(layerInfo2)));
        DescribeLayerRequest describeRequest = getRequest(params, kvp);
        assertNotNull(describeRequest);
        assertNotNull(describeRequest.getLayers());
        assertEquals(2, describeRequest.getLayers().size());
    }
}
