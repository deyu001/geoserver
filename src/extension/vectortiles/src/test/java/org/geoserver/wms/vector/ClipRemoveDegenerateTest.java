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

package org.geoserver.wms.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.geoserver.wms.vector.PipelineBuilder.ClipRemoveDegenerateGeometries;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class ClipRemoveDegenerateTest {

    /*
     * Test normal case - i.e. clipping a line gives you a line (Non-degenerative cases)
     */
    @Test
    public void testSimple() throws Exception {
        ClipRemoveDegenerateGeometries clip =
                new ClipRemoveDegenerateGeometries(new Envelope(0, 10, 0, 10));

        // these are all completely inside the envelope

        Geometry result = clip._run(fromWKT("LINESTRING(1 1,2 2)"));
        assertEquals(result, fromWKT("LINESTRING(1 1,2 2)"));

        result = clip._run(fromWKT("MULTILINESTRING( (1 1,2 2), (5 5,7 7) )"));
        assertEquals(result, fromWKT("MULTILINESTRING( (1 1,2 2), (5 5,7 7) )"));

        result = clip._run(fromWKT("POINT(1 1)"));
        assertEquals(result, fromWKT("POINT(1 1)"));

        result = clip._run(fromWKT("MULTIPOINT(1 1,2 2 )"));
        assertEquals(result, fromWKT("MULTIPOINT(1 1,2 2 )"));

        result = clip._run(fromWKT("POLYGON((1 1,1 2,2 2,2 1,1 1))"));
        assertEquals(result, fromWKT("POLYGON((1 1,1 2,2 2,2 1,1 1))"));

        result =
                clip._run(
                        fromWKT("MULTIPOLYGON(((1 1,1 2,2 2,2 1,1 1)),((4 4,4 5,5 5,5 4, 4 4)) )"));
        assertEquals(
                result, fromWKT("MULTIPOLYGON(((1 1,1 2,2 2,2 1,1 1)),((4 4,4 5,5 5,5 4, 4 4)) )"));
    }

    /*
     * Currently, the geometryClipper used D-S to do line clipping (instead of the JTS routines), so this should always pass. Leave in incase the
     * geometryClipper algorithm changes!
     */
    @Test
    public void testClipDegenerativeLine() throws Exception {
        ClipRemoveDegenerateGeometries clip =
                new ClipRemoveDegenerateGeometries(new Envelope(0, 10, 0, 10));

        Geometry result = clip._run(fromWKT("LINESTRING(-1 -1,0 0)")); // intersection is POINT(0 0)
        assertNull(result);

        result = clip._run(fromWKT("LINESTRING(0 0, -10 0, -10 5,0 5)")); // intersection is
        // MULTIPOINT(0 0,0 5)
        assertNull(result);

        // intersection is LINESTRING(2 2, 3 0) + POINT(5 0) + POINT(7 0)
        result = clip._run(fromWKT("LINESTRING (2 2, 4 -2 , 5 0, 6 -2, 7 0)"));
        assertEquals(result, fromWKT("LINESTRING(2 2, 3 0)"));

        // splits a simple line into two lines
        result =
                clip._run(
                        fromWKT(
                                "LINESTRING (-0.9886673520149238 3.828985034223336, 5.746284581525119 -1.9900134363552608, 11.511403436635396 3.5595869568817347)"));
        assertEquals(
                result,
                fromWKT(
                        "MULTILINESTRING ((0 2.9747764420824416, 3.4430282894472706 0), (7.813580093078642 0, 10 2.1046845832981296))"));
    }

    /*
     * This is the meat of the class - handling polygon-polygon intersections. We look at several case - where the Polygon-Polygon intersection
     * returns points, lines, and multipolygons.
     */
    @Test
    public void testClipDegenerativePolygon() throws Exception {
        ClipRemoveDegenerateGeometries clip =
                new ClipRemoveDegenerateGeometries(new Envelope(0, 10, 0, 10));

        Geometry result =
                clip._run(fromWKT("POLYGON( (-10 -10, 0 -10, 0 0,-10 0,-10 -10))")); // intersection
        // is POINT(0 0)
        assertNull(result);

        result =
                clip._run(
                        fromWKT("POLYGON( (-10 -10, 10 -10, 10 0,-10 0,-10 -10))")); // intersection
        // is
        // LINESTRING
        // (10 0, 0 0)
        assertNull(result);

        result =
                clip._run(
                        fromWKT("POLYGON( (-10 -10, 10 -10, 10 0,-10 0,-10 -10))")); // intersection
        // is
        // LINESTRING
        // (10 0, 0 0)
        assertNull(result);

        // intersection is GEOMETRYCOLLECTION (POINT (10 0), LINESTRING (1 0, 3 0), LINESTRING (5 0,
        // 6 0))
        result =
                clip._run(
                        fromWKT(
                                "POLYGON ((1  -3 , 1 0, 3 0, 3 -1, 5 -1, 5 0, 6 0, 7 -1, 10 -1, 10 0, 12 -3, 1 -3))"));
        assertNull(result);

        // intersection is POINT (10 0), LINESTRING (1 0, 3 0), LINESTRING (5 0, 6 0)
        // POLYGON ((10 3.32105421863856, 8.332506124004496 3.936744265159977, 10 4.060919341031982,
        // 10 3.32105421863856)))
        result =
                clip._run(
                        fromWKT(
                                "POLYGON ((1 -3, 1 0, 3 0, 3 -1, 5 -1, 5 0, 6 0, 7 -1, 10 -1, 10 0, 10.993923885434695 -1.490885828152043, 11.834681129445318 2.6436334939202886, 8.332506124004496 3.936744265159977, 13.397189978026608 4.313901573438219, 12 -3, 1 -3))"));
        assertEquals(
                result,
                fromWKT(
                        "POLYGON ((10 3.32105421863856, 8.332506124004496 3.936744265159977, 10 4.060919341031982, 10 3.32105421863856)))"));

        // intersection is POINT (10 0) LINESTRING (1 0, 3 0) LINESTRING (5 0, 6 0)
        // POLYGON ((10 3.32105421863856, 8.332506124004496 3.936744265159977, 10 4.399937008492062,
        // 10 3.32105421863856))
        // POLYGON ((10 5.774894160989392, 9.03294112509266 6.576845423107674, 10 6.680458873990603,
        // 10 5.774894160989392)))
        result =
                clip._run(
                        fromWKT(
                                "POLYGON ((1 -3, 1 0, 3 0, 3 -1, 5 -1, 5 0, 6 0, 7 -1, 10 -1, 10 0, 10.993923885434695 -1.490885828152043, 11.834681129445318 2.6436334939202886, 8.332506124004496 3.936744265159977, 11.242005359293794 4.744938497184782, 9.03294112509266 6.576845423107674, 12.050199591318599 6.900123115917596, 13.397189978026608 4.313901573438219, 12 -3, 1 -3))"));
        assertEquals(
                result,
                fromWKT(
                        "MULTIPOLYGON (((10 3.32105421863856, 8.332506124004496 3.936744265159977, 10 4.399937008492062, 10 3.32105421863856)), ((10 5.774894160989392, 9.03294112509266 6.576845423107674, 10 6.680458873990603, 10 5.774894160989392)))"));
    }

    // points are easy - either they're in the envelop or not
    @Test
    public void testClipDegenerativePoint() throws Exception {
        ClipRemoveDegenerateGeometries clip =
                new ClipRemoveDegenerateGeometries(new Envelope(0, 10, 0, 10));

        Geometry result = clip._run(fromWKT("MULTIPOINT(1 1, 2 2)"));
        assertEquals(result, fromWKT("MULTIPOINT(1 1, 2 2)"));
    }

    /*
     * Some datasets will have GeometryCollection geometrys
     */
    @Test
    public void testClipDegenerativeGC() throws Exception {
        ClipRemoveDegenerateGeometries clip =
                new ClipRemoveDegenerateGeometries(new Envelope(0, 10, 0, 10));

        // intersection is POINT (10 0) LINESTRING (1 0, 3 0) LINESTRING (5 0, 6 0)
        // POLYGON ((10 3.32105421863856, 8.332506124004496 3.936744265159977, 10 4.399937008492062,
        // 10 3.32105421863856))
        // POLYGON ((10 5.774894160989392, 9.03294112509266 6.576845423107674, 10 6.680458873990603,
        // 10 5.774894160989392)))
        // NOTE: geometrycollection is the polygon, from above, twice
        Geometry result =
                clip._run(
                        fromWKT(
                                "GEOMETRYCOLLECTION(POLYGON ((1 -3, 1 0, 3 0, 3 -1, 5 -1, 5 0, 6 0, 7 -1, 10 -1, 10 0, 10.993923885434695 -1.490885828152043, 11.834681129445318 2.6436334939202886, 8.332506124004496 3.936744265159977, 11.242005359293794 4.744938497184782, 9.03294112509266 6.576845423107674, 12.050199591318599 6.900123115917596, 13.397189978026608 4.313901573438219, 12 -3, 1 -3)),POLYGON ((1 -3, 1 0, 3 0, 3 -1, 5 -1, 5 0, 6 0, 7 -1, 10 -1, 10 0, 10.993923885434695 -1.490885828152043, 11.834681129445318 2.6436334939202886, 8.332506124004496 3.936744265159977, 11.242005359293794 4.744938497184782, 9.03294112509266 6.576845423107674, 12.050199591318599 6.900123115917596, 13.397189978026608 4.313901573438219, 12 -3, 1 -3)) )"));
        assertEquals(
                result,
                fromWKT(
                        "GEOMETRYCOLLECTION(MULTIPOLYGON (((10 3.32105421863856, 8.332506124004496 3.936744265159977, 10 4.399937008492062, 10 3.32105421863856)), ((10 5.774894160989392, 9.03294112509266 6.576845423107674, 10 6.680458873990603, 10 5.774894160989392))),MULTIPOLYGON (((10 3.32105421863856, 8.332506124004496 3.936744265159977, 10 4.399937008492062, 10 3.32105421863856)), ((10 5.774894160989392, 9.03294112509266 6.576845423107674, 10 6.680458873990603, 10 5.774894160989392))))"));

        // these lines dont intersect the envelope
        result =
                clip._run(
                        fromWKT(
                                "GEOMETRYCOLLECTION(LINESTRING(-1 -1,-2 -2),LINESTRING(-1 -1,-2 -2))"));
        assertNull(result);
    }

    public Geometry fromWKT(String wkt) throws ParseException {
        return new WKTReader().read(wkt);
    }
}
