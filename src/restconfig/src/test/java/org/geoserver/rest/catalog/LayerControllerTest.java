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

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LayerControllerTest extends CatalogRESTTestSupport {

    @After
    public void revertChanges() throws IOException {
        revertLayer(SystemTestData.BUILDINGS);
        revertLayer(SystemTestData.BRIDGES);
        StyleInfo si = getCatalog().getStyleByName("cite", "foo");
        if (si != null) {
            getCatalog().remove(si);
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
    }

    @Test
    public void testGetListAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/layers.xml", 200);
        assertEquals("layers", dom.getDocumentElement().getNodeName());

        // verify layer name and links for cite:Buildings
        assertXpathExists("//layer[name='cite:Buildings']", dom);
        assertThat(
                xp.evaluate("//layer[name='cite:Buildings']/atom:link/@href", dom),
                endsWith(RestBaseController.ROOT_PATH + "/layers/cite%3ABuildings.xml"));
    }

    @Test
    public void testGetListInWorkspaceAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/cite/layers.xml", 200);
        assertEquals("layers", dom.getDocumentElement().getNodeName());
        print(dom);
        // verify layer name and links for cite:Buildings
        assertXpathExists("//layer[name='Buildings']", dom);
        assertThat(
                xp.evaluate("//layer[name='Buildings']/atom:link/@href", dom),
                endsWith(RestBaseController.ROOT_PATH + "/workspaces/cite/layers/Buildings.xml"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/layers/cite:Buildings.xml", 200);
        assertEquals("layer", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("Buildings", "/layer/name", dom);
        // check the layer name is actually the first child (GEOS-3336 risked modifying
        // the order)
        assertXpathEvaluatesTo("Buildings", "/layer/*[1]", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver" + ROOT_PATH + "/styles/Buildings.xml",
                "/layer/defaultStyle/atom:link/attribute::href",
                dom);
    }

    @Test
    public void testGetInWorkspaceAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/cite/layers/Buildings.xml", 200);
        assertEquals("layer", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("Buildings", "/layer/name", dom);
        // check the layer name is actually the first child (GEOS-3336 risked modifying
        // the order)
        assertXpathEvaluatesTo("Buildings", "/layer/*[1]", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver" + ROOT_PATH + "/styles/Buildings.xml",
                "/layer/defaultStyle/atom:link/attribute::href",
                dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(ROOT_PATH + "/layers/cite:Buildings.html", 200);
    }

    @Test
    public void testGetWrongLayer() throws Exception {
        // Parameters for the request
        String layer = "cite:Buildingssssss";
        // Request path
        String requestPath = ROOT_PATH + "/layers/" + layer + ".html";
        // Exception path
        String exception = "No such layer: " + layer;
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertTrue(response.getContentAsString().contains(exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        String message = response.getContentAsString();
        assertFalse(message, message.contains(exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/layers.xml", 200);
        assertXpathEvaluatesTo(catalog.getLayers().size() + "", "count(//layer)", dom);
    }

    @Test
    public void testGetAllInWorkspaceAsXML() throws Exception {
        Document dom = getAsDOM(ROOT_PATH + "/workspaces/cite/layers.xml", 200);
        int count =
                catalog.getResourcesByNamespace("cite", ResourceInfo.class)
                        .stream()
                        .mapToInt(info -> catalog.getLayers(info).size())
                        .sum();
        assertXpathEvaluatesTo(count + "", "count(//layer)", dom);
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        getAsDOM(ROOT_PATH + "/layers.html", 200);
    }

    @Test
    public void testPut() throws Exception {
        LayerInfo l = catalog.getLayerByName("cite:Buildings");
        assertEquals("Buildings", l.getDefaultStyle().getName());
        String xml =
                "<layer>"
                        + "<defaultStyle>Forests</defaultStyle>"
                        + "<styles>"
                        + "<style>Ponds</style>"
                        + "</styles>"
                        + "</layer>";
        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/layers/cite:Buildings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        l = catalog.getLayerByName("cite:Buildings");
        assertEquals("Forests", l.getDefaultStyle().getName());
        assertNotNull(l.getDateModified());
    }

    @Test
    public void testPutInWorkspace() throws Exception {
        LayerInfo l = catalog.getLayerByName("cite:Buildings");
        assertEquals("Buildings", l.getDefaultStyle().getName());
        String xml =
                "<layer>"
                        + "<defaultStyle>Forests</defaultStyle>"
                        + "<styles>"
                        + "<style>Ponds</style>"
                        + "</styles>"
                        + "</layer>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/workspaces/cite/layers/Buildings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        l = catalog.getLayerByName("cite:Buildings");
        assertEquals("Forests", l.getDefaultStyle().getName());
        assertNotNull(l.getDateModified());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        LayerInfo l = catalog.getLayerByName("cite:Buildings");

        assertTrue(l.isEnabled());
        boolean isAdvertised = l.isAdvertised();
        boolean isOpaque = l.isOpaque();
        boolean isQueryable = l.isQueryable();

        String xml =
                "<layer>"
                        + "<defaultStyle>Forests</defaultStyle>"
                        + "<styles>"
                        + "<style>Ponds</style>"
                        + "</styles>"
                        + "</layer>";
        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/layers/cite:Buildings", xml, "text/xml");
        assertEquals(200, response.getStatus());

        l = catalog.getLayerByName("cite:Buildings");

        assertTrue(l.isEnabled());
        assertEquals(isAdvertised, l.isAdvertised());
        assertEquals(isOpaque, l.isOpaque());
        assertEquals(isQueryable, l.isQueryable());
        assertNotNull(l.getDateModified());
    }

    @Test
    public void testUpdateStyleJSON() throws Exception {
        LayerInfo l = catalog.getLayerByName("cite:Buildings");
        assertEquals("Buildings", l.getDefaultStyle().getName());
        JSONObject json = (JSONObject) getAsJSON(ROOT_PATH + "/layers/cite:Buildings.json");
        // print(json);
        JSONObject layer = (JSONObject) json.get("layer");
        JSONObject style = (JSONObject) layer.get("defaultStyle");
        style.put("name", "polygon");
        style.put("href", "http://localhost:8080/geoserver/rest/styles/polygon.json");
        String updatedJson = json.toString();
        MockHttpServletResponse response =
                putAsServletResponse(
                        ROOT_PATH + "/layers/cite:Buildings", updatedJson, "application/json");
        assertEquals(200, response.getStatus());

        l = catalog.getLayerByName("cite:Buildings");
        assertEquals("polygon", l.getDefaultStyle().getName());
        assertNotNull(l.getDateModified());
    }

    @Test
    public void testDelete() throws Exception {
        assertNotNull(catalog.getLayerByName("cite:Buildings"));
        assertEquals(
                200, deleteAsServletResponse(ROOT_PATH + "/layers/cite:Buildings").getStatus());
        assertNull(catalog.getLayerByName("cite:Buildings"));
    }

    @Test
    public void testDeleteInWorkspace() throws Exception {
        assertNotNull(catalog.getLayerByName("cite:Buildings"));
        assertEquals(
                200,
                deleteAsServletResponse(ROOT_PATH + "/workspaces/cite/layers/Buildings")
                        .getStatus());
        assertNull(catalog.getLayerByName("cite:Buildings"));
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getLayerByName("cite:Buildings"));
        assertNotNull(catalog.getFeatureTypeByName("cite", "Buildings"));

        assertEquals(
                200, deleteAsServletResponse(ROOT_PATH + "/layers/cite:Buildings").getStatus());

        assertNull(catalog.getLayerByName("cite:Buildings"));
        assertNotNull(catalog.getFeatureTypeByName("cite", "Buildings"));

        assertNotNull(catalog.getLayerByName("cite:Bridges"));
        assertNotNull(catalog.getFeatureTypeByName("cite", "Bridges"));

        assertEquals(
                200,
                deleteAsServletResponse(ROOT_PATH + "/layers/cite:Bridges?recurse=true")
                        .getStatus());

        assertNull(catalog.getLayerByName("cite:Bridges"));
        assertNull(catalog.getFeatureTypeByName("cite", "Bridges"));
    }

    @Test
    public void testPutWorkspaceStyle() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("foo"));
        assertNull(cat.getStyleByName("cite", "foo"));

        String xml = "<style>" + "<name>foo</name>" + "<filename>foo.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/cite/styles", xml);

        // System.out.println(response.getContentAsString());
        assertEquals(201, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith(MediaType.TEXT_PLAIN_VALUE));
        assertNotNull(cat.getStyleByName("cite", "foo"));

        xml =
                "<layer>"
                        + "<defaultStyle>"
                        + "<name>foo</name>"
                        + "<workspace>cite</workspace>"
                        + "</defaultStyle>"
                        + "<enabled>true</enabled>"
                        + "</layer>";
        response =
                putAsServletResponse(ROOT_PATH + "/layers/cite:Buildings", xml, "application/xml");
        assertEquals(200, response.getStatus());

        LayerInfo l = cat.getLayerByName("cite:Buildings");
        assertNotNull(l.getDefaultStyle());
        assertEquals("foo", l.getDefaultStyle().getName());
        assertNotNull(l.getDefaultStyle().getWorkspace());
        assertNotNull(l.getDateModified());

        Document dom = getAsDOM(ROOT_PATH + "/layers/cite:Buildings.xml", 200);
        assertXpathExists("/layer/defaultStyle/name[text() = 'cite:foo']", dom);
        assertXpathExists("/layer/defaultStyle/workspace[text() = 'cite']", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver" + ROOT_PATH + "/workspaces/cite/styles/foo.xml",
                "//defaultStyle/atom:link/@href",
                dom);
    }

    @Test
    public void testPutWorkspaceAlternateStyle() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("foo"));
        assertNull(cat.getStyleByName("cite", "foo"));

        String xml = "<style>" + "<name>foo</name>" + "<filename>foo.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                postAsServletResponse(ROOT_PATH + "/workspaces/cite/styles", xml);
        assertEquals(201, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith(MediaType.TEXT_PLAIN_VALUE));
        assertNotNull(cat.getStyleByName("cite", "foo"));

        xml =
                "<layer>"
                        + "<styles>"
                        + "<style>"
                        + "<name>foo</name>"
                        + "<workspace>cite</workspace>"
                        + "</style>"
                        + "</styles>"
                        + "<enabled>true</enabled>"
                        + "</layer>";
        response =
                putAsServletResponse(ROOT_PATH + "/layers/cite:Buildings", xml, "application/xml");
        assertEquals(200, response.getStatus());

        LayerInfo l = cat.getLayerByName("cite:Buildings");
        assertNotNull(l.getDefaultStyle());
        StyleInfo style = l.getStyles().iterator().next();
        assertEquals("foo", style.getName());
        assertNotNull(style.getWorkspace());
        assertNotNull(l.getDateModified());

        Document dom = getAsDOM(ROOT_PATH + "/layers/cite:Buildings.xml", 200);
        assertXpathExists("/layer/styles/style/name[text() = 'cite:foo']", dom);
        assertXpathExists("/layer/styles/style/workspace[text() = 'cite']", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver" + ROOT_PATH + "/workspaces/cite/styles/foo.xml",
                "//styles/style/atom:link/@href",
                dom);
    }

    @Test
    public void testPutDefaultWMSInterpolationMethod() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo l = cat.getLayerByName("cite:Buildings");
        assertNotNull(l);
        assertNull(l.getDefaultWMSInterpolationMethod());

        Document dom = getAsDOM(ROOT_PATH + "/layers/cite:Buildings.xml", 200);
        assertEquals("layer", dom.getDocumentElement().getNodeName());
        assertXpathNotExists("/layer/defaultWMSInterpolationMethod", dom);

        String xml =
                "<layer>"
                        + "<defaultWMSInterpolationMethod>"
                        + "Nearest"
                        + "</defaultWMSInterpolationMethod>"
                        + "<enabled>true</enabled>"
                        + "</layer>";
        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/layers/cite:Buildings", xml, "application/xml");
        assertEquals(200, response.getStatus());

        l = cat.getLayerByName("cite:Buildings");
        assertNotNull(l.getDefaultWMSInterpolationMethod());
        assertEquals(LayerInfo.WMSInterpolation.Nearest, l.getDefaultWMSInterpolationMethod());
        assertNotNull(l.getDateModified());

        dom = getAsDOM(ROOT_PATH + "/layers/cite:Buildings.xml", 200);
        assertXpathEvaluatesTo("1", "count(/layer/defaultWMSInterpolationMethod)", dom);
        assertXpathExists("/layer/defaultWMSInterpolationMethod[text() = 'Nearest']", dom);
    }
}
