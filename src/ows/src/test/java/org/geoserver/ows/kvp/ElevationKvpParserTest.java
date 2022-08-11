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

package org.geoserver.ows.kvp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geoserver.platform.ServiceException;
import org.geotools.util.NumberRange;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the elevation kvp parser
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class ElevationKvpParserTest {

    @Test
    public void testPeriod() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        @SuppressWarnings("unchecked")
        List<Double> elements = new ArrayList<>((Collection<Double>) parser.parse("1/100/1"));
        Assert.assertTrue(elements.get(0) instanceof Double);
        Assert.assertEquals(100, elements.size());
        Assert.assertEquals(1.0, elements.get(0), 0d);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMixed() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        List<Object> elements =
                new ArrayList<>((Collection<Object>) parser.parse("5,3,4,1,2,8.9,1/9"));
        Assert.assertTrue(elements.get(0) instanceof NumberRange);
        Assert.assertEquals(1.0, ((NumberRange<Double>) elements.get(0)).getMinimum(), 0d);
        Assert.assertEquals(9.0, ((NumberRange<Double>) elements.get(0)).getMaximum(), 0d);
    }

    @Test
    public void testOutOfOrderSequence() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        @SuppressWarnings("unchecked")
        List<Object> elements = new ArrayList<>((Collection) parser.parse("5,3,4,1,2,8.9"));
        Assert.assertEquals(1.0, elements.get(0));
        Assert.assertEquals(2.0, elements.get(1));
        Assert.assertEquals(3.0, elements.get(2));
        Assert.assertEquals(4.0, elements.get(3));
        Assert.assertEquals(5.0, elements.get(4));
        Assert.assertEquals(8.9, elements.get(5));
    }

    @Test
    public void testOrderedSequence() throws ParseException {
        final ElevationKvpParser parser = new ElevationKvpParser("ELEVATION");
        @SuppressWarnings("unchecked")
        List<Object> elements = new ArrayList((Collection) parser.parse("1,2,3,4,5,8.9"));
        Assert.assertEquals(1.0, elements.get(0));
        Assert.assertEquals(2.0, elements.get(1));
        Assert.assertEquals(3.0, elements.get(2));
        Assert.assertEquals(4.0, elements.get(3));
        Assert.assertEquals(5.0, elements.get(4));
        Assert.assertEquals(8.9, elements.get(5));
    }

    @Test
    public void testInfiniteLoopZeroInterval() {
        String value = "0/0/0";
        ServiceException e =
                Assert.assertThrows(
                        ServiceException.class,
                        () -> new ElevationKvpParser("ELEVATION").parse(value));
        Assert.assertEquals(
                "Exceeded 100 iterations parsing elevations, bailing out.", e.getMessage());
        Assert.assertEquals(ServiceException.INVALID_PARAMETER_VALUE, e.getCode());
        Assert.assertEquals("elevation", e.getLocator());
    }

    @Test
    public void testInfiniteLoopPositiveInfinity() {
        String value = "Infinity/Infinity/1";
        ServiceException e =
                Assert.assertThrows(
                        ServiceException.class,
                        () -> new ElevationKvpParser("ELEVATION").parse(value));
        Assert.assertEquals(
                "Exceeded 100 iterations parsing elevations, bailing out.", e.getMessage());
        Assert.assertEquals(ServiceException.INVALID_PARAMETER_VALUE, e.getCode());
        Assert.assertEquals("elevation", e.getLocator());
    }

    @Test
    public void testInfiniteLoopNegativeInfinity() {
        String value = "-Infinity/-Infinity/1";
        ServiceException e =
                Assert.assertThrows(
                        ServiceException.class,
                        () -> new ElevationKvpParser("ELEVATION").parse(value));
        Assert.assertEquals(
                "Exceeded 100 iterations parsing elevations, bailing out.", e.getMessage());
        Assert.assertEquals(ServiceException.INVALID_PARAMETER_VALUE, e.getCode());
        Assert.assertEquals("elevation", e.getLocator());
    }
}
