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

package org.geoserver.wfs.kvp;

import java.net.URLDecoder;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.PropertyName;

/**
 * Tests for {@link Filter_2_0_0_KvpParser}, the parser for Filter 2.0 in KVP requests.
 *
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class Filter_2_0_0_KvpParserTest {

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} can be parsed from percent-encoded form into
     * a {@link PropertyIsLike} object.
     *
     * @param expectedLiteral expected decoded filter literal
     * @param encodedLiteral percent-encoded filter literal
     * @param matchCase value of {@code matchCase} filter attribute or {@code null} if none
     */
    private static void parsePropertyIsLike(
            String expectedLiteral, String encodedLiteral, Boolean matchCase) throws Exception {
        String encodedXml =
                "%3Cfes:Filter" //
                        + "%20xmlns:fes=%22http://www.opengis.net/fes/2.0%22%3E" //
                        + "%3Cfes:PropertyIsLike" //
                        + "%20wildCard=%22*%22" //
                        + "%20singleChar=%22%25%22" //
                        + "%20escapeChar=%22!%22" //
                        + (matchCase == null ? "" : "%20matchCase=%22" + matchCase + "%22") //
                        + "%3E" //
                        + "%3Cfes:ValueReference%3E" //
                        + "topp:STATE_NAME" //
                        + "%3C/fes:ValueReference%3E" //
                        + "%3Cfes:Literal%3E" //
                        + encodedLiteral //
                        + "%3C/fes:Literal%3E%" //
                        + "3C/fes:PropertyIsLike%3E" //
                        + "%3C/fes:Filter%3E";
        String xml = URLDecoder.decode(encodedXml, "UTF-8");
        @SuppressWarnings("unchecked")
        List<Filter> filters = (List<Filter>) new Filter_2_0_0_KvpParser(null).parse(xml);
        Assert.assertEquals(1, filters.size());
        PropertyIsLike propertyIsLike = (PropertyIsLike) filters.get(0);
        Assert.assertEquals("*", propertyIsLike.getWildCard());
        Assert.assertEquals("%", propertyIsLike.getSingleChar());
        Assert.assertEquals("!", propertyIsLike.getEscape());
        Assert.assertEquals(matchCase == null ? true : matchCase, propertyIsLike.isMatchingCase());
        Assert.assertEquals(
                "topp:STATE_NAME",
                ((PropertyName) propertyIsLike.getExpression()).getPropertyName());
        Assert.assertEquals(expectedLiteral, propertyIsLike.getLiteral());
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with an ASCII literal can be parsed from
     * percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeAsciiLiteral() throws Exception {
        parsePropertyIsLike("Illino*", "Illino*", null);
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with a non-ASCII literal can be parsed from
     * percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeNonAsciiLiteral() throws Exception {
        parsePropertyIsLike("ü*", "%C3%BC*", null);
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with {@code matchCase="true"} can be parsed
     * from percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeMatchCaseTrue() throws Exception {
        parsePropertyIsLike("Illino*", "Illino*", true);
    }

    /**
     * Test that Filter 2.0 {@code fes:PropertyIsLike} with {@code matchCase="false"} can be parsed
     * from percent-encoded form into a {@link PropertyIsLike} object.
     */
    @Test
    public void testPropertyIsLikeMatchCaseFalse() throws Exception {
        parsePropertyIsLike("Illino*", "Illino*", false);
    }
}
