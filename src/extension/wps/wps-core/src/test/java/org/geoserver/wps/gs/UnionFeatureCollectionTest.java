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
import org.geotools.process.vector.UnionFeatureCollection;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

public class UnionFeatureCollectionTest extends WPSTestSupport {

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
    GeometryFactory gf = new GeometryFactory();

    @Test
    @SuppressWarnings("PMD.UseAssertEqualsInsteadOfAssertTrue") // JTS geometry equality
    public void testExecute() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        DefaultFeatureCollection secondFeatures =
                new DefaultFeatureCollection(null, b.getFeatureType());
        Geometry[] firstArrayGeometry = new Geometry[5];
        Geometry[] secondArrayGeometry = new Geometry[5];
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate firstArray[] = new Coordinate[5];
            for (int j = 0; j < 4; j++) {
                firstArray[j] = new Coordinate(j + numFeatures, j + numFeatures);
            }
            firstArray[4] = new Coordinate(0 + numFeatures, 0 + numFeatures);
            LinearRing shell = gf.createLinearRing(new CoordinateArraySequence(firstArray));
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            firstArrayGeometry[numFeatures] = gf.createPolygon(shell, null);
            features.add(b.buildFeature(numFeatures + ""));
        }
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[5];
            for (int j = 0; j < 4; j++) {
                array[j] = new Coordinate(j + numFeatures + 50, j + numFeatures + 50);
            }
            array[4] = new Coordinate(0 + numFeatures + 50, 0 + numFeatures + 50);
            LinearRing shell = gf.createLinearRing(new CoordinateArraySequence(array));
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            secondArrayGeometry[numFeatures] = gf.createPolygon(shell, null);
            secondFeatures.add(b.buildFeature(numFeatures + ""));
        }
        UnionFeatureCollection process = new UnionFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, secondFeatures);
        assertEquals(5, output.size());

        Geometry[] union = new Geometry[10];
        for (int i = 0; i < firstArrayGeometry.length; i++) {
            union[i] = firstArrayGeometry[i];
        }
        for (int i = 0; i < secondArrayGeometry.length; i++) {
            union[i + 5] = secondArrayGeometry[i];
        }
        GeometryCollection unionCollection = new GeometryCollection(union, new GeometryFactory());
        try (SimpleFeatureIterator iterator = output.features()) {
            for (int h = 0; h < unionCollection.getNumGeometries(); h++) {
                Geometry expected = unionCollection.getGeometryN(h);
                SimpleFeature sf = iterator.next();
                assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
            }
        }
    }
}
