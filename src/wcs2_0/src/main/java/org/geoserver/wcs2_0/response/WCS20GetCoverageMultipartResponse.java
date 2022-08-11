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

package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.ExtensionType;
import net.opengis.wcs20.GetCoverageType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wcs.responses.CoverageEncoder;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.response.GMLCovHandler.CoverageData;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Returns a single coverage encoded in the specified output format (eventually the native one)
 * along with the XML describing the coverage, in a MIME multipart package
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20GetCoverageMultipartResponse extends Response {

    CoverageResponseDelegateFinder responseFactory;

    EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    public WCS20GetCoverageMultipartResponse(
            CoverageResponseDelegateFinder responseFactory,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        super(GridCoverage.class);
        this.responseFactory = responseFactory;
        this.envelopeDimensionsMapper = envelopeDimensionsMapper;
    }

    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    public String getMimeType(Object value, Operation operation) {
        return "multipart/related";
    }

    @Override
    public boolean canHandle(Operation operation) {
        Object firstParam = operation.getParameters()[0];
        if (!(firstParam instanceof GetCoverageType)) {
            // we only handle WCS 2.0 requests
            return false;
        }

        GetCoverageType getCoverage = (GetCoverageType) firstParam;

        // this class only handles encoding the coverage with mediatype
        String mediaType = getCoverage.getMediaType();
        return mediaType != null && mediaType.equals("multipart/related");
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        // grab the coverage
        GridCoverage2D coverage = (GridCoverage2D) value;

        // grab the format
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String format = getCoverage.getFormat();
        if (format == null) {
            format = "image/tiff";
        }

        // extract additional extensions
        final Map<String, String> encodingParameters = new HashMap<>();
        final ExtensionType extension = getCoverage.getExtension();
        if (extension != null) {
            final EList<ExtensionItemType> extensions = extension.getContents();
            for (ExtensionItemType ext : extensions) {
                encodingParameters.put(ext.getName(), ext.getSimpleContent());
            }
        }

        // grab the delegate
        CoverageResponseDelegate delegate = responseFactory.encoderFor(format);

        // use javamail classes to actually encode the document
        try {
            MimeMultipart multipart = new MimeMultipart();
            multipart.setSubType("related");

            String fileName =
                    "/coverages/"
                            + getCoverage.getCoverageId()
                            + "."
                            + delegate.getFileExtension(format);

            // coverages xml structure, which is very close to the DescribeFeatureType output
            BodyPart coveragesPart = new MimeBodyPart();
            FileReference reference =
                    new FileReference(
                            fileName,
                            delegate.getMimeType(format),
                            delegate.getConformanceClass(format));
            final CoverageData coveragesData =
                    new CoverageData(coverage, reference, envelopeDimensionsMapper);
            coveragesPart.setDataHandler(new DataHandler(coveragesData, "geoserver/coverages20"));
            coveragesPart.setHeader("Content-ID", "wcs");
            coveragesPart.setHeader("Content-Type", "application/gml+xml");
            multipart.addBodyPart(coveragesPart);

            // the actual coverage
            BodyPart coveragePart = new MimeBodyPart();
            CoverageEncoder encoder =
                    new CoverageEncoder(delegate, coverage, format, encodingParameters);
            coveragePart.setDataHandler(new DataHandler(encoder, "geoserver/coverageDelegate"));
            coveragePart.setHeader("Content-ID", fileName);
            coveragePart.setHeader("Content-Type", delegate.getMimeType(format));
            coveragePart.setHeader("Content-Transfer-Encoding", "binary");
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

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        // the only thing that can open this format normally available on a desktop is a e-mail
        // client
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        return getCoverage.getCoverageId() + ".eml";
    }

    /**
     * A special mime message that does not set any header other than the content type
     *
     * @author Andrea Aime - GeoSolutions
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
