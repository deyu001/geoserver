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

package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.util.ISO8601Formatter;
import org.geoserver.wms.WMS;
import org.geoserver.wms.capabilities.DimensionHelper.Mode;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.xml.sax.Attributes;

/**
 * A test for proper ISO8601 formatting.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class DimensionHelperTest {

    protected DimensionHelper dimensionHelper;

    @Before
    public void setUp() {

        dimensionHelper =
                new DimensionHelper(Mode.WMS13, WMS.get()) {

                    @Override
                    protected void element(String element, String content, Attributes atts) {
                        // Capabilities_1_3_0_Translator.this.element(element, content, atts);
                    }

                    @Override
                    protected void element(String element, String content) {
                        // Capabilities_1_3_0_Translator.this.element(element, content);
                    }
                };
    }

    @Test
    public void testGetCustomDomainRepresentation() {
        final String[] vals = new String[] {"value with spaces", "value", "  other values "};
        final List<String> values = new ArrayList<>();
        for (String val : vals) values.add(val);
        DimensionInfo dimensionInfo = new DimensionInfoImpl();
        dimensionInfo.setPresentation(DimensionPresentation.LIST);
        dimensionInfo.setResolution(BigDecimal.valueOf(1));
        String customDimRepr = dimensionHelper.getCustomDomainRepresentation(dimensionInfo, values);
        // value with spaces,value
        Assert.equals(customDimRepr, vals[0] + "," + vals[1] + "," + vals[2].trim());
        // System.out.print(vals.toString());

    }

    @Test
    public void testNegativeYears() {
        ISO8601Formatter fmt = new ISO8601Formatter();

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.clear();

        // base assertion
        cal.set(Calendar.YEAR, 1);
        assertEquals("0001-01-01T00:00:00.000Z", fmt.format(cal.getTime()));

        // according to the spec, the year before is year 0000
        cal.add(Calendar.YEAR, -1);
        assertEquals("0000-01-01T00:00:00.000Z", fmt.format(cal.getTime()));

        // and now where negative territory
        cal.add(Calendar.YEAR, -1);
        assertEquals("-0001-01-01T00:00:00.000Z", fmt.format(cal.getTime()));

        // and real negative
        cal.set(Calendar.YEAR, 265000001);
        assertEquals("-265000000-01-01T00:00:00.000Z", fmt.format(cal.getTime()));
    }

    /**
     * The goal if this test is to verify behavior of a similar, but not complete, format provided
     * by the standard libraries. The incomplete pattern does not support BC dates properly, so we
     * will not test compliance here.
     *
     * <p>The random seed is not specified to allow various test runs broader coverage.
     */
    @Test
    public void testFormatterFuzz() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        ISO8601Formatter fmt = new ISO8601Formatter();

        GregorianCalendar cal = new GregorianCalendar();
        Random r = new Random();
        for (int i = 0; i < 1000; i++) {
            cal.set(Calendar.YEAR, 1 + r.nextInt(3000));
            cal.set(Calendar.DAY_OF_YEAR, 1 + r.nextInt(365));
            cal.set(Calendar.HOUR_OF_DAY, r.nextInt(24));
            cal.set(Calendar.MINUTE, r.nextInt(60));
            cal.set(Calendar.SECOND, r.nextInt(60));
            cal.set(Calendar.MILLISECOND, r.nextInt(1000));
            assertEquals(df.format(cal.getTime()), fmt.format(cal.getTime()));
        }
    }

    @Test
    public void testPadding() throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        ISO8601Formatter fmt = new ISO8601Formatter();

        assertEquals("0010-01-01T00:01:10.001Z", fmt.format(df.parse("0010-01-01T00:01:10.001")));
        assertEquals("0100-01-01T00:01:10.011Z", fmt.format(df.parse("0100-01-01T00:01:10.011")));
        assertEquals("1000-01-01T00:01:10.111Z", fmt.format(df.parse("1000-01-01T00:01:10.111")));
    }
}
