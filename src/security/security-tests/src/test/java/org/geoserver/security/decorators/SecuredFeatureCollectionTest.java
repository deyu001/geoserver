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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

public class SecuredFeatureCollectionTest extends SecureObjectsTest {

    private SimpleFeatureStore store;

    private SimpleFeature feature;

    @Before
    public void setUp() throws Exception {
        SimpleFeatureType schema = createNiceMock(SimpleFeatureType.class);
        expect(schema.getTypeName()).andReturn("testSchema").anyTimes();
        expect(schema.getName()).andReturn(new NameImpl("testSchema")).anyTimes();
        replay(schema);

        feature = createNiceMock(SimpleFeature.class);
        expect(feature.getID()).andReturn("testSchema.1").anyTimes();
        expect(feature.getType()).andReturn(schema).anyTimes();
        expect(feature.getFeatureType()).andReturn(schema).anyTimes();
        replay(feature);

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(feature);

        store = createNiceMock(SimpleFeatureStore.class);
        expect(store.getSchema()).andReturn(schema).anyTimes();
        expect(store.getFeatures()).andReturn(fc).anyTimes();
        expect(store.getFeatures((Filter) anyObject())).andReturn(fc).anyTimes();
        expect(store.getFeatures((Query) anyObject())).andReturn(fc).anyTimes();
        replay(store);
        /*expect(fc.features()).andReturn(it).anyTimes();
        expect(fc.sort(sort)).andReturn(fc).anyTimes();
        expect(fc.subCollection(Filter.INCLUDE)).andReturn(fc).anyTimes();
        expect(fc.getSchema()).andReturn(schema).anyTimes();
        replay(fc);*/
    }

    @Test
    public void testHide() throws Exception {

        SecuredFeatureStore<SimpleFeatureType, SimpleFeature> ro =
                new SecuredFeatureStore<>(store, WrapperPolicy.hide(null));

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(feature);

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.addFeatures(fc);
            fail("Should have failed with an UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // ok
        }

        try {
            ro.removeFeatures(Filter.INCLUDE);
            fail("Should have failed with an UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // ok
        }
    }

    @Test
    public void testReadOnly() throws Exception {
        SecuredFeatureStore<SimpleFeatureType, SimpleFeature> ro =
                new SecuredFeatureStore<>(store, WrapperPolicy.readOnlyHide(null));

        // let's check the iterator, should allow read but not remove
        FeatureCollection rofc = ro.getFeatures();
        try (FeatureIterator roit = rofc.features()) {
            roit.hasNext();
            roit.next();

            // check derived collections are still read only and share the same
            // challenge policy
            SecuredFeatureCollection sorted =
                    (SecuredFeatureCollection) rofc.sort(SortBy.NATURAL_ORDER);
            assertEquals(ro.policy, sorted.policy);
            rofc.subCollection(Filter.INCLUDE);
            assertEquals(ro.policy, sorted.policy);
        }
    }

    @Test
    public void testChallenge() throws Exception {

        SecuredFeatureStore<SimpleFeatureType, SimpleFeature> ro =
                new SecuredFeatureStore<>(store, WrapperPolicy.readOnlyChallenge(null));

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        fc.add(feature);

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.addFeatures(fc);
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }

        try {
            ro.removeFeatures(Filter.INCLUDE);
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.removeFeatures(ECQL.toFilter("IN ('testSchema.1')"));
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
        try {
            ro.removeFeatures(Filter.EXCLUDE);
            fail("Should have failed with a spring security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }

        // let's check the iterator, should allow read but not remove
        FeatureCollection rofc = ro.getFeatures();
        try (FeatureIterator roit = rofc.features()) {
            roit.hasNext();
            roit.next();

            // check derived collections are still read only and share the same
            // challenge policy
            SecuredFeatureCollection sorted =
                    (SecuredFeatureCollection) rofc.sort(SortBy.NATURAL_ORDER);
            assertEquals(ro.policy, sorted.policy);
            rofc.subCollection(Filter.INCLUDE);
            assertEquals(ro.policy, sorted.policy);
        }
    }
}
