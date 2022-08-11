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

package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LayerWorkspaceTest extends WMSTestSupport {

    private Catalog catalog;

    @Before
    public void setCatalog() throws Exception {
        catalog = getCatalog();
    }

    LayerInfo layer(Catalog cat, QName name) {
        return cat.getLayerByName(getLayerId(name));
    }

    /** Test layer names order from GetCapabilities */
    @Test
    public void testLayerOrderGetCapabilities() throws Exception {
        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        List<String> originalList = layerNameList(doc);
        assertFalse(originalList.isEmpty());
        List<String> names =
                originalList.stream().map(x -> removeLayerPrefix(x)).collect(Collectors.toList());
        List<String> orderedNames = names.stream().sorted().collect(Collectors.toList());
        assertEquals(orderedNames, names);
    }

    /** Test layer names order from GetCapabilities on workspace */
    @Test
    public void testWorkspaceLayerOrderGetCapabilities() throws Exception {
        Document doc =
                getAsDOM("/cite/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        List<String> originalList = layerNameList(doc);
        assertFalse(originalList.isEmpty());
        assertTrue(originalList.stream().noneMatch(x -> x.indexOf(":") > -1));
        List<String> orderedNames = originalList.stream().sorted().collect(Collectors.toList());
        assertEquals(orderedNames, originalList);
    }

    /** removes prefix from layer name */
    private String removeLayerPrefix(String prefixedName) {
        if (prefixedName.indexOf(":") > -1) {
            return prefixedName.split(":")[1];
        }
        return prefixedName;
    }

    /** returns list of prefixed layer names from document */
    private List<String> layerNameList(Document doc) throws Exception {
        List<Node> nlist = xpathList("//WMT_MS_Capabilities/Capability/Layer/Layer/Name", doc);
        List<String> result = new ArrayList<>();
        nlist.forEach(
                x -> {
                    result.add(x.getTextContent().trim());
                });
        return result;
    }

    private List<Node> xpathList(String xpathString, Document doc) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathString);
        NodeList nlist = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        List<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < nlist.getLength(); i++) {
            nodeList.add(nlist.item(i));
        }
        return nodeList;
    }

    @Test
    public void testGlobalCapabilities() throws Exception {
        LayerInfo layer = layer(catalog, MockData.PRIMITIVEGEOFEATURE);
        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathExists("//Layer[Name='" + layer.prefixedName() + "']", doc);
    }

    @Test
    public void testGlobalDescribeLayer() throws Exception {
        LayerInfo layer = layer(catalog, MockData.PRIMITIVEGEOFEATURE);
        Document doc =
                getAsDOM(
                        "/wms?service=WMS&request=describeLayer&version=1.1.1&LAYERS="
                                + layer.getName(),
                        true);
        assertXpathExists("//LayerDescription[@name='" + layer.prefixedName() + "']", doc);
    }

    @Test
    public void testWorkspaceCapabilities() throws Exception {
        Document doc = getAsDOM("/sf/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathExists(
                "//Layer[Name='" + MockData.PRIMITIVEGEOFEATURE.getLocalPart() + "']", doc);
    }

    @Test
    public void testWorkspaceDescribeLayer() throws Exception {
        Document doc =
                getAsDOM(
                        "/sf/wms?service=WMS&request=describeLayer&version=1.1.1&LAYERS="
                                + MockData.PRIMITIVEGEOFEATURE.getLocalPart(),
                        true);
        assertXpathExists(
                "//LayerDescription[@name='" + MockData.PRIMITIVEGEOFEATURE.getLocalPart() + "']",
                doc);
    }
}
