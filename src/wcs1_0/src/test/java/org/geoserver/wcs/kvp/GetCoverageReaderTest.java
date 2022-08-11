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

package org.geoserver.wcs.kvp;

import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.HashMap;
import java.util.Map;
import net.opengis.wcs10.GetCoverageType;
import org.geoserver.wcs.test.WCSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class GetCoverageReaderTest extends WCSTestSupport {

    static Wcs10GetCoverageRequestReader reader;

    @Before
    public void setUp() {
        reader = new Wcs10GetCoverageRequestReader(getCatalog());
    }

    @Test
    public void testMissingParams() throws Exception {
        Map<String, Object> raw = baseMap();

        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("Hey, format is missing, this should have failed");
        } catch (WcsException e) {
            assertEquals("MissingParameterValue", e.getCode());
        }

        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("SourceCoverage", layerId);
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("Hey, format is missing, this should have failed");
        } catch (WcsException e) {
            assertEquals("MissingParameterValue", e.getCode());
        }

        raw.put("format", "image/tiff");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("Hey, boundingBox is missing, this should have failed");
        } catch (WcsException e) {
            assertEquals("MissingParameterValue", e.getCode());
        }

        raw.put("version", "1.0.0");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("crs", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
        } catch (WcsException e) {
            fail("This time all mandatory params where provided?");
            assertEquals("MissingParameterValue", e.getCode());
        }
    }

    private Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WCS");
        raw.put("version", "1.0.0");
        raw.put("request", "GetCoverage");
        return raw;
    }

    @Test
    public void testUnknownCoverageParams() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = "fairyTales:rumpelstilskin";
        raw.put("sourcecoverage", layerId);
        raw.put("format", "SuperCoolFormat");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("crs", "EPSG:4326");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("That coverage is not registered???");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("sourcecoverage", e.getLocator());
        }
    }

    @Test
    public void testBasic() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("CRS", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getSourceCoverage());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat().getValue());
        assertEquals("EPSG:4326", getCoverage.getOutput().getCrs().getValue());
    }

    @Test
    public void testInterpolation() throws Exception {
        Map<String, Object> raw = baseMap();
        String layerId = getLayerId(TASMANIA_BM);
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("CRS", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");
        raw.put("interpolation", "nearest neighbor");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getSourceCoverage());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat().getValue());
        assertEquals("nearest neighbor", getCoverage.getInterpolationMethod().toString());

        // bilinear
        raw = baseMap();
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("CRS", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");
        raw.put("interpolation", "bilinear");

        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getSourceCoverage());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat().getValue());
        assertEquals("bilinear", getCoverage.getInterpolationMethod().toString());

        // nearest
        raw = baseMap();
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("CRS", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");
        raw.put("interpolation", "nearest");

        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getSourceCoverage());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat().getValue());
        assertEquals("nearest neighbor", getCoverage.getInterpolationMethod().toString());

        // bicubic
        raw = baseMap();
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("CRS", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");
        raw.put("interpolation", "bicubic");

        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getSourceCoverage());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat().getValue());
        assertEquals("bicubic", getCoverage.getInterpolationMethod().toString());
    }

    @Test
    public void testUnsupportedCRS() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("CRS", "urn:ogc:def:crs:EPSG:6.6:-1000");
        raw.put("width", "150");
        raw.put("height", "150");

        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals("crs", e.getLocator());
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }
}
