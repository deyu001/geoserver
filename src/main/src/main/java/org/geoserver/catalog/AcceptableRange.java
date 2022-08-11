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

package org.geoserver.catalog;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import org.geoserver.ows.kvp.TimeParser;
import org.geotools.util.DateRange;
import org.geotools.util.Range;

/**
 * Represents the parsed acceptable range. For elevation it's simple numbers, for dates it's a
 * number of milliseconds.
 */
public class AcceptableRange {

    /**
     * Parses the acceptable range
     *
     * @param spec The specification from the UI
     * @param dataType The target data type (e.g. {@link Date}
     * @return An {@link AcceptableRange} object, or null if the spec was null or empty
     */
    public static AcceptableRange getAcceptableRange(String spec, Class dataType)
            throws ParseException {
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }

        String[] split = spec.split("/");
        if (split.length > 2) {
            throw new IllegalArgumentException(
                    "Invalid acceptable range specification, must be either a single "
                            + "value, or two values split by a forward slash");
        }
        Number before = parseValue(split[0], dataType);
        Number after = before;
        if (split.length == 2) {
            after = parseValue(split[1], dataType);
        }
        // avoid complications in case the search range is empty
        if (before.doubleValue() == 0 && after.doubleValue() == 0) {
            return null;
        }
        return new AcceptableRange(before, after, dataType);
    }

    private static Number parseValue(String s, Class dataType) throws ParseException {
        if (Date.class.isAssignableFrom(dataType)) {
            return TimeParser.parsePeriod(s);
        }
        // TODO: add support for Number, e.g., elevation
        throw new IllegalArgumentException("Unsupported value type " + dataType);
    }

    private Number before;
    private Number after;
    private Class dataType;

    public AcceptableRange(Number before, Number after, Class dataType) {
        this.before = before;
        this.after = after;
        this.dataType = dataType;
    }

    @SuppressWarnings("unchecked")
    public Range getSearchRange(Object value) {
        if (value instanceof Range) {
            Range range = (Range) value;
            Range before = getSearchRangeOnSingleValue(range.getMinValue());
            Range after = getSearchRangeOnSingleValue(range.getMaxValue());
            return before.union(after);
        } else {
            return getSearchRangeOnSingleValue(value);
        }
    }

    public Range getSearchRangeOnSingleValue(Object value) {
        if (Date.class.isAssignableFrom(dataType)) {
            Date center = (Date) value;
            Calendar cal = Calendar.getInstance();
            cal.setTime(center);
            cal.setTimeInMillis(cal.getTimeInMillis() - before.longValue());
            Date min = cal.getTime();
            cal.setTime(center);
            cal.setTimeInMillis(cal.getTimeInMillis() + after.longValue());
            Date max = cal.getTime();
            return new DateRange(min, max);
        }
        // TODO: add support for Number, e.g., elevation
        throw new IllegalArgumentException("Unsupported value type " + dataType);
    }

    /** Before offset */
    public Number getBefore() {
        return before;
    }

    /** After offset */
    public Number getAfter() {
        return after;
    }

    /** The range data type */
    public Class getDataType() {
        return dataType;
    }
}
