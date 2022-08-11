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

package org.geoserver.wfs;

import java.util.Arrays;
import java.util.List;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredObjects;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.MaxVisitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

public class FeatureCollectionDelegationTest extends GeoServerSystemTestSupport {
    private final WrapperPolicy policy =
            WrapperPolicy.readOnlyHide(new AccessLimits(CatalogMode.HIDE));
    private static final String FEATURE_TYPE_NAME = "testType";
    private FeatureVisitor lastVisitor = null;

    // These collections will delegate the MaxVisitor
    // As an example, ReTypingFeatureCollections may not delegate.
    private List<SimpleFeatureCollection> maxVisitorCollections;

    // These collection will delegate the CountVisitor
    private List<SimpleFeatureCollection> countVisitorCollections;

    @Before
    public void setUp() throws Exception {
        lastVisitor = null;

        GeometryFactory fac = new GeometryFactory();
        Point p = fac.createPoint(new Coordinate(8, 9));

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(FEATURE_TYPE_NAME);
        builder.add("geom", Point.class);

        SimpleFeatureType ft = builder.buildFeatureType();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(ft);
        b.add(p);

        ListFeatureCollection visitorCollection =
                new ListFeatureCollection(ft) {
                    public void accepts(FeatureVisitor visitor, ProgressListener progress) {
                        lastVisitor = visitor;
                    };

                    @Override
                    public SimpleFeatureCollection subCollection(Filter filter) {
                        if (filter == Filter.INCLUDE) {
                            return this;
                        } else {
                            return super.subCollection(filter);
                        }
                    }
                };
        SimpleFeatureSource featureSource = DataUtilities.source(visitorCollection);

        maxVisitorCollections =
                Arrays.asList(
                        new FeatureSizeFeatureCollection(
                                visitorCollection, featureSource, Query.ALL),
                        new FilteringSimpleFeatureCollection(visitorCollection, Filter.INCLUDE));
        countVisitorCollections =
                Arrays.asList(
                        new FeatureSizeFeatureCollection(
                                visitorCollection, featureSource, Query.ALL),
                        new FilteringSimpleFeatureCollection(visitorCollection, Filter.INCLUDE),
                        new RetypingFeatureCollection(
                                visitorCollection, visitorCollection.getSchema()),
                        SecuredObjects.secure(visitorCollection, policy));
    }

    @Test
    public void testMaxVisitorDelegation() {
        MaxVisitor visitor =
                new MaxVisitor(CommonFactoryFinder.getFilterFactory2().property("value"));
        assertOptimalVisit(visitor, maxVisitorCollections);
    }

    @Test
    public void testCountVisitorDelegation() {
        FeatureVisitor visitor = new CountVisitor();
        assertOptimalVisit(visitor, countVisitorCollections);
    }

    private void assertOptimalVisit(
            FeatureVisitor visitor, List<SimpleFeatureCollection> collections) {
        collections.forEach(
                simpleFeatureCollection -> {
                    try {
                        lastVisitor = null;
                        simpleFeatureCollection.accepts(visitor, null);
                    } catch (Exception e) {
                        Assert.fail();
                    }
                    Assert.assertSame(lastVisitor, visitor);
                });
    }
}
