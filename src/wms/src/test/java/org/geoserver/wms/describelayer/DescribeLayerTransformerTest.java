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

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import javax.xml.transform.TransformerException;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit test suite for {@link DescribeLayerTransformer}
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class DescribeLayerTransformerTest {

    /**
     * A request for the tests to fill up with the test spficic parameters. setUp creates it whit a
     * mocked up catalog
     */
    private DescribeLayerRequest request;

    private DescribeLayerTransformer transformer;

    private CatalogImpl catalog;

    private FeatureTypeInfoImpl featureTypeInfo;

    private CoverageInfoImpl coverageInfo;

    private LayerInfoImpl vectorLayerInfo;

    private LayerInfoImpl coverageLayerInfo;

    /** Sets up a base request with a mocked up geoserver and catalog for the tests */
    @Before
    public void setUp() throws Exception {
        GeoServerImpl geoServerImpl = new GeoServerImpl();
        catalog = new CatalogImpl();
        geoServerImpl.setCatalog(catalog);

        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setPrefix("fakeWs");
        ns.setURI("http://fakews.org");

        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("fakeWs");
        workspace.setName("fakeWs");

        DataStoreInfoImpl dataStoreInfo = new DataStoreInfoImpl(catalog);
        dataStoreInfo.setName("fakeDs");
        dataStoreInfo.setId("fakeDs");
        dataStoreInfo.setWorkspace(workspace);

        featureTypeInfo = new FeatureTypeInfoImpl(catalog);
        featureTypeInfo.setNamespace(ns);
        featureTypeInfo.setName("states");
        featureTypeInfo.setStore(dataStoreInfo);

        vectorLayerInfo = new LayerInfoImpl();
        vectorLayerInfo.setResource(featureTypeInfo);
        vectorLayerInfo.setId("states");
        vectorLayerInfo.setName("states");

        catalog.add(workspace);
        catalog.add(ns);
        catalog.add(dataStoreInfo);
        catalog.add(featureTypeInfo);
        catalog.add(vectorLayerInfo);

        CoverageStoreInfoImpl coverageStoreInfo = new CoverageStoreInfoImpl(catalog);
        coverageStoreInfo.setId("coverageStore");
        coverageStoreInfo.setName("coverageStore");
        coverageStoreInfo.setWorkspace(workspace);

        coverageInfo = new CoverageInfoImpl(catalog);
        coverageInfo.setNamespace(ns);
        coverageInfo.setName("fakeCoverage");
        coverageInfo.setStore(coverageStoreInfo);

        coverageLayerInfo = new LayerInfoImpl();
        coverageLayerInfo.setResource(coverageInfo);
        coverageLayerInfo.setId("fakeCoverage");
        coverageLayerInfo.setName("fakeCoverage");

        catalog.add(coverageStoreInfo);
        catalog.add(coverageInfo);
        catalog.add(coverageLayerInfo);

        geoServerImpl.add(new WMSInfoImpl());
        request = new DescribeLayerRequest();
        request.setBaseUrl("http://localhost:8080/geoserver");
        request.setVersion(WMS.VERSION_1_1_1.toString());
    }

    @Test
    public void testPreconditions() throws TransformerException {
        try {
            new DescribeLayerTransformer(null);
            Assert.fail("expected NPE on null base url");
        } catch (NullPointerException e) {
            Assert.assertTrue(true);
        }

        transformer = new DescribeLayerTransformer("http://geoserver.org");
        try {
            transformer.transform(null);
            Assert.fail("expected IAE on null request");
        } catch (TransformerException e) {
            Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
        try {
            transformer.transform(new Object());
            fail("expected IAE on argument non a DescribeLayerRequest instance");
        } catch (TransformerException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    /**
     * Test the root element name and version attribute.
     *
     * <p>This test does not set a requested layer to the request and {@link
     * DescribeLayerTransformer} does not care since checking the mandatory arguments shall be done
     * prior to using the transformer, so it'll return an empty root element in this case.
     */
    @Test
    public void testRootElement() throws Exception {
        transformer = new DescribeLayerTransformer("http://geoserver.org");
        Document dom = WMSTestSupport.transform(request, transformer);
        Element root = dom.getDocumentElement();
        assertEquals("WMS_DescribeLayerResponse", root.getNodeName());
        assertEquals("1.1.1", root.getAttribute("version"));
    }

    @Test
    public void testDTDLocation() throws Exception {
        final String expected =
                "!DOCTYPE WMS_DescribeLayerResponse SYSTEM \"http://geoserver.org/schemas/wms/1.1.1/WMS_DescribeLayerResponse.dtd\"";
        transformer = new DescribeLayerTransformer("http://geoserver.org");
        StringWriter writer = new StringWriter();
        transformer.transform(request, writer);
        assertTrue(writer.getBuffer().indexOf(expected) > 0);
    }

    @Test
    public void testSingleVectorLayer() throws Exception {
        MapLayerInfo mapLayerInfo = new MapLayerInfo(vectorLayerInfo);

        request.addLayer(mapLayerInfo);

        final String serverBaseUrl = "http://geoserver.org";

        transformer = new DescribeLayerTransformer(serverBaseUrl);
        final Document dom = WMSTestSupport.transform(request, transformer);

        final String layerDescPath = "/WMS_DescribeLayerResponse/LayerDescription";
        assertXpathExists(layerDescPath, dom);
        assertXpathEvaluatesTo("fakeWs:states", layerDescPath + "/@name", dom);

        final String expectedWfsAtt = serverBaseUrl + "/wfs?";
        assertXpathExists(layerDescPath + "/@wfs", dom);
        assertXpathEvaluatesTo(expectedWfsAtt, layerDescPath + "/@wfs", dom);

        assertXpathExists(layerDescPath + "/@owsURL", dom);
        assertXpathEvaluatesTo(expectedWfsAtt, layerDescPath + "/@owsURL", dom);

        assertXpathExists(layerDescPath + "/@owsType", dom);
        assertXpathEvaluatesTo("WFS", layerDescPath + "/@owsType", dom);

        assertXpathExists(layerDescPath + "/Query", dom);
        assertXpathEvaluatesTo("fakeWs:states", layerDescPath + "/Query/@typeName", dom);
    }

    @Test
    public void testSingleRasterLayer() throws Exception {
        MapLayerInfo mapLayerInfo = new MapLayerInfo(coverageLayerInfo);

        request.addLayer(mapLayerInfo);

        final String serverBaseUrl = "http://geoserver.org";

        transformer = new DescribeLayerTransformer(serverBaseUrl);
        final Document dom = WMSTestSupport.transform(request, transformer);

        final String layerDescPath = "/WMS_DescribeLayerResponse/LayerDescription";
        assertXpathExists(layerDescPath, dom);
        assertXpathEvaluatesTo("fakeWs:fakeCoverage", layerDescPath + "/@name", dom);

        // no wfs attribute for a coverage layer
        assertXpathEvaluatesTo("", layerDescPath + "/@wfs", dom);

        assertXpathExists(layerDescPath + "/@owsURL", dom);
        final String expectedOWSURLAtt = serverBaseUrl + "/wcs?";
        assertXpathEvaluatesTo(expectedOWSURLAtt, layerDescPath + "/@owsURL", dom);

        assertXpathExists(layerDescPath + "/@owsType", dom);
        assertXpathEvaluatesTo("WCS", layerDescPath + "/@owsType", dom);

        assertXpathExists(layerDescPath + "/Query", dom);
        assertXpathEvaluatesTo("fakeWs:fakeCoverage", layerDescPath + "/Query/@typeName", dom);
    }

    @Test
    public void testMultipleLayers() throws Exception {
        request.addLayer(new MapLayerInfo(vectorLayerInfo));
        request.addLayer(new MapLayerInfo(coverageLayerInfo));

        final String serverBaseUrl = "http://geoserver.org";

        transformer = new DescribeLayerTransformer(serverBaseUrl);
        final Document dom = WMSTestSupport.transform(request, transformer);

        final String layerDescPath1 = "/WMS_DescribeLayerResponse/LayerDescription[1]";
        final String layerDescPath2 = "/WMS_DescribeLayerResponse/LayerDescription[2]";

        assertXpathExists(layerDescPath1, dom);
        assertXpathExists(layerDescPath2, dom);
        assertXpathEvaluatesTo("fakeWs:states", layerDescPath1 + "/@name", dom);
        assertXpathEvaluatesTo("fakeWs:fakeCoverage", layerDescPath2 + "/@name", dom);
    }
}
