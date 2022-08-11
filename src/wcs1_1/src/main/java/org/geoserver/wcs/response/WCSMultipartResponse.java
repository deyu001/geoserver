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

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.opengis.wcs11.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.response.CoveragesHandler.CoveragesData;
import org.geoserver.wcs.responses.CoverageEncoder;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;

public class WCSMultipartResponse extends Response {

    MimeMultipart multipart;

    Catalog catalog;

    CoverageResponseDelegateFinder responseFactory;

    public WCSMultipartResponse(Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        super(GridCoverage[].class);
        this.catalog = catalog;
        this.multipart = new MimeMultipart();
        this.responseFactory = responseFactory;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        // javamail outputs multipart/mixed, but in our case we're producing multipart/related
        return multipart
                .getContentType()
                .replace("mixed", "related")
                .replace("\n", "")
                .replace("\r", "");
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    public String getAttachmentFileName(Object value, Operation operation) {
        final GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        final String identifier = request.getIdentifier().getValue();
        return identifier.replace(':', '_') + ".eml";
    }

    @Override
    public boolean canHandle(Operation operation) {
        // this one can handle GetCoverage responses where store = false
        if (!(operation.getParameters()[0] instanceof GetCoverageType)) return false;

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        return !getCoverage.getOutput().isStore();
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GridCoverage[] coverages = (GridCoverage[]) value;

        // grab the delegate for coverage encoding
        GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = request.getOutput().getFormat();
        CoverageResponseDelegate delegate = responseFactory.encoderFor(outputFormat);
        if (delegate == null)
            throw new WcsException("Could not find encoder for output format " + outputFormat);

        // grab the coverage info for Coverages document encoding
        final GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        CoverageInfo coverageInfo = catalog.getCoverageByName(request.getIdentifier().getValue());

        // use javamail classes to actually encode the document
        try {
            // coverages xml structure (always set the headers after the data
            // handlers, setting
            // the data handlers kills some of them)
            BodyPart coveragesPart = new MimeBodyPart();
            final CoveragesData coveragesData = new CoveragesData(coverageInfo, request);
            coveragesPart.setDataHandler(new DataHandler(coveragesData, "geoserver/coverages11"));
            coveragesPart.setHeader("Content-ID", "<urn:ogc:wcs:1.1:coverages>");
            coveragesPart.setHeader("Content-Type", "text/xml");
            multipart.addBodyPart(coveragesPart);

            // the actual coverage
            BodyPart coveragePart = new MimeBodyPart();
            CoverageEncoder encoder =
                    new CoverageEncoder(delegate, coverage, outputFormat, new HashMap<>());
            coveragePart.setDataHandler(new DataHandler(encoder, "geoserver/coverageDelegate"));
            coveragePart.setHeader("Content-ID", "<theCoverage>");
            coveragePart.setHeader("Content-Type", delegate.getMimeType(outputFormat));
            coveragePart.setHeader("Content-Transfer-Encoding", "base64");
            multipart.addBodyPart(coveragePart);

            // write out the multipart (we need to use mime message trying to
            // encode directly with multipart or BodyPart does not set properly
            // the encodings and binary files gets ruined
            MimeMessage message = new GeoServerMimeMessage();
            message.setContent(multipart);
            message.writeTo(output);
            output.flush();
        } catch (MessagingException e) {
            throw new WcsException("Error occurred while encoding the mime multipart response", e);
        }
    }

    /**
     * A special mime message that does not set any header other than the content type
     *
     * @author Andrea Aime - TOPP
     */
    private static class GeoServerMimeMessage extends MimeMessage {
        public GeoServerMimeMessage() {
            super((Session) null);
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            // it's just ugly to see ...
            removeHeader("Message-ID");
        }
    }
}
