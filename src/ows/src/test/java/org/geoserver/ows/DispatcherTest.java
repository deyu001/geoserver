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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.test.CodeExpectingHttpServletResponse;
import org.geotools.util.Version;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Document;

public class DispatcherTest {
    @Test
    public void testReadContextAndPath() throws Exception {
        Dispatcher dispatcher = new Dispatcher();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Request req = new Request();
        req.httpRequest = request;

        dispatcher.init(req);
        Assert.assertNull(req.context);
        Assert.assertEquals("hello", req.path);

        request.setRequestURI("/geoserver/foo/hello");
        dispatcher.init(req);
        Assert.assertEquals("foo", req.context);
        Assert.assertEquals("hello", req.path);

        request.setRequestURI("/geoserver/foo/baz/hello/");
        dispatcher.init(req);
        Assert.assertEquals("foo/baz", req.context);
        Assert.assertEquals("hello", req.path);
    }

    @Test
    public void testReadOpContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        Map map = dispatcher.readOpContext(req);

        Assert.assertEquals("hello", map.get("service"));

        request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/foobar/hello");
        request.setMethod("get");
        map = dispatcher.readOpContext(req);
        Assert.assertEquals("hello", map.get("service"));

        request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/foobar/hello/");
        request.setMethod("get");
        map = dispatcher.readOpContext(req);

        Assert.assertEquals("hello", map.get("service"));
    }

    @Test
    public void testReadOpPost() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("post");

        String body = "<Hello service=\"hello\"/>";

        DelegatingServletInputStream input =
                new DelegatingServletInputStream(new ByteArrayInputStream(body.getBytes()));

        Dispatcher dispatcher = new Dispatcher();

        try (BufferedReader buffered = new BufferedReader(new InputStreamReader(input))) {
            buffered.mark(2048);

            Map map = dispatcher.readOpPost(buffered);

            Assert.assertNotNull(map);
            Assert.assertEquals("Hello", map.get("request"));
            Assert.assertEquals("hello", map.get("service"));
        }
    }

    @Test
    public void testParseKVP() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContextPath("/geoserver");

            request.addParameter("service", "hello");
            request.addParameter("request", "Hello");
            request.addParameter("message", "Hello world!");

            request.setQueryString("service=hello&request=hello&message=Hello World!");

            Request req = new Request();
            req.setHttpRequest(request);

            dispatcher.parseKVP(req);

            Message message = (Message) dispatcher.parseRequestKVP(Message.class, req);
            Assert.assertEquals(new Message("Hello world!"), message);
        }
    }

    @Test
    public void testParseXML() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");
        File file = File.createTempFile("geoserver", "req");
        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            String body = "<Hello service=\"hello\" message=\"Hello world!\"/>";
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(body.getBytes());
                output.flush();
            }

            try (BufferedReader input =
                    new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {

                input.mark(8192);

                Request req = new Request();
                req.setInput(input);

                Object object = dispatcher.parseRequestXML(null, input, req);
                Assert.assertEquals(new Message("Hello world!"), object);
            }
        } finally {
            file.delete();
        }
    }

    @Test
    public void testHelloOperationGet() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            MockHttpServletRequest request =
                    new MockHttpServletRequest() {
                        String encoding;

                        public int getServerPort() {
                            return 8080;
                        }

                        public String getCharacterEncoding() {
                            return encoding;
                        }

                        public void setCharacterEncoding(String encoding) {
                            this.encoding = encoding;
                        }
                    };

            request.setScheme("http");
            request.setServerName("localhost");

            request.setContextPath("/geoserver");
            request.setMethod("GET");

            MockHttpServletResponse response = new MockHttpServletResponse();

            request.addParameter("service", "hello");
            request.addParameter("request", "Hello");
            request.addParameter("version", "1.0.0");
            request.addParameter("message", "Hello world!");

            request.setRequestURI(
                    "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
            request.setQueryString("service=hello&request=hello&message=HelloWorld");

            dispatcher.callbacks.add(
                    new AbstractDispatcherCallback() {
                        @Override
                        public Object operationExecuted(
                                Request request, Operation operation, Object result) {
                            Operation op = Dispatcher.REQUEST.get().getOperation();
                            Assert.assertNotNull(op);
                            Assert.assertTrue(op.getService().getService() instanceof HelloWorld);
                            Assert.assertTrue(op.getParameters()[0] instanceof Message);
                            return result;
                        }
                    });

            dispatcher.handleRequest(request, response);
            Assert.assertEquals("Hello world!", response.getContentAsString());
        }
    }

    @Test
    public void testHelloOperationPost() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            final String body =
                    "<Hello service=\"hello\" message=\"Hello world!\" version=\"1.0.0\" />";
            MockHttpServletRequest request =
                    new MockHttpServletRequest() {
                        String encoding;

                        public int getServerPort() {
                            return 8080;
                        }

                        public String getCharacterEncoding() {
                            return encoding;
                        }

                        public void setCharacterEncoding(String encoding) {
                            this.encoding = encoding;
                        }

                        @SuppressWarnings("PMD.CloseResource")
                        public ServletInputStream getInputStream() {
                            final ServletInputStream stream = super.getInputStream();
                            return new ServletInputStream() {
                                public int read() throws IOException {
                                    return stream.read();
                                }

                                public int available() {
                                    return body.length();
                                }
                            };
                        }
                    };

            request.setScheme("http");
            request.setServerName("localhost");
            request.setContextPath("/geoserver");
            request.setMethod("POST");
            request.setRequestURI("http://localhost/geoserver/ows");
            request.setContentType("application/xml");
            request.setContent(body.getBytes("UTF-8"));

            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.handleRequest(request, response);
            Assert.assertEquals("Hello world!", response.getContentAsString());
        }
    }

    /** Tests mixed get/post situations for cases in which there is no kvp parser */
    @Test
    public void testHelloOperationMixed() throws Exception {
        URL url = getClass().getResource("applicationContextOnlyXml.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            final String body =
                    "<Hello service=\"hello\" message=\"Hello world!\" version=\"1.0.0\" />";

            MockHttpServletRequest request =
                    new MockHttpServletRequest() {
                        String encoding;

                        public int getServerPort() {
                            return 8080;
                        }

                        public String getCharacterEncoding() {
                            return encoding;
                        }

                        public void setCharacterEncoding(String encoding) {
                            this.encoding = encoding;
                        }

                        @SuppressWarnings("PMD.CloseResource")
                        public ServletInputStream getInputStream() {
                            final ServletInputStream stream = super.getInputStream();
                            return new ServletInputStream() {
                                public int read() throws IOException {
                                    return stream.read();
                                }

                                public int available() {
                                    return body.length();
                                }
                            };
                        }
                    };

            request.setScheme("http");
            request.setServerName("localhost");
            request.setContextPath("/geoserver");
            request.setMethod("POST");
            request.setRequestURI("http://localhost/geoserver/ows");
            request.setContentType("application/xml");
            request.setContent(body.getBytes("UTF-8"));

            MockHttpServletResponse response = new MockHttpServletResponse();

            request.addParameter("strict", "true");

            dispatcher.handleRequest(request, response);
            Assert.assertEquals("Hello world!", response.getContentAsString());
        }
    }

    @Test
    public void testHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("httpErrorCodeException", HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testWrappedHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("wrappedHttpErrorCodeException", HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testBadRequestHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("badRequestHttpErrorCodeException", HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testHttpErrorCodeExceptionWithContentType() throws Exception {
        CodeExpectingHttpServletResponse rsp =
                assertHttpErrorCode(
                        "httpErrorCodeExceptionWithContentType", HttpServletResponse.SC_OK);
        Assert.assertEquals("application/json", rsp.getContentType());
    }

    private CodeExpectingHttpServletResponse assertHttpErrorCode(
            String requestType, int expectedCode) throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            MockHttpServletRequest request =
                    new MockHttpServletRequest() {
                        String encoding;

                        public int getServerPort() {
                            return 8080;
                        }

                        public String getCharacterEncoding() {
                            return encoding;
                        }

                        public void setCharacterEncoding(String encoding) {
                            this.encoding = encoding;
                        }
                    };

            request.setScheme("http");
            request.setServerName("localhost");

            request.setContextPath("/geoserver");
            request.setMethod("GET");

            CodeExpectingHttpServletResponse response =
                    new CodeExpectingHttpServletResponse(new MockHttpServletResponse());

            request.addParameter("service", "hello");
            request.addParameter("request", requestType);
            request.addParameter("version", "1.0.0");

            request.setRequestURI(
                    "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
            request.setQueryString("service=hello&request=hello&message=HelloWorld");

            dispatcher.handleRequest(request, response);
            Assert.assertEquals(expectedCode, response.getStatusCode());

            Assert.assertEquals(expectedCode >= 400, response.isError());
            return response;
        }
    }

    /**
     * Assert that if the service bean implements the optional {@link DirectInvocationService}
     * operation, then the dispatcher executes the operation through its {@link
     * DirectInvocationService#invokeDirect} method instead of through {@link Method#invoke
     * reflection}.
     */
    @Test
    public void testDirectInvocationService() throws Throwable {

        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            final AtomicBoolean invokeDirectCalled = new AtomicBoolean();
            DirectInvocationService serviceBean =
                    new DirectInvocationService() {

                        @Override
                        public Object invokeDirect(String operationName, Object[] parameters)
                                throws IllegalArgumentException, Exception {
                            invokeDirectCalled.set(true);
                            if ("concat".equals(operationName)) {
                                String param1 = (String) parameters[0];
                                String param2 = (String) parameters[1];
                                return concat(param1, param2);
                            }
                            throw new IllegalArgumentException("Unknown operation name");
                        }

                        public String concat(String param1, String param2) {
                            return param1 + param2;
                        }
                    };

            Service service =
                    new Service(
                            "directCallService",
                            serviceBean,
                            new Version("1.0.0"),
                            Collections.singletonList("concat"));
            Method method = serviceBean.getClass().getMethod("concat", String.class, String.class);
            Object[] parameters = {"p1", "p2"};
            Operation opDescriptor = new Operation("concat", service, method, parameters);

            Object result = dispatcher.execute(new Request(), opDescriptor);
            Assert.assertEquals("p1p2", result);
            Assert.assertTrue(invokeDirectCalled.get());
        }
    }

    @Test
    public void testDispatchWithNamespace() throws Exception {
        URL url = getClass().getResource("applicationContextNamespace.xml");
        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            MockHttpServletRequest request =
                    new MockHttpServletRequest() {
                        String encoding;

                        public int getServerPort() {
                            return 8080;
                        }

                        public String getCharacterEncoding() {
                            return encoding;
                        }

                        public void setCharacterEncoding(String encoding) {
                            this.encoding = encoding;
                        }
                    };

            request.setScheme("http");
            request.setServerName("localhost");

            request.setContextPath("/geoserver");
            request.setMethod("POST");

            MockHttpServletResponse response = new MockHttpServletResponse();

            request.setContentType("application/xml");
            request.setContent(
                    "<h:Hello service='hello' message='Hello world!' xmlns:h='http://hello.org' />"
                            .getBytes("UTF-8"));
            request.setRequestURI("http://localhost/geoserver/hello");

            dispatcher.handleRequest(request, response);
            Assert.assertEquals("Hello world!", response.getContentAsString());

            request.setContent(
                    "<h:Hello service='hello' message='Hello world!' xmlns:h='http://hello.org/v2' />"
                            .getBytes("UTF-8"));

            response = new MockHttpServletResponse();
            dispatcher.handleRequest(request, response);
            Assert.assertEquals("Hello world!:V2", response.getContentAsString());
        }
    }

    public MockHttpServletRequest setupRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest() {
                    String encoding;

                    public int getServerPort() {
                        return 8080;
                    }

                    public String getCharacterEncoding() {
                        return encoding;
                    }

                    public void setCharacterEncoding(String encoding) {
                        this.encoding = encoding;
                    }
                };

        request.setScheme("http");
        request.setServerName("localhost");

        request.setContextPath("/geoserver");
        request.setMethod("GET");

        request.addParameter("service", "hello");
        request.addParameter("request", "Hello");
        request.addParameter("version", "1.0.0");
        request.addParameter("message", "Hello world!");

        request.setRequestURI(
                "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
        request.setQueryString("service=hello&request=hello&message=HelloWorld");

        return request;
    }

    @Test
    public void testDispatcherCallback() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            TestDispatcherCallback callback = new TestDispatcherCallback();

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback);

            dispatcher.handleRequest(request, response);
            Assert.assertEquals("Hello world!", response.getContentAsString());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback.dispatcherStatus.get());
        }
    }

    @Test
    public void testDispatcherCallbackFailInit() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            final TestDispatcherCallback callback1 = new TestDispatcherCallback();
            final TestDispatcherCallback callback2 = new TestDispatcherCallback();
            TestDispatcherCallback callbackFail =
                    new TestDispatcherCallback() {
                        @Override
                        public Request init(Request request) {
                            dispatcherStatus.set(Status.INIT);
                            throw new RuntimeException("TestDispatcherCallbackFailInit");
                        }
                    };

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback1);
            dispatcher.callbacks.add(callbackFail);
            dispatcher.callbacks.add(callback2);

            dispatcher.handleRequest(request, response);

            Assert.assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
        }
    }

    @Test
    public void testDispatcherCallbackFailServiceDispatched() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            final TestDispatcherCallback callback1 = new TestDispatcherCallback();
            final TestDispatcherCallback callback2 = new TestDispatcherCallback();
            TestDispatcherCallback callbackFail =
                    new TestDispatcherCallback() {
                        @Override
                        public Service serviceDispatched(Request request, Service service) {
                            dispatcherStatus.set(Status.SERVICE_DISPATCHED);
                            throw new RuntimeException(
                                    "TestDispatcherCallbackFailServiceDispatched");
                        }
                    };

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback1);
            dispatcher.callbacks.add(callbackFail);
            dispatcher.callbacks.add(callback2);

            dispatcher.handleRequest(request, response);

            Assert.assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
        }
    }

    @Test
    public void testDispatcherCallbackFailOperationDispatched() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            final TestDispatcherCallback callback1 = new TestDispatcherCallback();
            final TestDispatcherCallback callback2 = new TestDispatcherCallback();
            TestDispatcherCallback callbackFail =
                    new TestDispatcherCallback() {
                        @Override
                        public Operation operationDispatched(Request request, Operation operation) {
                            dispatcherStatus.set(Status.OPERATION_DISPATCHED);
                            throw new RuntimeException(
                                    "TestDispatcherCallbackFailOperationDispatched");
                        }
                    };

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback1);
            dispatcher.callbacks.add(callbackFail);
            dispatcher.callbacks.add(callback2);

            dispatcher.handleRequest(request, response);

            Assert.assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
        }
    }

    @Test
    public void testDispatcherCallbackFailOperationExecuted() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            final TestDispatcherCallback callback1 = new TestDispatcherCallback();
            final TestDispatcherCallback callback2 = new TestDispatcherCallback();
            TestDispatcherCallback callbackFail =
                    new TestDispatcherCallback() {
                        @Override
                        public Object operationExecuted(
                                Request request, Operation operation, Object result) {
                            dispatcherStatus.set(Status.OPERATION_EXECUTED);
                            throw new RuntimeException(
                                    "TestDispatcherCallbackFailOperationExecuted");
                        }
                    };

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback1);
            dispatcher.callbacks.add(callbackFail);
            dispatcher.callbacks.add(callback2);

            dispatcher.handleRequest(request, response);

            Assert.assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
        }
    }

    @Test
    public void testDispatcherCallbackFailResponseDispatched() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            final TestDispatcherCallback callback1 = new TestDispatcherCallback();
            final TestDispatcherCallback callback2 = new TestDispatcherCallback();
            TestDispatcherCallback callbackFail =
                    new TestDispatcherCallback() {
                        @Override
                        public Response responseDispatched(
                                Request request,
                                Operation operation,
                                Object result,
                                Response response) {
                            dispatcherStatus.set(Status.RESPONSE_DISPATCHED);
                            throw new RuntimeException(
                                    "TestDispatcherCallbackFailResponseDispatched");
                        }
                    };

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback1);
            dispatcher.callbacks.add(callbackFail);
            dispatcher.callbacks.add(callback2);

            dispatcher.handleRequest(request, response);

            Assert.assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
        }
    }

    @Test
    public void testDispatcherCallbackFailFinished() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {
            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            final AtomicBoolean firedCallback = new AtomicBoolean(false);
            TestDispatcherCallback callback1 = new TestDispatcherCallback();
            TestDispatcherCallback callback2 =
                    new TestDispatcherCallback() {
                        @Override
                        public void finished(Request request) {
                            firedCallback.set(true);
                            super.finished(request);
                        }
                    };
            TestDispatcherCallback callbackFail =
                    new TestDispatcherCallback() {
                        @Override
                        public void finished(Request request) {
                            dispatcherStatus.set(Status.FINISHED);
                            // cleanups must continue even if an error was thrown
                            throw new Error("TestDispatcherCallbackFailFinished");
                        }
                    };

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback1);
            dispatcher.callbacks.add(callbackFail);
            dispatcher.callbacks.add(callback2);

            dispatcher.handleRequest(request, response);
            Assert.assertEquals("Hello world!", response.getContentAsString());
            Assert.assertTrue(firedCallback.get());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
            Assert.assertEquals(
                    TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
        }
    }

    @Test
    public void testErrorSavedOnRequestOnGenericException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        MockHttpServletResponse response = new MockHttpServletResponse();
        req.setHttpResponse(response);

        RuntimeException genericError = new RuntimeException("foo");
        dispatcher.exception(genericError, null, req);

        Assert.assertEquals("Exception did not get saved", genericError, req.error);
    }

    @Test
    public void testErrorSavedOnRequestOnNon304ErrorCodeException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        MockHttpServletResponse response = new MockHttpServletResponse();
        req.setHttpResponse(response);

        RuntimeException genericError = new HttpErrorCodeException(500, "Internal Server Error");
        dispatcher.exception(genericError, null, req);

        Assert.assertEquals("Exception did not get saved", genericError, req.error);
    }

    @Test
    public void testNoErrorOn304ErrorCodeException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        MockHttpServletResponse response = new MockHttpServletResponse();
        req.setHttpResponse(response);

        RuntimeException error = new HttpErrorCodeException(304, "Not Modified");
        dispatcher.exception(error, null, req);

        Assert.assertNull("Exception erroneously saved", req.error);
    }

    @Test
    public void testDispatchXMLException() throws Exception {
        // This test ensures that the text of the exception indicates that a wrong XML has been set
        URL url = getClass().getResource("applicationContextNamespace.xml");
        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            MockHttpServletRequest request =
                    new MockHttpServletRequest() {
                        String encoding;

                        public int getServerPort() {
                            return 8080;
                        }

                        public String getCharacterEncoding() {
                            return encoding;
                        }

                        public void setCharacterEncoding(String encoding) {
                            this.encoding = encoding;
                        }
                    };

            request.setScheme("http");
            request.setServerName("localhost");

            request.setContextPath("/geoserver");
            request.setMethod("POST");

            MockHttpServletResponse response = new MockHttpServletResponse();

            request.setContentType("application/xml");
            request.setContent("<h:Hello xmlns:h='http:/hello.org' />".getBytes("UTF-8"));
            request.setRequestURI("http://localhost/geoserver/hello");

            response = new MockHttpServletResponse();

            // Dispatch the request
            ModelAndView mov = dispatcher.handleRequestInternal(request, response);
            // Service exception, null is returned.
            Assert.assertNull(mov);
            // Check the response
            Assert.assertTrue(response.getContentAsString().contains("Could not parse the XML"));
        }
    }

    @Test
    public void testDispatchKVPException() throws Exception {
        // This test ensures that the text of the exception indicates that a wrong KVP has been set
        URL url = getClass().getResource("applicationContext4.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {

            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

            MockHttpServletRequest request =
                    new MockHttpServletRequest() {
                        String encoding;

                        public int getServerPort() {
                            return 8080;
                        }

                        public String getCharacterEncoding() {
                            return encoding;
                        }

                        public void setCharacterEncoding(String encoding) {
                            this.encoding = encoding;
                        }
                    };
            request.setScheme("http");
            request.setServerName("localhost");

            request.setContextPath("/geoserver");
            request.setMethod("GET");

            // request.setupAddParameter("service", "hello");
            request.addParameter("request", "Hello");
            // request.setupAddParameter("message", "Hello world!");
            request.setRequestURI("http://localhost/geoserver/hello");

            request.setQueryString("message=Hello World!");

            MockHttpServletResponse response = new MockHttpServletResponse();

            response = new MockHttpServletResponse();

            // Dispatch the request
            ModelAndView mov = dispatcher.handleRequestInternal(request, response);
            // Service exception, null is returned.
            Assert.assertNull(mov);
            // Check the response
            Assert.assertTrue(response.getContentAsString().contains("Could not parse the KVP"));
        }
    }

    @Test
    public void testMultiPartFormUpload() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("post");

        String xml = "<Hello service='hello' message='Hello world!' version='1.0.0' />";

        MimeMultipart body = new MimeMultipart();
        request.setContentType(body.getContentType());

        InternetHeaders headers = new InternetHeaders();
        headers.setHeader(
                "Content-Disposition", "form-data; name=\"upload\"; filename=\"request.xml\"");
        headers.setHeader("Content-Type", "application/xml");
        body.addBodyPart(new MimeBodyPart(headers, xml.getBytes()));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        body.writeTo(bout);

        request.setContent(bout.toByteArray());

        MockHttpServletResponse response = new MockHttpServletResponse();

        URL url = getClass().getResource("applicationContext.xml");
        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {
            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            dispatcher.handleRequestInternal(request, response);

            Assert.assertEquals("Hello world!", response.getContentAsString());
        }
    }

    @Test
    public void testMultiPartFormUploadWithBodyField() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("post");

        String xml = "<Hello service='hello' message='Hello world!' version='1.0.0' />";

        MimeMultipart body = new MimeMultipart();
        request.setContentType(body.getContentType());

        InternetHeaders headers = new InternetHeaders();
        headers.setHeader("Content-Disposition", "form-data; name=\"body\";");
        headers.setHeader("Content-Type", "application/xml");
        body.addBodyPart(new MimeBodyPart(headers, xml.getBytes()));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        body.writeTo(bout);

        request.setContent(bout.toByteArray());

        MockHttpServletResponse response = new MockHttpServletResponse();

        URL url = getClass().getResource("applicationContext.xml");
        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {
            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            dispatcher.handleRequestInternal(request, response);

            Assert.assertEquals("Hello world!", response.getContentAsString());
        }
    }

    @Test
    public void testErrorThrowingResponse() throws Exception {
        URL url = getClass().getResource("applicationContext-errorResponse.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {
            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            dispatcher.handleRequest(request, response);
            // the output is not there
            final String outputContent = response.getContentAsString();
            assertThat(outputContent, not(containsString("Hello world!")));
            // only the exception
            Document dom = XMLUnit.buildTestDocument(outputContent);
            Assert.assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        }
    }
}
