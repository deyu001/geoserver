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

package org.vfny.geoserver.crs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.referencing.CRS;
import org.geotools.referencing.factory.epsg.CoordinateOperationFactoryUsingWKT;
import org.junit.AfterClass;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.ConcatenatedOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;

public class OvverideTransformationsTest extends GeoServerSystemTestSupport {

    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static final String SOURCE_CRS = "EPSG:TEST1";
    private static final String TARGET_CRS = "EPSG:TEST2";

    private static final double[] SRC_TEST_POINT = {39.592654167, 3.084896111};
    private static final double[] DST_TEST_POINT = {39.594235744481225, 3.0844689951999427};
    private static String OLD_TMP_VALUE;

    @AfterClass
    public static void clearTemp() {
        if (OLD_TMP_VALUE == null) {
            System.clearProperty(JAVA_IO_TMPDIR);
        } else {
            System.setProperty(JAVA_IO_TMPDIR, OLD_TMP_VALUE);
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        OLD_TMP_VALUE = System.getProperty(JAVA_IO_TMPDIR);
        System.setProperty(JAVA_IO_TMPDIR, new File("./target").getCanonicalPath());

        super.onSetUp(testData);

        // setup the grid file, the definitions and the tx overrides
        new File(testData.getDataDirectoryRoot(), "user_projections").mkdir();
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream("test_epsg.properties"),
                "user_projections/epsg.properties");
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream(
                        "test_epsg_operations.properties"),
                "user_projections/epsg_operations.properties");
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream("stgeorge.las"),
                "user_projections/stgeorge.las");
        testData.copyTo(
                OvverideTransformationsTest.class.getResourceAsStream("stgeorge.los"),
                "user_projections/stgeorge.los");

        CRS.reset("all");
    }

    /** Test method for {@link CoordinateOperationFactoryUsingWKT#createCoordinateOperation}. */
    @Test
    public void testCreateOperationFromCustomCodes() throws Exception {
        // Test CRSs
        CoordinateReferenceSystem source = CRS.decode(SOURCE_CRS);
        CoordinateReferenceSystem target = CRS.decode(TARGET_CRS);
        MathTransform mt = CRS.findMathTransform(source, target, true);

        // Test MathTransform
        double[] p = new double[2];
        mt.transform(SRC_TEST_POINT, 0, p, 0, 1);
        assertEquals(p[0], DST_TEST_POINT[0], 1e-8);
        assertEquals(p[1], DST_TEST_POINT[1], 1e-8);
    }

    /** Test method for {@link CoordinateOperationFactoryUsingWKT#createCoordinateOperation}. */
    @Test
    public void testOverrideEPSGOperation() throws Exception {
        // Test CRSs
        CoordinateReferenceSystem source = CRS.decode("EPSG:4269");
        CoordinateReferenceSystem target = CRS.decode("EPSG:4326");
        MathTransform mt = CRS.findMathTransform(source, target, true);

        // Test MathTransform
        double[] p = new double[2];
        mt.transform(SRC_TEST_POINT, 0, p, 0, 1);
        assertEquals(p[0], DST_TEST_POINT[0], 1e-8);
        assertEquals(p[1], DST_TEST_POINT[1], 1e-8);
    }

    /** Check we are actually using the EPSG database for anything not in override */
    @Test
    public void testFallbackOnEPSGDatabaseStd() throws Exception {
        // Test CRSs
        CoordinateReferenceSystem source = CRS.decode("EPSG:3002");
        CoordinateReferenceSystem target = CRS.decode("EPSG:4326");
        CoordinateOperation co =
                CRS.getCoordinateOperationFactory(true).createOperation(source, target);
        ConcatenatedOperation cco = (ConcatenatedOperation) co;
        // the EPSG one only has two steps, the non EPSG one 4
        assertEquals(2, cco.getOperations().size());
    }

    /** See if we can use the stgeorge grid shift files as the ESPG db would like us to */
    @Test
    public void testNadCon() throws Exception {
        CoordinateReferenceSystem crs4138 = CRS.decode("EPSG:4138");
        CoordinateReferenceSystem crs4326 = CRS.decode("EPSG:4326");
        MathTransform mt = CRS.findMathTransform(crs4138, crs4326);

        assertTrue(mt.toWKT().contains("NADCON"));

        double[] src = new double[] {-169.625, 56.575};
        double[] expected = new double[] {-169.62744, 56.576034};
        double[] p = new double[2];
        mt.transform(src, 0, p, 0, 1);
        assertEquals(expected[0], p[0], 1e-6);
        assertEquals(expected[1], p[1], 1e-6);
    }
}
