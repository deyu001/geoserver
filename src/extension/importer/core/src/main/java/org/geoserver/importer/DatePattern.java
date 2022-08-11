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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates a date format and regular expression.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DatePattern implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    final String format;
    final String regex;
    final boolean strict;
    final boolean forceGmt;

    Pattern pattern;

    /**
     * Constructor with defaults, <tt>forceGmt</tt> set to <tt>true</tt> and <tt>strict</tt> set to
     * <tt>false</tt>.
     */
    public DatePattern(String format, String regex) {
        this(format, regex, true, false);
    }

    /**
     * Constructor.
     *
     * @param format The date format
     * @param regex The regular expression to pull the date out of a another string.
     * @param forceGmt Whether the pattern should assume the GMT time zone.
     * @param strict Whether or not this pattern must apply the regular expression to match before
     *     parsing a date.
     */
    public DatePattern(String format, String regex, boolean forceGmt, boolean strict) {
        this.format = format;
        this.regex = regex;
        this.forceGmt = forceGmt;
        this.strict = strict;
    }

    public String getFormat() {
        return format;
    }

    public SimpleDateFormat dateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.CANADA);
        if (forceGmt) {
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return dateFormat;
    }

    public String getRegex() {
        return regex;
    }

    public Pattern pattern() {
        if (pattern == null) {
            // wrap the regex in a group and match anything around it (use reluctant wildcard
            // matching)
            pattern = Pattern.compile(".*?(" + regex + ").*?", Pattern.CASE_INSENSITIVE);
        }
        return pattern;
    }

    /** When true the {@link #matchAndParse(String)} method should be used. */
    public boolean isStrict() {
        return strict;
    }

    public Date matchAndParse(String str) {
        Matcher m = pattern().matcher(str);
        if (!m.matches()) {
            return null;
        }

        str = m.group(1);
        return doParse(str);
    }

    public Date parse(String str) {
        if (isStrict()) {
            // matchAndParse should be called
            return null;
        }

        return doParse(str);
    }

    Date doParse(String str) {
        /*
         * We do not use the standard method DateFormat.parse(String), because
         * if the parsing stops before the end of the string, the remaining
         * characters are just ignored and no exception is thrown. So we have to
         * ensure that the whole string is correct for the format.
         */
        ParsePosition pos = new ParsePosition(0);
        Date p = dateFormat().parse(str, pos);
        if (p != null && pos.getIndex() == str.length()) {
            return p;
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (forceGmt ? 1231 : 1237);
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((regex == null) ? 0 : regex.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DatePattern other = (DatePattern) obj;
        if (forceGmt != other.forceGmt) return false;
        if (format == null) {
            if (other.format != null) return false;
        } else if (!format.equals(other.format)) return false;
        if (regex == null) {
            if (other.regex != null) return false;
        } else if (!regex.equals(other.regex)) return false;
        return true;
    }
}
