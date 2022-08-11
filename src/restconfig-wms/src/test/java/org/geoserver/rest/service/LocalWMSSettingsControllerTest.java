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

package org.geoserver.rest.service;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSON;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LocalWMSSettingsControllerTest extends CatalogRESTTestSupport {

    @After
    public void clearLocalWorkspace() throws Exception {
        LocalWorkspace.remove();
        revertService(WMSInfo.class, "sf");
    }

    @Before
    public void initLocalWMS() throws Exception {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WMSInfo wmsInfo = geoServer.getService(ws, WMSInfo.class);
        if (wmsInfo != null) {
            geoServer.remove(wmsInfo);
        }
        wmsInfo = geoServer.getFactory().create(WMSInfo.class);

        wmsInfo.setName("WMS");
        wmsInfo.setWorkspace(ws);
        geoServer.add(wmsInfo);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");
        // assertEquals("wms", wmsinfo.get("id"));
        assertEquals("WMS", wmsinfo.get("name"));
        JSONObject workspace = (JSONObject) wmsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
        JSONObject watermark = (JSONObject) wmsinfo.get("watermark");
        assertEquals("false", watermark.get("enabled").toString().trim());
        assertEquals("Nearest", wmsinfo.get("interpolation"));
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wms/workspace/name", dom);
        assertXpathEvaluatesTo("WMS", "/wms/name", dom);
        assertXpathEvaluatesTo("false", "/wms/watermark/enabled", dom);
        assertXpathEvaluatesTo("Nearest", "/wms/interpolation", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.html");
    }

    @Test
    public void testCreateAsJSON() throws Exception {
        removeLocalWorkspace();
        String input =
                "{'wms': {'id' : 'wms_sf', 'workspace':{'name':'sf'},'name' : 'WMS', 'enabled': 'true'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings/",
                        input,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");
        assertEquals("WMS", wmsinfo.get("name"));
        assertEquals("true", wmsinfo.get("enabled").toString().trim());
        JSONObject workspace = (JSONObject) wmsinfo.get("workspace");
        assertNotNull(workspace);
        assertEquals("sf", workspace.get("name"));
    }

    @Test
    public void testCreateAsXML() throws Exception {
        removeLocalWorkspace();
        String xml =
                "<wms>"
                        + "<id>wms_sf</id>"
                        + "<workspace>"
                        + "<name>sf</name>"
                        + "</workspace>"
                        + "<name>OGC:WMS</name>"
                        + "<enabled>false</enabled>"
                        + "<interpolation>Nearest</interpolation>"
                        + "</wms>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.xml");
        assertEquals("wms", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
        assertXpathEvaluatesTo("sf", "/wms/workspace/name", dom);
        assertXpathEvaluatesTo("OGC:WMS", "/wms/name", dom);
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
        assertXpathEvaluatesTo("Nearest", "/wms/interpolation", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json =
                "{'wms': {'id':'wms','workspace':{'name':'sf'},'enabled':'false','name':'WMS'}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings/",
                        json,
                        "text/json");
        assertEquals(200, response.getStatus());
        JSON jsonMod =
                getAsJSON(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wmsinfo = (JSONObject) jsonObject.get("wms");
        // assertEquals("wms", wmsinfo.get("id"));
        assertEquals("false", wmsinfo.get("enabled").toString().trim());
    }

    @Test
    public void testPutAsXML() throws Exception {
        String xml =
                "<wms>"
                        + "<id>wms</id>"
                        + "<workspace>"
                        + "<name>sf</name>"
                        + "</workspace>"
                        + "<enabled>false</enabled>"
                        + "</wms>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("false", "/wms/enabled", dom);
    }

    @Test
    public void testPutFullAsXML() throws Exception {
        String xml =
                IOUtils.toString(
                        LocalWMSSettingsControllerTest.class.getResourceAsStream("wms.xml"),
                        "UTF-8");
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.xml");
        assertXpathEvaluatesTo("true", "/wms/enabled", dom);
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/services/wms/workspaces/sf/settings")
                        .getStatus());
        boolean thrown = false;
        try {
            getAsJSON(RestBaseController.ROOT_PATH + "/services/wms/workspaces/sf/settings.json");
        } catch (JSONException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    private void removeLocalWorkspace() {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        WMSInfo wmsInfo = geoServer.getService(ws, WMSInfo.class);
        geoServer.remove(wmsInfo);
    }
}
