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

package org.geoserver.kml.utils;

import static org.geoserver.kml.utils.KmlCentroidOptions.CLIP;
import static org.geoserver.kml.utils.KmlCentroidOptions.CONTAIN;
import static org.geoserver.kml.utils.KmlCentroidOptions.SAMPLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

public class KmlCentroidBuilderTest {

    Geometry cShapeGeom;

    @Before
    public void setUp() throws Exception {
        cShapeGeom =
                new WKTReader()
                        .read(
                                "POLYGON ((-112.534433451864 43.8706532611928,-112.499157652296 44.7878240499628,-99.6587666095152 44.7878240499628,-99.7242788087131 43.2155312692142,-111.085391877449 43.099601544023,-110.744593363875 36.1862602686501,-98.6760836215473 35.9436771582516,-98.7415958207452 33.5197257879307,-111.77852346112 33.9783111823157,-111.758573671673 34.6566040234952,-113.088767445077 34.7644575726901,-113.023255245879 43.8706532611928,-112.534433451864 43.8706532611928))");
    }

    @Test
    public void testSampleForPoint() throws Exception {
        Geometry g = cShapeGeom;

        KmlCentroidOptions opts1 =
                KmlCentroidOptions.create(ImmutableMap.of(CONTAIN, "true", SAMPLE, "2"));
        KmlCentroidOptions opts2 =
                KmlCentroidOptions.create(ImmutableMap.of(CONTAIN, "true", SAMPLE, "10"));

        KmlCentroidBuilder builder = new KmlCentroidBuilder();

        Coordinate c = builder.geometryCentroid(g, null, opts1);
        assertFalse(g.contains(g.getFactory().createPoint(c)));

        c = builder.geometryCentroid(g, null, opts2);
        assertTrue(g.contains(g.getFactory().createPoint(c)));
    }

    @Test
    public void testClip() {
        Geometry g = cShapeGeom;
        KmlCentroidOptions opts1 = KmlCentroidOptions.create(ImmutableMap.of());
        KmlCentroidOptions opts2 = KmlCentroidOptions.create(ImmutableMap.of(CLIP, "true"));
        opts2.isClip();

        KmlCentroidBuilder builder = new KmlCentroidBuilder();

        Coordinate c = builder.geometryCentroid(g, null, opts1);
        assertFalse(g.contains(g.getFactory().createPoint(c)));

        Envelope bbox =
                new Envelope(
                        -106.603059724489, -103.655010760585, 34.6334331742943, 36.9918723454173);
        c = builder.geometryCentroid(g, bbox, opts2);
        assertTrue(g.contains(g.getFactory().createPoint(c)));
    }

    @Test
    public void testCaseInsensitivity() {
        KmlCentroidOptions opts =
                KmlCentroidOptions.create(
                        ImmutableMap.of(
                                CONTAIN.toUpperCase(),
                                "true",
                                CLIP.toUpperCase(),
                                "true",
                                SAMPLE.toUpperCase(),
                                "12"));
        assertTrue(opts.isContain());
        assertTrue(opts.isClip());
        assertEquals(12, opts.getSamples());
    }
}
