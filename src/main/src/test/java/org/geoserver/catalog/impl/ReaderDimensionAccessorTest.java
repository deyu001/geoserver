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

package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.junit.Test;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;

public class ReaderDimensionAccessorTest {

    static class MockDimensionReader extends AbstractGridCoverage2DReader {

        Map<String, String> metadata = new HashMap<>();

        @Override
        public Format getFormat() {

            return null;
        }

        @Override
        public GridCoverage2D read(GeneralParameterValue[] parameters)
                throws IllegalArgumentException, IOException {
            return null;
        }

        @Override
        public String[] getMetadataNames() {
            Set<String> keys = metadata.keySet();
            return keys.toArray(new String[keys.size()]);
        }

        @Override
        public String getMetadataValue(String coverageName, String name) {
            return super.getMetadataValue(name);
        }

        @Override
        public String getMetadataValue(String name) {
            return metadata.get(name);
        }
    };

    @Test
    public void testMixedTimeExtraction() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put(GridCoverage2DReader.HAS_TIME_DOMAIN, "true");
        reader.metadata.put(
                GridCoverage2DReader.TIME_DOMAIN,
                "2016-02-23T03:00:00.000Z/2016-02-23T03:00:00.000Z/PT1S,2016-02-23T06:00:00.000Z,2016-02-23T09:00:00.000Z/2016-02-23T12:00:00.000Z/PT1S");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        TreeSet<Object> domain = accessor.getTimeDomain();
        assertEquals(3, domain.size());
        Iterator<Object> it = domain.iterator();
        Date firstEntry = (Date) it.next();
        assertEquals(accessor.getTimeFormat().parse("2016-02-23T03:00:00.000Z"), firstEntry);
        Date secondEntry = (Date) it.next();
        assertEquals(accessor.getTimeFormat().parse("2016-02-23T06:00:00.000Z"), secondEntry);
        DateRange thirdEntry = (DateRange) it.next();
        assertEquals(
                accessor.getTimeFormat().parse("2016-02-23T09:00:00.000Z"),
                thirdEntry.getMinValue());
        assertEquals(
                accessor.getTimeFormat().parse("2016-02-23T12:00:00.000Z"),
                thirdEntry.getMaxValue());
    }

    @Test
    public void testMixedElevationExtraction() throws IOException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put(GridCoverage2DReader.HAS_ELEVATION_DOMAIN, "true");
        reader.metadata.put(GridCoverage2DReader.ELEVATION_DOMAIN, "0/0/0,10,15/20/1");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        TreeSet<Object> domain = accessor.getElevationDomain();
        assertEquals(3, domain.size());
        Iterator<Object> it = domain.iterator();
        Number firstEntry = (Number) it.next();
        assertEquals(0, firstEntry.doubleValue(), 0d);
        Number secondEntry = (Number) it.next();
        assertEquals(10, secondEntry.doubleValue(), 0d);
        NumberRange thirdEntry = (NumberRange) it.next();
        assertEquals(15, thirdEntry.getMinimum(), 0d);
        assertEquals(20, thirdEntry.getMaximum(), 0d);
    }

    private static SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    static {
        DF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testCustomTimeDimensionConvertion() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put("HAS_MYDIM_DOMAIN", "true");
        reader.metadata.put("MYDIM_DOMAIN_DATATYPE", "java.util.Date");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        List<Object> converted =
                accessor.convertDimensionValue(
                        "MYDIM",
                        Arrays.asList(
                                "2001-05-01T00:00:00.000Z",
                                "2001-05-02T00:00:00.000Z",
                                "2001-05-03T00:00:00.000Z"));
        assertEquals(3, converted.size());
        assertEquals(DF.parse("2001-05-01 00:00:00"), converted.get(0));
        assertEquals(DF.parse("2001-05-02 00:00:00"), converted.get(1));
        assertEquals(DF.parse("2001-05-03 00:00:00"), converted.get(2));
    }

    @Test
    public void testCustomDepthDimensionConvertion() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put("HAS_MYDIM_DOMAIN", "true");
        reader.metadata.put("MYDIM_DOMAIN_DATATYPE", "java.lang.Double");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        List<Object> converted = accessor.convertDimensionValue("MYDIM", Arrays.asList("10/20"));
        assertEquals(1, converted.size());
        NumberRange<Double> expected = new NumberRange<>(Double.class, 10d, 20d);
        assertEquals(expected, converted.get(0));
    }

    @Test
    public void testCustomCloudCoverDimensionConvertion() throws IOException, ParseException {
        MockDimensionReader reader = new MockDimensionReader();
        reader.metadata.put("HAS_MYDIM_DOMAIN", "true");
        reader.metadata.put("MYDIM_DOMAIN_DATATYPE", "java.lang.Integer");
        ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
        List<Object> converted = accessor.convertDimensionValue("MYDIM", Arrays.asList("75/100"));
        assertEquals(1, converted.size());
        NumberRange<Double> expected = new NumberRange<>(Double.class, 75d, 100d);
        assertEquals(expected, converted.get(0));
    }
}
