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

package org.geoserver.cluster.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ClusterControllerTest extends GeoServerSystemTestSupport {

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testGetConfigurationXML() throws Exception {
        Document dom = getAsDOM("rest/cluster.xml");
        // print(dom);
        // checking a property that's unlikely to change
        assertXpathEvaluatesTo(
                "VirtualTopic.geoserver", "/properties/property[@name='topicName']/@value", dom);
    }

    @Test
    public void testGetConfigurationHTML() throws Exception {
        Document dom = getAsDOM("rest/cluster.html");
        assertEquals("html", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testGetConfigurationJSON() throws Exception {
        // get JSON properties
        JSON json = getAsJSON("rest/cluster.json");
        assertThat(json, notNullValue());
        assertThat(json, instanceOf(JSONObject.class));
        JSONObject jsonObject = (JSONObject) json;
        assertThat(jsonObject.get("properties"), notNullValue());
        assertThat(jsonObject.get("properties"), instanceOf(JSONObject.class));
        assertThat(jsonObject.getJSONObject("properties").get("property"), notNullValue());
        assertThat(
                jsonObject.getJSONObject("properties").get("property"),
                instanceOf(JSONArray.class));
        JSONArray properties = jsonObject.getJSONObject("properties").getJSONArray("property");
        assertThat(properties.size(), is(15));
        // check properties exist
        checkPropertyExists(properties, "toggleSlave");
        checkPropertyExists(properties, "connection");
        checkPropertyExists(properties, "topicName");
        checkPropertyExists(properties, "brokerURL");
        checkPropertyExists(properties, "durable");
        checkPropertyExists(properties, "xbeanURL");
        checkPropertyExists(properties, "toggleMaster");
        checkPropertyExists(properties, "embeddedBroker");
        checkPropertyExists(properties, "CLUSTER_CONFIG_DIR");
        checkPropertyExists(properties, "embeddedBrokerProperties");
        checkPropertyExists(properties, "connection.retry");
        checkPropertyExists(properties, "readOnly");
        checkPropertyExists(properties, "instanceName");
        checkPropertyExists(properties, "group");
        checkPropertyExists(properties, "connection.maxwait");
    }

    @Test
    public void testUpdateConfiguration() throws Exception {
        String config = "<properties><property name=\"toggleSlave\" value=\"false\"/></properties>";
        MockHttpServletResponse response = postAsServletResponse("rest/cluster.xml", config);
        assertEquals(201, response.getStatus());
        Document dom = getAsDOM("rest/cluster.xml");
        // print(dom);
        // checking the property just modified
        assertXpathEvaluatesTo("false", "/properties/property[@name='toggleSlave']/@value", dom);
    }

    /** Helper method that checks if a property exists. */
    private void checkPropertyExists(JSONArray properties, String expectedName) {
        boolean found = false;
        for (Object json : properties) {
            assertThat(json, instanceOf(JSONObject.class));
            JSONObject jsonObject = (JSONObject) json;
            assertThat(jsonObject.get("@name"), notNullValue());
            if (jsonObject.get("@name").equals(expectedName)) {
                assertThat(jsonObject.get("@value"), notNullValue());
                found = true;
            }
        }
        assertThat(found, is(true));
    }
}
