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

package org.geoserver.rest.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;

public class MBStyleControllerTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;

    private static SimpleNamespaceContext namespaceContext;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        catalog = getCatalog();

        namespaceContext = new org.springframework.util.xml.SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("sld", "http://www.opengis.net/sld");
        namespaceContext.bindNamespaceUri("ogc", "http://www.opengis.net/ogc");

        testData.addStyle(
                null,
                "teststyle",
                "teststyle.json",
                this.getClass(),
                catalog,
                Collections.singletonMap(StyleProperty.FORMAT, MBStyleHandler.FORMAT));

        testData.addStyle(
                null,
                "multilayer",
                "multilayer.json",
                this.getClass(),
                catalog,
                Collections.singletonMap(StyleProperty.FORMAT, MBStyleHandler.FORMAT));
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void getBodyAsJsonUsingAcceptHeader() throws Exception {
        MockHttpServletRequest request = createRequest("/rest/styles/teststyle");
        request.setMethod("GET");
        request.addHeader("Accept", MBStyleHandler.MIME_TYPE);
        MockHttpServletResponse response = dispatch(request);

        assertEquals(200, response.getStatus());
        assertEquals(MBStyleHandler.MIME_TYPE, response.getContentType());
        String responseContent = response.getContentAsString();
        String expected =
                IOUtils.toString(this.getClass().getResourceAsStream("teststyle.json"), "UTF-8");
        assertEquals(expected, responseContent);
    }

    @Test
    public void getInfoAsJsonUsingExtension() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/styles/teststyle.json");
        assertEquals(200, response.getStatus());
        String responseContent = response.getContentAsString();
        assertEquals("application/json", response.getContentType());

        // Assert that the response contains the style info as json
        assertEquals(
                "{\"style\":{\"name\":\"teststyle\","
                        + "\"format\":\"mbstyle\","
                        + "\"languageVersion\":{\"version\":\"1.0.0\"},"
                        + "\"filename\":\"teststyle.json\"}}",
                responseContent);
    }

    @Test
    public void getBodyAsSLDUsingAcceptHeader() throws Exception {
        MockHttpServletRequest request = createRequest("/rest/styles/teststyle");
        request.setMethod("GET");
        request.addHeader("Accept", SLDHandler.MIMETYPE_10);
        MockHttpServletResponse response = dispatch(request);

        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        String content = response.getContentAsString();
        assertTrue(content.contains("<sld:Name>test-layer</sld:Name>"));
    }

    @Test
    public void getBodyAsSLDUsingExtension() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/styles/teststyle.sld");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        String content = response.getContentAsString();
        assertTrue(content.contains("<sld:Name>test-layer</sld:Name>"));
    }

    @Test
    public void getBodyAsSLDMultiLayer() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/styles/multilayer.sld");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        // two named layers have been generated, they both show up in output
        assertThat(
                dom,
                hasXPath(
                        "//sld:NamedLayer[1]/sld:Name",
                        namespaceContext,
                        equalTo("test-source-layer1")));
        assertThat(
                dom,
                hasXPath(
                        "//sld:NamedLayer[2]/sld:Name",
                        namespaceContext,
                        equalTo("test-source-layer2")));
    }

    @Test
    public void getAsHTML() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/styles/teststyle.html");
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_HTML_VALUE, response.getContentType());
        String content = response.getContentAsString();
        assertTrue(
                content.contains(
                        "<a href=\"http://localhost:8080/geoserver/rest/styles/teststyle"));
    }

    public String newMbStyle() {
        String jsonBody =
                "{"
                        + "    \"layers\": ["
                        + "        {"
                        + "            \"id\": \"foo\","
                        + "            \"type\": \"circle\","
                        + "            \"layout\": {"
                        + "                \"visibility\": \"visible\""
                        + "            },"
                        + "            \"paint\": {"
                        + "                \"circle-color\": \"#FFFFFF\""
                        + "            }"
                        + "        }"
                        + "    ]"
                        + "}";
        return jsonBody;
    }

    @Test
    public void testRawPutJson() throws Exception {
        String jsonBody = newMbStyle();
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        String xml =
                "<style>"
                        + "<name>foo</name>"
                        + "<format>"
                        + MBStyleHandler.FORMAT
                        + "</format>"
                        + "<filename>foo.json</filename>"
                        + "</style>";
        MockHttpServletResponse response = postAsServletResponse("/rest/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("foo"));

        // step 2 define mbstyle json
        response =
                putAsServletResponse(
                        "/rest/styles/foo?raw=true", jsonBody, MBStyleHandler.MIME_TYPE);
        assertEquals(200, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();
        Resource resource = resources.get("/styles/foo.json");
        String definition = new String(resource.getContents());
        assertTrue("is json", definition.contains("\"circle-color\": \"#FFFFFF\""));

        StyleInfo styleInfo = catalog.getStyleByName("foo");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        String contentOut = new String(out.toByteArray());
        assertTrue(contentOut.contains("<sld:Name>foo</sld:Name>"));
        catalog.remove(styleInfo);
    }

    @Test
    public void testPostJson() throws Exception {
        String jsonBody = newMbStyle();
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        MockHttpServletResponse response =
                postAsServletResponse("/rest/styles?name=foo", jsonBody, MBStyleHandler.MIME_TYPE);
        assertEquals(201, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();
        Resource resource = resources.get("/styles/foo.json");
        String definition = new String(resource.getContents());
        assertTrue("is json", definition.contains("\"circle-color\": \"#FFFFFF\""));

        StyleInfo styleInfo = catalog.getStyleByName("foo");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        String contentOut = new String(out.toByteArray());
        assertTrue(contentOut.contains("<sld:Name>foo</sld:Name>"));
        catalog.remove(styleInfo);
    }
}
