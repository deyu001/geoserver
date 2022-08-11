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

package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import javax.media.jai.RasterFactory;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

/** @author ETj <etj at geo-solutions.it> */
public class Coverage2RenderedImageAdapterTest extends WPSTestSupport {

    protected static final double NODATA = 3.0d;

    public Coverage2RenderedImageAdapterTest() {}

    /**
     * creates a coverage for testing purposes-
     *
     * @param width width in pixel of the raster
     * @param height height in pixel of the raster
     * @param envX0 envelope minx
     * @param envY0 envelope miny
     * @param envWidth envelope width
     * @param envHeight envelope height
     * @return the test coverage
     */
    protected static GridCoverage2D createTestCoverage(
            final int width,
            final int height,
            final double envX0,
            final double envY0,
            final double envWidth,
            final double envHeight) {

        final GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        WritableRaster raster =
                RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < 50 && y < 50) { // upper left square: vertical lines
                    if (x % 5 == 0) raster.setSample(x, y, 0, 0);
                    else raster.setSample(x, y, 0, width);
                } else if (x < 50 && y > height - 50) { // lower left square: horizontal lines
                    if (y % 5 == 0) raster.setSample(x, y, 0, 0);
                    else raster.setSample(x, y, 0, width);
                } else if (x > width - 50
                        && y < 50) { // upper right square: descending diagonal lines
                    if ((x - y) % 5 == 0) raster.setSample(x, y, 0, 0);
                    else raster.setSample(x, y, 0, width);
                } else if (x > width - 50
                        && y > height - 50) { // lower right square: ascending diagonal lines
                    if ((x + y) % 5 == 0) raster.setSample(x, y, 0, 0);
                    else raster.setSample(x, y, 0, width);
                } else if (x % 50 == 0 || y % 50 == 0 || (x - y) % 100 == 0)
                    raster.setSample(x, y, 0, 0); // bigger lines
                else raster.setSample(x, y, 0, x + y); // normal background
            }
        }
        final Color[] colors =
                new Color[] {Color.BLUE, Color.CYAN, Color.WHITE, Color.YELLOW, Color.RED};

        return factory.create(
                "Float coverage",
                raster,
                new Envelope2D(DefaultGeographicCRS.WGS84, envX0, envY0, envWidth, envHeight),
                null,
                null,
                null,
                new Color[][] {colors},
                null);
    }

    @Test
    public void testSame() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500, 500, 0, 0, 10, 10);
        GridCoverage2D dst = createTestCoverage(500, 500, 0, 0, 10, 10);

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(src, dst, NODATA);

        // --- internal points should stay the same
        Point2D psrc = new Point2D.Double(2d, 3d); // this is on dst gc
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        assertEquals(2d, pdst.getX(), 0d);
        assertEquals(3d, pdst.getY(), 0d);

        // --- external points should not be remapped
        psrc = new Point2D.Double(600d, 600d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst);

        //        view(cria, dst.getGridGeometry(), src.getSampleDimensions());

        //        Viewer.show(src);
        //        Thread.sleep(15000);
    }

    @Test
    public void testSameWorldSmallerDstRaster() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500, 500, 0, 0, 10, 10);
        GridCoverage2D dst = createTestCoverage(250, 250, 0, 0, 10, 10);

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(dst, src, NODATA);

        // --- internal points should double coords (no interp on coords)
        Point2D psrc = new Point2D.Double(13d, 16d); // this is on dst gc
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        assertNotNull("Can't convert " + psrc, pdst);
        assertEquals(26d, pdst.getX(), 0d);
        assertEquals(32d, pdst.getY(), 0d);

        // --- external points should not be remapped
        psrc = new Point2D.Double(600d, 600d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst);

        //        new Viewer(getName(), cria);
        //        Thread.sleep(15000);
    }

    /** Same raster dimension, subset word area */
    @Test
    public void testSameRasterSmallerWorld() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500, 500, 0, 0, 10, 10);
        GridCoverage2D dst = createTestCoverage(500, 500, 0, 0, 5, 5);

        //        double nodata[] = src.getSampleDimension(0).getNoDataValues();

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(dst, src, NODATA);

        // --- internal points should halves coords (no interp on coords)
        Point2D psrc = new Point2D.Double(0d, 0d);
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        // System.out.println(pdst);
        assertEquals(0d, pdst.getX(), 0d);
        assertEquals(250d, pdst.getY(), 0d);

        psrc = new Point2D.Double(20d, 30d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertEquals(10d, pdst.getX(), 0d);
        assertEquals(250d + 15d, pdst.getY(), 0d);
        // System.out.println(pdst);

        //        new Viewer(getName(), cria);
        //        Thread.sleep(15000);
    }

    /** Same raster dimension, subset word area */
    @Test
    public void testSameRasterTranslatedWorld0() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500, 500, 0, 0, 5, 5);
        GridCoverage2D dst = createTestCoverage(500, 500, 2, 2, 5, 5);

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(dst, src, NODATA);

        // --- internal points should halves coords (no interp on coords)
        Point2D psrc = new Point2D.Double(0d, 499d); // this is on dst gc
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        assertNotNull(pdst);
        assertEquals(200d, pdst.getX(), 0d);
        assertEquals(299d, pdst.getY(), 0d);

        // --- points not inside dest but inside src shoud be remapped on a novalue cell
        psrc = new Point2D.Double(0d, 0d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst); // should not map on src raster

        double val = cria.getData().getSampleFloat(0, 0, 0);
        assertEquals("Value should be noData", NODATA, val, 0d);

        //        new Viewer(getName(), cria);
        //        Thread.sleep(20000);
    }
}
