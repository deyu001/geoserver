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

package org.geoserver.nsg.timeout;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.geotools.data.simple.SimpleFeatureStore;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.servlets.ServiceStrategyFactory;
import org.w3c.dom.Document;

public class TimeoutTest extends WFS20TestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        SimpleFeatureStore fs =
                (SimpleFeatureStore)
                        getCatalog().getFeatureTypeByName("Fifteen").getFeatureSource(null, null);
        // double the number of features at each iteration to get a large amount of them, breaking
        // eventual output buffering in GML encoder. Size goes like this, 15, 30, 60, 120, 240, 480
        for (int i = 0; i < 5; i++) {
            fs.addFeatures(fs.getFeatures());
        }

        // prime the various beans and data structure involved so that we don't get one of those
        // large "one time" initialization happening during encoding and the like
        getAsDOM("wfs?request=GetFeature&typenames=cdf:Fifteen&version=2.0.0&service=wfs");
    }

    @Before
    public void resetTimeout() {
        // set the default value to one second
        setTimeout(1);
        // set the execution delaySeconds to zero
        setExecutionDelay(0);
        setEncodeDelay(0, 0);
        // set the service strategy to fast, otherwise tests with small outputs will
        // be playing with buffering and get unexpected results
        ServiceStrategyFactory serviceStrategyFactory =
                GeoServerExtensions.bean(ServiceStrategyFactory.class);
        serviceStrategyFactory.setServiceStrategy("SPEED");
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/nsg/timeout/timeoutApplicationContext.xml");
    }

    @Test
    public void testNoTimeout() throws Exception {
        setTimeout(0);

        // no timeout happening
        Document dom =
                getAsDOM("wfs?request=GetFeature&typenames=cdf:Fifteen&version=2.0.0&service=wfs");
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", dom);
        assertXpathEvaluatesTo("480", "count(//cdf:Fifteen)", dom);
    }

    @Test
    public void testTimeoutBeforeEncoding() throws Exception {
        // timeout in one, but delay two, should not even start encoding
        setTimeout(1);
        setExecutionDelay(2);

        Document dom =
                getAsDOM("wfs?request=GetFeature&typenames=cdf:Fifteen&version=2.0.0&service=wfs");
        // print(dom);
        checkOws11Exception(dom, "2.0.0", TimeoutVerifier.TIMEOUT_EXCEPTION_CODE, "GetFeature");
    }

    @Test
    public void testTimeoutOnGMLEncodingStart() throws Exception {
        // timeout in two, but delay three right away, so that streaming does not even start writing
        // stuff out
        setTimeout(2);
        setExecutionDelay(0);
        setEncodeDelay(3, 0);

        Document dom =
                getAsDOM("wfs?request=GetFeature&typenames=cdf:Fifteen&version=2.0.0&service=wfs");
        // print(dom);
        checkOws11Exception(dom, "2.0.0", TimeoutVerifier.TIMEOUT_EXCEPTION_CODE, "GetFeature");
    }

    @Test
    public void testTimeoutAfterStreamingEncodingStart() throws Exception {
        setTimeout(2);
        setExecutionDelay(0);
        setEncodeDelay(3, 400);

        Document dom =
                getAsDOM("wfs?request=GetFeature&typenames=cdf:Fifteen&version=2.0.0&service=wfs");
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", dom);
        assertXpathEvaluatesTo("480", "count(//cdf:Fifteen)", dom);
    }

    @Test
    public void testTimeoutShapefileEncoding() throws Exception {
        // timeout in 2 seconds, encode 14 feature before delay, the stream is not yet written
        // to though, so the results should be an exception
        setTimeout(2);
        setExecutionDelay(0);
        setEncodeDelay(3, 14);

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?request=GetFeature&typenames=cdf:Fifteen&version=2.0.0&service=wfs&outputFormat=SHAPE-ZIP");
        assertEquals("application/xml", response.getContentType());
        // This one does not work due to a bug in MockHttpServletResponse, asking for header values
        // to be non null, while the javadoc does not make any such request
        // assertNull(response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        // print(dom);
        checkOws11Exception(dom, "2.0.0", TimeoutVerifier.TIMEOUT_EXCEPTION_CODE, "GetFeature");
    }

    public void setTimeout(int timeout) {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.getMetadata().put(TimeoutCallback.TIMEOUT_CONFIG_KEY, timeout);
        gs.save(wfs);
    }

    public void setExecutionDelay(int executionDelay) {
        GetFeatureWaitOnExecuteCallback wait =
                GeoServerExtensions.bean(GetFeatureWaitOnExecuteCallback.class);
        wait.delaySeconds = executionDelay;
    }

    public void setEncodeDelay(int encodingDelay, int afterFeatures) {
        GetFeatureWaitOnEncodeCallback wait =
                GeoServerExtensions.bean(GetFeatureWaitOnEncodeCallback.class);
        wait.delaySeconds = encodingDelay;
        wait.delayAfterFeatures = afterFeatures;
    }
}
