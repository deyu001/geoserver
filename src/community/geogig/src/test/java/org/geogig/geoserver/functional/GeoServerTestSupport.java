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

package org.geogig.geoserver.functional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.w3c.dom.Document;

/** Helper class for running mock http requests. */
@TestSetup(run = TestSetupFrequency.ONCE)
class GeoServerTestSupport extends GeoServerSystemTestSupport {

    public void setUpGeoServer() throws Exception {
        // use the OGC standard for axis order
        //
        // must be done *before* super.oneTimeSetUp() to ensure CRS factories
        // configured before data is loaded
        //
        // if this property is null, GeoServerAbstractTestSupport.oneTimeSetUp()
        // will blow away our changes
        System.setProperty("org.geotools.referencing.forceXY", "false");
        // yes, we need this too
        Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, false);
        // if this is set to anything but "http", GeoServerAbstractTestSupport.oneTimeSetUp()
        // will blow away our changes
        Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, "http");
        // apply changes
        CRS.reset("all");
        doSetup();
    }

    public void shutDownUpGeoServer() throws Exception {
        doTearDownClass();
        // undo the changes made for this suite and reset
        System.clearProperty("org.geotools.referencing.forceXY");
        Hints.removeSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER);
        Hints.removeSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING);
        CRS.reset("all");
    }

    /** Override to avoid creating default geoserver test data */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {}

    /** @return the catalog used by the test helper */
    public Catalog getCatalog() {
        return super.getCatalog();
    }

    /**
     * Issue a POST request to the provided URL with the given file passed as form data.
     *
     * @param resourceUri the url to issue the request to
     * @param formFieldName the form field name for the file to be posted
     * @param file the file to post
     * @return the response to the request
     */
    public MockHttpServletResponse postFile(String resourceUri, String formFieldName, File file)
            throws Exception {

        try (FileInputStream fis = new FileInputStream(file)) {
            MockMultipartFile mFile = new MockMultipartFile(formFieldName, fis);
            MockMultipartHttpServletRequestBuilder requestBuilder =
                    MockMvcRequestBuilders.fileUpload(new URI(resourceUri)).file(mFile);

            MockHttpServletRequest request =
                    requestBuilder.buildRequest(applicationContext.getServletContext());

            /**
             * Duplicated from GeoServerSystemTestSupport#createRequest to do the same work on the
             * MockMultipartHttpServletRequest
             */
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(8080);
            request.setContextPath("/geoserver");
            request.setRequestURI(
                    ResponseUtils.stripQueryString(
                            ResponseUtils.appendPath("/geoserver/", resourceUri)));
            // request.setRequestURL(ResponseUtils.appendPath("http://localhost:8080/geoserver",
            // path ) );
            request.setQueryString(ResponseUtils.getQueryString(resourceUri));
            request.setRemoteAddr("127.0.0.1");
            request.setServletPath(
                    ResponseUtils.makePathAbsolute(ResponseUtils.stripRemainingPath(resourceUri)));
            request.setPathInfo(
                    ResponseUtils.makePathAbsolute(
                            ResponseUtils.stripBeginningPath(
                                    ResponseUtils.stripQueryString(resourceUri))));
            request.addHeader("Host", "localhost:8080");

            // deal with authentication
            if (username != null) {
                String token = username + ":";
                if (password != null) {
                    token += password;
                }
                request.addHeader(
                        "Authorization",
                        "Basic " + new String(Base64.encodeBase64(token.getBytes())));
            }

            kvp(request, resourceUri);

            request.setUserPrincipal(null);
            /** End duplication */
            return dispatch(request);
        }
    }

    /** Copied from parent class to do the same work on MockMultipartHttpServletRequest. */
    private void kvp(MockHttpServletRequest request, String path) {
        Map<String, Object> params = KvpUtils.parseQueryString(path);
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value instanceof String) {
                request.addParameter(key, (String) value);
            } else {
                String[] values = (String[]) value;
                request.addParameter(key, values);
            }
        }
    }

    /**
     * Issue a POST request to the provided URL with the given content.
     *
     * @param contentType the content type of the data
     * @param resourceUri the url to issue the request to
     * @param postContent the content to be posted
     * @return the response to the request
     */
    public MockHttpServletResponse postContent(
            String contentType, String resourceUri, String postContent) throws Exception {

        MockHttpServletRequest req = createRequest(resourceUri);

        req.setContentType(contentType);
        req.addHeader("Content-Type", contentType);
        req.setMethod("POST");
        req.setContent(postContent == null ? null : postContent.getBytes());

        return dispatch(req);
    }

    /**
     * Issue a request with the given {@link HttpMethod} to the provided resource URI.
     *
     * @param method the http method to use
     * @param resourceUri the uri to issue the request to
     * @return the response to the request
     */
    public MockHttpServletResponse callInternal(HttpMethod method, String resourceUri)
            throws Exception {
        MockHttpServletRequest request = super.createRequest(resourceUri);
        request.setMethod(method.name());

        return dispatch(request, null);
    }

    public MockHttpServletResponse callWithContentTypeInternal(
            HttpMethod method, String resourceUri, String payload, String contentType)
            throws Exception {
        MockHttpServletRequest request = super.createRequest(resourceUri);
        request.setMethod(method.name());
        // set the JSON payload
        request.setContent(payload.getBytes());
        request.setContentType(contentType);

        return dispatch(request, null);
    }

    /**
     * Provide access to the helper function that turns the response into a {@link Document}.
     *
     * @param stream the stream to read as a document
     * @return the {@link Document}
     */
    public Document getDom(InputStream stream) throws Exception {
        return dom(stream);
    }
}
