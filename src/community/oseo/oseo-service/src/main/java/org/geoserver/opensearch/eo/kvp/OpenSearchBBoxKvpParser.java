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

package org.geoserver.opensearch.eo.kvp;

import org.geoserver.platform.OWS20Exception;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A parser that allows dateline crossing envelopes
 *
 * @author Andrea Aime
 */
public class OpenSearchBBoxKvpParser extends BBoxKvpParser {

    @Override
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
        if (countco > 4) {
            throw new IllegalArgumentException(
                    "Too many coordinates, openSearch cannot handle non flat envelopes yet");
        }

        CoordinateReferenceSystem crs = srs == null ? null : CRS.decode(srs, true);
        if (crs != null && !CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crs)) {
            throw new OWS20Exception(
                    "OpenSearch for EO requests only support boundig boxes in WGS84",
                    OWS20Exception.OWSExceptionCode.InvalidParameterValue,
                    "box");
        }
        minx = rollLongitude(minx);
        maxx = rollLongitude(maxx);

        if (minx > maxx) {
            // dateline crossing case
            return new ReferencedEnvelope[] {
                new ReferencedEnvelope(minx, 180, miny, maxy, crs),
                new ReferencedEnvelope(-180, maxx, miny, maxy, crs),
            };
        } else {
            return new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        }
    }

    protected static double rollLongitude(final double x) {
        double mod = (x + 180) % 360;
        if (mod == 0) {
            return x > 0 ? 180 : -180;
        } else {
            return mod - 180;
        }
    }
}
