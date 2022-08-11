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

package org.geoserver.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.geotools.util.DateRange;

/**
 * Formats date/times into ISO8601
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ISO8601Formatter {

    private final GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

    private void pad(StringBuilder buf, int value, int amt) {
        if (amt == 2 && value < 10) {
            buf.append('0');
        } else if (amt == 4 && value < 1000) {
            if (value >= 100) {
                buf.append("0");
            } else if (value >= 10) {
                buf.append("00");
            } else {
                buf.append("000");
            }
        } else if (amt == 3 && value < 100) {
            if (value >= 10) {
                buf.append('0');
            } else {
                buf.append("00");
            }
        }
        buf.append(value);
    }

    /**
     * Formats the specified object either as a single time, if it's a Date, or as a continuous
     * interval, if it's a DateRange (and will throw an {@link IllegalArgumentException} otherwise)
     */
    public String format(Object date) {
        if (date instanceof Date) {
            return format((Date) date);
        } else if (date instanceof DateRange) {
            DateRange range = (DateRange) date;
            StringBuilder sb = new StringBuilder();
            format(range.getMinValue(), sb);
            sb.append("/");
            format(range.getMaxValue(), sb);
            sb.append("/PT1S");
            return sb.toString();
        } else {
            throw new IllegalArgumentException(
                    "Date argument should be either a Date or a "
                            + "DateRange, however this one is neither: "
                            + date);
        }
    }

    /** Formats the specified Date in ISO8601 format */
    public String format(Date date) {
        return format(date, new StringBuilder()).toString();
    }

    public StringBuilder format(Date date, StringBuilder buf) {
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        if (cal.get(Calendar.ERA) == GregorianCalendar.BC) {
            if (year > 1) {
                buf.append('-');
            }
            year = year - 1;
        }
        pad(buf, year, 4);
        buf.append('-');
        pad(buf, cal.get(Calendar.MONTH) + 1, 2);
        buf.append('-');
        pad(buf, cal.get(Calendar.DAY_OF_MONTH), 2);
        buf.append('T');
        pad(buf, cal.get(Calendar.HOUR_OF_DAY), 2);
        buf.append(':');
        pad(buf, cal.get(Calendar.MINUTE), 2);
        buf.append(':');
        pad(buf, cal.get(Calendar.SECOND), 2);
        buf.append('.');
        pad(buf, cal.get(Calendar.MILLISECOND), 3);
        buf.append('Z');

        return buf;
    }
}
