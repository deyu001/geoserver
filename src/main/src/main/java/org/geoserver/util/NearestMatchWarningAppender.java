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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.springframework.http.HttpHeaders;

/** Appends warning messages in case of nearest match */
public class NearestMatchWarningAppender extends AbstractDispatcherCallback {

    static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    public enum WarningType {
        Nearest,
        Default,
        NotFound
    }

    static final ThreadLocal<List<String>> WARNINGS =
            ThreadLocal.withInitial(() -> new ArrayList<>());

    /**
     * Adds a default value or nearest value warning.
     *
     * @param layerName Mandatory, the layer being used
     * @param dimension The dimension name
     * @param value The actual value used, or null if it's a {@link WarningType#NotFound} warning
     *     type
     * @param unit The measure unit of measure
     */
    public static void addWarning(
            String layerName,
            String dimension,
            Object value,
            String unit,
            WarningType warningType) {
        List<String> warnings = WARNINGS.get();
        if (warningType == WarningType.NotFound) {
            warnings.add("99 No nearest value found on " + layerName + ": " + dimension);
        } else {
            String type = (warningType == WarningType.Nearest) ? "Nearest value" : "Default value";
            String unitSpec = unit == null ? "" : unit;
            String valueSpec = formatValue(value);
            warnings.add(
                    "99 " + type + " used: " + dimension + "=" + valueSpec + " " + unitSpec + " ("
                            + layerName + ")");
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(UTC_TZ);
            return sdf.format(value);
        } else if (value == null) {
            return "-";
        } else {
            // numbers mostly?
            return value.toString();
        }
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        List<String> warnings = WARNINGS.get();
        if (warnings != null && !warnings.isEmpty()) {
            HttpServletResponse httpResponse = request.getHttpResponse();
            for (String warning : warnings) {
                httpResponse.addHeader(HttpHeaders.WARNING, warning);
            }
            return super.responseDispatched(request, operation, result, response);
        }

        return response;
    }

    @Override
    public void finished(Request request) {
        WARNINGS.remove();
    }
}
