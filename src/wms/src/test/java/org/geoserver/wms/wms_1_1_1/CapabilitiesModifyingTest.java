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

package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CapabilitiesModifyingTest extends GeoServerSystemTestSupport {

    @Before
    public void resetWmsConfigChanges() {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.OGC_EXCEPTION_REPORT);
        getGeoServer().save(global);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        for (FeatureTypeInfo ft : catalog.getFeatureTypes()) {
            ft.setLatLonBoundingBox(null);
            catalog.save(ft);
        }

        // create a misconfigured layer group
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.LAKES)));
        lg.getStyles().add(null);
        lg.setName("test");
        lg.setMode(Mode.NAMED);

        catalog.add(lg);
    }

    @Test
    public void testMisconfiguredLayerGeneratesErrorDocumentInDefaultConfig() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wms?service=WMS&request=GetCapabilities&version=1.1.1");
        assertTrue(
                "Response does not contain ServiceExceptionReport: "
                        + response.getContentAsString(),
                response.getContentAsString().endsWith("</ServiceExceptionReport>"));
    }

    @Test
    public void testMisconfiguredLayerIsSkippedWhenWMSServiceIsConfiguredThatWay()
            throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);

        Document caps = getAsDOM("wms?service=WMS&request=GetCapabilities&version=1.1.1");

        assertEquals("WMT_MS_Capabilities", caps.getDocumentElement().getTagName());
        // we misconfigured all the layers in the server, so there should be no named layers now.
        XMLAssert.assertXpathEvaluatesTo("", "//Layer/Name/text()", caps);
    }

    @Test
    public void testMisconfiguredLayerGeneratesErrorDocumentInDefaultConfig_1_3_0()
            throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("wms?service=WMS&request=GetCapabilities&version=1.3.0");
        assertTrue(
                "Response does not contain ServiceExceptionReport: "
                        + response.getContentAsString(),
                response.getContentAsString().endsWith("</ServiceExceptionReport>"));
    }

    @Test
    public void testMisconfiguredLayerIsSkippedWhenWMSServiceIsConfiguredThatWay_1_3_0()
            throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);

        Document caps = getAsDOM("wms?service=WMS&request=GetCapabilities&version=1.3.0");

        assertEquals("WMS_Capabilities", caps.getDocumentElement().getTagName());
        // we misconfigured all the layers in the server, so there should be no named layers now.
        XMLAssert.assertXpathEvaluatesTo("", "//Layer/Name/text()", caps);
    }
}
