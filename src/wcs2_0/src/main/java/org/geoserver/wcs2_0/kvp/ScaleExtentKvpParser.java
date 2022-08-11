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

import net.opengis.wcs20.ScaleToExtentType;
import net.opengis.wcs20.TargetAxisExtentType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * Parses the WCS 2.0 {@link ScaleToExtentType} from KVP
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ScaleExtentKvpParser extends KvpParser {

    public ScaleExtentKvpParser() {
        super("scaleExtent", ScaleToExtentType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        // clean up extra space
        value = value.trim();

        ScaleToExtentType se = Wcs20Factory.eINSTANCE.createScaleToExtentType();

        int base = 0;
        for (; ; ) {
            // search the open parenthesis
            int idxOpen = value.indexOf("(", base);
            if (idxOpen == -1) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }
            int idxNextOpen = value.indexOf("(", idxOpen + 1);

            // search the closed parens
            int idxClosed = value.indexOf(")", idxOpen);
            if (idxClosed == -1 || (idxNextOpen > 0 && idxClosed > idxNextOpen)) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }
            int idxNextClosed = value.indexOf(")", idxClosed + 1);

            // the comma between the parens (we start from base to make sure it's actually between
            // the parens)
            int idxMid = value.indexOf(",", base);
            if (idxMid == -1 || idxMid >= idxClosed - 1 || idxMid <= idxOpen + 1) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }
            int idxNextMid = value.indexOf(",", idxMid + 1);
            if (idxNextMid != -1 && idxNextMid < idxClosed) {
                throw new WCS20Exception(
                        "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                        WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                        "scaleExtent");
            }

            // extract the three components
            String axisName = value.substring(base, idxOpen);
            String low = value.substring(idxOpen + 1, idxMid);
            String high = value.substring(idxMid + 1, idxClosed);

            try {
                TargetAxisExtentType te = Wcs20Factory.eINSTANCE.createTargetAxisExtentType();
                te.setAxis(axisName.trim());
                te.setLow(Double.valueOf(low));
                te.setHigh(Double.valueOf(high));

                se.getTargetAxisExtent().add(te);
            } catch (NumberFormatException e) {
                WCS20Exception ex =
                        new WCS20Exception(
                                "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                                WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                                "scaleExtent");
                ex.initCause(e);
                throw ex;
            }

            // we should also have a comma after the closed parens
            int idxSeparator = value.indexOf(",", idxClosed);
            if (idxSeparator == -1) {
                if (idxClosed == value.length() - 1) {
                    return se;
                } else {
                    throw new WCS20Exception(
                            "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                            WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                            "scaleExtent");
                }
            } else {
                if (idxSeparator > idxNextClosed) {
                    throw new WCS20Exception(
                            "Invalid ScaleExtent syntax, expecting a comma separate list of axisName(min,max)*",
                            WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                            "scaleExtent");
                }
                base = idxSeparator + 1;
            }
        }
    }
}
