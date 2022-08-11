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

package org.geoserver.wcs.response;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * An implementation of {@link ServiceExceptionHandler} which outputs as service exception in a
 * <code>ServiceExceptionReport</code> document.
 *
 * <p>This handler is referred to as "legacy" as newer services move to the ows style exception
 * report. See {@link org.geoserver.ows.OWS10ServiceExceptionHandler}.
 *
 * <p>
 *
 * <h3>Version</h3>
 *
 * By default this exception handler will output a <code>ServiceExceptionReport</code> which is of
 * version <code>1.2.0</code>. This may be overriden with {@link #setVersion(String)}.
 *
 * <p>
 *
 * <h3>DTD and Schema</h3>
 *
 * By default, no DTD or XML Schema reference will be included in the document. The methods {@link
 * #setDTDLocation(String)} and {@link #setSchemaLocation(String)} can be used to override this
 * behaviour. Only one of these methods should be set per instance of this class.
 *
 * <p>The supplied value should be relative, and will be appended to the result of {@link
 * OWS#getSchemaBaseURL()}.
 *
 * <p>
 *
 * <h3>Content Type</h3>
 *
 * The default content type for the created document is <code>text/xml</code>, this can be
 * overridden with {@link #setContentType(String)}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LegacyServiceExceptionHandler extends ServiceExceptionHandler {
    /** the version of the service exceptoin report. */
    protected String version = "1.2.0";

    /** Location of document type defintion for document */
    protected String dtdLocation = null;

    /** Location of schema for document. */
    protected String schemaLocation = null;

    /** The content type of the produced document */
    protected String contentType = "text/xml";

    /** The central configuration, used to decide whether to dump a verbose stack trace, or not */
    protected GeoServer geoServer;

    public LegacyServiceExceptionHandler(List<Service> services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    public LegacyServiceExceptionHandler(Service service, GeoServer geoServer) {
        super(service);
        this.geoServer = geoServer;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDTDLocation(String dtd) {
        this.dtdLocation = dtd;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void handleServiceException(ServiceException exception, Request request) {
        String tab = "   ";
        StringBuffer sb = new StringBuffer();

        // xml header TODO: should the encoding the server default?
        sb.append("<?xml version=\"1.0\"");
        sb.append(" encoding=\"UTF-8\"");

        if (dtdLocation != null) {
            sb.append(" standalone=\"no\"");
        }

        sb.append("?>");

        // dtd location
        if (dtdLocation != null) {
            String fullDtdLocation = buildSchemaURL(baseURL(request.getHttpRequest()), dtdLocation);
            sb.append("<!DOCTYPE ServiceExceptionReport SYSTEM \"" + fullDtdLocation + "\"> ");
        }

        // root element
        sb.append("<ServiceExceptionReport version=\"" + version + "\" ");

        // xml schema location
        if ((schemaLocation != null) && (dtdLocation == null)) {
            String fullSchemaLocation =
                    buildSchemaURL(baseURL(request.getHttpRequest()), schemaLocation);

            sb.append("xmlns=\"http://www.opengis.net/ogc\" ");
            sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
            sb.append(
                    "xsi:schemaLocation=\"http://www.opengis.net/ogc " + fullSchemaLocation + "\"");
        }

        sb.append(">");

        // write out the service exception
        sb.append(tab + "<ServiceException");

        // exception code
        if ((exception.getCode() != null) && !exception.getCode().equals("")) {
            sb.append(" code=\"" + ResponseUtils.encodeXML(exception.getCode()) + "\"");
        }

        // exception locator
        if ((exception.getLocator() != null) && !exception.getLocator().equals("")) {
            sb.append(" locator=\"" + ResponseUtils.encodeXML(exception.getLocator()) + "\"");
        }

        sb.append(">");

        // message
        if ((exception.getMessage() != null)) {
            sb.append("\n" + tab + tab);
            OwsUtils.dumpExceptionMessages(exception, sb, true);

            if (geoServer.getSettings().isVerboseExceptions()) {
                ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
                exception.printStackTrace(new PrintStream(stackTrace));

                sb.append("\nDetails:\n");
                sb.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
            }
        }

        sb.append("\n</ServiceException>");
        sb.append("</ServiceExceptionReport>");

        HttpServletResponse response = request.getHttpResponse();
        response.setContentType(contentType);

        // TODO: server encoding?
        response.setCharacterEncoding("UTF-8");

        try {
            response.getOutputStream().write(sb.toString().getBytes());
            response.getOutputStream().flush();
        } catch (IOException e) {
            // throw new RuntimeException(e);
            // Hmm, not much we can do here.  I guess log the fact that we couldn't write out the
            // exception and be done with it...
            LOGGER.log(
                    Level.INFO, "Problem writing exception information back to calling client:", e);
        }
    }
}
