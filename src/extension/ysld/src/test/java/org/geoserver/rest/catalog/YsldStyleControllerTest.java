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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.ysld.YsldHandler;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class YsldStyleControllerTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;
    protected static XpathEngine xp;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRawPutYSLD() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        String xml =
                "<style>"
                        + "<name>foo</name>"
                        + "<format>ysld</format>"
                        + "<filename>foo.yaml</filename>"
                        + "</style>";
        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("foo"));

        String content = newYSLD();
        response = putAsServletResponse("/rest/styles/foo?raw=true", content, YsldHandler.MIMETYPE);
        assertEquals(200, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/foo.yaml");

        String definition = new String(resource.getContents());
        assertTrue("is yaml", definition.contains("stroke-color: '#FF0000'"));

        StyleInfo styleInfo = catalog.getStyleByName("foo");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        content = new String(out.toByteArray());
        assertTrue(content.contains("<sld:Name>foo</sld:Name>"));
        catalog.remove(styleInfo);
    }

    @Test
    public void testPostYSLD() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        String content = newYSLD();
        MockHttpServletResponse response =
                postAsServletResponse("/rest/styles?name=foo", content, YsldHandler.MIMETYPE);
        assertEquals(201, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/foo.yaml");

        String definition = new String(resource.getContents());
        assertTrue("is yaml", definition.contains("stroke-color: '#FF0000'"));

        StyleInfo styleInfo = catalog.getStyleByName("foo");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        content = new String(out.toByteArray());
        assertTrue(content.contains("<sld:Name>foo</sld:Name>"));
        catalog.remove(styleInfo);
    }

    @Test
    public void testPutYSLD() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("bar"));

        String xml =
                "<style>"
                        + "<name>bar</name>"
                        + "<format>ysld</format>"
                        + "<filename>bar.yaml</filename>"
                        + "</style>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("bar"));

        String content = newYSLD();
        response = putAsServletResponse("/rest/styles/bar", content, YsldHandler.MIMETYPE);
        assertEquals(200, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/bar.yaml");

        String definition = new String(resource.getContents());
        assertTrue("is yaml", definition.contains("stroke-color: '#FF0000'"));

        StyleInfo styleInfo = catalog.getStyleByName("bar");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        content = new String(out.toByteArray());
        assertTrue(content.contains("<sld:Name>bar</sld:Name>"));

        catalog.remove(styleInfo);
    }

    String newYSLD() {
        return "title: valid ysld\n"
                + "symbolizers:\n"
                + "- line:\n"
                + "    stroke-width: 1.0\n"
                + "    stroke-color: '#FF0000'";
    }
}
