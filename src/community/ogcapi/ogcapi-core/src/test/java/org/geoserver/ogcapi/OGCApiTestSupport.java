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

package org.geoserver.ogcapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.JsonContext;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.mock.web.MockHttpServletResponse;

public class OGCApiTestSupport extends GeoServerSystemTestSupport {

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(path, expectedHttpCode);
        return getAsJSONPath(response);
    }

    protected DocumentContext getAsJSONPath(MockHttpServletResponse response)
            throws UnsupportedEncodingException {
        assertThat(response.getContentType(), containsString("json"));
        JsonContext json = (JsonContext) JsonPath.parse(response.getContentAsString());
        if (!isQuietTests()) {
            print(json(response));
        }
        return json;
    }

    protected MockHttpServletResponse getAsMockHttpServletResponse(
            String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);

        assertEquals(expectedHttpCode, response.getStatus());
        return response;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        registerNamespaces(namespaces);

        CiteTestData.registerNamespaces(namespaces);

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    /**
     * Allows subclasses to register namespaces. By default, does not add any, subclasses can
     * manipulate the namespaces map as they see fit.
     */
    protected void registerNamespaces(Map<String, String> namespaces) {}

    protected Document getAsJSoup(String url) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        assertEquals("text/html", response.getContentType());

        LOGGER.log(Level.INFO, "Last request returned\n:" + response.getContentAsString());

        // parse the HTML
        Document document = Jsoup.parse(response.getContentAsString());
        return document;
    }

    protected byte[] getAsByteArray(String url) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        return response.getContentAsByteArray();
    }

    protected JsonContext convertYamlToJsonPath(String yaml) throws Exception {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        JsonContext json = (JsonContext) JsonPath.parse(jsonWriter.writeValueAsString(obj));
        return json;
    }

    /** Returns a single element out of an array, checking that there is just one */
    protected <T> T readSingle(DocumentContext json, String path) {
        List<Object> items = json.read(path);
        assertEquals(
                "Found "
                        + items.size()
                        + " items for this path, but was expecting one: "
                        + path
                        + "\n"
                        + items,
                1,
                items.size());
        return (T) items.get(0);
    }

    /** Checks the specified jsonpath exists in the document */
    protected boolean exists(DocumentContext json, String path) {
        try {
            List items = json.read(path);
            return items.size() > 0;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Verifies the given JSONPath evaluates to the expected list
     *
     * @param json The document
     * @param path The path
     * @param expected The expected list
     * @param <T>
     */
    protected <T> void assertJSONList(DocumentContext json, String path, T... expected) {
        List<T> selfRels = json.read(path);
        assertThat(selfRels, Matchers.containsInAnyOrder(expected));
    }
}
