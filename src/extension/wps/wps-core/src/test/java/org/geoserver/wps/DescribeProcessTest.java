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

package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import org.junit.Test;
import org.w3c.dom.Document;

public class DescribeProcessTest extends WPSTestSupport {

    @Test
    public void testGetBuffer() throws Exception { // Standard Test A.4.3.1
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=JTS:buffer");
        // print(d);
        testBufferDescription(d);
    }

    @Test
    public void testPostBuffer() throws Exception { // Standard Test A.4.3.2
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                        + "<DescribeProcess xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                        + "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                        + "    <ows:Identifier>JTS:buffer</ows:Identifier>\r\n"
                        + "</DescribeProcess>";
        Document d = postAsDOM(root(), request);
        // print(d);
        testBufferDescription(d);
    }

    @Test
    public void testGetBufferFeatureCollection() throws Exception { // Standard Test A.4.3.1
        Document d =
                getAsDOM(
                        root()
                                + "service=wps&request=describeprocess&identifier=gs:BufferFeatureCollection");
        print(d);

        // check that we advertise the base64 encoding for application/zip
        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";
        assertXpathExists(
                base + "/Input[1]/ComplexData/Supported/Format[MimeType='application/zip']", d);
        assertXpathEvaluatesTo(
                "base64",
                base
                        + "/Input[1]/ComplexData/Supported/Format[MimeType='application/zip']/Encoding",
                d);
    }

    private void testBufferDescription(Document d) throws Exception { // Standard Test A.4.3.3
        // first off, let's check it's schema compliant
        checkValidationErrors(d);
        assertXpathExists("/wps:ProcessDescriptions", d);

        assertXpathEvaluatesTo("true", "//ProcessDescription/@storeSupported", d);
        assertXpathEvaluatesTo("true", "//ProcessDescription/@statusSupported", d);

        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";

        // first parameter
        assertXpathExists(base + "/Input[1]", d);
        assertXpathExists(base + "/Input[1]/ComplexData", d);

        assertXpathEvaluatesTo(
                "text/xml; subtype=gml/3.1.1",
                base + "/Input[1]/ComplexData/Default/Format/MimeType/child::text()",
                d);
        assertXpathEvaluatesTo(
                "text/xml; subtype=gml/3.1.1",
                base + "/Input[1]/ComplexData/Supported/Format[1]/MimeType/child::text()",
                d);
        assertXpathEvaluatesTo(
                "text/xml; subtype=gml/2.1.2",
                base + "/Input[1]/ComplexData/Supported/Format[2]/MimeType/child::text()",
                d);
        assertXpathEvaluatesTo(
                "application/wkt",
                base + "/Input[1]/ComplexData/Supported/Format[3]/MimeType/child::text()",
                d);

        // second parameter
        assertXpathExists(base + "/Input[2]", d);
        assertXpathEvaluatesTo("distance", base + "/Input[2]/ows:Identifier/child::text()", d);
        assertXpathExists(base + "/Input[2]/LiteralData", d);

        assertXpathEvaluatesTo(
                "xs:double", base + "/Input[2]/LiteralData/ows:DataType/child::text()", d);

        // output
        base = "/wps:ProcessDescriptions/ProcessDescription/ProcessOutputs";
        assertXpathExists(base + "/Output", d);
        assertXpathExists(base + "/Output/ComplexOutput", d);
    }

    /** Tests encoding of bounding box outputs */
    @Test
    public void testBounds() throws Exception {
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=gs:Bounds");
        // print(d);
        checkValidationErrors(d);
        assertXpathEvaluatesTo(
                "EPSG:4326", "//Output[ows:Identifier='bounds']/BoundingBoxOutput/Default/CRS", d);
        assertXpathEvaluatesTo(
                "EPSG:4326",
                "//Output[ows:Identifier='bounds']/BoundingBoxOutput/Supported/CRS",
                d);
    }

    @Test
    public void testDefaultValues() throws Exception {
        Document d =
                getAsDOM(
                        root()
                                + "service=wps&request=describeprocess&identifier=gs:GeorectifyCoverage");
        // print(d);
        checkValidationErrors(d);
        assertXpathEvaluatesTo(
                "true", "//Input[ows:Identifier='transparent']/LiteralData/DefaultValue", d);
        assertXpathEvaluatesTo(
                "false", "//Input[ows:Identifier='store']/LiteralData/DefaultValue", d);
    }

    @Test
    public void testMultiRaw() throws Exception {
        Document d =
                getAsDOM(root() + "service=wps&request=describeprocess&identifier=gs:MultiRaw");
        // print(d);
        checkValidationErrors(d);
        // only one input (we have two, but one is the chosen mime type for the outputs
        assertXpathEvaluatesTo("1", "count(//Input)", d);
        assertXpathEvaluatesTo("1", "count(//Input[ows:Identifier='id']/LiteralData)", d);

        // three outputs, two complex, one literal
        assertXpathEvaluatesTo("3", "count(//Output)", d);
        assertXpathEvaluatesTo(
                "text/plain",
                "//Output[ows:Identifier='text']/ComplexOutput/Supported/Format/MimeType",
                d);
        assertXpathEvaluatesTo(
                "application/zip",
                "//Output[ows:Identifier='binary']/ComplexOutput/Supported/Format[1]/MimeType",
                d);
        assertXpathEvaluatesTo(
                "image/png",
                "//Output[ows:Identifier='binary']/ComplexOutput/Supported/Format[2]/MimeType",
                d);
        assertXpathEvaluatesTo("1", "count(//Output[ows:Identifier='literal']/LiteralOutput)", d);
    }
}
