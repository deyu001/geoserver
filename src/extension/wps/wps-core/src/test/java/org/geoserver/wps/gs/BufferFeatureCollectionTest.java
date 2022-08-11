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

package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.vector.BufferFeatureCollection;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

public class BufferFeatureCollectionTest extends WPSTestSupport {

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    @Test
    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue") // JTS geometry equality
    public void testExecutePoint() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int i = 0; i < 2; i++) {
            b.add(gf.createPoint(new Coordinate(i, i)));
            b.add(i);
            features.add(b.buildFeature(i + ""));
        }
        Double distance = Double.valueOf(500);
        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, distance, null);
        assertEquals(2, output.size());

        try (SimpleFeatureIterator iterator = output.features()) {
            for (int i = 0; i < 2; i++) {
                Geometry expected = gf.createPoint(new Coordinate(i, i)).buffer(distance);
                SimpleFeature sf = iterator.next();
                assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
            }
        }
    }

    @Test
    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue") // JTS geometry equality
    public void testExecuteLineString() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 4 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            b.add(gf.createLineString(array));
            b.add(0);
            features.add(b.buildFeature(numFeatures + ""));
        }
        Double distance = Double.valueOf(500);
        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, distance, null);
        assertEquals(5, output.size());
        try (SimpleFeatureIterator iterator = output.features()) {
            for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
                Coordinate[] array = new Coordinate[4];
                int j = 0;
                for (int i = 0 + numFeatures; i < 4 + numFeatures; i++) {
                    array[j] = new Coordinate(i, i);
                    j++;
                }
                Geometry expected = gf.createLineString(array).buffer(distance);
                SimpleFeature sf = iterator.next();
                assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
            }
        }
    }

    @Test
    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue") // JTS geometry equality
    public void testExecutePolygon() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            array[3] = new Coordinate(numFeatures, numFeatures);
            LinearRing shell =
                    new LinearRing(new CoordinateArraySequence(array), new GeometryFactory());
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            features.add(b.buildFeature(numFeatures + ""));
        }
        Double distance = Double.valueOf(500);
        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, distance, null);
        assertEquals(5, output.size());
        try (SimpleFeatureIterator iterator = output.features()) {
            for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
                Coordinate[] array = new Coordinate[4];
                int j = 0;
                for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                    array[j] = new Coordinate(i, i);
                    j++;
                }
                array[3] = new Coordinate(numFeatures, numFeatures);
                LinearRing shell =
                        new LinearRing(new CoordinateArraySequence(array), new GeometryFactory());
                Geometry expected = gf.createPolygon(shell, null).buffer(distance);

                SimpleFeature sf = iterator.next();
                assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
            }
        }
    }

    @Test
    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue") // JTS geometry equality
    public void testExecuteBufferAttribute() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);
        tb.add("buffer", Double.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            array[3] = new Coordinate(numFeatures, numFeatures);
            LinearRing shell =
                    new LinearRing(new CoordinateArraySequence(array), new GeometryFactory());
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            b.add(500);
            features.add(b.buildFeature(numFeatures + ""));
        }

        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, null, "buffer");
        assertEquals(5, output.size());
        try (SimpleFeatureIterator iterator = output.features()) {
            for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
                Coordinate[] array = new Coordinate[4];
                int j = 0;
                for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                    array[j] = new Coordinate(i, i);
                    j++;
                }
                array[3] = new Coordinate(numFeatures, numFeatures);
                LinearRing shell =
                        new LinearRing(new CoordinateArraySequence(array), new GeometryFactory());
                Geometry expected = gf.createPolygon(shell, null).buffer(500);

                SimpleFeature sf = iterator.next();
                assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
            }
        }
    }
}
