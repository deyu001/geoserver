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

package org.geoserver.wps.ppio;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.ppio.CoveragePPIO.JPEGPPIO;
import org.geoserver.wps.ppio.CoveragePPIO.PNGPPIO;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.image.util.ImageUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Testing CoveragePPIOs is able to encode PNG and JPEG formats. */
public class CoveragePPIOTest {

    File geotiff = new File("./target/testInput.tiff");

    File targetPng = new File("./target/output.png");

    File targetJpeg = new File("./target/output.jpeg");

    GeoTiffReader reader;

    GridCoverage2D coverage;

    @Before
    public void prepareGeoTiff() throws IOException {
        try (InputStream is = SystemTestData.class.getResourceAsStream("tazbm.tiff")) {
            FileUtils.copyInputStreamToFile(is, geotiff);
        }
        reader = new GeoTiffReader(geotiff);
    }

    @After
    public void cleanup() {
        if (coverage != null) {
            ImageUtilities.disposeImage(coverage.getRenderedImage());
        }
        if (reader != null) {
            reader.dispose();
        }
    }

    private GridCoverage2D getCoverage() throws IOException {
        coverage = reader.read(null);
        return new GridCoverageFactory()
                .create(
                        coverage.getName(),
                        coverage.getRenderedImage(),
                        coverage.getEnvelope(),
                        coverage.getSampleDimensions(),
                        null,
                        null);
    }

    @Test
    public void testPNGEncode() throws Exception {
        GridCoverage2D coverage = getCoverage();
        PNGPPIO ppio = new PNGPPIO();
        testIsFormat(coverage, ppio, targetPng, "PNG");
    }

    @Test
    public void testJPEGEncode() throws Exception {
        GridCoverage2D coverage = getCoverage();
        JPEGPPIO ppio = new JPEGPPIO();
        testIsFormat(coverage, ppio, targetJpeg, "JPEG");
    }

    @Test
    public void testEncodeQuality() throws Exception {
        GridCoverage2D coverage = getCoverage();
        JPEGPPIO ppio = new JPEGPPIO();
        Map<String, Object> encodingParams = new HashMap<>();

        File highQualityFile = new File("./target/outputHiQ.jpg");
        encodingParams.put(CoveragePPIO.QUALITY_KEY, "0.99");
        try (FileOutputStream fos = new FileOutputStream(highQualityFile)) {
            ppio.encode(coverage, encodingParams, fos);
        }
        final long highQualityFileSize = highQualityFile.length();

        File lowQualityFile = new File("./target/outputLoQ.jpg");
        encodingParams.put(CoveragePPIO.QUALITY_KEY, "0.01");
        try (FileOutputStream fos = new FileOutputStream(lowQualityFile)) {
            ppio.encode(coverage, encodingParams, fos);
        }
        final long lowQualityFileSize = lowQualityFile.length();
        assertTrue(highQualityFileSize > lowQualityFileSize);
    }

    private void testIsFormat(
            GridCoverage2D coverage, CoveragePPIO ppio, File encodedFile, String formatName)
            throws Exception {
        try (FileOutputStream fos = new FileOutputStream(encodedFile)) {
            ppio.encode(coverage, fos);
        }
        try (FileImageInputStream fis = new FileImageInputStream(encodedFile)) {
            ImageReader imageReader = null;
            try {
                imageReader = ImageIO.getImageReaders(fis).next();
                imageReader.setInput(fis);
                assertTrue(formatName.equalsIgnoreCase(imageReader.getFormatName()));
                assertNotNull(imageReader.read(0));
            } finally {
                if (imageReader != null) {
                    try {
                        imageReader.dispose();
                    } catch (Throwable t) {
                        // Ignore it.
                    }
                }
            }
        }
    }
}
