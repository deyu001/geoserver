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

package org.geoserver.wcs2_0.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testing range subsetting capabilities
 *
 * @author Simone Giannecchini, GeoSolutions TODO more tests with a landsat
 */
public class RangeSubsetExtentionTest extends WCSTestSupport {

    @Test
    public void testBasic() throws Exception {

        final File xml =
                new File("./src/test/resources/rangesubset/requestGetCoverageRangeSubsetting.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(1, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void mixed() throws Exception {

        final File xml =
                new File(
                        "./src/test/resources/rangesubset/requestGetCoverageRangeSubsettingInterval2.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(5, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void testWrong() throws Exception {

        final File xml =
                new File(
                        "./src/test/resources/rangesubset/requestGetCoverageWrongRangeSubsetting.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.NoSuchField.getExceptionCode(), "Band1");
    }

    @Test
    public void test9to3() throws Exception {

        final File xml =
                new File(
                        "./src/test/resources/rangesubset/requestGetCoverageRangeSubsetting9to3.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("gtiff", "gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:32611", true)));
        assertEquals(68, reader.getOriginalGridRange().getSpan(0));
        assertEquals(56, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(3, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("multiband")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void test9to4() throws Exception {

        final File xml =
                new File(
                        "./src/test/resources/rangesubset/requestGetCoverageRangeSubsetting9to4.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("gtiff", "gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:32611", true)));
        assertEquals(68, reader.getOriginalGridRange().getSpan(0));
        assertEquals(56, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(4, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("multiband")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void test9to7() throws Exception {

        final File xml =
                new File(
                        "./src/test/resources/rangesubset/requestGetCoverageRangeSubsetting9to7.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("gtiff", "gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:32611", true)));
        assertEquals(68, reader.getOriginalGridRange().getSpan(0));
        assertEquals(56, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(7, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("multiband")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }

    @Test
    public void testRange() throws Exception {

        final File xml =
                new File(
                        "./src/test/resources/rangesubset/requestGetCoverageRangeSubsettingInterval.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(360, reader.getOriginalGridRange().getSpan(0));
        assertEquals(360, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertEquals(3, coverage.getSampleDimensions().length);

        GridCoverage2D sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
        scheduleForCleaning(sourceCoverage);
    }
}
