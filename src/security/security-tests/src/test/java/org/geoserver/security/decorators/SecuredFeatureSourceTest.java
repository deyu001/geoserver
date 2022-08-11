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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

public class SecuredFeatureSourceTest extends SecureObjectsTest {

    @Test
    public void testReadOnlyFeatureSourceDataStore() throws Exception {
        // build up the mock
        DataStore ds = createNiceMock(DataStore.class);
        replay(ds);
        SimpleFeatureSource fs = createNiceMock(SimpleFeatureSource.class);
        SimpleFeatureCollection fc = createNiceMock(SimpleFeatureCollection.class);
        expect(fc.getSchema()).andReturn(createNiceMock(SimpleFeatureType.class));
        replay(fc);
        expect(fs.getDataStore()).andReturn(ds);
        expect(fs.getFeatures()).andReturn(fc).anyTimes();
        expect(fs.getFeatures((Filter) anyObject())).andReturn(fc).anyTimes();
        expect(fs.getFeatures((Query) anyObject())).andReturn(fc).anyTimes();
        replay(fs);

        SecuredFeatureSource<SimpleFeatureType, SimpleFeature> ro =
                new SecuredFeatureSource<>(fs, WrapperPolicy.hide(null));
        assertTrue(ro.getDataStore() instanceof ReadOnlyDataStore);
        ro.getFeatures();
        assertTrue(ro.policy.isHide());
        assertTrue(ro.getFeatures(Filter.INCLUDE) instanceof SecuredFeatureCollection);
        assertTrue(ro.getFeatures(new Query()) instanceof SecuredFeatureCollection);
    }

    @Test
    public void testReadOnlyFeatureStore() throws Exception {
        // build up the mock
        SimpleFeatureType schema = createNiceMock(SimpleFeatureType.class);
        expect(schema.getName()).andReturn(new NameImpl("testFT"));
        replay(schema);
        SimpleFeatureStore fs = createNiceMock(SimpleFeatureStore.class);
        expect(fs.getSchema()).andReturn(schema);
        replay(fs);

        SecuredFeatureStore<SimpleFeatureType, SimpleFeature> ro =
                new SecuredFeatureStore<>(fs, WrapperPolicy.readOnlyChallenge(null));
        try {
            ro.addFeatures(createNiceMock(SimpleFeatureCollection.class));
            fail("This should have thrown a security exception");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
    }

    @Test
    public <T extends FeatureType, F extends Feature> void testReadOnlyFeatureSourceDataAccess()
            throws Exception {
        // build the mock up
        @SuppressWarnings("unchecked")
        DataAccess<T, F> da = createNiceMock(DataAccess.class);
        replay(da);
        @SuppressWarnings("unchecked")
        FeatureSource<T, F> fs = createNiceMock(FeatureSource.class);
        expect(fs.getDataStore()).andReturn(da);
        replay(fs);

        SecuredFeatureSource<T, F> ro =
                new SecuredFeatureSource<>(fs, WrapperPolicy.readOnlyChallenge(null));
        assertTrue(ro.getDataStore() instanceof ReadOnlyDataAccess);
    }

    @Test
    public <T extends FeatureType, F extends Feature>
            void testSecuredFeatureSourceLoggingWithComplex() throws Exception {
        // build up the mock
        @SuppressWarnings("unchecked")
        T schema = (T) createNiceMock(ComplexFeatureTypeImpl.class);
        expect(schema.getName()).andReturn(new NameImpl("testComplexFt"));
        @SuppressWarnings("unchecked")
        List<PropertyDescriptor> descriptors = createNiceMock(List.class);
        expect(descriptors.size()).andReturn(3).anyTimes();
        replay(descriptors);
        expect(schema.getDescriptors()).andReturn(descriptors).anyTimes();
        replay(schema);
        @SuppressWarnings("unchecked")
        DataAccess<T, F> store = createNiceMock(DataAccess.class);
        replay(store);
        @SuppressWarnings("unchecked")
        FeatureStore<T, F> fStore = createNiceMock(FeatureStore.class);
        expect(fStore.getSchema()).andReturn(schema).anyTimes();
        @SuppressWarnings("unchecked")
        FeatureCollection<T, F> fc = createNiceMock(FeatureCollection.class);
        expect(fStore.getDataStore()).andReturn(store);
        expect(fStore.getFeatures()).andReturn(fc).anyTimes();
        expect(fStore.getFeatures((Filter) anyObject())).andReturn(fc).anyTimes();
        expect(fStore.getFeatures((Query) anyObject())).andReturn(fc).anyTimes();
        expect(fc.getSchema()).andReturn(schema).anyTimes();
        replay(fStore);
        replay(fc);

        // custom LogHandler to intercept log messages
        class LogHandler extends Handler {
            List<String> messages = new ArrayList<>();

            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        }
        Logger logger = Logging.getLogger(SecuredFeatureSource.class);
        LogHandler customLogHandler = new LogHandler();
        logger.addHandler(customLogHandler);
        customLogHandler.setLevel(Level.SEVERE);
        logger.addHandler(customLogHandler);
        try {
            SecuredFeatureStore ro =
                    new SecuredFeatureStore<>(fStore, WrapperPolicy.readOnlyHide(null));
            Query q = new Query("testComplextFt");
            List<PropertyName> pnames = new ArrayList<>(1);
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            pnames.add(ff.property("someProperty"));
            q.setProperties(pnames);
            ro.getFeatures(q);
            String notExpectedMessage =
                    "Complex store returned more properties than allowed by security (because they are required by the schema). Either the security setup is broken or you have a security breach";
            assertFalse(customLogHandler.messages.contains(notExpectedMessage));
        } finally {
            logger.removeHandler(customLogHandler);
        }
    }
}
