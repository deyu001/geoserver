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

import net.opengis.wcs20.InterpolationAxesType;
import net.opengis.wcs20.InterpolationAxisType;
import net.opengis.wcs20.InterpolationMethodType;
import net.opengis.wcs20.InterpolationType;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * KVP parser for the WCS 2.0 {@link InterpolationType}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InterpolationKvpParser extends KvpParser {

    public InterpolationKvpParser() {
        super("interpolation", InterpolationType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        InterpolationType result = Wcs20Factory.eINSTANCE.createInterpolationType();

        // remove space
        value = value.trim();

        // single value?
        if (value.matches("http://www.opengis.net/def/interpolation/OGC/1/[^,:]*")) {
            // single value then
            InterpolationMethodType method = Wcs20Factory.eINSTANCE.createInterpolationMethodType();
            method.setInterpolationMethod(value);
            result.setInterpolationMethod(method);
            return result;
        }

        // minimal validation of the multi-axis case
        if (value.matches(".*,\\s*,.*")) {
            // two consequent commas
            throwInvalidSyntaxException();
        } else if (value.startsWith(",") || value.endsWith(",")) {
            throwInvalidSyntaxException();
        }

        InterpolationAxesType axes = Wcs20Factory.eINSTANCE.createInterpolationAxesType();
        result.setInterpolationAxes(axes);
        String[] components = value.split("\\s*,\\s*");
        for (String component : components) {
            // minimal validation of the content
            if (!component.matches(".*:http://www.opengis.net/def/interpolation/OGC/1/.*")) {
                // not a regular axis:interpolation structure
                throwInvalidSyntaxException();
            } else if (component.matches(".*:\\s*:.*")) {
                // two consequent columns
                throwInvalidSyntaxException();
            }

            int idx = component.lastIndexOf(":", component.lastIndexOf(":") - 1);
            InterpolationAxisType ia = Wcs20Factory.eINSTANCE.createInterpolationAxisType();
            ia.setAxis(component.substring(0, idx));
            ia.setInterpolationMethod(component.substring(idx + 1));

            axes.getInterpolationAxis().add(ia);
        }

        return result;
    }

    protected void throwInvalidSyntaxException() {
        throw new WCS20Exception(
                "Invalid Interpolation syntax, expecting either a single interpolation value, "
                        + "or a comma separated list of axis:interpolation specs",
                WCS20Exception.WCS20ExceptionCode.InvalidEncodingSyntax,
                "interpolation");
    }
}
