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

package org.geoserver.csw.store.internal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geotools.filter.v1_1.OGC;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetCapabilitiesTest extends CSWInternalTestSupport {

    static XpathEngine xpath = XMLUnit.newXpathEngine();

    static {
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("ows", OWS.NAMESPACE);
        prefixMap.put("ogc", OGC.NAMESPACE);
        NamespaceContext nameSpaceContext = new SimpleNamespaceContext(prefixMap);
        xpath.setNamespaceContext(nameSpaceContext);
    }

    @Test
    public void testGetBasic() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        > 0);
        assertEquals("5", xpath.evaluate("count(//ows:Operation)", dom));

        // basic check on GetCapabilities operation constraint
        assertEquals(
                "XML",
                xpath.evaluate(
                        "//ows:OperationsMetadata/ows:Operation[@name=\"GetCapabilities\"]/ows:Constraint/ows:Value",
                        dom));

        // check we have csw:AnyText among the queriables
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetRecords']/ows:Constraint[@name='SupportedDublinCoreQueryables' and ows:Value = 'csw:AnyText'])",
                dom);

        // check we have dc:subject among the domain property names
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetDomain']/ows:Parameter[@name='PropertyName' and ows:Value = 'dc:title'])",
                dom);
    }

    @Test
    public void testPostBasic() throws Exception {
        Document dom = postAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        > 0);
        assertEquals("5", xpath.evaluate("count(//ows:Operation)", dom));

        // basic check on GetCapabilities operation constraint
        assertEquals(
                "XML",
                xpath.evaluate(
                        "//ows:OperationsMetadata/ows:Operation[@name=\"GetCapabilities\"]/ows:Constraint/ows:Value",
                        dom));
    }

    @Test
    public void testSections() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetCapabilities&sections=ServiceIdentification,ServiceProvider");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);
        assertEquals("1", xpath.evaluate("count(//ows:ServiceIdentification)", dom));
        assertEquals("1", xpath.evaluate("count(//ows:ServiceProvider)", dom));
        assertEquals("0", xpath.evaluate("count(//ows:OperationsMetadata)", dom));
        // this one is mandatory, cannot be skipped
        assertEquals("1", xpath.evaluate("count(//ogc:Filter_Capabilities)", dom));

        assertEquals(
                0,
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength());
        assertEquals("0", xpath.evaluate("count(//ows:Operation)", dom));
    }
}
