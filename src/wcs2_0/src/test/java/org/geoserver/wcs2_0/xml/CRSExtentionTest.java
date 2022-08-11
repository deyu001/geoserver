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

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** @author Simone Giannecchini, GeoSolutions SAS */
public class CRSExtentionTest extends WCSTestSupport {
    @Test
    public void capabilties() throws Exception {
        final File xml = new File("./src/test/resources/getcapabilities/getCap.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        Document dom = postAsDOM("wcs", request);
        //         print(dom);

        // check the KVP extension 1.0.1
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs'])",
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
    public void reprojectTo3857XML() throws Exception {
        final File xml = new File("./src/test/resources/crs/requestGetCoverageOutputCRS.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {1.6308305401213994E7, -5543147.203861462},
                            new double[] {1.6475284637403902E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 360);

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void testGetCoverageSubsettingCRSFullXML() throws Exception {
        final File xml = new File("./src/test/resources/crs/requestGetCoverageSubsettingCRS.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {1.6308305401213994E7, -5388389.272818998},
                            new double[] {1.636396514661063E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertEquals(gridRange.getSpan(0), 120);
            assertEquals(gridRange.getSpan(1), 120);

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void testGetCoverageSubsettingTrimCRSXML() throws Exception {
        final File xml =
                new File("./src/test/resources/crs/requestGetCoverageSubsettingTrimCRS.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {1.6308305401213994E7, -5543147.203861462},
                            new double[] {1.6475284637403902E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 360);

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    @Test
    public void subsettingNativeCRSReprojectTo3857() throws Exception {
        final File xml =
                new File("./src/test/resources/crs/requestGetCoverageSubsettingTrimCRS2.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        GeoTiffReader readerTarget = new GeoTiffReader(file);
        GridCoverage2D targetCoverage = null;
        try {
            targetCoverage = readerTarget.read(null);
            final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857", true);
            Assert.assertTrue(
                    CRS.equalsIgnoreMetadata(
                            targetCoverage.getCoordinateReferenceSystem(), targetCRS));

            // checks
            final GridEnvelope gridRange = targetCoverage.getGridGeometry().getGridRange();

            final GeneralEnvelope expectedEnvelope =
                    new GeneralEnvelope(
                            new double[] {1.6308305401213994E7, -5543147.203861462},
                            new double[] {1.6475284637403902E7, -5311971.846945147});
            expectedEnvelope.setCoordinateReferenceSystem(targetCRS);

            final double scale = getScale(targetCoverage);
            assertEnvelopeEquals(
                    expectedEnvelope, scale, (GeneralEnvelope) targetCoverage.getEnvelope(), scale);
            assertEquals(gridRange.getSpan(0), 360);
            assertEquals(gridRange.getSpan(1), 360);

        } finally {
            try {
                readerTarget.dispose();
            } catch (Exception e) {
                // TODO: handle exception
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
