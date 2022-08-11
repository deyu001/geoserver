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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.ScalingType;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.kvp.WCS20GetCoverageRequestReader;
import org.geoserver.wcs2_0.response.MIMETypeMapper;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

/**
 * Testing WCS 2.0 Core {@link GetCoverage}
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Emanuele Tajariol, GeoSolutions SAS
 */
public class GetCoverageTest extends WCSTestSupport {

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    private static final QName TIMESERIES =
            new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);
    private GridCoverage2DReader coverageReader;
    private AffineTransform2D originalMathTransform;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, getCatalog());
        setupRasterDimension(
                TIMESERIES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
    }

    @Before
    public void getRainReader() throws IOException {
        // get the original transform
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(RAIN));
        coverageReader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        originalMathTransform =
                (AffineTransform2D) coverageReader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
    }

    @Test
    public void testAllowSubsamplingOnScaleFactor() throws Exception {
        // setup a request
        Map<String, Object> raw = setupGetCoverageRain();
        raw.put("scalefactor", "0.5");
        assertScalingByHalf(raw);
    }

    @Test
    public void testAllowSubsamplingOnScaleExtent() throws Exception {
        GridEnvelope range = coverageReader.getOriginalGridRange();
        int width = range.getSpan(0);
        int height = range.getSpan(1);

        // setup a request
        Map<String, Object> raw = setupGetCoverageRain();
        raw.put("scaleextent", "i(0," + (width / 2) + "),j(0," + (height / 2) + ")");
        assertScalingByHalf(raw);
    }

    @Test
    public void getNearestTime() throws Exception {
        // get the original transform
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(TIMESERIES));
        GetCoverageType gc =
                parse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=timeseries&subset=time(\"2019-01-03T00:00:00Z\")");
        coverageReader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        EnvelopeAxesLabelsMapper axesMapper =
                GeoServerExtensions.bean(EnvelopeAxesLabelsMapper.class);
        MIMETypeMapper mimeMapper = GeoServerExtensions.bean(MIMETypeMapper.class);
        GetCoverage getCoverage = new GetCoverage(service, getCatalog(), axesMapper, mimeMapper);
        boolean hasCoverage = true;
        GridCoverage gridCoverage = null;
        try {
            // Requested a missing time will thrown an exception of requested time out of range
            gridCoverage = getCoverage.run(gc);
        } catch (WCS20Exception e) {
            hasCoverage = false;
            assertTrue(e.getMessage().contains("Requested time subset does not intersect"));
            assertEquals(404, (int) e.getHttpCode());
        }
        assertFalse(hasCoverage);

        // Enabling nearestMatch on timeDimension
        setupNearestMatch(TIMESERIES, "time", true, "P5D", true);
        getCoverage = new GetCoverage(service, getCatalog(), axesMapper, mimeMapper);
        try {
            gridCoverage = getCoverage.run(gc);
            hasCoverage = true;
        } catch (WCS20Exception e) {
            hasCoverage = false;
        }
        assertNotNull(gridCoverage);
        assertTrue(hasCoverage);
        scheduleForCleaning(gridCoverage);
    }

    protected GetCoverageType parse(String url) throws Exception {
        Map<String, Object> rawKvp = new CaseInsensitiveMap<>(KvpUtils.parseQueryString(url));
        Map<String, Object> kvp = new CaseInsensitiveMap<>(parseKvp(rawKvp));
        WCS20GetCoverageRequestReader reader = new WCS20GetCoverageRequestReader();
        GetCoverageType gc = (GetCoverageType) reader.createRequest();
        return (GetCoverageType) reader.read(gc, kvp, rawKvp);
    }

    private void assertScalingByHalf(Map<String, Object> raw) throws Exception {
        Map<String, Object> kvp = parseKvp(raw);
        WCS20GetCoverageRequestReader reader = new WCS20GetCoverageRequestReader();
        GetCoverageType getCoverageRequest =
                (GetCoverageType) reader.read(reader.createRequest(), kvp, raw);

        // setup a getcoverage object we can observe
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        EnvelopeAxesLabelsMapper axesMapper =
                GeoServerExtensions.bean(EnvelopeAxesLabelsMapper.class);
        MIMETypeMapper mimeMapper = GeoServerExtensions.bean(MIMETypeMapper.class);
        GetCoverage getCoverage =
                new GetCoverage(service, getCatalog(), axesMapper, mimeMapper) {
                    @Override
                    MathTransform getMathTransform(
                            GridCoverage2DReader reader,
                            Envelope subset,
                            GridCoverageRequest request,
                            PixelInCell pixelInCell,
                            ScalingType scaling)
                            throws IOException {
                        MathTransform mt =
                                super.getMathTransform(
                                        reader, subset, request, pixelInCell, scaling);

                        // check we are giving the reader the expected scaling factor
                        AffineTransform2D actual = (AffineTransform2D) mt;
                        assertEquals(
                                0.5, originalMathTransform.getScaleX() / actual.getScaleX(), 1e-6);
                        assertEquals(
                                0.5, originalMathTransform.getScaleY() / actual.getScaleY(), 1e-6);

                        return mt;
                    }
                };
        GridCoverage result = getCoverage.run(getCoverageRequest);
        scheduleForCleaning(result);
    }

    private Map<String, Object> setupGetCoverageRain() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WCS");
        raw.put("request", "GetCoverage");
        raw.put("version", "2.0.1");
        raw.put("coverageId", "sf__rain");
        raw.put("format", "image/tiff");
        return raw;
    }
}
