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

package org.geoserver.wfs.kvp;

import java.util.List;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** @author Niels Charlier : added 3D BBOX support */
public class BBoxKvpParser extends KvpParser {
    public BBoxKvpParser() {
        super("bbox", Envelope.class);
    }

    public Object parse(String value) throws Exception {
        List unparsed = KvpUtils.readFlat(value, KvpUtils.INNER_DELIMETER);

        // check to make sure that the bounding box has 4 coordinates
        if (unparsed.size() < 4) {
            throw new IllegalArgumentException(
                    "Requested bounding box contains wrong"
                            + "number of coordinates (should have "
                            + "4): "
                            + unparsed.size());
        }

        int countco = 4;
        if (unparsed.size() == 6 || unparsed.size() == 7) { // 3d-coordinates
            countco = 6;
        }

        // if it does, store them in an array of doubles
        double[] bbox = new double[countco];

        for (int i = 0; i < countco; i++) {
            try {
                bbox[i] = Double.parseDouble((String) unparsed.get(i));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Bounding box coordinate " + i + " is not parsable:" + unparsed.get(i));
            }
        }

        // ensure the values are sane
        double minx = bbox[0];
        double miny = bbox[1];
        double minz = 0, maxx = 0, maxy = 0, maxz = 0;
        if (countco == 6) {
            minz = bbox[2];
            maxx = bbox[3];
            maxy = bbox[4];
            maxz = bbox[5];
        } else {
            maxx = bbox[2];
            maxy = bbox[3];
        }

        // check for srs
        String srs = null;
        if (unparsed.size() > countco) {
            // merge back the CRS definition, in case it is an AUTO one
            StringBuilder sb = new StringBuilder();
            for (int i = countco; i < unparsed.size(); i++) {
                sb.append(unparsed.get(i));
                if (i < (unparsed.size() - 1)) {
                    sb.append(",");
                }
            }
            srs = sb.toString();
        }

        return buildEnvelope(countco, minx, miny, minz, maxx, maxy, maxz, srs);
    }

    protected Object buildEnvelope(
            int countco,
            double minx,
            double miny,
            double minz,
            double maxx,
            double maxy,
            double maxz,
            String srs)
            throws NoSuchAuthorityCodeException, FactoryException {
        if (minx > maxx) {
            throw new ServiceException(
                    "illegal bbox, minX: " + minx + " is " + "greater than maxX: " + maxx);
        }

        if (miny > maxy) {
            throw new ServiceException(
                    "illegal bbox, minY: " + miny + " is " + "greater than maxY: " + maxy);
        }

        if (minz > maxz) {
            throw new ServiceException(
                    "illegal bbox, minZ: " + minz + " is " + "greater than maxZ: " + maxz);
        }

        if (countco == 6) {
            CoordinateReferenceSystem crs = srs != null ? CRS.decode(srs) : null;
            return new ReferencedEnvelope3D(minx, maxx, miny, maxy, minz, maxz, crs);
        } else {
            CoordinateReferenceSystem crs = srs != null ? CRS.decode(srs) : null;
            if (crs == null || crs.getCoordinateSystem().getDimension() == 2) {
                return new SRSEnvelope(minx, maxx, miny, maxy, srs);
            } else if (crs.getCoordinateSystem().getDimension() == 3) {
                return new ReferencedEnvelope3D(
                        minx, maxx, miny, maxy, -Double.MAX_VALUE, Double.MAX_VALUE, crs);
            } else {
                throw new WFSException(
                        "Unexpected BBOX, can only handle 2D or 3D ones",
                        "bbox",
                        "InvalidParameterValue");
            }
        }
    }
}
