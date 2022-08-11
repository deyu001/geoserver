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

package org.geoserver.security.decorators;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.Arrays;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.MaxVisitor;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.PropertyName;
import org.opengis.util.ProgressListener;

public class SecuredSimpleFeatureCollectionTest {

    FeatureVisitor lastVisitor = null;
    private ListFeatureCollection collection;

    @Before
    public void setup() throws SchemaException {
        GeoServerExtensionsHelper.singleton(
                "secureDataFactory", new DefaultSecureDataFactory(), SecuredObjectFactory.class);
        this.lastVisitor = null;
        SimpleFeatureType originalSchema =
                DataUtilities.createType(
                        "BasicPolygons", "the_geom:MultiPolygon:srid=4326,ID:String,value:int");
        collection =
                new ListFeatureCollection(originalSchema) {
                    public void accepts(FeatureVisitor visitor, ProgressListener progress)
                            throws IOException {
                        lastVisitor = visitor;
                    };
                };
    }

    @Test
    public void testMaxVisitorDelegation() throws SchemaException, IOException {
        MaxVisitor visitor =
                new MaxVisitor(CommonFactoryFinder.getFilterFactory2().property("value"));
        WrapperPolicy policy =
                WrapperPolicy.hide(
                        new VectorAccessLimits(CatalogMode.HIDE, null, null, null, null));
        assertOptimalVisit(visitor, policy);
    }

    @Test
    public void testMaxOnHiddenField() throws SchemaException, IOException {
        MaxVisitor visitor =
                new MaxVisitor(CommonFactoryFinder.getFilterFactory2().property("value"));
        PropertyName geom = CommonFactoryFinder.getFilterFactory2().property("the_geom");
        WrapperPolicy policy =
                WrapperPolicy.hide(
                        new VectorAccessLimits(
                                CatalogMode.HIDE, Arrays.asList(geom), null, null, null));
        SecuredSimpleFeatureCollection secured =
                new SecuredSimpleFeatureCollection(collection, policy);
        secured.accepts(visitor, null);
        assertNull(lastVisitor);
    }

    @Test
    public void testCountVisitorDelegation() throws SchemaException, IOException {
        FeatureVisitor visitor = new CountVisitor();
        WrapperPolicy policy =
                WrapperPolicy.hide(
                        new VectorAccessLimits(CatalogMode.HIDE, null, null, null, null));
        assertOptimalVisit(visitor, policy);
    }

    private void assertOptimalVisit(FeatureVisitor visitor, WrapperPolicy policy)
            throws IOException {
        SecuredSimpleFeatureCollection secured =
                new SecuredSimpleFeatureCollection(collection, policy);
        secured.accepts(visitor, null);
        assertSame(lastVisitor, visitor);
    }
}
