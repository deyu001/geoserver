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

package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Testing Scaling Extension KVP
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class RangeSubsetKvpTest extends WCSKVPTestSupport {

    @Test
    public void capabilties() throws Exception {
        Document dom = getAsDOM("wcs?reQueSt=GetCapabilities&seErvIce=WCS");
        // print(dom);

        // check the KVP extension 1.0.1
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/record-subsetting'])",
                dom);

        // proper case enforcing on values
        dom = getAsDOM("wcs?request=Getcapabilities&service=wCS");
        // print(dom);

        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])",
                dom);
        assertXpathEvaluatesTo(
                "1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void test9to3() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__multiband&&Format=image/tiff&RANGESUBSET=Band1,Band6,Band7");

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

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__multiband&&Format=image/tiff&RANGESUBSET=Band1,Band6,Band9,Band4");

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

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__multiband&&Format=image/tiff&RANGESUBSET=Band1,Band6,Band4,Band9,Band8,Band7,Band2");

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
    public void testBasic() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=RED_BAND");

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
    public void testRange() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=RED_BAND:BLUE_BAND");

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

    @Test
    public void mixed() throws Exception {

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=RED_BAND:BLUE_BAND,RED_BAND,GREEN_BAND");

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

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&&Format=image/tiff&RANGESUBSET=Band1,GREEN_BAND");

        assertEquals("application/xml", response.getContentType());
        checkOws20Exception(
                response, 404, WCS20ExceptionCode.NoSuchField.getExceptionCode(), "Band1");
    }
}
