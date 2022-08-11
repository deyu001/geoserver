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

package org.geoserver.importer.mosaic;

import com.google.common.base.Preconditions;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotools.util.logging.Logging;

/**
 * Extracts timestamps from granule file names.
 *
 * <p>This class needs a regular expression in order to extract the raw timestamp from the filename
 * and a date format to parse the exracted timestamp into a date object.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class FilenameTimeHandler extends TimeHandler {

    private static final long serialVersionUID = 1L;

    public static final String FILENAME_REGEX = "filenameRegex";

    public static final String TIME_FORMAT = "timeFormat";

    static Logger LOGGER = Logging.getLogger(FilenameTimeHandler.class);

    String filenameRegex;
    Pattern filenamePattern;

    SimpleDateFormat timeFormat;

    @Override
    public void init(Map<String, Object> props) {
        if (props.containsKey(FILENAME_REGEX)) {
            setFilenameRegex(props.get(FILENAME_REGEX).toString());
        }

        if (props.containsKey(TIME_FORMAT)) {
            setTimeFormat(props.get(TIME_FORMAT).toString());
        }
    }

    public String getFilenameRegex() {
        return filenameRegex;
    }

    public void setFilenameRegex(String filenameRegex) {
        this.filenameRegex = filenameRegex;
        filenamePattern = Pattern.compile(".*(" + filenameRegex + ").*");
    }

    public String getTimeFormat() {
        return timeFormat != null ? timeFormat.toPattern() : null;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = new SimpleDateFormat(timeFormat);
    }

    @Override
    public Date computeTimestamp(Granule g) {
        Preconditions.checkNotNull(filenamePattern);
        Preconditions.checkNotNull(timeFormat);

        String filename = g.getFile().getName();

        // TODO: add a reason for cases why timestamp can't be determined
        Matcher m = filenamePattern.matcher(g.getFile().getName());
        if (!m.matches() || m.groupCount() != 2) {
            // report back message
            String msg =
                    "Failure parsing time from file "
                            + filename
                            + " with pattern "
                            + getFilenameRegex();
            g.setMessage(msg);

            LOGGER.log(Level.WARNING, msg);
            return null;
        }

        try {
            return timeFormat.parse(m.group(1));
        } catch (ParseException e) {
            String msg =
                    "Failure parsing timestamp with pattern "
                            + timeFormat.toPattern()
                            + ": "
                            + e.getLocalizedMessage();
            g.setMessage(msg);

            LOGGER.log(Level.WARNING, msg, e);
            return null;
        }
    }
}
