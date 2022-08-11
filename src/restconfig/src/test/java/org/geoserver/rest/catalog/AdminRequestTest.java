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
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class AdminRequestTest extends CatalogRESTTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("global");
        lg.getLayers().add(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getLayers().add(catalog.getLayerByName("sf:AggregateGeoFeature"));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        lg = catalog.getFactory().createLayerGroup();
        lg.setName("local");
        lg.setWorkspace(catalog.getWorkspaceByName("sf"));
        lg.getLayers().add(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getLayers().add(catalog.getLayerByName("sf:AggregateGeoFeature"));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        Catalog cat = getCatalog();

        // add two workspace specific styles
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("sf_style");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("sf.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("cite_style");
        s.setWorkspace(cat.getWorkspaceByName("cite"));
        s.setFilename("cite.sld");
        cat.add(s);

        addUser("cite", "cite", null, Collections.singletonList("ROLE_CITE_ADMIN"));
        addUser("sf", "sf", null, Collections.singletonList("ROLE_SF_ADMIN"));

        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_CITE_ADMIN");
        addLayerAccessRule("sf", "*", AccessMode.ADMIN, "ROLE_SF_ADMIN");
    }

    @After
    public void clearAdminRequest() {
        AdminRequest.finish();
    }

    @Override
    public void login() throws Exception {
        // skip the login by default
    }

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    @Test
    public void testWorkspaces() throws Exception {
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml").getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName("workspace").getLength());

        super.login();
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(
                getCatalog().getWorkspaces().size(),
                dom.getElementsByTagName("workspace").getLength());

        loginAsCite();
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml").getStatus());
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(1, dom.getElementsByTagName("workspace").getLength());
    }

    @Test
    public void testWorkspacesWithProxyHeaders() throws Exception {
        GeoServerInfo ginfo = getGeoServer().getGlobal();
        SettingsInfo settings = getGeoServer().getGlobal().getSettings();
        ginfo.setUseHeadersProxyURL(true);
        settings.setProxyBaseUrl(
                "${X-Forwarded-Proto}://${X-Forwarded-Host}/${X-Forwarded-Path} ${X-Forwarded-Proto}://${X-Forwarded-Host}");
        ginfo.setSettings(settings);
        getGeoServer().save(ginfo);
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml").getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName("workspace").getLength());

        super.login();
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(
                getCatalog().getWorkspaces().size(),
                dom.getElementsByTagName("workspace").getLength());

        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml").getStatus());
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(
                getCatalog().getWorkspaces().size(),
                dom.getElementsByTagName("workspace").getLength());
    }

    @Test
    public void testWorkspace() throws Exception {
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf.xml")
                        .getStatus());
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cite.xml")
                        .getStatus());

        loginAsCite();
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf.xml")
                        .getStatus());
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cite.xml")
                        .getStatus());
    }

    @Test
    public void testGlobalLayerGroupReadOnly() throws Exception {
        loginAsSf();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.xml");
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups/global.xml");
        assertEquals("layerGroup", dom.getDocumentElement().getNodeName());

        String xml =
                "<layerGroup>"
                        + "<styles>"
                        + "<style>polygon</style>"
                        + "<style>line</style>"
                        + "</styles>"
                        + "</layerGroup>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layergroups/global", xml, "text/xml");
        assertEquals(405, response.getStatus());

        xml =
                "<layerGroup>"
                        + "<name>newLayerGroup</name>"
                        + "<layers>"
                        + "<layer>Ponds</layer>"
                        + "<layer>Forests</layer>"
                        + "</layers>"
                        + "<styles>"
                        + "<style>polygon</style>"
                        + "<style>point</style>"
                        + "</styles>"
                        + "</layerGroup>";
        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layergroups", xml, "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testLocalLayerGroupHidden() throws Exception {
        loginAsSf();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.xml");
        print(dom);
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/cite/layergroups.xml");
        assertEquals(404, response.getStatus());

        response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/cite/layergroups.xml");
        assertEquals(404, response.getStatus());

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.xml");
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/layergroups.xml");
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("local", "//layerGroup/name", dom);
    }

    @Test
    public void testGlobalStyleReadOnly() throws Exception {
        loginAsSf();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.xml");

        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);
        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles/point.xml");
        assertEquals("style", dom.getDocumentElement().getNodeName());

        String xml = "<style>" + "<filename>foo.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/point", xml, "text/xml");
        assertEquals(405, response.getStatus());

        xml = "<style>" + "<name>foo</name>" + "<filename>foo.sld</filename>" + "</style>";
        response = postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml, "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testLocalStyleHidden() throws Exception {
        loginAsCite();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.xml");
        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);
        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);

        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/styles.xml");
        assertEquals(404, response.getStatus());

        loginAsSf();

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.xml");

        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);
        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/styles.xml");
        assertEquals(1, dom.getElementsByTagName("style").getLength());
        assertXpathEvaluatesTo("sf_style", "//style/name", dom);
    }
}
