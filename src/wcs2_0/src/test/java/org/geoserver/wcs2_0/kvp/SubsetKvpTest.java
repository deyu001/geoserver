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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Testing Scaling Extension KVP
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class SubsetKvpTest extends WCSKVPTestSupport {

    private Logger LOGGER = Logging.getLogger(SubsetKvpTest.class);

    @Test
    public void capabilties() throws Exception {
        Document dom = getAsDOM("wcs?reQueSt=GetCapabilities&seErvIce=WCS");
        //         print(dom);

        // check the KVP extension 1.0.1
        XMLAssert.assertXpathEvaluatesTo(
                "1",
                "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_protocol-binding_get-kvp/1.0.1'])",
                dom);

        // proper case enforcing on values
        dom = getAsDOM("wcs?request=Getcapabilities&service=wCS");
        // print(dom);

        // check that we have the crs extension
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1",
                "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void trim() throws Exception {

        // check we can read it as a TIFF and it is similar to the original one
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        GeoTiffReader readerTarget = null;
        try {

            // source
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();

            // subsample using the original extension
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Long("
                                    + sourceEnvelope.x
                                    + ","
                                    + (sourceEnvelope.x + sourceEnvelope.width / 2)
                                    + ")"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Lat("
                                    + sourceEnvelope.y
                                    + ","
                                    + (sourceEnvelope.y + sourceEnvelope.height / 2)
                                    + ")");

            assertEquals("image/tiff", response.getContentType());
            byte[] tiffContents = getBinary(response);
            File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
            FileUtils.writeByteArrayToFile(file, tiffContents);
            readerTarget = new GeoTiffReader(file);
            targetCoverage = readerTarget.read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEquals(180, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(180, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(1));

            // === request does not intersect
            response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Long("
                                    + (sourceEnvelope.x + 1.1 * sourceEnvelope.width)
                                    + ","
                                    + (sourceEnvelope.x + 1.2 * sourceEnvelope.width)
                                    + ")"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Lat("
                                    + (sourceEnvelope.y + 1.1 * sourceEnvelope.height)
                                    + ","
                                    + (sourceEnvelope.y + 1.2 * sourceEnvelope.height)
                                    + ")");
            assertEquals("application/xml", response.getContentType());
            checkOws20Exception(
                    response, 404, WCS20ExceptionCode.InvalidSubsetting.getExceptionCode(), "");

            // === trim low > high Lat
            response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Long("
                                    + (sourceEnvelope.x)
                                    + ","
                                    + (sourceEnvelope.x + sourceEnvelope.width)
                                    + ")"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Lat("
                                    + (sourceEnvelope.y + sourceEnvelope.height)
                                    + ","
                                    + (sourceEnvelope.y)
                                    + ")");
            assertEquals("application/xml", response.getContentType());
            checkOws20Exception(
                    response,
                    404,
                    WCS20ExceptionCode.InvalidSubsetting.getExceptionCode(),
                    Double.toString((sourceEnvelope.y + sourceEnvelope.height)));
        } finally {
            try {
                if (readerTarget != null) {
                    readerTarget.dispose();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }
    }

    @Test
    public void sliceError() throws Exception {

        // check we can read it as a TIFF and it is similare to the original one
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        GeoTiffReader readerTarget = null;
        try {

            // === slicing on LONG
            // source
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();

            // === error slice point outside coverage
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Long("
                                    + sourceEnvelope.x
                                    + ","
                                    + (sourceEnvelope.x + sourceEnvelope.width)
                                    + ")"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Lat("
                                    + sourceEnvelope.y * 0.9
                                    + ")");
            assertEquals("application/xml", response.getContentType());
            checkOws20Exception(
                    response,
                    404,
                    WCS20ExceptionCode.InvalidSubsetting.getExceptionCode(),
                    Double.toString(sourceEnvelope.y * 0.9));

            // === error slice point outside coverage
            response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Long("
                                    + sourceEnvelope.x * 0.9
                                    + ")"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Lat("
                                    + sourceEnvelope.y
                                    + ","
                                    + (sourceEnvelope.y + sourceEnvelope.height / 2)
                                    + ")");
            assertEquals("application/xml", response.getContentType());
            checkOws20Exception(
                    response,
                    404,
                    WCS20ExceptionCode.InvalidSubsetting.getExceptionCode(),
                    Double.toString(sourceEnvelope.x * 0.9));
        } finally {
            try {
                if (readerTarget != null) {
                    readerTarget.dispose();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }
    }

    @Test
    public void slice() throws Exception {

        // check we can read it as a TIFF and it is similare to the original one
        GridCoverage2D targetCoverage = null, sourceCoverage = null;
        GeoTiffReader readerTarget = null;
        try {

            // === slicing on LONG
            // source
            sourceCoverage =
                    (GridCoverage2D)
                            this.getCatalog()
                                    .getCoverageByName("BlueMarble")
                                    .getGridCoverageReader(null, null)
                                    .read(null);
            final Envelope2D sourceEnvelope = sourceCoverage.getEnvelope2D();

            // subsample using the original extension
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                    + "&coverageId=wcs__BlueMarble&&Format=image/tiff"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Long("
                                    + sourceEnvelope.x
                                    + ")"
                                    + "&subset=http://www.opengis.net/def/axis/OGC/0/Lat("
                                    + sourceEnvelope.y
                                    + ","
                                    + (sourceEnvelope.y + sourceEnvelope.height / 2)
                                    + ")");

            assertEquals("image/tiff", response.getContentType());
            byte[] tiffContents = getBinary(response);
            File file = File.createTempFile("bm_gtiff", "bm_gtiff.tiff", new File("./target"));
            FileUtils.writeByteArrayToFile(file, tiffContents);
            readerTarget = new GeoTiffReader(file);
            targetCoverage = readerTarget.read(null);

            // checks
            assertEquals(
                    sourceCoverage.getCoordinateReferenceSystem(),
                    targetCoverage.getCoordinateReferenceSystem());
            assertEquals(1, targetCoverage.getGridGeometry().getGridRange().getSpan(0));
            assertEquals(180, targetCoverage.getGridGeometry().getGridRange().getSpan(1));
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(0));
            assertEquals(0, targetCoverage.getGridGeometry().getGridRange().getLow(1));

        } finally {
            try {
                if (readerTarget != null) {
                    readerTarget.dispose();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(targetCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
            try {
                scheduleForCleaning(sourceCoverage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }
    }
}
