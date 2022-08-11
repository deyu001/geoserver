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

package org.geoserver.ows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xpath.XPathAPI;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class OWS10ServiceExceptionHandlerTest {

    private static OWS10ServiceExceptionHandler handler;
    private static MockHttpServletRequest request;
    private static MockHttpServletResponse response;
    private static Request requestInfo;

    private static final String XML_TYPE_TEXT = "text/xml";

    @BeforeClass
    public static void setupClass()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
                    SecurityException {
        // Playing with System.Properties and Static boolean fields can raises issues
        // when running Junit tests via Maven, due to initialization orders.
        // So let's change the fields via reflections for these tests
        Field field = OWS10ServiceExceptionHandler.class.getDeclaredField("CONTENT_TYPE");
        field.setAccessible(true);
        field.set(null, XML_TYPE_TEXT);
    }

    @AfterClass
    public static void teardownClass() {
        System.clearProperty("ows10.exception.xml.responsetype");
    }

    @Before
    public void setUp() throws Exception {
        HelloWorld helloWorld = new HelloWorld();
        Service service =
                new Service(
                        "hello",
                        helloWorld,
                        new Version("1.0.0"),
                        Collections.singletonList("hello"));

        request =
                new MockHttpServletRequest() {
                    public int getServerPort() {
                        return 8080;
                    }
                };

        request.setScheme("http");
        request.setServerName("localhost");

        request.setContextPath("geoserver");

        response = new MockHttpServletResponse();

        handler = new OWS10ServiceExceptionHandler();

        requestInfo = new Request();
        requestInfo.setHttpRequest(request);
        requestInfo.setHttpResponse(response);
        requestInfo.setService(service.getId());
        requestInfo.setVersion(service.getVersion().toString());
    }

    @Test
    public void testHandleServiceException() throws Exception {
        ServiceException exception = new ServiceException("hello service exception");
        exception.setCode("helloCode");
        exception.setLocator("helloLocator");
        exception.getExceptionText().add("helloText");
        handler.handleServiceException(exception, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testHandleServiceExceptionEncoding() throws Exception {
        String message = "foo & <foo> \"foo's\"";

        ServiceException exception = new ServiceException(message);
        exception.setLocator("test-locator");

        handler.handleServiceException(exception, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);

        Node exceptionText =
                XPathAPI.selectSingleNode(
                        doc, "ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()");
        assertNotNull(exceptionText);
        assertEquals(
                "round-tripped through character entities",
                message,
                exceptionText.getTextContent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleServiceExceptionEncodingMore() throws Exception {
        String message1 = "foo & <foo> \"foo's\"";
        String message2 = "a \"different\" <message>";

        ServiceException exception = new ServiceException(message1);
        exception.setLocator("test-locator");
        exception.getExceptionText().add(message2);

        handler.handleServiceException(exception, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);

        Node exceptionText =
                XPathAPI.selectSingleNode(
                        doc, "ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()");
        assertNotNull(exceptionText);
        String message = message1 + "\n" + message2;
        assertEquals(
                "round-tripped through character entities",
                message,
                exceptionText.getTextContent());
    }

    @Test
    public void testHandleServiceExceptionCauses() throws Exception {
        // create a stack of three exceptions
        IllegalArgumentException illegalArgument =
                new IllegalArgumentException("Illegal argument here");
        IOException ioException = new IOException("I/O exception here");
        ioException.initCause(illegalArgument);
        ServiceException serviceException = new ServiceException("hello service exception");
        serviceException.setCode("helloCode");
        serviceException.setLocator("helloLocator");
        serviceException.getExceptionText().add("helloText");
        serviceException.initCause(ioException);
        handler.handleServiceException(serviceException, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        Node exceptionTextNode =
                XPathAPI.selectSingleNode(
                        doc, "ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()");
        assertNotNull(exceptionTextNode);
        // normalise whitespace
        String exceptionText = exceptionTextNode.getNodeValue().replaceAll("\\s+", " ");
        assertNotEquals(exceptionText.indexOf(illegalArgument.getMessage()), -1);
        assertNotEquals(exceptionText.indexOf(ioException.getMessage()), -1);
        assertNotEquals(exceptionText.indexOf(serviceException.getMessage()), -1);
    }

    @Test
    public void testHandleServiceExceptionNullMessages() throws Exception {
        // create a stack of three exceptions
        NullPointerException npe = new NullPointerException();
        ServiceException serviceException = new ServiceException("hello service exception");
        serviceException.setCode("helloCode");
        serviceException.setLocator("helloLocator");
        serviceException.getExceptionText().add("NullPointerException");
        serviceException.initCause(npe);
        handler.handleServiceException(serviceException, requestInfo);

        InputStream input = new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        Node exceptionTextNode =
                XPathAPI.selectSingleNode(
                        doc, "ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()");
        assertNotNull(exceptionTextNode);
        // normalise whitespace
        String exceptionText = exceptionTextNode.getNodeValue().replaceAll("\\s+", " ");
        // used to contain an extra " null" at the end
        assertEquals("hello service exception NullPointerException", exceptionText);
    }

    @Test
    public void exceptionType() throws Exception {
        String message1 = "foo & <foo> \"foo's\"";
        String message2 = "a \"different\" <message>";

        ServiceException exception = new ServiceException(message1);
        exception.setLocator("test-locator");
        exception.getExceptionText().add(message2);

        handler.handleServiceException(exception, requestInfo);
        assertEquals(XML_TYPE_TEXT, response.getContentType());
    }
}
