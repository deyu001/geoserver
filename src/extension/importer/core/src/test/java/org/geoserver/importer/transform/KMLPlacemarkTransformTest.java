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

package org.geoserver.importer.transform;

import java.util.ArrayList;
import java.util.List;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.Folder;
import org.geotools.styling.FeatureTypeStyle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public class KMLPlacemarkTransformTest {

    private KMLPlacemarkTransform kmlPlacemarkTransform;

    private SimpleFeatureType origType;

    private SimpleFeatureType transformedType;

    @Before
    public void setUp() throws Exception {
        kmlPlacemarkTransform = new KMLPlacemarkTransform();

        SimpleFeatureTypeBuilder origBuilder = new SimpleFeatureTypeBuilder();
        origBuilder.setName("origtype");
        origBuilder.add("name", String.class);
        origBuilder.add("description", String.class);
        origBuilder.add("LookAt", Point.class);
        origBuilder.add("Region", LinearRing.class);
        origBuilder.add("Style", FeatureTypeStyle.class);
        origBuilder.add("Geometry", Geometry.class);
        origBuilder.setDefaultGeometry("Geometry");
        origType = origBuilder.buildFeatureType();

        SimpleFeatureTypeBuilder transformedBuilder = new SimpleFeatureTypeBuilder();
        transformedBuilder.setName("transformedtype");
        transformedBuilder.add("name", String.class);
        transformedBuilder.add("description", String.class);
        transformedBuilder.add("LookAt", Point.class);
        transformedBuilder.add("Region", LinearRing.class);
        transformedBuilder.add("Style", String.class);
        transformedBuilder.add("Geometry", Geometry.class);
        transformedBuilder.setDefaultGeometry("Geometry");
        transformedBuilder.add("Folder", String.class);
        transformedType = transformedBuilder.buildFeatureType();
    }

    @Test
    public void testFeatureType() throws Exception {
        SimpleFeatureType result = kmlPlacemarkTransform.convertFeatureType(origType);
        assertBinding(result, "LookAt", Point.class);
        assertBinding(result, "Region", LinearRing.class);
        assertBinding(result, "Folder", String.class);
    }

    private void assertBinding(SimpleFeatureType ft, String attr, Class<?> expectedBinding) {
        AttributeDescriptor descriptor = ft.getDescriptor(attr);
        Class<?> binding = descriptor.getType().getBinding();
        Assert.assertEquals(expectedBinding, binding);
    }

    @Test
    public void testGeometry() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        GeometryFactory gf = new GeometryFactory();
        fb.set("Geometry", gf.createPoint(new Coordinate(3d, 4d)));
        SimpleFeature feature = fb.buildFeature("testgeometry");
        Assert.assertEquals(
                "Unexpected Geometry class",
                Point.class,
                feature.getAttribute("Geometry").getClass());
        Assert.assertEquals(
                "Unexpected default geometry",
                Point.class,
                feature.getDefaultGeometry().getClass());
        SimpleFeature result = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        Assert.assertEquals(
                "Invalid Geometry class", Point.class, result.getAttribute("Geometry").getClass());
        Assert.assertEquals(
                "Unexpected default geometry",
                Point.class,
                feature.getDefaultGeometry().getClass());
    }

    @Test
    public void testLookAtProperty() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        GeometryFactory gf = new GeometryFactory();
        Coordinate c = new Coordinate(3d, 4d);
        fb.set("LookAt", gf.createPoint(c));
        SimpleFeature feature = fb.buildFeature("testlookat");
        Assert.assertEquals(
                "Unexpected LookAt attribute class",
                Point.class,
                feature.getAttribute("LookAt").getClass());
        SimpleFeature result = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        Assert.assertEquals(
                "Invalid LookAt attribute class",
                Point.class,
                result.getAttribute("LookAt").getClass());
    }

    @Test
    public void testFolders() throws Exception {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(origType);
        List<Folder> folders = new ArrayList<>(2);
        folders.add(new Folder("foo"));
        folders.add(new Folder("bar"));
        fb.featureUserData("Folder", folders);
        SimpleFeature feature = fb.buildFeature("testFolders");
        SimpleFeature newFeature = kmlPlacemarkTransform.convertFeature(feature, transformedType);
        Assert.assertEquals("foo -> bar", newFeature.getAttribute("Folder"));
    }
}
