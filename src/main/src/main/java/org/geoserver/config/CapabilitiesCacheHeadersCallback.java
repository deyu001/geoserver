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

package org.geoserver.config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpHeaders;

/**
 * Adds proper caching headers to capabilites response clients paying attention to HTTP headers do
 * not think they are cacheable
 *
 * <p>The callback can be turned off by setting "CAPABILITIES_CACHE_CONTROL_ENABLED" to "false",
 * either as a system, environment or servlet context variable.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CapabilitiesCacheHeadersCallback extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(CapabilitiesCacheHeadersCallback.class);

    boolean capabilitiesCacheHeadersEnabled;

    GeoServer gs;

    public CapabilitiesCacheHeadersCallback(GeoServer gs) {
        this.gs = gs;

        // initialize headers processing by grabbing the default from a property
        final String value = GeoServerExtensions.getProperty("CAPABILITIES_CACHE_CONTROL_ENABLED");
        if (value != null) {
            capabilitiesCacheHeadersEnabled = Boolean.parseBoolean(value);
        } else {
            capabilitiesCacheHeadersEnabled = true;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Cache control for capabilities requests and 304 support is enabled: "
                            + capabilitiesCacheHeadersEnabled);
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        if (handleCachingHeaders(request)) {
            return new RevalidateTagResponse(response);
        }

        return response;
    }

    /** Returns true if the caching headers are enabled and the request is a GetCapabilities one */
    private boolean handleCachingHeaders(Request request) {
        return capabilitiesCacheHeadersEnabled
                && "GetCapabilities".equalsIgnoreCase(request.getRequest());
    }

    /**
     * Returns true if the callback will handle cache headers in GetCapabilities requests/responses
     */
    public boolean isCapabilitiesCacheHeadersEnabled() {
        return capabilitiesCacheHeadersEnabled;
    }

    /** Enables/disables the caching headers processing for this callback */
    public void setCapabilitiesCacheHeadersEnabled(boolean capabilitiesCacheHeadersEnabled) {
        this.capabilitiesCacheHeadersEnabled = capabilitiesCacheHeadersEnabled;
    }

    /**
     * A Response wrapper adding caching headers on demand
     *
     * @author aaime
     */
    private class RevalidateTagResponse extends Response {

        Response delegate;

        public RevalidateTagResponse(Response delegate) {
            super(delegate.getBinding());
            this.delegate = delegate;
        }

        public boolean canHandle(Operation operation) {
            return delegate.canHandle(operation);
        }

        public String getMimeType(Object value, Operation operation) throws ServiceException {
            return delegate.getMimeType(value, operation);
        }

        /**
         * See if we have to add cache control headers. Won't alter them if the response already set
         * them.
         */
        public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
            String[][] headers = delegate.getHeaders(value, operation);
            if (headers == null) {
                // if no headers at all, add and exit
                return new String[][] {{HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate"}};
            } else {
                // will add only if not already there
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map) ArrayUtils.toMap(headers);
                map.putIfAbsent(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate");
                headers = new String[map.size()][2];
                int i = 0;
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    headers[i][0] = entry.getKey();
                    headers[i][1] = entry.getValue();
                    i++;
                }
            }

            return headers;
        }

        public void write(Object value, OutputStream output, Operation operation)
                throws IOException, ServiceException {
            delegate.write(value, output, operation);
        }

        public String getPreferredDisposition(Object value, Operation operation) {
            return delegate.getPreferredDisposition(value, operation);
        }

        public String getAttachmentFileName(Object value, Operation operation) {
            return delegate.getAttachmentFileName(value, operation);
        }

        public String getCharset(Operation operation) {
            return delegate.getCharset(operation);
        }
    }
}
