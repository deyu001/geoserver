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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testing Scaling Extension
 *
 * @author Simone Giannecchini, GeoSolution SAS
 */
public class ScalingExtentionTest extends WCSTestSupport {

    private GridCoverage2D sourceCoverage;

    @Before
    public void setup() throws Exception {
        // check we can read it as a TIFF and it is similare to the origina one

        sourceCoverage =
                (GridCoverage2D)
                        this.getCatalog()
                                .getCoverageByName("BlueMarble")
                                .getGridCoverageReader(null, null)
                                .read(null);

        // enable verbose exceptions to get better debugging info
        GeoServer gs = getGeoServer();
        GeoServerInfo gsInfo = gs.getGlobal();
        SettingsInfo settings = gsInfo.getSettings();
        settings.setVerboseExceptions(true);
        gs.save(gsInfo);
    }

    @After
    public void close() {
        try {
            if (sourceCoverage != null) {
                scheduleForCleaning(sourceCoverage);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Test
    public void testScaleAxesByFactorXML() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageScaleAxesByFactor.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(1260, reader.getOriginalGridRange().getSpan(0));
        assertEquals(1260, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }

    @Test
    public void testScaleToSizeXML() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageScaleToSize.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        if ("application/xml".equals(response.getContentType())) {
            LOGGER.warning("Error message: " + response.getContentAsString());
            Runtime runtime = Runtime.getRuntime();
            LOGGER.warning("Max memory: " + runtime.maxMemory());
            LOGGER.warning("Free memory: " + runtime.freeMemory());
            LOGGER.warning("Total memory: " + runtime.totalMemory());
        }
        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(1000, reader.getOriginalGridRange().getSpan(0));
        assertEquals(1000, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }

    @Test
    public void testScaleToExtentXML() throws Exception {
        final File xml = new File("./src/test/resources/requestGetCoverageScaleToExtent.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(200, reader.getOriginalGridRange().getSpan(0));
        assertEquals(300, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }

    @Test
    public void testScaleByFactorXML() throws Exception {

        final File xml = new File("./src/test/resources/requestGetCoverageScaleByFactor.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

        assertEquals("image/tiff", response.getContentType());
        byte[] tiffContents = getBinary(response);
        File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
        FileUtils.writeByteArrayToFile(file, tiffContents);

        // check the tiff structure is the one requested
        final GeoTiffReader reader = new GeoTiffReader(file);
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        reader.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326", true)));
        assertEquals(900, reader.getOriginalGridRange().getSpan(0));
        assertEquals(900, reader.getOriginalGridRange().getSpan(1));
        final GridCoverage2D coverage = reader.read(null);
        assertNotNull(coverage);
        assertEnvelopeEquals(sourceCoverage, coverage);
        reader.dispose();
        scheduleForCleaning(coverage);
    }
}
