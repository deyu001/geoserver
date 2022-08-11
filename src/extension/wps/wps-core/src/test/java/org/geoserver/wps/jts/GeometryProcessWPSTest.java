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

package org.geoserver.wps.jts;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.geojson.geom.GeometryJSON;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GeometryProcessWPSTest extends WPSTestSupport {

    @Test
    public void testBufferCapabilities() throws Exception {
        // buffer uses an enumerated attribute, make sure it's not blacklisted because of that
        Document d = getAsDOM("wps?service=wps&request=getcapabilities");
        // print(d);
        checkValidationErrors(d);
        assertXpathEvaluatesTo(
                "1", "count(//wps:Process/ows:Identifier[text() = 'JTS:buffer'])", d);
    }

    @Test
    public void testDescribeBuffer() throws Exception {
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=JTS:buffer");
        // print(d);
        checkValidationErrors(d);

        // check we get the right type declarations for primitives
        assertXpathEvaluatesTo(
                "xs:double",
                "//Input[ows:Identifier/text()='distance']/LiteralData/ows:DataType/text()",
                d);
        assertXpathEvaluatesTo(
                "xs:int",
                "//Input[ows:Identifier/text()='quadrantSegments']/LiteralData/ows:DataType/text()",
                d);

        // check we have the list of possible values for enumerations
        assertXpathEvaluatesTo(
                "3",
                "count(//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value)",
                d);
        assertXpathEvaluatesTo(
                "Round",
                "//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value[1]/text()",
                d);
        assertXpathEvaluatesTo(
                "Flat",
                "//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value[2]/text()",
                d);
        assertXpathEvaluatesTo(
                "Square",
                "//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value[3]/text()",
                d);
    }

    @Test
    public void testExecuteBuffer() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>JTS:buffer</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>geom</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(0 0)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>distance</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>1</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>capStyle</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>Round</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "    <wps:RawDataOutput mimeType=\"application/wkt\">"
                        + "        <ows:Identifier>result</ows:Identifier>"
                        + "    </wps:RawDataOutput>"
                        + "  </wps:ResponseForm>"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        // System.out.println(response.getOutputStreamContent());
        assertEquals("application/wkt", response.getContentType());
        Geometry g = new WKTReader().read(response.getContentAsString());
        assertTrue(g instanceof Polygon);
    }

    @Test
    public void testLinearRingHandling() throws Exception {
        String xml =
                "<p0:Execute xmlns:p0=\"http://www.opengis.net/wps/1.0.0\" service=\"WPS\" version=\"1.0.0\"><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1.1\">geo:boundary</p1:Identifier><p0:DataInputs><p0:Input><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1.1\">geom</p1:Identifier><p0:Data><p0:ComplexData mimeType=\"application/wkt\">POLYGON((-7748880.179438027 939258.2035682453,-704443.6526761837 1956787.9241005117,-626172.1357121635 -4070118.8821290648,-8375052.315150191 -4539747.983913188,-7748880.179438027 939258.2035682453))</p0:ComplexData></p0:Data></p0:Input></p0:DataInputs><p0:ResponseForm><p0:RawDataOutput mimeType=\"application/wkt\"><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1.1\">result</p1:Identifier></p0:RawDataOutput></p0:ResponseForm></p0:Execute>";
        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        Geometry g = new WKTReader().read(response.getContentAsString());
        assertTrue(g instanceof LineString); // will actually produce a LinearRing.
        assertFalse(
                g
                        instanceof
                        LinearRing); // got to explicitly check since LineString is a supertype of
        // LinearRing
    }

    @Test
    public void testJsonResponse() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>JTS:buffer</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>geom</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(0 0)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>distance</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>1</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>capStyle</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:LiteralData>Round</wps:LiteralData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "    <wps:RawDataOutput mimeType=\"application/json\">"
                        + "        <ows:Identifier>result</ows:Identifier>"
                        + "    </wps:RawDataOutput>"
                        + "  </wps:ResponseForm>"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse("wps", xml);
        // System.out.println(response.getOutputStreamContent());
        assertEquals("application/json", response.getContentType());
        Geometry g = new GeometryJSON().read(response.getContentAsString());
        assertTrue(g instanceof Polygon);
    }
}
