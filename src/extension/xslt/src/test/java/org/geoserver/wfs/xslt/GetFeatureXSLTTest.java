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

package org.geoserver.wfs.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GetFeatureXSLTTest extends XSLTTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // TODO Auto-generated method stub
        super.setUpTestData(testData);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void setupXSLT() throws IOException {
        File dd = getTestData().getDataDirectoryRoot().getCanonicalFile();
        File wfs = new File(dd, "wfs");
        File transform = new File(wfs, "transform");
        deleteDirectory(transform);
        assertTrue(transform.mkdirs());
        FileUtils.copyDirectory(new File("src/test/resources/org/geoserver/wfs/xslt"), transform);

        // makes sure the output format list is updated
        XSLTOutputFormatUpdater updater = applicationContext.getBean(XSLTOutputFormatUpdater.class);
        updater.run();
    }

    @Test
    public void testGetCapabilities() throws Exception {
        // force the list of output formats provided by the xslt output format to be updated
        XSLTOutputFormatUpdater updater =
                (XSLTOutputFormatUpdater) applicationContext.getBean("xsltOutputFormatUpdater");
        updater.run();

        // now we can run the request
        Document dom = getAsDOM("wfs?service=wfs&version=1.1.0&request=GetCapabilities");
        // print(dom);

        XMLAssert.assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetFeature']/ows:Parameter[@name = 'outputFormat']/ows:Value[text() = 'HTML'])",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetFeature']/ows:Parameter[@name = 'outputFormat' and ows:Value = 'text/html; subtype=xslt'])",
                dom);
    }

    @Test
    public void testOutputFormatWrongCase() throws Exception {
        Document d =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&outputFormat="
                                + "text/html; subtype=xslt".toUpperCase());
        // print(d);

        checkOws10Exception(d, ServiceException.INVALID_PARAMETER_VALUE, "outputFormat");
    }

    @Test
    public void testGeneralOutput() throws Exception {
        Document d =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BUILDINGS)
                                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");
        // print(d);

        // two features
        XMLAssert.assertXpathEvaluatesTo("2", "count(//h2)", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//h2[text() = 'Buildings.1107531701010'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//h2[text() = 'Buildings.1107531701011'])", d);

        // check the first
        XMLAssert.assertXpathEvaluatesTo(
                "113",
                "//h2[text() = 'Buildings.1107531701010']/following-sibling::table/tr[td='cite:FID']/td[2]",
                d);
        XMLAssert.assertXpathEvaluatesTo(
                "123 Main Street",
                "//h2[text() = 'Buildings.1107531701010']/following-sibling::table/tr[td='cite:ADDRESS']/td[2]",
                d);
    }

    @Test
    public void testHeaders() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BUILDINGS)
                                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");

        assertEquals("text/html; subtype=xslt", response.getContentType());
        assertEquals("inline; filename=Buildings.html", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testHeadersTwoLayers() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BUILDINGS)
                                + ","
                                + getLayerId(MockData.LAKES)
                                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");

        assertEquals("text/html; subtype=xslt", response.getContentType());
        assertEquals(
                "inline; filename=Buildings_Lakes.html", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testLayerSpecific() throws Exception {
        Document d =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BRIDGES)
                                + "&version=1.0.0&service=wfs&outputFormat=text/html; subtype=xslt");
        // print(d);

        // just one features
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul)", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'ID: Bridges.1107531599613'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'FID: 110'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'Name: Cam Bridge'])", d);
    }

    @Test
    public void testMimeType() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BRIDGES)
                                + "&version=1.0.0&service=wfs&outputFormat=HTML");
        assertEquals("text/html", response.getContentType());

        Document d = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));

        // just one features
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul)", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'ID: Bridges.1107531599613'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'FID: 110'])", d);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ul[li = 'Name: Cam Bridge'])", d);
    }

    @Test
    public void testLayerSpecificOnOtherLayer() throws Exception {
        Document d =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&version=1.1.0&service=wfs&outputFormat=HTML");

        print(d);
        checkOws10Exception(d, ServiceException.INVALID_PARAMETER_VALUE, "typeName");
    }

    @Test
    public void testIncompatibleMix() throws Exception {
        Document d =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BRIDGES)
                                + ","
                                + getLayerId(MockData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&outputFormat=text/html; subtype=xslt");
        // print(d);

        checkOws10Exception(d, ServiceException.INVALID_PARAMETER_VALUE, "typeName");
    }

    @Test
    public void testNoOutputFormats() throws Exception {
        // clean up the config
        File dd = getTestData().getDataDirectoryRoot();
        File wfs = new File(dd, "wfs");
        File transform = new File(wfs, "transform");
        if (transform.exists()) {
            deleteDirectory(transform);
        }

        // makes sure the output format list is updated
        XSLTOutputFormatUpdater updater = applicationContext.getBean(XSLTOutputFormatUpdater.class);
        updater.run();

        // now run a GML2 request, it should work fine (GEOS-5804)
        Document d =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(MockData.BRIDGES)
                                + ","
                                + getLayerId(MockData.BUILDINGS)
                                + "&version=1.0.0&service=wfs");
        XMLAssert.assertXpathEvaluatesTo("1", "count(/wfs:FeatureCollection)", d);
    }
}
