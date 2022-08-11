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

import java.util.List;
import net.opengis.wcs10.AxisSubsetType;
import net.opengis.wcs10.IntervalType;
import net.opengis.wcs10.TypedLiteralType;
import net.opengis.wcs10.Wcs10Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.KvpUtils.Tokenizer;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * WCS 1.0.0 BBoxKvpParser.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class AxisSubsetKvpParser extends KvpParser {
    public AxisSubsetKvpParser() {
        super("band", AxisSubsetType.class);
        setService("wcs");
    }

    @SuppressWarnings("unchecked") // due to EMF model having no generics
    public Object parse(String value) throws Exception {
        final AxisSubsetType axisSubset = Wcs10Factory.eINSTANCE.createAxisSubsetType();

        axisSubset.setName("Band");

        if (value.contains("/")) {
            List<String> unparsed = KvpUtils.readFlat(value, new Tokenizer("/"));

            IntervalType interval = Wcs10Factory.eINSTANCE.createIntervalType();
            TypedLiteralType min = Wcs10Factory.eINSTANCE.createTypedLiteralType();
            TypedLiteralType max = Wcs10Factory.eINSTANCE.createTypedLiteralType();
            TypedLiteralType res = Wcs10Factory.eINSTANCE.createTypedLiteralType();
            if (unparsed.size() == 2) {
                min.setValue(unparsed.get(0));
                max.setValue(unparsed.get(1));

                interval.setMin(min);
                interval.setMax(max);
            } else {
                min.setValue(unparsed.get(0));
                max.setValue(unparsed.get(1));
                res.setValue(unparsed.get(2));

                interval.setMin(min);
                interval.setMax(max);
                interval.setRes(res);
            }

            axisSubset.getInterval().add(interval);
        } else {
            List<String> unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);

            if (unparsed.isEmpty()) {
                throw new WcsException(
                        "Requested axis subset contains wrong number of values (should have at least 1): "
                                + unparsed.size(),
                        WcsExceptionCode.InvalidParameterValue,
                        "band");
            }

            for (String bandValue : unparsed) {
                TypedLiteralType singleValue = Wcs10Factory.eINSTANCE.createTypedLiteralType();
                singleValue.setValue(bandValue);

                axisSubset.getSingleValue().add(singleValue);
            }
        }

        return axisSubset;
    }
}
