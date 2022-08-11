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

package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;

import java.awt.Point;
import java.awt.geom.Point2D;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

public class QuickTileCacheTest {
    QuickTileCache cache = new QuickTileCache();

    @Test
    public void testMetaCoordinates() {
        Point orig = new Point(0, 0);
        assertEquals(orig, cache.getMetaTileCoordinates(orig));

        Point t10 = new Point(1, 0);
        assertEquals(orig, cache.getMetaTileCoordinates(t10));

        Point t01 = new Point(1, 0);
        assertEquals(orig, cache.getMetaTileCoordinates(t01));

        Point t33 = new Point(3, 3);
        assertEquals(new Point(3, 3), cache.getMetaTileCoordinates(t33));

        Point tm1m1 = new Point(-1, -1);
        assertEquals(new Point(-3, -3), cache.getMetaTileCoordinates(tm1m1));

        Point tm3m3 = new Point(-3, -3);
        assertEquals(new Point(-3, -3), cache.getMetaTileCoordinates(tm3m3));

        Point tm4m4 = new Point(-4, -4);
        assertEquals(new Point(-6, -6), cache.getMetaTileCoordinates(tm4m4));

        Point t4m4 = new Point(4, -4);
        assertEquals(new Point(3, -6), cache.getMetaTileCoordinates(t4m4));

        Point tm44 = new Point(-4, 4);
        assertEquals(new Point(-6, 3), cache.getMetaTileCoordinates(tm44));
    }

    @Test
    public void testTileCoordinatesNaturalOrigin() {
        Point2D origin = new Point2D.Double(0, 0);
        Envelope env = new Envelope(30, 60, 30, 60);
        Point tc = cache.getTileCoordinates(env, origin);
        assertEquals(new Point(1, 1), tc);

        env = new Envelope(-30, 0, -30, 0);
        tc = cache.getTileCoordinates(env, origin);
        assertEquals(new Point(-1, -1), tc);
    }

    @Test
    public void testInnerTileOffsets() {
        Envelope meta =
                new Envelope(1215736.8585492, 1215744.0245205, 5455471.361398601, 5455478.5273699);
        Envelope box1 =
                new Envelope(1215736.8585492, 1215739.2472063, 5455476.1387128, 5455478.5273699);
        Envelope box2 =
                new Envelope(1215739.2472063, 1215741.6358635, 5455476.1387128, 5455478.5273699);
        assertEquals(new Point(0, 2), cache.getTileOffsetsInMeta(box1, meta));
        assertEquals(new Point(1, 2), cache.getTileOffsetsInMeta(box2, meta));
    }
}
