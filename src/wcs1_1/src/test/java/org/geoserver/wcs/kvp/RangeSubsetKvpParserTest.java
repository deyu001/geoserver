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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.List;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.RangeSubsetType;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class RangeSubsetKvpParserTest {
    RangeSubsetKvpParser parser = new RangeSubsetKvpParser();

    @Test
    public void testSimpleFields() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance;temperature");
        assertNotNull(rs);
        assertEquals(2, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertNull(field.getInterpolationType());
        field = (FieldSubsetType) rs.getFieldSubset().get(1);
        assertEquals("temperature", field.getIdentifier().getValue());
        assertNull(field.getInterpolationType());
    }

    @Test
    public void testInvalidInterpolation() throws Exception {
        try {
            parser.parse("radiance:mindReadingWarper");
            fail("We do not support _that_ interpolation!");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("RangeSubset", e.getLocator());
        }
    }

    @Test
    public void testInterpolation() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance:linear;temperature:nearest");
        assertNotNull(rs);
        assertEquals(2, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertEquals("linear", field.getInterpolationType());
        field = (FieldSubsetType) rs.getFieldSubset().get(1);
        assertEquals("temperature", field.getIdentifier().getValue());
        assertEquals("nearest", field.getInterpolationType());
    }

    @Test
    public void testAxisSingleKey() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance[bands[Red]]");
        assertNotNull(rs);
        assertEquals(1, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertEquals(1, field.getAxisSubset().size());
        AxisSubsetType axis = (AxisSubsetType) field.getAxisSubset().get(0);
        assertEquals("bands", axis.getIdentifier());
        List keys = axis.getKey();
        assertEquals(1, keys.size());
        assertEquals("Red", keys.get(0));
    }

    @Test
    public void testAxisKeys() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance[bands[Red,Green,Blue]]");
        assertNotNull(rs);
        assertEquals(1, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertEquals(1, field.getAxisSubset().size());
        AxisSubsetType axis = (AxisSubsetType) field.getAxisSubset().get(0);
        assertEquals("bands", axis.getIdentifier());
        List keys = axis.getKey();
        assertEquals(3, keys.size());
        assertEquals("Red", keys.get(0));
        assertEquals("Green", keys.get(1));
        assertEquals("Blue", keys.get(2));
    }
}
