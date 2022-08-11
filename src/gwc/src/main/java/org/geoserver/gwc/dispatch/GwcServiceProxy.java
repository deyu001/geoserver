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

package org.geoserver.gwc.dispatch;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.gwc.config.GWCServiceEnablementInterceptor;
import org.geoserver.ows.DisabledServiceCheck;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Response;
import org.geotools.util.Version;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.GeoWebCacheExtensions;

/**
 * Service bean used as service implementation for the GeoServer {@link Dispatcher} when processing
 * GWC service requests.
 *
 * <p>See the package documentation for more insights on how these all fit together.
 */
public class GwcServiceProxy {

    private final ServiceInfoImpl serviceInfo;

    private final GeoWebCacheDispatcher gwcDispatcher;

    public GwcServiceProxy() {
        serviceInfo = new ServiceInfoImpl();
        serviceInfo.setId("gwc");
        serviceInfo.setName("gwc");
        serviceInfo.setEnabled(true);
        serviceInfo.setVersions(ImmutableList.of(new Version("1.0.0")));
        gwcDispatcher = GeoWebCacheExtensions.bean(GeoWebCacheDispatcher.class);
    }

    /**
     * This method is here to assist the {@link DisabledServiceCheck} callback, that uses reflection
     * to find such a method and check if the returned service info is {@link
     * ServiceInfo#isEnabled() enabled} (and hence avoid the WARNING message it spits out if this
     * method is not found); not though, that in the interest of keeping a single {@link
     * GwcServiceProxy} to proxy all gwc provided services (wmts, tms, etc), the service info
     * returned here will always be enabled, we already have a GWC {@link
     * GWCServiceEnablementInterceptor service interceptor} aspect that decorates specific gwc
     * services to check for enablement.
     */
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    /**
     * This method is the only operation defined for the {@link org.geoserver.platform.Service} bean
     * descriptor, and is meant to execute all requests to /gwc/service/*, delegating to the {@code
     * GeoWebCacheDispatcher}'s {@link GeoWebCacheDispatcher#handleRequest(HttpServletRequest,
     * HttpServletResponse) handleRequest(HttpServletRequest, HttpServletResponse)} method, and
     * return a response object so that the GeoServer {@link Dispatcher} looks up a {@link Response}
     * that finally writes the result down to the client response stream.
     *
     * @see GwcOperationProxy
     * @see GwcResponseProxy
     */
    public GwcOperationProxy dispatch(HttpServletRequest rawRequest, HttpServletResponse rawRespose)
            throws Exception {

        //        DispatcherController.BASE_URL.set(ResponseUtils.baseURL(rawRequest));

        ResponseWrapper responseWrapper = new ResponseWrapper(rawRespose);

        gwcDispatcher.handleRequest(rawRequest, responseWrapper);

        final String contentType = responseWrapper.getContentType();
        final Map<String, String> headers = responseWrapper.getHeaders();
        final byte[] bytes = responseWrapper.out.getBytes();

        return new GwcOperationProxy(contentType, headers, bytes);
    }

    /** */
    private final class ResponseWrapper extends HttpServletResponseWrapper {

        final BufferedServletOutputStream out = new BufferedServletOutputStream();
        Map<String, String> headers = new LinkedHashMap<>();

        private ResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return out;
        }

        @Override
        public void setHeader(String name, String value) {
            headers.put(name, value);
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }

    private static class BufferedServletOutputStream extends ServletOutputStream {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(4096);

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            outputStream.write(b, off, len);
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }
}
