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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.util.NullProgressListener;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.process.raster.ContourProcess;
import org.geotools.util.factory.GeoTools;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.Envelope;

/**
 * Test class for the contour process.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ContourProcessTest extends BaseRasterToVectorTest {

    /**
     * Test basic capabilities for the contour process. It works on the DEM tiff and produces a
     * shapefile. Nothing more nothing less.
     */
    @Test
    public void testProcessStandaloneBasicValues() throws Exception {
        GridCoverage2D gc = extractCoverageSubset();

        // extract just two isolines
        final double levels[] = new double[2];
        levels[0] = 1500;
        levels[1] = 1700;
        final ContourProcess process = new ContourProcess();
        final SimpleFeatureCollection fc =
                process.execute(
                        gc, 0, levels, null, false, false, null, new NullProgressListener());

        assertNotNull(fc);
        assertTrue(fc.size() > 0);

        try (SimpleFeatureIterator fi = fc.features()) {
            while (fi.hasNext()) {
                SimpleFeature sf = fi.next();
                Double value = (Double) sf.getAttribute("value");
                assertTrue(value == 1500.0 || value == 1700.0);
            }
        }
    }

    private GridCoverage2D extractCoverageSubset() throws IOException {
        // get the coverage
        CoverageInfo dem = getCatalog().getCoverageByName(DEM.getLocalPart());
        GridCoverage2D gc = (GridCoverage2D) dem.getGridCoverage(null, GeoTools.getDefaultHints());

        // extract only a small part of it
        Envelope fullEnvelope = gc.getEnvelope();
        GeneralEnvelope subset = new GeneralEnvelope(fullEnvelope.getCoordinateReferenceSystem());
        double minX = fullEnvelope.getMinimum(0);
        double minY = fullEnvelope.getMinimum(1);
        double offsetX = fullEnvelope.getSpan(0) / 5;
        double offsetY = fullEnvelope.getSpan(1) / 5;
        subset.setEnvelope(minX + offsetX, minY + offsetY, minX + offsetX * 2, minY + offsetY * 2);
        gc = (GridCoverage2D) new Operations(null).crop(gc, subset);

        scheduleForDisposal(gc);

        return gc;
    }

    /**
     * Test basic capabilities for the contour process. It works on the DEM tiff and produces a
     * shapefile. Nothing more nothing less.
     */
    @Test
    public void testProcessStandaloneBasicInterval() throws Exception {
        final GridCoverage2D gc = extractCoverageSubset();

        final double step = 100;
        final ContourProcess process = new ContourProcess();
        final SimpleFeatureCollection fc =
                process.execute(
                        gc,
                        0,
                        null,
                        Double.valueOf(step),
                        false,
                        false,
                        null,
                        new NullProgressListener());

        assertNotNull(fc);
        assertTrue(fc.size() > 0);

        try (SimpleFeatureIterator fi = fc.features()) {
            while (fi.hasNext()) {
                SimpleFeature sf = fi.next();
                Double value = (Double) sf.getAttribute("value");
                assertTrue(value > 0);
            }
        }
    }
}
