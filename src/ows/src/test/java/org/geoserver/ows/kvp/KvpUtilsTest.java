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

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.junit.Assert;
import org.junit.Test;

public class KvpUtilsTest {
    @Test
    public void testEmptyString() {
        Assert.assertEquals(0, KvpUtils.readFlat("").size());
    }

    @Test
    public void testTrailingEmtpyStrings() {
        Assert.assertEquals(
                Arrays.asList(new String[] {"x", "", "x", "", ""}), KvpUtils.readFlat("x,,x,,"));
    }

    @Test
    public void testEmtpyNestedString() {
        List result = KvpUtils.readNested("");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(0, ((List) result.get(0)).size());
    }

    @Test
    public void testNullKvp() {
        KvpMap<String, String> result = KvpUtils.toStringKVP(null);
        Assert.assertNull(result);
    }

    @Test
    public void testStarNestedString() {
        List result = KvpUtils.readNested("*");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(0, ((List) result.get(0)).size());
    }

    @Test
    public void testWellKnownTokenizers() {
        String[] expected;
        List actual;

        expected = new String[] {"1", "2", "3", ""};
        actual = KvpUtils.readFlat("1,2,3,", KvpUtils.INNER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {"abc", "def", ""};
        actual = KvpUtils.readFlat("(abc)(def)()", KvpUtils.OUTER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {"abc"};
        actual = KvpUtils.readFlat("(abc)", KvpUtils.OUTER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {""};
        actual = KvpUtils.readFlat("()", KvpUtils.OUTER_DELIMETER);
        assertKvp(expected, actual);

        expected = new String[] {"", "A=1", "B=2", ""};
        actual = KvpUtils.readFlat(";A=1;B=2;", KvpUtils.CQL_DELIMITER);
        assertKvp(expected, actual);

        expected = new String[] {"ab", "cd", "ef", ""};
        actual = KvpUtils.readFlat("ab&cd&ef&", KvpUtils.KEYWORD_DELIMITER);
        assertKvp(expected, actual);

        expected = new String[] {"A", "1 "};
        actual = KvpUtils.readFlat("A=1 ", KvpUtils.VALUE_DELIMITER);
        assertKvp(expected, actual);
    }

    @Test
    public void testRadFlatUnkownDelimiter() {
        List actual;

        final String[] expected = new String[] {"1", "2", "3", ""};
        actual = KvpUtils.readFlat("1^2^3^", "\\^");
        assertKvp(expected, actual);

        actual = KvpUtils.readFlat("1-2-3-", "-");
        assertKvp(expected, actual);
    }

    private void assertKvp(String[] expected, List actual) {
        List expectedList = Arrays.asList(expected);
        Assert.assertEquals(expectedList.size(), actual.size());
        Assert.assertEquals(expectedList, actual);
    }

    @Test
    public void testEscapedTokens() {
        // test trivial scenarios
        List<String> actual = KvpUtils.escapedTokens("", ',');
        Assert.assertEquals(Arrays.asList(""), actual);

        actual = KvpUtils.escapedTokens(",", ',');
        Assert.assertEquals(Arrays.asList("", ""), actual);

        actual = KvpUtils.escapedTokens("a,b", ',');
        Assert.assertEquals(Arrays.asList("a", "b"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',');
        Assert.assertEquals(Arrays.asList("a", "b", "c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 2);
        Assert.assertEquals(Arrays.asList("a", "b,c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 1);
        Assert.assertEquals(Arrays.asList("a,b,c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 0);
        Assert.assertEquals(Arrays.asList("a", "b", "c"), actual);

        actual = KvpUtils.escapedTokens("a,b,c", ',', 1000);
        Assert.assertEquals(Arrays.asList("a", "b", "c"), actual);

        // test escaped data
        actual = KvpUtils.escapedTokens("\\\\,\\\\", ',');
        Assert.assertEquals(Arrays.asList("\\\\", "\\\\"), actual);

        actual = KvpUtils.escapedTokens("a\\,b,c", ',');
        Assert.assertEquals(Arrays.asList("a\\,b", "c"), actual);

        actual = KvpUtils.escapedTokens("a\\,b,c,d", ',', 2);
        Assert.assertEquals(Arrays.asList("a\\,b", "c,d"), actual);

        // test error conditions
        try {
            KvpUtils.escapedTokens(null, ',');
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            KvpUtils.escapedTokens("", '\\');
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            KvpUtils.escapedTokens("\\", '\\');
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testUnescape() {
        // test trivial scenarios
        String actual = KvpUtils.unescape("abc");
        Assert.assertEquals("abc", actual);

        // test escape sequences
        actual = KvpUtils.unescape("abc\\\\");
        Assert.assertEquals("abc\\", actual);

        actual = KvpUtils.unescape("abc\\d");
        Assert.assertEquals("abcd", actual);

        // test error conditions
        try {
            KvpUtils.unescape(null);
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            KvpUtils.unescape("\\");
            Assert.fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testParseQueryString() {
        Map<String, Object> kvp =
                KvpUtils.parseQueryString(
                        "geoserver?request=WMS&version=1.0.0&CQL_FILTER=NAME='geoserver'");
        Assert.assertEquals(3, kvp.size());
        Assert.assertEquals("WMS", kvp.get("request"));
        Assert.assertEquals("1.0.0", kvp.get("version"));
        Assert.assertEquals("NAME='geoserver'", kvp.get("CQL_FILTER"));
    }

    @Test
    public void testParseQueryStringRepeated() {
        Map<String, Object> kvp =
                KvpUtils.parseQueryString(
                        "geoserver?request=WMS&version=1.0.0&version=2.0.0&CQL_FILTER=NAME"
                                + "='geoserver'");
        Assert.assertEquals(3, kvp.size());
        Assert.assertEquals("WMS", kvp.get("request"));
        assertArrayEquals(new String[] {"1.0.0", "2.0.0"}, (String[]) kvp.get("version"));
        Assert.assertEquals("NAME='geoserver'", kvp.get("CQL_FILTER"));
    }
}
