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

package org.geoserver.params.extractor;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public final class Utils {

    private static final Logger LOGGER = Logging.getLogger(Utils.class);

    private Utils() {}

    public static void info(Logger logger, String message, Object... messageArguments) {
        logger.info(() -> String.format(message, messageArguments));
    }

    public static void debug(Logger logger, String message, Object... messageArguments) {
        logger.fine(() -> String.format(message, messageArguments));
    }

    public static void error(
            Logger logger, Throwable cause, String message, Object... messageArguments) {
        logger.log(Level.SEVERE, cause, () -> String.format(message, messageArguments));
    }

    public static void checkCondition(
            boolean condition, String failMessage, Object... failMessageArguments) {
        if (!condition) {
            throw exception(failMessage, failMessageArguments);
        }
    }

    public static <T> T withDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static ParamsExtractorException exception(String message, Object... messageArguments) {
        return new ParamsExtractorException(null, message, messageArguments);
    }

    public static ParamsExtractorException exception(
            Throwable cause, String message, Object... messageArguments) {
        return new ParamsExtractorException(cause, message, messageArguments);
    }

    private static final class ParamsExtractorException extends RuntimeException {

        public ParamsExtractorException(
                Throwable cause, String message, Object... messageArguments) {
            super(String.format(message, messageArguments), cause);
        }
    }

    public static <T extends Closeable> void closeQuietly(T closable) {
        try {
            closable.close();
        } catch (Exception exception) {
            Utils.error(LOGGER, exception, "Something bad happen when closing.");
        }
    }

    public static Map<String, String[]> parseParameters(Optional<String> queryString)
            throws UnsupportedEncodingException {
        Map<String, String[]> parameters = new HashMap<>();
        if (!queryString.isPresent()) {
            return parameters;
        }
        final String[] parametersParts = queryString.get().split("&");
        for (String parametersPart : parametersParts) {
            String[] parameterParts = parametersPart.split("=");
            if (parameterParts.length < 2) {
                continue;
            }
            String name = URLDecoder.decode(parameterParts[0], "UTF-8");
            String value = URLDecoder.decode(parameterParts[1], "UTF-8");
            String[] values = parameters.get(name);
            if (values == null) {
                parameters.put(name, new String[] {value});
            } else {
                values = Arrays.copyOf(values, value.length() + 1);
                values[value.length()] = value;
                parameters.put(name, values);
            }
        }
        return parameters;
    }

    public static <K, V> Map.Entry<K, V> caseInsensitiveSearch(String key, Map<K, V> map) {
        if (map != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (entry.getKey() instanceof String
                        && ((String) entry.getKey()).equalsIgnoreCase(key)) {
                    return entry;
                }
            }
        }
        return null;
    }
}
