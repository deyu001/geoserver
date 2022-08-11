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


package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests for a multivalued xlink:href ClientProperty mapping without feature chaining.
 *
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class MultivaluedXlinkHrefTest extends AbstractAppSchemaTestSupport {

    @Override
    protected MultivaluedXlinkHrefMockData createTestData() {
        return new MultivaluedXlinkHrefMockData();
    }

    /**
     * Test that GetFeature returns a single feature with two gsml:occurrence, each with expected
     * xlink:href.
     */
    @Test
    public void testGetFeature() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetFeature&typenames=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature, typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.1", "//gsml:GeologicUnit/@gml:id", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.2",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.3",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    /**
     * Test that GetFeature filter on first gsml:occurrence/@xlink:href returns a single feature
     * with two gsml:occurrence, each with expected xlink:href.
     */
    @Test
    public void testGetFeatureFilterFirstXlinkHref() throws Exception {
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + ">" //
                        + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">" //
                        + "        <fes:Filter>" //
                        + "            <fes:PropertyIsEqualTo>" //
                        + "                <fes:ValueReference>gsml:occurrence/@xlink:href</fes:ValueReference>" //
                        + "                <fes:Literal>http://resource.example.org/mapped-feature/mf.2</fes:Literal>" //
                        + "            </fes:PropertyIsEqualTo>" //
                        + "        </fes:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.1", "//gsml:GeologicUnit/@gml:id", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.2",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.3",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    /**
     * Test that GetFeature filter on second gsml:occurrence/@xlink:href returns a single feature
     * with two gsml:occurrence, each with expected xlink:href.
     */
    @Test
    public void testGetFeatureFilterSecondXlinkHref() throws Exception {
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + ">" //
                        + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">" //
                        + "        <fes:Filter>" //
                        + "            <fes:PropertyIsEqualTo>" //
                        + "                <fes:ValueReference>gsml:occurrence/@xlink:href</fes:ValueReference>" //
                        + "                <fes:Literal>http://resource.example.org/mapped-feature/mf.3</fes:Literal>" //
                        + "            </fes:PropertyIsEqualTo>" //
                        + "        </fes:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.1", "//gsml:GeologicUnit/@gml:id", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.2",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.3",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    /**
     * Test that GetFeature filter on nonexistent gsml:occurrence/@xlink:href returns no features.
     */
    @Test
    public void testGetFeatureFilterNonexistentXlinkHref() throws Exception {
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + ">" //
                        + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">" //
                        + "        <fes:Filter>" //
                        + "            <fes:PropertyIsEqualTo>" //
                        + "                <fes:ValueReference>gsml:occurrence/@xlink:href</fes:ValueReference>" //
                        + "                <fes:Literal>http://resource.example.org/mapped-feature/does-not-exist</fes:Literal>" //
                        + "            </fes:PropertyIsEqualTo>" //
                        + "        </fes:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));
        assertXpathCount(0, "//gsml:GeologicUnit", doc);
    }
}
