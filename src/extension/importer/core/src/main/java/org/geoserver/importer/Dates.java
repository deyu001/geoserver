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

package org.geoserver.importer;

import com.google.common.collect.Collections2;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing/encoding dates.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class Dates {

    static List<DatePattern> PATTERNS =
            Arrays.asList(
                    dp(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}Z"),
                    dp(
                            "yyyy-MM-dd'T'HH:mm:sss'Z'",
                            "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,3}Z"),
                    dp(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2}Z"),
                    dp("yyyy-MM-dd'T'HH:mm'Z'", "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{1,2}Z"),
                    dp("yyyy-MM-dd'T'HH'Z'", "\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}Z"),
                    dp("yyyy-MM-dd", "\\d{4}-\\d{1,2}-\\d{1,2}"),
                    dp("yyyy-MM", "\\d{4}-\\d{1,2}"),
                    dp("yyyyMMdd", "\\d{6,8}", true, true),
                    dp("yyyyMM", "\\d{5,6}", true, true),
                    dp("yyyy", "\\d{4}"));

    /**
     * Returns list of all patterns, optionally filtering out ones that require a strict match.
     *
     * @param strict when <tt>false</tt> those patterns that require a strict match (ie. a pattern
     *     match and a date parse) are filtered out.
     */
    public static Collection<DatePattern> patterns(boolean strict) {
        Collection<DatePattern> patterns = PATTERNS;
        if (!strict) {
            patterns = Collections2.filter(patterns, input -> input != null && !input.isStrict());
        }
        return patterns;
    }

    public static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    static DatePattern dp(String format, String regex) {
        return new DatePattern(format, regex);
    }

    static DatePattern dp(String format, String regex, boolean forceGmt, boolean strict) {
        return new DatePattern(format, regex, forceGmt, strict);
    }

    public static Date matchAndParse(String str) {
        return parse(str, true);
    }

    public static Date parse(String str) {
        return parse(str, false);
    }

    static Date parse(String str, boolean match) {
        Collection<DatePattern> patterns = patterns(match);

        for (DatePattern dp : patterns) {
            Date parsed = match ? dp.matchAndParse(str) : dp.parse(str);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    static Date parseDate(DatePattern dp, String str) {
        Pattern p = dp.pattern();
        Matcher m = p.matcher(str);
        if (m.matches()) {
            String match = m.group(1);
            try {
                Date parsed = dp.dateFormat().parse(match);
                if (parsed != null) {
                    return parsed;
                }
            } catch (ParseException e) {
                // ignore
            }
        }
        return null;
    }
}
