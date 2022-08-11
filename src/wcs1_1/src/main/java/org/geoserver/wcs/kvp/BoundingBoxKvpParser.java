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

package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.Arrays;
import java.util.List;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.Ows11Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * This and wfs BBoxKvpParser share a lot, it's just they don't share the same output type. Find a
 * way to create one common superclass.
 *
 * @author Andrea Aime
 */
public class BoundingBoxKvpParser extends KvpParser {
    public BoundingBoxKvpParser() {
        super("BoundingBox", BoundingBoxType.class);
    }

    public Object parse(String value) throws Exception {
        List unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);

        // check to make sure that the bounding box has 4 coordinates
        if (unparsed.size() < 4) {
            throw new WcsException(
                    "Requested bounding box contains wrong"
                            + "number of coordinates (should have at least 4): "
                            + unparsed.size(),
                    WcsExceptionCode.InvalidParameterValue,
                    "BoundingBox");
        }

        // if it does, store them in an array of doubles
        int size = unparsed.size();
        Double[] lower = new Double[(int) Math.floor(size / 2.0)];
        Double[] upper = new Double[lower.length];

        for (int i = 0; i < lower.length; i++) {
            try {
                lower[i] = Double.parseDouble((String) unparsed.get(i));
            } catch (NumberFormatException e) {
                throw new WcsException(
                        "Bounding box coordinate is not parsable:" + unparsed.get(i * 2),
                        WcsExceptionCode.InvalidParameterValue,
                        "BoundingBox");
            }

            try {
                upper[i] = Double.parseDouble((String) unparsed.get(lower.length + i));
            } catch (NumberFormatException e) {
                throw new WcsException(
                        "Bounding box coordinate is not parsable:" + unparsed.get(i * 2 + 1),
                        WcsExceptionCode.InvalidParameterValue,
                        "BoundingBox");
            }
        }

        // check for crs
        String crsName = null;
        CoordinateReferenceSystem crs = null;
        if (unparsed.size() % 2 == 1) {
            crsName = (String) unparsed.get(unparsed.size() - 1);
            try {
                if ("urn:ogc:def:crs:OGC:1.3:CRS84".equals(crsName)) {
                    crsName = "EPSG:4326";
                } else {
                    crs = CRS.decode(crsName);
                    if (crs.getCoordinateSystem().getDimension() != lower.length)
                        throw new WcsException(
                                "CRS specified has dimension "
                                        + crs.getCoordinateSystem().getDimension()
                                        + " but bbox specified has "
                                        + lower.length,
                                InvalidParameterValue,
                                "BoundingBox");
                }
            } catch (Exception e) {
                throw new WcsException(
                        "Could not recognize crs " + crsName, InvalidParameterValue, "BoundingBox");
            }
        }

        // we do not check that lower <= higher because in the case of geographic
        // bbox we have to accept the case where the lower coordinate is higher
        // than the high one and handle it as antimeridian crossing, better do that once
        // in the code that handles GetCoverage (the same check must be performed for xml requests)

        BoundingBoxType bbt = Ows11Factory.eINSTANCE.createBoundingBoxType();
        bbt.setCrs(crsName);
        bbt.setLowerCorner(Arrays.asList(lower));
        bbt.setUpperCorner(Arrays.asList(upper));
        return bbt;
    }
}
