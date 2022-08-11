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

package org.geoserver.importer.rest;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import org.geoserver.importer.DatePattern;
import org.geoserver.importer.Dates;
import org.geoserver.importer.transform.DateFormatTransform;
import org.junit.Test;

/** @author Ian Schneider <ischneider@opengeo.org> */
public class DateFormatTransformTest extends TransformTestSupport {

    public DateFormatTransformTest() {}

    @Test
    public void testExtents() throws Exception {
        // this is mostly a verification of the extents of the builtin date parsing
        DateFormatTransform transform = new DateFormatTransform("not used", null);

        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));

        int minYear = -292269052; // this is the text value
        Date parsed = transform.parseDate("" + minYear);
        cal.setTime(parsed);

        // the real value is the minYear - 1 since 0BC == 1AD
        assertEquals(minYear - 1, -cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.BC, cal.get(Calendar.ERA));

        cal.setTimeInMillis(Long.MAX_VALUE);
        int maxYear = cal.get(Calendar.YEAR);
        parsed = transform.parseDate("" + maxYear);
        cal.setTime(parsed);
        assertEquals(maxYear, cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.AD, cal.get(Calendar.ERA));
    }

    @Test
    public void testTransformSuccess() throws ParseException {
        DateFormatTransform transform = new DateFormatTransform("not used", null);

        Date now = new Date();

        // make a big shuffled list of patterns to ensure caching of last pattern
        // doesn't cause any problems
        List<String> patterns = new ArrayList<>();
        patterns.addAll(
                Collections2.transform(
                        Dates.patterns(false),
                        new Function<DatePattern, String>() {

                            @Override
                            public String apply(DatePattern input) {
                                return input.dateFormat().toPattern();
                            }
                        }));

        Collections.shuffle(patterns);

        for (String f : patterns) {
            SimpleDateFormat fmt = new SimpleDateFormat(f);
            fmt.setTimeZone(Dates.UTC_TZ);
            Date expected = fmt.parse(fmt.format(now));
            Date parsed = transform.parseDate(fmt.format(now));
            assertEquals(expected, parsed);
        }
    }

    @Test
    public void testTransformSuccessCustomFormat() throws ParseException {
        String customFormat = "yyyy-MM-dd'X'00";
        DateFormatTransform transform = new DateFormatTransform("not used", customFormat);

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat(customFormat);
        fmt.setTimeZone(Dates.UTC_TZ);
        Date expected = fmt.parse(fmt.format(now));
        Date parsed = transform.parseDate(fmt.format(now));
        assertEquals(expected, parsed);
    }

    @Test
    public void testJSON() throws Exception {
        doJSONTest(new DateFormatTransform("foo", null));
        doJSONTest(new DateFormatTransform("foo", "yyyy-MM-dd"));
        doJSONTest(new DateFormatTransform("foo", null, null, "LIST"));
        doJSONTest(new DateFormatTransform("foo", null, null, "DISCRETE_INTERVAL"));
        doJSONTest(new DateFormatTransform("foo", null, null, "CONTINUOUS_INTERVAL"));
        doJSONTest(new DateFormatTransform("foo", null, "enddate", null));
    }
}
