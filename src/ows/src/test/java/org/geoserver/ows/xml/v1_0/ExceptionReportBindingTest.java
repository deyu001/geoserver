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

package org.geoserver.ows.xml.v1_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.ows10.ExceptionReportType;
import net.opengis.ows10.ExceptionType;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.test.XMLTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ExceptionReportBindingTest extends XMLTestSupport {

    @Override
    protected Configuration createConfiguration() {
        return new OWSConfiguration();
    }

    Document dom(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes()));
    }

    @Test
    public void testParseServiceException() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<ows:ExceptionReport version=\"1.0.0\"\n"
                        + "  xsi:schemaLocation=\"http://www.opengis.net/ows http://demo.opengeo.org/geoserver/schemas/ows/1.0.0/owsExceptionReport.xsd\"\n"
                        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ows=\"http://www.opengis.net/ows\">\n"
                        + "  <ows:Exception exceptionCode=\"InvalidParameterValue\" locator=\"service\">\n"
                        + "    <ows:ExceptionText>No service: ( madeUp )</ows:ExceptionText>\n"
                        + "  </ows:Exception>\n"
                        + "</ows:ExceptionReport>\n"
                        + "";

        document = dom(xml);
        Object result = parse(OWS.EXCEPTIONREPORT);

        assertNotNull(result);
        assertTrue(result instanceof ExceptionReportType);
        ExceptionReportType er = (ExceptionReportType) result;

        assertEquals("1.0.0", er.getVersion());
        assertEquals(1, er.getException().size());
        ExceptionType ex = er.getException().get(0);
        assertEquals("InvalidParameterValue", ex.getExceptionCode());
        assertEquals("service", ex.getLocator());
        assertEquals(1, ex.getExceptionText().size());
        assertEquals("No service: ( madeUp )", ex.getExceptionText().get(0));
    }
}
