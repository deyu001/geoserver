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

package org.geoserver.wfs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.logging.Logging;

/**
 * Dispatcher callback that will enforce configured GML MIME type for WFS GML responses. If no GML
 * enforcing MIME type is configured nothing will be done.
 */
public final class WFSGMLMimeTypeEnforcer extends AbstractDispatcherCallback {

    private static final Logger LOGGER = Logging.getLogger(WFSGMLMimeTypeEnforcer.class);

    private final GeoServer geoserver;

    public WFSGMLMimeTypeEnforcer(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        Service service = operation.getService();
        if (service == null
                || service.getId() == null
                || !service.getId().equalsIgnoreCase("wfs")) {
            // not a WFS service so we are not interested in it
            return response;
        }
        String responseMimeType = response.getMimeType(result, operation);
        if (!isGmlBased(responseMimeType)) {
            // no a GML based response
            return response;
        }
        WFSInfo wfs = geoserver.getService(WFSInfo.class);
        GMLInfo gmlInfo =
                wfs.getGML().get(WFSInfo.Version.negotiate(service.getVersion().toString()));
        if (gmlInfo == null || !gmlInfo.getMimeTypeToForce().isPresent()) {
            // we don't need to force any specific MIME type
            return response;
        }
        // enforce the configured MIME type
        String mimeType = gmlInfo.getMimeTypeToForce().get();
        LOGGER.info(
                String.format(
                        "Overriding MIME type '%s' with '%s' for WFS operation '%s'.",
                        responseMimeType, mimeType, operation.getId()));
        return new ResponseWrapper(response, mimeType);
    }

    /** Helper method that checks if a MIME type is GML based. */
    private boolean isGmlBased(String candidateMimeType) {
        if (candidateMimeType == null) {
            // unlikely situation but in this we don't consider this MIME type a GML one
            return false;
        }
        // check if the MIME type contains GML
        candidateMimeType = candidateMimeType.toLowerCase();
        return candidateMimeType.contains("gml");
    }

    /** Helper wrapper for responses to use the configured MIME type. */
    private static final class ResponseWrapper extends Response {

        private final Response response;
        private final String mimeType;

        public ResponseWrapper(Response response, String mimeType) {
            super(response.getBinding(), mimeType);
            this.response = response;
            this.mimeType = mimeType;
        }

        @Override
        public String getMimeType(Object value, Operation operation) throws ServiceException {
            return mimeType;
        }

        @Override
        public void write(Object value, OutputStream output, Operation operation)
                throws IOException, ServiceException {
            response.write(value, output, operation);
        }

        @Override
        public boolean canHandle(Operation operation) {
            return response.canHandle(operation);
        }

        @Override
        public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
            return response.getHeaders(value, operation);
        }

        @Override
        public String getPreferredDisposition(Object value, Operation operation) {
            return response.getPreferredDisposition(value, operation);
        }

        @Override
        public String getAttachmentFileName(Object value, Operation operation) {
            return response.getAttachmentFileName(value, operation);
        }

        @Override
        public String getCharset(Operation operation) {
            return response.getCharset(operation);
        }
    }
}
