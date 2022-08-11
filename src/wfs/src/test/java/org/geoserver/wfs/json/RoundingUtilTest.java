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

package org.geoserver.wfs.json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import org.junit.Test;

/**
 * Test rounding
 *
 * @author Dean Povey
 */
public class RoundingUtilTest {

    @Test
    public void testSpecialCases() {
        for (int numDecimals = 0; numDecimals < 17; numDecimals++) {
            assertThat(Double.isNaN(RoundingUtil.round(Double.NaN, numDecimals)), is(true));
            assertThat(
                    RoundingUtil.round(Double.NEGATIVE_INFINITY, numDecimals),
                    is(equalTo(Double.NEGATIVE_INFINITY)));
            assertThat(
                    RoundingUtil.round(Double.POSITIVE_INFINITY, numDecimals),
                    is(equalTo(Double.POSITIVE_INFINITY)));
        }
    }

    @Test
    public void testSpecificCases() {
        assertThat(RoundingUtil.round(0d, 0), is(equalTo(0d)));
        assertThat(RoundingUtil.round(0.1, 0), is(equalTo(0d)));
        assertThat(RoundingUtil.round(0.1, 1), is(equalTo(0.1)));
        assertThat(RoundingUtil.round(0.1, 2), is(equalTo(0.1)));
        assertThat(RoundingUtil.round(0.05, 1), is(equalTo(0.1)));
        assertThat(RoundingUtil.round(-0.05, 1), is(equalTo(0d)));
        assertThat(RoundingUtil.round(0.0000001, 5), is(equalTo(0d)));
        assertThat(RoundingUtil.round(1.0000001, 7), is(equalTo(1.0000001)));
        assertThat(RoundingUtil.round(1.00000015, 7), is(equalTo(1.0000002)));
        assertThat(RoundingUtil.round(1E-3, 7), is(equalTo(0.001)));
        assertThat(RoundingUtil.round(1E-4, 3), is(equalTo(0d)));
        assertThat(RoundingUtil.round(1E-10, 10), is(equalTo(1E-10)));
    }

    @Test
    public void testNoRoundingWhenPrecisionWouldBeExceeded() {
        // Test cases where precision is exceeded.
        assertThat(RoundingUtil.round(1.01234567890123456E12, 1), is(equalTo(1.0123456789012E12)));
        assertThat(RoundingUtil.round(1.01234567890123456E12, 2), is(equalTo(1.01234567890123E12)));
        assertThat(RoundingUtil.round(1.01234567890123456E13, 1), is(equalTo(1.01234567890123E13)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E13, 2), is(equalTo(1.012345678901235E13)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E14, 1), is(equalTo(1.012345678901235E14)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E14, 2), is(equalTo(1.0123456789012346E14)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E15, 1), is(equalTo(1.0123456789012345E15)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E15, 2), is(equalTo(1.0123456789012345E15)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E16, 1), is(equalTo(1.0123456789012346E16)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E16, 2), is(equalTo(1.0123456789012346E16)));
        assertThat(
                RoundingUtil.round(1.0123456789012345E17, 1), is(equalTo(1.0123456789012345E17)));
        assertThat(
                RoundingUtil.round(1.0123456789012345E18, 1), is(equalTo(1.0123456789012345E18)));
        assertThat(
                RoundingUtil.round(1.01234567890123451E19, 1), is(equalTo(1.0123456789012345E19)));
        assertThat(RoundingUtil.round(Double.MIN_VALUE, 15), is(equalTo(0d)));
        assertThat(RoundingUtil.round(Double.MAX_VALUE, 1), is(equalTo(Double.MAX_VALUE)));
    }

    @Test
    public void testRandomRoundingVsBigDecimal() {
        Random r = new Random();
        for (int i = 0; i < 10000; i++) {
            double value = r.nextDouble();
            for (int numDecimals = 0; numDecimals <= 8; numDecimals++) {
                double expected =
                        new BigDecimal(Double.toString(value))
                                .setScale(numDecimals, RoundingMode.HALF_UP)
                                .doubleValue();
                double actual = RoundingUtil.round(value, numDecimals);
                assertThat(actual, is(equalTo(expected)));
            }
        }
    }
}
