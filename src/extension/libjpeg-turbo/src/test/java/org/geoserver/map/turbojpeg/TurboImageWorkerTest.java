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

package org.geoserver.map.turbojpeg;

import it.geosolutions.imageio.plugins.turbojpeg.TurboJpegUtilities;
import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import org.geotools.image.ImageWorker;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testing directly the {@link org.geoserver.map.turbojpeg.TurboJpegImageWorker}.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class TurboImageWorkerTest extends Assert {

    static final String ERROR_LIB_MESSAGE =
            "The TurboJpeg native library hasn't been loaded: Skipping test";

    static boolean SKIP_TESTS = false;

    static final Logger LOGGER = Logger.getLogger(TurboImageWorkerTest.class.toString());

    @BeforeClass
    public static void setup() {
        SKIP_TESTS = !TurboJpegUtilities.isTurboJpegAvailable();
    }

    @Test
    public void errors() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        try {
            new TurboJpegImageWorker(ImageIO.read(input))
                    .writeTurboJPEG(new FileOutputStream(output), 1.5f);
            fail("We should not be allowed to specify compression ratios > 1");
        } catch (Exception e) {
            // TODO: handle exception
        }

        try {
            new TurboJpegImageWorker(ImageIO.read(input))
                    .writeTurboJPEG(new FileOutputStream(output), -.5f);
            fail("We should not be allowed to specify compression ratios > 1");
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Test
    public void writeIndexedWithAlpha() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // Create paletted image
        final byte bb[] = new byte[256];
        for (int i = 0; i < 200; i++) {
            bb[i] = (byte) i;
        }
        int noDataValue = 200;
        NoDataContainer noData = new NoDataContainer(noDataValue);
        final IndexColorModel icm = new IndexColorModel(8, 256, bb, bb, bb);
        final WritableRaster raster =
                RasterFactory.createWritableRaster(icm.createCompatibleSampleModel(512, 512), null);
        for (int i = raster.getMinX(); i < raster.getMinX() + raster.getWidth(); i++) {
            for (int j = raster.getMinY(); j < raster.getMinY() + raster.getHeight(); j++) {
                if (i - raster.getMinX() < raster.getWidth() / 2) {
                    raster.setSample(i, j, 0, (i + j) / 32);
                } else {
                    raster.setSample(i, j, 0, 200);
                }
            }
        }
        // Set no data
        BufferedImage bi = new BufferedImage(icm, raster, false, null);
        PlanarImage planarImage = PlanarImage.wrapRenderedImage(bi);
        planarImage.setProperty(NoDataContainer.GC_NODATA, noData);

        // create output file
        final File output = TestData.temp(this, "outputNoAlpha.jpeg");
        new TurboJpegImageWorker(planarImage).writeTurboJPEG(new FileOutputStream(output), .5f);
        // No exceptions occurred so far.
        assertTrue("Unable to create output file", output.exists() && output.isFile());
    }

    @Test
    public void writer() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergb.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        new TurboJpegImageWorker(ImageIO.read(input))
                .writeTurboJPEG(new FileOutputStream(output), .5f);
        assertTrue("Unable to create output file", output.exists() && output.isFile());

        new ImageWorker(output).getBufferedImage().flush();
    }

    @Test
    public void testWriterBandSelect() throws IOException {
        if (SKIP_TESTS) {
            LOGGER.warning(ERROR_LIB_MESSAGE);
            return;
        }

        // test-data
        final File input = TestData.file(this, "testmergba.png");
        assertTrue("Unable to find test data", input.exists() && input.isFile() && input.canRead());

        // create output file
        final File output = TestData.temp(this, "output.jpeg");
        new TurboJpegImageWorker(ImageIO.read(input))
                .writeTurboJPEG(new FileOutputStream(output), .5f);
        assertTrue("Unable to create output file", output.exists() && output.isFile());

        new ImageWorker(output).getBufferedImage().flush();
    }
}
