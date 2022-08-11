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

package org.geoserver.opensearch.eo.kvp;

import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.FULL_RANGE_PATTERN;
import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.LEFT_RANGE_PATTERN;
import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.RIGHT_RANGE_PATTERN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class RangePatternsTest {

    @Test
    public void testFullRangePattern() {
        // check matches
        assertFullRangeMatch("[10,20]", "[", "10", "20", "]");
        assertFullRangeMatch("]10,20[", "]", "10", "20", "[");
        assertFullRangeMatch("[2010-09-21,2010-09-22]", "[", "2010-09-21", "2010-09-22", "]");
        // check failures
        assertPatternNotMatch(FULL_RANGE_PATTERN, "abcd");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10,20");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "2010-09-21");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "[10");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10]");
        assertPatternNotMatch(FULL_RANGE_PATTERN, "10,20,30");
    }

    private void assertFullRangeMatch(
            String testRange, String g1, String g2, String g3, String g4) {
        Matcher matcher = FULL_RANGE_PATTERN.matcher(testRange);
        assertTrue(matcher.matches());
        assertEquals(g1, matcher.group(1));
        assertEquals(g2, matcher.group(2));
        assertEquals(g3, matcher.group(3));
        assertEquals(g4, matcher.group(4));
    }

    private void assertPatternNotMatch(Pattern pattern, String testRange) {
        Matcher matcher = pattern.matcher(testRange);
        assertFalse(matcher.matches());
    }

    @Test
    public void testLeftRangePattern() {
        // check matches
        assertLeftRangeMatch("[10", "[", "10");
        assertLeftRangeMatch("]10", "]", "10");
        assertLeftRangeMatch("[2010-09-21", "[", "2010-09-21");

        // check failures
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "abcd");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "2010-09-21");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "[10,20]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20,30");
    }

    private void assertLeftRangeMatch(String testRange, String g1, String g2) {
        Matcher matcher = LEFT_RANGE_PATTERN.matcher(testRange);
        assertTrue(matcher.matches());
        assertEquals(g1, matcher.group(1));
        assertEquals(g2, matcher.group(2));
    }

    @Test
    public void testRightRangePattern() {
        // check matches
        assertRightRangeMatch("10]", "10", "]");
        assertRightRangeMatch("10[", "10", "[");
        assertRightRangeMatch("2010-09-21]", "2010-09-21", "]");

        // check failures
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "abcd");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "2010-09-21");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "[10,20]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10]");
        assertPatternNotMatch(LEFT_RANGE_PATTERN, "10,20,30");
    }

    private void assertRightRangeMatch(String testRange, String g1, String g2) {
        Matcher matcher = RIGHT_RANGE_PATTERN.matcher(testRange);
        assertTrue(matcher.matches());
        assertEquals(g1, matcher.group(1));
        assertEquals(g2, matcher.group(2));
    }
}
