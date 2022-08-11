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
import java.util.Collections;
import java.util.HashMap;
import net.opengis.wcs10.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Response object for the store=true path, that is, one that stores the coverage on disk and
 * returns its path thru the Coverages document
 *
 * @author Andrea Aime - TOPP
 */
public class Wcs10GetCoverageResponse extends Response {
    private static final Hints LENIENT_HINT = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

    // private final static Hints IGNORE_OVERVIEWS = new Hints(
    // Hints.IGNORE_COVERAGE_OVERVIEW, Boolean.TRUE);

    private static final Hints hints = new Hints(new HashMap<>(5));

    static {
        // ///////////////////////////////////////////////////////////////////
        //
        // HINTS
        //
        // ///////////////////////////////////////////////////////////////////
        hints.add(LENIENT_HINT);
        // hints.add(IGNORE_OVERVIEWS);
    }

    Catalog catalog;

    private CoverageResponseDelegateFinder responseFactory;

    public Wcs10GetCoverageResponse(
            Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        super(GridCoverage[].class);
        this.catalog = catalog;
        this.responseFactory = responseFactory;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        if (!(operation.getParameters()[0] instanceof GetCoverageType))
            throw new WcsException(
                    "Cannot handle object of type: " + operation.getParameters()[0].getClass());

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = getCoverage.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);
        return getCoverage.getSourceCoverage() + "." + delegate.getFileExtension(outputFormat);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (!(operation.getParameters()[0] instanceof GetCoverageType))
            throw new WcsException(
                    "Cannot handle object of type: " + operation.getParameters()[0].getClass());

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = getCoverage.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);

        return delegate.getMimeType(outputFormat);
    }

    private CoverageResponseDelegate getResponseDelegate(String outputFormat) {
        CoverageResponseDelegate delegate = responseFactory.encoderFor(outputFormat);
        if (delegate == null) {
            throw new WcsException("Could not find encoder for output format " + outputFormat);
        }
        return delegate;
    }

    @Override
    public boolean canHandle(Operation operation) {
        if (!(operation.getParameters()[0] instanceof GetCoverageType)) return false;

        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = getCoverage.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);

        return delegate.canProduce(outputFormat);
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        GridCoverage[] coverages = (GridCoverage[]) value;

        // grab the delegate for coverage encoding
        GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = request.getOutput().getFormat().getValue();
        CoverageResponseDelegate delegate = getResponseDelegate(outputFormat);

        // grab the coverage info for Coverages document encoding
        final GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        // ImageIOUtilities.visualize(coverage.getRenderedImage());

        // write the coverage
        delegate.encode(coverage, outputFormat, Collections.emptyMap(), output);
        output.flush();
    }
}
