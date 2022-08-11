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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import net.opengis.ows11.BoundingBoxType;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class BoundingBoxKvpParserTest {

    BoundingBoxKvpParser parser = new BoundingBoxKvpParser();

    @Test
    public void test1DRange() throws Exception {
        executeFailingBBoxTest("10", "This bbox was invalid?");
        executeFailingBBoxTest("10,20", "This bbox was invalid?");
        executeFailingBBoxTest("10,20,30", "This bbox was invalid?");
    }

    @Test
    public void testNonNumericalRange() throws Exception {
        executeFailingBBoxTest("10,20,a,b", "This bbox was invalid?");
        executeFailingBBoxTest("a,20,30,b", "This bbox was invalid?");
    }

    @Test
    public void testOutOfDimRange() throws Exception {
        executeFailingBBoxTest(
                "10,20,30,40,50,60,EPSG:4326", "This bbox has more dimensions than the crs?");
        executeFailingBBoxTest(
                "10,20,30,40,EPSG:4979", "This bbox has less dimensions than the crs?");
    }

    @Test
    public void testUnknownCRS() throws Exception {
        executeFailingBBoxTest(
                "10,20,30,40,50,60,EPSG:MakeNoPrisoners!",
                "This crs should definitely be unknown...");
    }

    void executeFailingBBoxTest(String bbox, String message) throws Exception {
        try {
            parser.parse(bbox);
            fail(message);
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("BoundingBox", e.getLocator());
        }
    }

    @Test
    public void testNoCrs() throws Exception {
        BoundingBoxType bbox = (BoundingBoxType) parser.parse("10,20,15,30");
        assertEquals(2, bbox.getLowerCorner().size());
        assertEquals(10.0, bbox.getLowerCorner().get(0));
        assertEquals(20.0, bbox.getLowerCorner().get(1));
        assertEquals(2, bbox.getUpperCorner().size());
        assertEquals(15.0, bbox.getUpperCorner().get(0));
        assertEquals(30.0, bbox.getUpperCorner().get(1));
        assertNull(bbox.getCrs());
    }

    @Test
    public void test2DNoCrs() throws Exception {
        BoundingBoxType bbox = (BoundingBoxType) parser.parse("10,20,15,30,EPSG:4326");
        assertEquals(2, bbox.getLowerCorner().size());
        assertEquals(10.0, bbox.getLowerCorner().get(0));
        assertEquals(20.0, bbox.getLowerCorner().get(1));
        assertEquals(2, bbox.getUpperCorner().size());
        assertEquals(15.0, bbox.getUpperCorner().get(0));
        assertEquals(30.0, bbox.getUpperCorner().get(1));
        assertEquals("EPSG:4326", bbox.getCrs());
    }

    @Test
    public void test3DNoCrs() throws Exception {
        BoundingBoxType bbox = (BoundingBoxType) parser.parse("10,20,15,30,40,50,EPSG:4979");
        assertEquals(3, bbox.getLowerCorner().size());
        assertEquals(10.0, bbox.getLowerCorner().get(0));
        assertEquals(20.0, bbox.getLowerCorner().get(1));
        assertEquals(15.0, bbox.getLowerCorner().get(2));
        assertEquals(3, bbox.getUpperCorner().size());
        assertEquals(30.0, bbox.getUpperCorner().get(0));
        assertEquals(40.0, bbox.getUpperCorner().get(1));
        assertEquals(50.0, bbox.getUpperCorner().get(2));
        assertEquals("EPSG:4979", bbox.getCrs());
    }

    @Test
    public void testWgs84FullExtent() throws Exception {
        BoundingBoxType bbox =
                (BoundingBoxType) parser.parse("-180,-90,180,90,urn:ogc:def:crs:EPSG:4326");
        assertEquals(2, bbox.getLowerCorner().size());
        assertEquals(-180.0, bbox.getLowerCorner().get(0));
        assertEquals(-90.0, bbox.getLowerCorner().get(1));
        assertEquals(2, bbox.getUpperCorner().size());
        assertEquals(180.0, bbox.getUpperCorner().get(0));
        assertEquals(90.0, bbox.getUpperCorner().get(1));
        assertEquals("urn:ogc:def:crs:EPSG:4326", bbox.getCrs());
    }
}
