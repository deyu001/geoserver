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

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import net.opengis.ows10.ExceptionReportType;
import net.opengis.ows10.ExceptionType;
import net.opengis.ows10.Ows10Factory;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.xml.v1_0.OWSConfiguration;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.xsd.Encoder;

/**
 * A default implementation of {@link ServiceExceptionHandler} which outputs as service exception in
 * a <code>ows:ExceptionReport</code> document.
 *
 * <p>This service exception handler will generate an OWS exception report, see <a
 * href="http://schemas.opengis.net/ows/1.0.0/owsExceptionReport.xsd">owsExceptionReport.xsd</a> .
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class OWS10ServiceExceptionHandler extends ServiceExceptionHandler {

    private static String CONTENT_TYPE =
            System.getProperty("ows10.exception.xml.responsetype", DEFAULT_XML_MIME_TYPE);

    protected boolean verboseExceptions = false;

    /** Constructor to be called if the exception is not for a particular service. */
    public OWS10ServiceExceptionHandler() {
        super(Collections.emptyList());
    }

    /**
     * Constructor to be called if the exception is for a particular service.
     *
     * @param services List of services this handler handles exceptions for.
     */
    public OWS10ServiceExceptionHandler(List<Service> services) {
        super(services);
    }

    /** Writes out an OWS ExceptionReport document. */
    public void handleServiceException(ServiceException exception, Request request) {
        Ows10Factory factory = Ows10Factory.eINSTANCE;

        ExceptionType e = factory.createExceptionType();

        if (exception.getCode() != null) {
            e.setExceptionCode(exception.getCode());
        } else {
            // set a default
            e.setExceptionCode("NoApplicableCode");
        }

        e.setLocator(exception.getLocator());

        // add the message
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(exception, sb, false);
        e.getExceptionText().add(sb.toString());
        e.getExceptionText().addAll(exception.getExceptionText());

        if (verboseExceptions) {
            // add the entire stack trace
            // exception.
            e.getExceptionText().add("Details:");
            ByteArrayOutputStream trace = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(trace));
            e.getExceptionText().add(new String(trace.toByteArray()));
        }

        ExceptionReportType report = factory.createExceptionReportType();
        report.setVersion("1.0.0");
        report.getException().add(e);

        if (!request.isSOAP()) {
            // there will already be a SOAP mime type
            request.getHttpResponse().setContentType(CONTENT_TYPE);
        }

        // response.setCharacterEncoding( "UTF-8" );
        OWSConfiguration configuration = new OWSConfiguration();

        XSDSchema result;
        try {
            result = configuration.getXSD().getSchema();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        Encoder encoder = new Encoder(configuration, result);
        encoder.setIndenting(true);
        encoder.setIndentSize(2);
        encoder.setLineWidth(60);
        encoder.setOmitXMLDeclaration(request.isSOAP());

        String schemaLocation =
                buildSchemaURL(
                        baseURL(request.getHttpRequest()), "ows/1.0.0/owsExceptionReport.xsd");
        encoder.setSchemaLocation(org.geoserver.ows.xml.v1_0.OWS.NAMESPACE, schemaLocation);

        try {
            encoder.encode(
                    report,
                    org.geoserver.ows.xml.v1_0.OWS.EXCEPTIONREPORT,
                    request.getHttpResponse().getOutputStream());
        } catch (Exception ex) {
            // throw new RuntimeException(ex);
            // Hmm, not much we can do here.  I guess log the fact that we couldn't write out the
            // exception and be done with it...
            LOGGER.log(
                    Level.INFO, "Problem writing exception information back to calling client:", e);
        } finally {
            try {
                request.getHttpResponse().getOutputStream().flush();
            } catch (IOException ioe) {
            }
        }
    }
}
