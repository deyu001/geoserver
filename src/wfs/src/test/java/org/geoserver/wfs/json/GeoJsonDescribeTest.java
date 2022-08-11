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

package org.geoserver.wfs.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;

/** @author carlo cancellieri - GeoSolutions */
public class GeoJsonDescribeTest extends WFSTestSupport {

    @Test
    public void testDescribePrimitiveGeoFeatureJSON() throws Exception {
        String output =
                getAsString(
                        "wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="
                                + JSONType.json
                                + "&typeName="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        testOutput(output);
    }

    private void testOutput(String output) {
        JSONObject description = JSONObject.fromObject(output);
        assertEquals(description.get("elementFormDefault"), "qualified");
        assertEquals(description.get("targetNamespace"), "http://cite.opengeospatial.org/gmlsf");
        assertEquals(description.get("targetPrefix"), "sf");
        JSONArray array = description.getJSONArray("featureTypes");
        // print(array);

        assertEquals(1, array.size());
        JSONObject feature = array.getJSONObject(0);

        assertEquals(feature.get("typeName"), "PrimitiveGeoFeature");

        JSONArray props = feature.getJSONArray("properties");
        assertNotNull(props);

        // description
        int i = 0;
        assertEquals("description", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // point property (second geometry)
        assertEquals("name", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // surfaceProperty property
        assertEquals("surfaceProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("gml:Polygon", props.getJSONObject(i).get("type"));
        assertEquals("Polygon", props.getJSONObject(i).get("localType"));

        ++i;
        // point property (second geometry)
        assertEquals("pointProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:Point", props.getJSONObject(i).get("type"));
        assertEquals("Point", props.getJSONObject(i).get("localType"));

        ++i;
        // curve property (second geometry)
        assertEquals("curveProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:LineString", props.getJSONObject(i).get("type"));
        assertEquals("LineString", props.getJSONObject(i).get("localType"));

        ++i;
        // int property
        assertEquals("intProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:int", props.getJSONObject(i).get("type"));
        assertEquals("int", props.getJSONObject(i).get("localType"));

        ++i;
        // Uri property
        assertEquals("uriProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // measurand property
        assertEquals("measurand", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // dateProperty time
        assertEquals("dateTimeProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:date-time", props.getJSONObject(i).get("type"));
        assertEquals("date-time", props.getJSONObject(i).get("localType"));

        ++i;
        // dateProperty time
        assertEquals("dateProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:date", props.getJSONObject(i).get("type"));
        assertEquals("date", props.getJSONObject(i).get("localType"));

        ++i;
        // boolean
        assertEquals("decimalProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:number", props.getJSONObject(i).get("type"));
        assertEquals("number", props.getJSONObject(i).get("localType"));

        ++i;
        // boolean
        assertEquals("booleanProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:boolean", props.getJSONObject(i).get("type"));
        assertEquals("boolean", props.getJSONObject(i).get("localType"));
    }

    @Test
    public void testDescribePrimitiveGeoFeatureJSONP() throws Exception {
        JSONType.setJsonpEnabled(true);
        String output =
                getAsString(
                        "wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="
                                + JSONType.jsonp
                                + "&typeName="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        JSONType.setJsonpEnabled(false);
        // removing specific parts
        output = output.substring(0, output.length() - 2);
        output = output.substring(JSONType.CALLBACK_FUNCTION.length() + 1, output.length());
        testOutput(output);
    }

    @Test
    public void testDescribePrimitiveGeoFeatureJSONPCustom() throws Exception {
        JSONType.setJsonpEnabled(true);
        String output =
                getAsString(
                        "wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="
                                + JSONType.jsonp
                                + "&typeName="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE)
                                + "&format_options="
                                + JSONType.CALLBACK_FUNCTION_KEY
                                + ":custom");
        JSONType.setJsonpEnabled(false);
        // removing specific parts
        assertTrue(output.startsWith("custom("));
        output = output.substring(0, output.length() - 2);
        output = output.substring("custom".length() + 1, output.length());
        testOutput(output);
    }
}
