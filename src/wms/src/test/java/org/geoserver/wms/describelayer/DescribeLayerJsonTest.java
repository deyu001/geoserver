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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit test suite for {@link JSONDescribeLayerResponse}
 *
 * @author Carlo Cancellieri - GeoSolutions
 * @version $Id$
 */
public class DescribeLayerJsonTest extends WMSTestSupport {

    @Test
    public void testBuild() throws Exception {
        try {
            new JSONDescribeLayerResponse(getWMS(), "fail");
            fail("Should fails");
        } catch (Exception e) {
        }
    }

    /** Tests jsonp with custom callback function */
    @Test
    public void testCustomJSONP() throws Exception {

        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.jsonp
                        + "&format_options="
                        + JSONType.CALLBACK_FUNCTION_KEY
                        + ":DescribeLayer";

        JSONType.setJsonpEnabled(true);
        String result = getAsString(request);
        JSONType.setJsonpEnabled(false);

        checkJSONPDescribeLayer(result, layer);
    }

    /** Tests JSON */
    @Test
    public void testSimpleJSON() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.json;

        String result = getAsString(request);
        // System.out.println(result);

        checkJSONDescribeLayer(result, layer);
    }

    /**
     * @param body Accepts:<br>
     *     DescribeLayer(...)<br>
     */
    private void checkJSONPDescribeLayer(String body, String layer) {
        assertNotNull(body);

        assertTrue(body.startsWith("DescribeLayer("));
        assertTrue(body.endsWith(")\n"));
        body = body.substring(0, body.length() - 2);
        body = body.substring("DescribeLayer(".length(), body.length());

        checkJSONDescribeLayer(body, layer);
    }

    /** Tests jsonp with custom callback function */
    @Test
    public void testJSONLayerGroup() throws Exception {

        String layer = NATURE_GROUP;
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.json;

        String result = getAsString(request);

        checkJSONDescribeLayerGroup(result, layer);
    }

    private void checkJSONDescribeLayer(String body, String layer) {
        assertNotNull(body);

        JSONObject rootObject = JSONObject.fromObject(body);
        // JSONObject subObject = rootObject.getJSONObject("WMS_DescribeLayerResponse");
        JSONArray layerDescs = rootObject.getJSONArray("layerDescriptions");

        JSONObject layerDesc = layerDescs.getJSONObject(0);

        assertEquals(layerDesc.get("layerName"), layer);
        // assertEquals(layerDesc.get("owsUrl"), "WFS");
        assertEquals(layerDesc.get("owsType"), "WFS");
    }

    private void checkJSONDescribeLayerGroup(String body, String layer) {
        assertNotNull(body);

        JSONObject rootObject = JSONObject.fromObject(body);

        JSONArray layerDescs = rootObject.getJSONArray("layerDescriptions");
        JSONObject layerDesc = layerDescs.getJSONObject(0);
        assertEquals(
                layerDesc.get("layerName"),
                MockData.LAKES.getPrefix() + ":" + MockData.LAKES.getLocalPart());
        assertTrue(layerDesc.get("owsURL").toString().endsWith("geoserver/wfs?"));
        assertEquals(layerDesc.get("owsType"), "WFS");

        layerDesc = layerDescs.getJSONObject(1);
        assertEquals(
                layerDesc.get("layerName"),
                MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart());
        assertTrue(layerDesc.get("owsURL").toString().endsWith("geoserver/wfs?"));
        assertEquals(layerDesc.get("owsType"), "WFS");
    }

    @Test
    public void testJSONDescribeLayerCharset() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.json;

        MockHttpServletResponse result = getAsServletResponse(request, "");
        assertEquals("UTF-8", result.getCharacterEncoding());
    }
}
