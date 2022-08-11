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

package org.geoserver.wcs2_0.kvp;

import net.opengis.wcs20.DimensionSliceType;
import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;

/**
 * Parses the WCS 2.0 subset key
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SubsetKvpParser extends KvpParser {

    public SubsetKvpParser() {
        super("subset", DimensionSubsetType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        // SubsetSpec: dimension [ , crs ] ( intervalOrPoint )
        // dimension: NCName
        // crs: anyURI
        // intervalOrPoint: interval | point
        // interval: low , high
        // low: point | *
        // high: point | *
        // point: number | " token " // " = ASCII 0x42

        // trim just in case
        value = value.trim();

        // first, locate the intervalOrPoint part
        int openIdx = value.indexOf("(");
        int closeIdx = value.indexOf(")");

        if (openIdx == -1 || closeIdx == -1 || closeIdx < value.length() - 1) {
            throw new OWS20Exception(
                    "Invalid syntax, dimension [ , crs ] ( intervalOrPoint ) is expected",
                    WCS20ExceptionCode.InvalidEncodingSyntax,
                    "subset");
        }

        // parse the first part, dimension[,crs]
        String dimension = null;
        String crs = null;
        String dimensionCrs = value.substring(0, openIdx);
        String[] dcElements = dimensionCrs.split("\\s*,\\s*");
        if (dcElements.length == 1) {
            dimension = dcElements[0];
            crs = null;
        } else if (dcElements.length == 2) {
            dimension = dcElements[0];
            crs = dcElements[1];
        } else {
            throw new OWS20Exception(
                    "Invalid syntax, dimension [ , crs ] ( intervalOrPoint ) is expected",
                    WCS20ExceptionCode.InvalidEncodingSyntax,
                    "subset");
        }

        // parse the second part, intervalOrPoint
        String valuePoint = value.substring(openIdx + 1, closeIdx);
        // split on all commas not contained in quotes
        String[] vpElements = valuePoint.split(",\\s*(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        if (vpElements.length == 1) {
            // point
            String point = parsePoint(vpElements[0], false);
            DimensionSliceType slice = Wcs20Factory.eINSTANCE.createDimensionSliceType();
            slice.setDimension(dimension);
            slice.setCRS(crs);
            slice.setSlicePoint(point);
            return slice;
        } else if (vpElements.length == 2) {
            String low = parsePoint(vpElements[0], true);
            String high = parsePoint(vpElements[1], true);

            DimensionTrimType trim = Wcs20Factory.eINSTANCE.createDimensionTrimType();
            trim.setDimension(dimension);
            trim.setCRS(crs);
            trim.setTrimLow(low);
            trim.setTrimHigh(high);
            return trim;
        } else {
            throw new OWS20Exception(
                    "Invalid syntax, dimension [ , crs ] ( intervalOrPoint ) "
                            + "where interval or point has either 1 or two elements",
                    WCS20ExceptionCode.InvalidEncodingSyntax,
                    "subset");
        }
    }

    private String parsePoint(String point, boolean allowStar) {
        point = point.trim();
        if ("*".equals(point)) {
            if (allowStar) {
                // "no" limit
                return null;
            } else {
                throw new OWS20Exception(
                        "Invalid usage of *, it can be used only when specifying an interval",
                        WCS20ExceptionCode.InvalidEncodingSyntax,
                        "subset");
            }
        } else if (point.startsWith("\"") && point.endsWith("\"")) {
            point = point.substring(1, point.length() - 1);
        } else {
            try {
                // check it is a number
                Double.parseDouble(point);
            } catch (NumberFormatException e) {
                throw new OWS20Exception(
                        "Invalid point value "
                                + point
                                + ", it is not a number and it's not between double quotes",
                        WCS20ExceptionCode.InvalidEncodingSyntax,
                        "subset");
            }
        }

        return point;
    }
}
