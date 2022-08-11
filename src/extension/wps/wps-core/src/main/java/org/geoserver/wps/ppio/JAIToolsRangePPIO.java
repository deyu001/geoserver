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

package org.geoserver.wps.ppio;

import org.geotools.util.Converter;
import org.jaitools.JAITools;
import org.jaitools.numeric.Range;

/**
 * Parses a {@link JAITools} range from a string defining it
 *
 * @author Andrea Aime - GeoSolutions
 * @author Emanuele Tajarol - GeoSolutions
 */
public class JAIToolsRangePPIO extends LiteralPPIO {

    static Converter CONVERTER =
            new JAIToolsRangeConverterFactory().createConverter(String.class, Range.class, null);

    /** Parses a single range from a string */
    public static Range<Double> parseRange(String sRange) {
        try {
            @SuppressWarnings("unchecked")
            Range<Double> result = CONVERTER.convert(sRange, Range.class);
            if (result == null) {
                throw new IllegalArgumentException("Bad range definition '" + sRange + "'");
            }

            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad range definition '" + sRange + "'", e);
        }
    }

    public JAIToolsRangePPIO() {
        super(Range.class);
    }

    /** Decodes the parameter (as a string) to its internal object implementation. */
    public Object decode(String value) throws Exception {
        return parseRange(value);
    }

    /** Encodes the internal object representation of a parameter as a string. */
    public String encode(Object value) throws Exception {
        throw new UnsupportedOperationException("JaiTools range not supported out of the box");
    }
}
