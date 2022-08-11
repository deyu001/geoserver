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

package org.geoserver.wcs2_0.eo;

import java.io.IOException;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Plugs into the GetCoverage request cycle and transforms a request for a single EO granule to one
 * against the coverage, but with the filter to limit it to the specified granule
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GetCoverageEOCallback extends AbstractDispatcherCallback {

    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory2();

    private EOCoverageResourceCodec codec;

    public GetCoverageEOCallback(EOCoverageResourceCodec codec) {
        this.codec = codec;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Object[] parameters = operation.getParameters();
        if (parameters != null
                && parameters.length > 0
                && parameters[0] instanceof GetCoverageType) {
            // check we are going against a granule
            GetCoverageType gc = (GetCoverageType) parameters[0];
            String coverageId = gc.getCoverageId();
            if (coverageId == null) {
                throw new WCS20Exception(
                        "Required parameter coverageId is missing",
                        WCS20Exception.WCS20ExceptionCode.MissingParameterValue,
                        "coverageId");
            }
            CoverageInfo coverage = codec.getGranuleCoverage(coverageId);
            if (coverage != null) {
                // set the actual coverage name
                String actualCoverageId = NCNameResourceCodec.encode(coverage);
                gc.setCoverageId(actualCoverageId);

                // extract the granule filter
                Filter granuleFilter = codec.getGranuleFilter(coverageId);

                // check the filter actually matches one granule
                if (!readerHasGranule(coverage, granuleFilter)) {
                    throw new WCS20Exception(
                            "Could not locate coverage " + coverageId,
                            WCS20ExceptionCode.NoSuchCoverage,
                            "coverageId");
                }

                // set and/or merge with the previous filter
                Filter previous = gc.getFilter();
                if (previous == null || previous == Filter.INCLUDE) {
                    gc.setFilter(granuleFilter);
                } else {
                    gc.setFilter(FF.and(previous, granuleFilter));
                }
            }
        }

        return operation;
    }

    private boolean readerHasGranule(CoverageInfo ci, Filter granuleFilter) {
        try {
            StructuredGridCoverage2DReader reader =
                    (StructuredGridCoverage2DReader) ci.getGridCoverageReader(null, null);
            String coverageName = codec.getCoverageName(ci);
            GranuleSource source = reader.getGranules(coverageName, true);
            return source.getCount(new Query(coverageName, granuleFilter)) > 0;
        } catch (IOException e) {
            throw new WCS20Exception(
                    "Could not determine if the coverage has the specified granule", e);
        }
    }
}
