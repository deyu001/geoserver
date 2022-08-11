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

import java.util.List;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.geometry.GeneralEnvelope;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Parsing a BBOX for WCS.
 *
 * <p>Notice that we make sure tht the BBOX is 2D since we support elevation only as a band of the
 * range!
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class BBoxKvpParser extends Wcs10KvpParser {
    public BBoxKvpParser() {
        super("bbox", GeneralEnvelope.class);
    }

    @SuppressWarnings("unchecked")
    public GeneralEnvelope parse(String value) throws Exception {
        List unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);
        final int size = unparsed.size();
        // check to make sure that the bounding box has 4 coordinates
        if (unparsed.size() != 4) {
            throw new WcsException(
                    "Requested bounding box contains wrong"
                            + "number of coordinates: "
                            + unparsed.size(),
                    InvalidParameterValue,
                    "bbox");
        }

        // if it does, store them in an array of doubles
        final double[] bbox = new double[size];
        for (int i = 0; i < size; i++) {
            try {
                bbox[i] = Double.parseDouble((String) unparsed.get(i));
            } catch (NumberFormatException e) {
                throw new WcsException(
                        "Bounding box coordinate " + i + " is not parsable:" + unparsed.get(i),
                        InvalidParameterValue,
                        "bbox");
            }
        }

        // ensure the values are sane
        double minx = bbox[0];
        double miny = bbox[1];
        double maxx = bbox[2];
        double maxy = bbox[3];
        //    	double minz = Double.NaN;
        //    	double maxz = Double.NaN;
        //        if(size==6){
        //        	minz = bbox[4];
        //        	maxz = bbox[5];
        //        }
        if (minx > maxx) {
            throw new WcsException(
                    "illegal bbox, minX: " + minx + " is " + "greater than maxX: " + maxx,
                    InvalidParameterValue,
                    "bbox");
        }

        if (miny > maxy) {
            throw new WcsException(
                    "illegal bbox, minY: " + miny + " is " + "greater than maxY: " + maxy,
                    InvalidParameterValue,
                    "bbox");
        }

        //        if (size== 6 &&minz > maxz) {
        //            throw new ServiceException("illegal bbox, minz: " + minz + " is "
        //                    + "greater than maxz: " + maxz);
        //        }

        // build the final envelope with no CRS
        final GeneralEnvelope envelope = new GeneralEnvelope(size / 2);
        //        if(size==4)
        envelope.setEnvelope(minx, miny, maxx, maxy);
        //        else
        //        	envelope.setEnvelope(minx,miny,minz,maxx,maxy,maxz);
        return envelope;
    }
}
