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
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.ExtensionType;
import net.opengis.wcs20.GetCoverageType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.Response;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geoserver.platform.Operation;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Returns a single coverage encoded in the specified output format (eventually the native one)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20GetCoverageResponse extends Response {

    public static final String COVERAGE_ID_PARAM = "coverageId";

    CoverageResponseDelegateFinder responseFactory;

    public WCS20GetCoverageResponse(CoverageResponseDelegateFinder responseFactory) {
        super(GridCoverage.class);
        this.responseFactory = responseFactory;
    }

    public String getMimeType(Object value, Operation operation) {
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String format = getCoverage.getFormat();
        if (format == null) {
            return "image/tiff";
        } else {
            CoverageResponseDelegate delegate = responseFactory.encoderFor(format);
            if (delegate == null) {
                throw new WCS20Exception(
                        "Unsupported format " + format,
                        OWSExceptionCode.InvalidParameterValue,
                        "format");
            } else {
                return format;
            }
        }
    }

    @Override
    public boolean canHandle(Operation operation) {
        Object firstParam = operation.getParameters()[0];
        if (!(firstParam instanceof GetCoverageType)) {
            // we only handle WCS 2.0 requests
            return false;
        }

        GetCoverageType getCoverage = (GetCoverageType) firstParam;
        // this class only handles encoding the coverage in its output format
        return getCoverage.getMediaType() == null;
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

        String coverageId = getCoverage.getCoverageId();
        if (coverageId != null) {
            encodingParameters.put(COVERAGE_ID_PARAM, coverageId);
        }

        // grab the delegate
        CoverageResponseDelegate delegate = responseFactory.encoderFor(format);
        delegate.encode(coverage, format, encodingParameters, output);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        // grab the format
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String format = getCoverage.getFormat();
        if (format == null) {
            format = "image/tiff";
        }

        // grab the delegate and thus the extension
        CoverageResponseDelegate delegate = responseFactory.encoderFor(format);

        // collect the name of the coverages that have been requested
        return delegate.getFileName((GridCoverage2D) value, getCoverage.getCoverageId(), format);
    }
}
