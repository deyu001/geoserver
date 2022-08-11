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

package org.geoserver.wfs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.IsolatedWorkspacesTest;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Contains tests related to isolated workspaces, this tests exercise WFS operations. An workspace
 * in GeoServer is composed of the workspace information and a namespace which has a special
 * relevance in WFS.
 */
public final class WfsIsolatedWorkspacesTest extends IsolatedWorkspacesTest {

    // WFS 1.1.0 namespaces
    private static final Map<String, String> NAMESPACES_WFS11 = new HashMap<>();

    // init WFS 1.1.0 namespaces
    static {
        NAMESPACES_WFS11.put("wfs", "http://www.opengis.net/wfs");
        NAMESPACES_WFS11.put("ows", "http://www.opengis.net/ows");
        NAMESPACES_WFS11.put("ogc", "http://www.opengis.net/ogc");
        NAMESPACES_WFS11.put("xs", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS11.put("xsd", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS11.put("gml", "http://www.opengis.net/gml");
        NAMESPACES_WFS11.put("xlink", "http://www.w3.org/1999/xlink");
        NAMESPACES_WFS11.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        NAMESPACES_WFS11.put("gs", "http://geoserver.org");
    }

    // WFS 2.0 namespaces
    private static final Map<String, String> NAMESPACES_WFS20 = new HashMap<>();

    // init WFS 2.0 namespaces
    static {
        NAMESPACES_WFS20.put("wfs", "http://www.opengis.net/wfs/2.0");
        NAMESPACES_WFS20.put("ows", "http://www.opengis.net/ows/1.1");
        NAMESPACES_WFS20.put("fes", "http://www.opengis.net/fes/2.0");
        NAMESPACES_WFS20.put("gml", "http://www.opengis.net/gml/3.2");
        NAMESPACES_WFS20.put("ogc", "http://www.opengis.net/ogc");
        NAMESPACES_WFS20.put("xs", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS20.put("xsd", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS20.put("xlink", "http://www.w3.org/1999/xlink");
        NAMESPACES_WFS20.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        NAMESPACES_WFS20.put("gs", "http://geoserver.org");
    }

    @Test
    public void getFeatureInfoOnLayerFromIsolatedWorkspaces() throws Exception {
        Catalog catalog = getCatalog();
        // adding two workspaces with the same URI, one of them is isolated
        createWorkspace("test_a1", "https://www.test_a.com", false);
        createWorkspace("test_a2", "https://www.test_a.com", true);
        // get created workspaces and associated namespaces
        WorkspaceInfo workspace1 = catalog.getWorkspaceByName("test_a1");
        NamespaceInfo namespace1 = catalog.getNamespaceByPrefix("test_a1");
        WorkspaceInfo workspace2 = catalog.getWorkspaceByName("test_a2");
        NamespaceInfo namespace2 = catalog.getNamespaceByPrefix("test_a2");
        // add a layer with the same name to both workspaces, layers have different content
        LayerInfo clonedLayer1 =
                cloneVectorLayerIntoWorkspace(workspace1, namespace1, "Lines", "layer_e");
        LayerInfo clonedLayer2 =
                cloneVectorLayerIntoWorkspace(workspace2, namespace2, "Points", "layer_e");
        assertThat(clonedLayer1.getId(), not(clonedLayer2.getId()));
        // test get feature requests for WFS 1.1.0
        MockHttpServletResponse response =
                getAsServletResponse(
                        "test_a1/wfs?SERVICE=wfs&VERSION=1.1.0&REQUEST=getFeature&typeName=layer_e&maxFeatures=1");
        evaluateAndCheckXpath(
                mergeNamespaces(NAMESPACES_WFS11, "test_a1", "https://www.test_a.com"),
                response,
                "count(//wfs:FeatureCollection/gml:featureMembers/test_a1:layer_e/test_a1:lineStringProperty)",
                "1");
        response =
                getAsServletResponse(
                        "test_a2/wfs?SERVICE=wfs&VERSION=1.1.0&REQUEST=getFeature&typeName=layer_e&maxFeatures=1");
        evaluateAndCheckXpath(
                mergeNamespaces(NAMESPACES_WFS11, "test_a2", "https://www.test_a.com"),
                response,
                "count(//wfs:FeatureCollection/gml:featureMembers/test_a2:layer_e/test_a2:pointProperty)",
                "1");
    }

    private Map<String, String> mergeNamespaces(
            Map<String, String> namespaces, String... extraNamespaces) {
        Map<String, String> finalNamespaces = new HashMap<>();
        finalNamespaces.putAll(namespaces);
        for (int i = 0; i < extraNamespaces.length; i += 2) {
            finalNamespaces.put(extraNamespaces[i], extraNamespaces[i + 1]);
        }
        return finalNamespaces;
    }

    private void evaluateAndCheckXpath(
            Map<String, String> namespaces,
            MockHttpServletResponse response,
            String xpath,
            String expectResult)
            throws Exception {
        // convert response to document
        Document document = null;
        try (InputStream input =
                new ByteArrayInputStream(response.getContentAsString().getBytes())) {
            // create the DOM document
            document = dom(input, true);
        }
        // configure the correct WFS namespaces
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        // validate the provided XPATH
        String result = xpathEngine.evaluate(xpath, document);
        assertThat(result, is(expectResult));
    }
}
