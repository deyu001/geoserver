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

package org.geoserver.wfs.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.OWS10ServiceExceptionHandler;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.json.JSONType;

/**
 * Handles a wfs service exception by producing an exception report.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Carlo Cancellieri - GeoSolutions
 */
public class WfsExceptionHandler extends OWS10ServiceExceptionHandler {

    GeoServer gs;

    /** @param services The wfs service descriptors. */
    public WfsExceptionHandler(List<Service> services, GeoServer gs) {
        super(services);
        this.gs = gs;
    }

    public WFSInfo getInfo() {
        return gs.getService(WFSInfo.class);
    }

    /** Encodes a ogc:ServiceExceptionReport to output. */
    public void handleServiceException(ServiceException exception, Request request) {

        boolean verbose = gs.getSettings().isVerboseExceptions();
        String charset = gs.getSettings().getCharset();
        // first of all check what kind of exception handling we must perform
        final String exceptions;
        try {
            exceptions = (String) request.getKvp().get("EXCEPTIONS");
            if (exceptions == null) {
                // use default
                handleDefault(exception, request, charset, verbose);
                return;
            }
        } catch (Exception e) {
            // width and height might be missing
            handleDefault(exception, request, charset, verbose);
            return;
        }
        if (JSONType.isJsonMimeType(exceptions)) {
            // use Json format
            JSONType.handleJsonException(LOGGER, exception, request, charset, verbose, false);
        } else if (JSONType.useJsonp(exceptions)) {
            // use JsonP format
            JSONType.handleJsonException(LOGGER, exception, request, charset, verbose, true);
        } else {
            handleDefault(exception, request, charset, verbose);
        }
    }

    private void handleDefault(
            ServiceException exception, Request request, String charset, boolean verbose) {
        if ("1.0.0".equals(request.getVersion())) {
            handle1_0(exception, request.getHttpResponse());
        } else {
            super.handleServiceException(exception, request);
        }
    }

    public void handle1_0(ServiceException e, HttpServletResponse response) {
        try {
            String tab = "   ";

            StringBuffer s = new StringBuffer();
            s.append("<?xml version=\"1.0\" ?>\n");
            s.append("<ServiceExceptionReport\n");
            s.append(tab + "version=\"1.2.0\"\n");
            s.append(tab + "xmlns=\"http://www.opengis.net/ogc\"\n");
            s.append(tab + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            s.append(tab);
            s.append("xsi:schemaLocation=\"http://www.opengis.net/ogc ");
            s.append(
                    ResponseUtils.appendPath(
                                    getInfo().getSchemaBaseURL(), "wfs/1.0.0/OGC-exception.xsd")
                            + "\">\n");

            s.append(tab + "<ServiceException");

            if ((e.getCode() != null) && !e.getCode().equals("")) {
                s.append(" code=\"" + ResponseUtils.encodeXML(e.getCode()) + "\"");
            }

            if ((e.getLocator() != null) && !e.getLocator().equals("")) {
                s.append(" locator=\"" + ResponseUtils.encodeXML(e.getLocator()) + "\"");
            }

            s.append(">");

            if (e.getMessage() != null) {
                s.append("\n" + tab + tab);
                OwsUtils.dumpExceptionMessages(e, s, true);

                if (verboseExceptions) {
                    ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
                    e.printStackTrace(new PrintStream(stackTrace));

                    s.append("\nDetails:\n");
                    s.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
                }
            }

            s.append("\n</ServiceException>");
            s.append("</ServiceExceptionReport>");

            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.getOutputStream().write(s.toString().getBytes());
            response.getOutputStream().flush();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
