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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.GetCoverageType;
import net.opengis.wcs11.RangeSubsetType;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs.test.WCSTestSupport;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class GetCoverageReaderTest extends WCSTestSupport {

    static GetCoverageRequestReader reader;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = (Catalog) applicationContext.getBean("catalog");
        reader = new GetCoverageRequestReader(catalog);
    }

    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WCS");
        raw.put("version", "1.1.1");
        raw.put("request", "GetCoverage");

        return raw;
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
        raw.put("identifier", layerId);
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

        raw.put("BoundingBox", "-45,146,-42,147");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
        } catch (WcsException e) {
            fail("This time all mandatory params where provided?");
            assertEquals("MissingParameterValue", e.getCode());
        }
    }

    @Test
    public void testUnknownCoverageParams() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = "fairyTales:rumpelstilskin";
        raw.put("identifier", layerId);
        raw.put("format", "SuperCoolFormat");
        raw.put("BoundingBox", "-45,146,-42,147");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("That coverage is not registered???");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("identifier", e.getLocator());
        }
    }

    @Test
    public void testBasic() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");
        raw.put("store", "false");
        raw.put("GridBaseCRS", "urn:ogc:def:crs:EPSG:6.6:4326");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getIdentifier().getValue());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat());
        assertFalse(getCoverage.getOutput().isStore());
        assertEquals(
                "urn:ogc:def:crs:EPSG:6.6:4326",
                getCoverage.getOutput().getGridCRS().getGridBaseCRS());
    }

    @Test
    public void testUnsupportedCRS() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("GridBaseCRS", "urn:ogc:def:crs:EPSG:6.6:-1000");

        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals("GridBaseCRS", e.getLocator());
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testGridTypes() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");

        raw.put("gridType", GridType.GT2dGridIn2dCrs.getXmlConstant());
        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridType.GT2dGridIn2dCrs.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridType());

        raw.put("gridType", GridType.GT2dSimpleGrid.getXmlConstant());
        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridType.GT2dSimpleGrid.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridType());

        // try with different case
        raw.put("gridType", GridType.GT2dSimpleGrid.getXmlConstant().toUpperCase());
        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridType.GT2dSimpleGrid.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridType());

        raw.put("gridType", GridType.GT2dGridIn3dCrs.getXmlConstant());
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridType", e.getLocator());
        }

        raw.put("gridType", "Hoolabaloola");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridType", e.getLocator());
        }
    }

    @Test
    public void testGridCS() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");

        raw.put("GridCS", GridCS.GCSGrid2dSquare.getXmlConstant());
        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridCS.GCSGrid2dSquare.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridCS());

        raw.put("GridCS", GridCS.GCSGrid2dSquare.getXmlConstant().toUpperCase());
        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridCS.GCSGrid2dSquare.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridCS());

        raw.put("GridCS", "Hoolabaloola");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridCS", e.getLocator());
        }
    }

    @Test
    public void testGridOrigin() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");
        raw.put("GridOrigin", "10.5,-30.2");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        Double[] origin = (Double[]) getCoverage.getOutput().getGridCRS().getGridOrigin();
        assertEquals(2, origin.length);
        assertEquals(0, Double.compare(10.5, origin[0]));
        assertEquals(0, Double.compare(-30.2, origin[1]));

        raw.put("GridOrigin", "12");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOrigin", e.getLocator());
        }

        raw.put("GridOrigin", "12,a");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOrigin", e.getLocator());
        }
    }

    @Test
    public void testGridOffsets() throws Exception {
        Map<String, Object> raw = baseMap();

        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");

        raw.put("GridOffsets", "10.5,-30.2");
        raw.put("GridType", GridType.GT2dSimpleGrid.getXmlConstant());
        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        Double[] offsets = (Double[]) getCoverage.getOutput().getGridCRS().getGridOffsets();
        assertEquals(2, offsets.length);
        assertEquals(0, Double.compare(10.5, offsets[0]));
        assertEquals(0, Double.compare(-30.2, offsets[1]));

        raw.put("GridOffsets", "12");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOffsets", e.getLocator());
        }

        raw.put("GridOffsets", "12,a");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOffsets", e.getLocator());
        }
    }

    /** Tests Bicubic (also called cubic) interpolation with a RangeSubset. */
    @Test
    public void testInterpolationBicubic() throws Exception {
        this.testRangeSubset("cubic");
    }

    /** Tests Bilinear (also called linear) interpolation with a RangeSubset. */
    @Test
    public void testInterpolationBilinear() throws Exception {
        this.testRangeSubset("linear");
    }

    /** Tests Nearest neighbor (also called nearest) interpolation with a RangeSubset. */
    @Test
    public void testInterpolationNearest() throws Exception {
        this.testRangeSubset("nearest");
    }

    protected Map<String, Object> parseKvp(Map<String, Object> raw) throws Exception {
        // parse like the dispatcher but make sure we don't change the original map
        Map<String, Object> input = new HashMap<>(raw);
        List<Throwable> errors = KvpUtils.parse(input);
        if (errors != null && !errors.isEmpty()) throw (Exception) errors.get(0);

        return caseInsensitiveKvp(input);
    }

    protected <V> Map<String, V> caseInsensitiveKvp(Map<String, V> input) {
        // make it case insensitive like the servlet+dispatcher maps
        Map<String, V> result = new HashMap<>();
        for (String key : input.keySet()) {
            result.put(key.toUpperCase(), input.get(key));
        }
        return new CaseInsensitiveMap<>(result);
    }

    /**
     * Tests valid range subset expressions, but with a mix of valid and invalid identifiers.
     *
     * @param interpolation The used interpolation method.
     */
    private void testRangeSubset(String interpolation) throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);

        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");
        raw.put("rangeSubset", "BlueMarble:" + interpolation + "[Bands[Red_band]]");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        RangeSubsetType rs = getCoverage.getRangeSubset();
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        AxisSubsetType axis = (AxisSubsetType) field.getAxisSubset().get(0);
        List keys = axis.getKey();

        assertNotNull(rs);
        assertEquals(1, rs.getFieldSubset().size());

        assertEquals("BlueMarble", field.getIdentifier().getValue());
        assertEquals(1, field.getAxisSubset().size());

        assertEquals("Bands", axis.getIdentifier());

        assertEquals(1, keys.size());
        assertEquals("Red_band", keys.get(0));

        assertEquals(field.getInterpolationType(), interpolation);
    }
}
