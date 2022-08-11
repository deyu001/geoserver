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

package org.geoserver.wcs2_0.exception;

import org.geoserver.platform.OWS20Exception;

/**
 * This defines an exception that can be turned into a valid xml service exception that wcs clients
 * will expect.
 *
 * <p>All errors should be wrapped in this before returning to clients.
 *
 * @author Emanuele Tajariol, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutionS SAS
 */
public class WCS20Exception extends OWS20Exception {
    /** */
    private static final long serialVersionUID = -6110652531274829497L;

    public static class WCS20ExceptionCode extends OWS20Exception.OWSExceptionCode {

        public static final WCS20ExceptionCode EmptyCoverageIdList =
                new WCS20ExceptionCode("emptyCoverageIdList", 404);
        public static final WCS20ExceptionCode InvalidEncodingSyntax =
                new WCS20ExceptionCode("InvalidEncodingSyntax", 400);

        // Scaling Extension
        public static final WCS20ExceptionCode InvalidScaleFactor =
                new WCS20ExceptionCode("InvalidScaleFactor", 404);
        public static final WCS20ExceptionCode InvalidExtent =
                new WCS20ExceptionCode("InvalidExtent", 404);
        public static final WCS20ExceptionCode ScaleAxisUndefined =
                new WCS20ExceptionCode("ScaleAxisUndefined", 404);

        // Interpolation Extension
        public static final WCS20ExceptionCode NoSuchAxis =
                new WCS20ExceptionCode("ScalingAxisUndefined", 404);
        public static final WCS20ExceptionCode InterpolationMethodNotSupported =
                new WCS20ExceptionCode("InterpolationMethodNotSupported", 404);

        // CRS Extension
        public static final WCS20ExceptionCode NotACrs = new WCS20ExceptionCode("NotACrs", 404);
        public static final WCS20ExceptionCode SubsettingCrsNotSupported =
                new WCS20ExceptionCode("SubsettingCrs-NotSupported", 404);
        public static final WCS20ExceptionCode OutputCrsNotSupported =
                new WCS20ExceptionCode("OutputCrs-NotSupported", 404);

        // CORE
        public static final WCS20ExceptionCode NoSuchCoverage =
                new WCS20ExceptionCode("NoSuchCoverage", 404);
        public static final WCS20ExceptionCode InvalidSubsetting =
                new WCS20ExceptionCode("InvalidSubsetting", 404);
        public static final WCS20ExceptionCode InvalidAxisLabel =
                new WCS20ExceptionCode("InvalidAxisLabel", 404);

        // RangeSubset extension
        public static final WCS20ExceptionCode NoSuchField =
                new WCS20ExceptionCode("NoSuchField", 404);

        protected WCS20ExceptionCode(String exceptionCode, Integer httpCode) {
            super(exceptionCode, httpCode);
        }
    }

    public WCS20Exception(String message) {
        super(message);
    }

    public WCS20Exception(Throwable e) {
        super(e);
    }

    public WCS20Exception(String message, String locator) {
        super(message, locator);
    }

    public WCS20Exception(String message, OWS20Exception.OWSExceptionCode code, String locator) {
        super(message, code, locator);
    }

    public WCS20Exception(
            String message, OWS20Exception.OWSExceptionCode code, String locator, Throwable cause) {
        super(message, code, locator);
        initCause(cause);
    }

    public WCS20Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
