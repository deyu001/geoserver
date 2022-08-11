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

package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.cat.csw20.DescribeRecordType;
import org.apache.commons.io.IOUtils;
import org.geoserver.csw.kvp.DescribeRecordKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.csw.CSWConfiguration;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DescribeRecordTest extends CSWSimpleTestSupport {

    @Test
    public void testKVPReaderNS() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put(
                "namespace",
                "xmlns(csw=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
        raw.put("typename", "csw:Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);

        assertDescribeRecordValid(dr);
    }

    private void assertDescribeRecordValid(DescribeRecordType dr) {
        assertEquals("CSW", dr.getService());
        assertEquals("2.0.2", dr.getVersion());
        assertEquals(2, dr.getTypeName().size());
        assertEquals(
                new QName("http://www.opengis.net/cat/csw/2.0.2", "Record"),
                dr.getTypeName().get(0));
        assertEquals(
                new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "RegistryPackage"),
                dr.getTypeName().get(1));
    }

    @Test
    public void testKVPReaderNoNamespace() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put("typename", "csw:Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);

        assertDescribeRecordValid(dr);
    }

    @Test
    public void testKVPReaderDefaultNamespace() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "CSW");
        raw.put("version", "2.0.2");
        raw.put("request", "DescribeRecord");
        raw.put(
                "namespace",
                "xmlns(=http://www.opengis.net/cat/csw/2.0.2),xmlns(rim=urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0)");
        raw.put("typename", "Record,rim:RegistryPackage");
        raw.put("schemalanguage", "XMLSCHEMA");
        raw.put("outputFormat", "application/xml");

        DescribeRecordKvpRequestReader reader = new DescribeRecordKvpRequestReader();
        Object request = reader.createRequest();
        DescribeRecordType dr = (DescribeRecordType) reader.read(request, parseKvp(raw), raw);

        assertDescribeRecordValid(dr);
    }

    @Test
    public void testXMLReader() throws Exception {
        CSWXmlReader reader =
                new CSWXmlReader(
                        "DescribeRecord",
                        "2.0.2",
                        new CSWConfiguration(),
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        DescribeRecordType dr =
                (DescribeRecordType)
                        reader.read(null, getResourceAsReader("DescribeRecord.xml"), null);
        assertDescribeRecordValid(dr);
    }

    // this is one of the CITE tests, unknown type names should just be ignored
    @Test
    public void testDummyRecord() throws Exception {
        Document dom =
                getAsDOM(
                        "csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=csw:DummyRecord");
        checkValidationErrors(dom);
        print(dom);

        assertXpathEvaluatesTo("1", "count(/csw:DescribeRecordResponse)", dom);
        assertXpathEvaluatesTo("0", "count(//csw:SchemaComponent)", dom);
    }

    @Test
    public void testBasicGetLocalSchema() throws Exception {
        Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord");
        checkValidationErrors(dom);
        print(dom);

        assertCswRecordSchema(dom, false);

        // check we can really read those schemas
        MockHttpServletResponse response =
                getAsServletResponse("/schemas/csw/2.0.2/rec-dcterms.xsd");
        assertEquals(200, response.getStatus());
        dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes("UTF-8")));
        assertXpathEvaluatesTo("dc:SimpleLiteral", "//xs:element[@name='abstract']/@type", dom);
    }

    @Test
    public void testBasicGetCanonicalSchema() throws Exception {
        try {
            CSWInfo csw = getGeoServer().getService(CSWInfo.class);
            csw.setCanonicalSchemaLocation(true);
            getGeoServer().save(csw);

            Document dom = getAsDOM("csw?service=CSW&version=2.0.2&request=DescribeRecord");
            checkValidationErrors(dom);
            // print(dom);

            assertCswRecordSchema(dom, true);
        } finally {
            CSWInfo csw = getGeoServer().getService(CSWInfo.class);
            csw.setCanonicalSchemaLocation(false);
            getGeoServer().save(csw);
        }
    }

    @Test
    public void testBasicPost() throws Exception {
        String request = IOUtils.toString(getResourceAsReader("DescribeCswRecord.xml"));
        Document dom = postAsDOM("csw", request);
        checkValidationErrors(dom);
        // print(dom);

        assertCswRecordSchema(dom, false);
    }

    private void assertCswRecordSchema(Document dom, boolean canonicalSchema) throws Exception {
        // print(dom);
        String root =
                canonicalSchema
                        ? "http://schemas.opengis.net"
                        : "http://localhost:8080/geoserver/schemas";
        assertXpathEvaluatesTo(
                "http://www.opengis.net/cat/csw/2.0.2 " + root + "/csw/2.0.2/CSW-discovery.xsd",
                "//csw:DescribeRecordResponse/@xsi:schemaLocation",
                dom);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'BriefRecord'])", dom);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'SummaryRecord'])", dom);
        assertXpathEvaluatesTo("1", "count(//xsd:element[@name = 'Record'])", dom);
        assertXpathEvaluatesTo(
                root + "/csw/2.0.2/rec-dcterms.xsd",
                "//xsd:import[@namespace = 'http://purl.org/dc/terms/']/@schemaLocation",
                dom);
    }

    @Test
    public void testAlternativeNamespacePrefix() throws Exception {
        Document dom =
                getAsDOM(
                        "csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=fuffa:Record&namespace=xmlns(fuffa=http://www.opengis.net/cat/csw/2.0.2)");
        assertCswRecordSchema(dom, false);
    }

    @Test
    public void testDefaultNamespacePrefix() throws Exception {
        Document dom =
                getAsDOM(
                        "csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=Record&namespace=xmlns(=http://www.opengis.net/cat/csw/2.0.2)");
        // print(dom);
        assertCswRecordSchema(dom, false);
    }

    @Test
    public void testMissingOutputFormat() throws Exception {
        Document dom =
                getAsDOM(
                        "csw?service=CSW&version=2.0.2&request=DescribeRecord&outputFormat=text/sgml");
        checkOws10Exception(dom, ServiceException.INVALID_PARAMETER_VALUE, "outputFormat");
    }

    @Test
    public void testInvalidSchemaLanguage() throws Exception {
        Document dom =
                getAsDOM(
                        "csw?service=CSW&version=2.0.2&request=DescribeRecord&schemaLanguage=http://purl.oclc.org/dsdl/schematron");
        checkOws10Exception(dom, ServiceException.INVALID_PARAMETER_VALUE, "schemaLanguage");
    }
}
