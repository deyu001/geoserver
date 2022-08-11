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

package org.vfny.geoserver.wms.responses.map.htmlimagemap.holes;

import javax.vecmath.GVector;
import org.locationtech.jts.geom.Coordinate;

/**
 * Represents a segment. It has the 2 ending points as properties (A,B).
 *
 * @author m.bartolomeoli
 */
public class LineSegment {
    public Vertex A;
    public Vertex B;

    public LineSegment() {}

    public LineSegment(Vertex a, Vertex b) {
        A = a;
        B = b;
    }

    /**
     * Checks if the segment intersects a "ray "starting from the given origin and "going" in the
     * given direction.
     */
    public Float intersectsWithRay(Coordinate origin, Coordinate direction) {
        float largestDistance =
                Math.max(
                                (float) (A.getPosition().x - origin.x),
                                (float) (B.getPosition().x - origin.x))
                        * 2f;
        GVector v = new GVector(new double[] {origin.x, origin.y});
        GVector d = new GVector(new double[] {direction.x, direction.y});
        d.scale(largestDistance);
        v.add(d);
        LineSegment raySegment =
                new LineSegment(
                        new Vertex(origin, 0),
                        new Vertex(new Coordinate(v.getElement(0), v.getElement(1)), 0));
        Coordinate intersection = findIntersection(this, raySegment);
        Float value = null;

        if (intersection != null) {
            v = new GVector(new double[] {origin.x, origin.y});
            v.sub(new GVector(new double[] {intersection.x, intersection.y}));
            double dist = v.norm();
            value = Float.valueOf((float) dist);
        }

        return value;
    }

    /** Gets the intersection point of the 2 given segments (null if they don't intersect). */
    public static Coordinate findIntersection(LineSegment a, LineSegment b) {
        double x1 = a.A.getPosition().x;
        double y1 = a.A.getPosition().y;
        double x2 = a.B.getPosition().x;
        double y2 = a.B.getPosition().y;
        double x3 = b.A.getPosition().x;
        double y3 = b.A.getPosition().y;
        double x4 = b.B.getPosition().x;
        double y4 = b.B.getPosition().y;

        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);

        double uaNum = (x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3);
        double ubNum = (x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3);

        double ua = uaNum / denom;
        double ub = ubNum / denom;

        if (clamp(ua, 0f, 1f) != ua || clamp(ub, 0f, 1f) != ub) return null;
        GVector v = new GVector(new double[] {a.A.getPosition().x, a.A.getPosition().y});
        GVector d = new GVector(new double[] {a.B.getPosition().x, a.B.getPosition().y});
        d.sub(v);
        d.scale(ua);
        d.add(v);
        return new Coordinate(d.getElement(0), d.getElement(1));
    }

    /** Restricts a value to be in the given range (min - max). */
    public static double clamp(double value, double min, double max) {
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }
}
