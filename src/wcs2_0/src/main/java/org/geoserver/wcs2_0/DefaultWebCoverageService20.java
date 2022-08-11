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

package org.geoserver.wcs2_0;

import static org.geoserver.wcs2_0.util.RequestUtils.checkService;
import static org.geoserver.wcs2_0.util.RequestUtils.checkVersion;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.opengis.wcs20.DescribeCoverageType;
import net.opengis.wcs20.DescribeEOCoverageSetType;
import net.opengis.wcs20.GetCapabilitiesType;
import net.opengis.wcs20.GetCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.MIMETypeMapper;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.StringUtils;
import org.geoserver.wcs2_0.util.WCS20DescribeCoverageExtension;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Default implementation of the Web Coverage Service 2.0
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class DefaultWebCoverageService20 implements WebCoverageService20 {

    protected Logger LOGGER = Logging.getLogger(DefaultWebCoverageService20.class);

    private MIMETypeMapper mimeMapper;

    private Catalog catalog;

    private GeoServer geoServer;

    private CoverageResponseDelegateFinder responseFactory;

    /** Utility class to map envelope dimension */
    private EnvelopeAxesLabelsMapper envelopeAxesMapper;

    /** Available extension points for the DescribeCoverage operation */
    private List<WCS20DescribeCoverageExtension> wcsDescribeCoverageExtensions;

    /**
     * Boolean indicating that at least an extension point for the DescribeCoverage operation is
     * available
     */
    private boolean availableDescribeCovExtensions;

    public DefaultWebCoverageService20(
            GeoServer geoServer,
            CoverageResponseDelegateFinder responseFactory,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper,
            MIMETypeMapper mimemappe) {
        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
        this.responseFactory = responseFactory;
        this.envelopeAxesMapper = envelopeDimensionsMapper;
        this.mimeMapper = mimemappe;
        this.wcsDescribeCoverageExtensions =
                GeoServerExtensions.extensions(WCS20DescribeCoverageExtension.class);
        this.availableDescribeCovExtensions =
                wcsDescribeCoverageExtensions != null && !wcsDescribeCoverageExtensions.isEmpty();
    }

    @Override
    public WCSInfo getServiceInfo() {
        return geoServer.getService(WCSInfo.class);
    }

    @Override
    public TransformerBase getCapabilities(GetCapabilitiesType request) {
        checkService(request.getService());

        return new GetCapabilities(getServiceInfo(), responseFactory).run(request);
    }

    @Override
    public WCS20DescribeCoverageTransformer describeCoverage(DescribeCoverageType request) {
        checkService(request.getService());
        checkVersion(request.getVersion());

        if (request.getCoverageId() == null || request.getCoverageId().isEmpty()) {
            throw new OWS20Exception(
                    "Required parameter coverageId missing",
                    WCS20Exception.WCS20ExceptionCode.EmptyCoverageIdList,
                    "coverageId");
        }

        // check coverages are legit
        List<String> badCoverageIds = new ArrayList<>();

        for (String encodedCoverageId : request.getCoverageId()) {
            String newCoverageID = encodedCoverageId;
            // Extension point for encoding the coverageId
            if (availableDescribeCovExtensions) {
                for (WCS20DescribeCoverageExtension ext : wcsDescribeCoverageExtensions) {
                    newCoverageID = ext.handleCoverageId(newCoverageID);
                }
            }
            LayerInfo layer = NCNameResourceCodec.getCoverage(catalog, newCoverageID);
            if (layer == null) {
                badCoverageIds.add(encodedCoverageId);
            }
        }
        if (!badCoverageIds.isEmpty()) {
            String mergedIds = StringUtils.merge(badCoverageIds);
            throw new WCS20Exception(
                    "Could not find the requested coverage(s): " + mergedIds,
                    WCS20Exception.WCS20ExceptionCode.NoSuchCoverage,
                    "coverageId");
        }

        WCSInfo wcs = getServiceInfo();

        WCS20DescribeCoverageTransformer describeTransformer =
                new WCS20DescribeCoverageTransformer(catalog, envelopeAxesMapper, mimeMapper);
        describeTransformer.setEncoding(
                Charset.forName(wcs.getGeoServer().getSettings().getCharset()));
        return describeTransformer;
    }

    @Override
    public GridCoverage getCoverage(GetCoverageType request) {
        checkService(request.getService());
        checkVersion(request.getVersion());

        if (request.getCoverageId() == null || "".equals(request.getCoverageId())) {
            throw new OWS20Exception(
                    "Required parameter coverageId missing",
                    WCS20Exception.WCS20ExceptionCode.EmptyCoverageIdList,
                    "coverageId");
        }

        return new GetCoverage(getServiceInfo(), catalog, envelopeAxesMapper, mimeMapper)
                .run(request);
    }

    @Override
    public TransformerBase describeEOCoverageSet(DescribeEOCoverageSetType request) {
        throw new ServiceException(
                "WCS-EO extension is not installed, thus the operation is not available");
    }
}
