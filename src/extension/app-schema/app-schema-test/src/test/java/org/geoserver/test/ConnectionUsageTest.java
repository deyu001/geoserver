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

package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.DataAccessMappingFeatureIterator;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.MappingFeatureCollection;
import org.geotools.data.complex.MappingFeatureSource;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.joining.JoiningNestedAttributeMapping;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.PropertyIsEqualTo;

public class ConnectionUsageTest extends AbstractAppSchemaTestSupport {

    private FilterFactoryImplNamespaceAware ff;

    private MockConnectionLifecycleListener connListener;

    private MappingFeatureSource mappingFs;

    private Transaction transaction;

    private JDBCDataStore sourceDataStore;

    private int nestedFeaturesCount;

    @Override
    protected ConnectionUsageMockData createTestData() {
        return new ConnectionUsageMockData();
    }

    @Before
    public void setUp() throws Exception {
        nestedFeaturesCount = 0;

        init();
    }

    @Test
    public void testConnectionSharedAmongNestedIterators() throws Exception {
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("C_nested_second"));

        try (FeatureIterator fIt = mappingFs.getFeatures(equals).features()) {
            testNestedIterators(fIt);
        }

        assertEquals(1, connListener.actionCountByDataStore.size());
        assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
        assertEquals(2, nestedFeaturesCount);
    }

    @Test
    public void testConnectionSharedIfTransactionIs() throws Exception {
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("C_nested_second"));

        FeatureCollection fc = mappingFs.getFeatures(equals);
        assertTrue(fc instanceof MappingFeatureCollection);
        MappingFeatureCollection mfc = (MappingFeatureCollection) fc;

        try (Transaction tx = new DefaultTransaction()) {

            // explicitly specifying the transaction to use
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(0, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(2, nestedFeaturesCount);

            // open another iterator using the same transaction
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            // no new connection has been opened
            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(0, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(4, nestedFeaturesCount);
        }

        // at this point transaction has been closed and so the connection has been released
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
    }

    @Test
    public void testConnectionNotSharedIfTransactionIsNot() throws Exception {
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("C_nested_second"));

        FeatureCollection fc = mappingFs.getFeatures(equals);
        assertTrue(fc instanceof MappingFeatureCollection);
        MappingFeatureCollection mfc = (MappingFeatureCollection) fc;

        try (Transaction tx = new DefaultTransaction()) {

            // explicitly specifying the transaction to use
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(0, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(2, nestedFeaturesCount);
        }

        // at this point transaction has been closed and so the connection has been released
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);

        try (Transaction tx = new DefaultTransaction()) {
            // open another iterator using a different transaction
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            // a new connection has been opened
            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(2, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(4, nestedFeaturesCount);
        }

        // at this point transaction has been closed and so the connection has been released
        assertEquals(2, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
    }

    /**
     * This test uses a conditionally joined feature with a broken mapping configuration to trigger
     * a RuntimeException when iterator.next() is called and verifies that no connection leak
     * occurs, even if the caller forgets to catch unchecked exceptions.
     */
    @Test
    @SuppressWarnings("TryFailThrowable")
    public void testNoConnectionLeakIfExceptionThrown() throws Exception {
        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(mappingFs.getMapping().getNamespaces());

        // this filter selects the feature with GML ID "scp.1", the only one which joins the broken
        // feature type ex:ConnectionUsageThirdNested
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("A_nested_second"));

        try (FeatureIterator fIt = mappingFs.getFeatures(equals).features()) {
            testNestedIterators(fIt);
            fail("Expected exception was not thrown!");
        } catch (Throwable e) {
            // This was expected
        }

        // iterator should have been automatically closed, so no connection should be leaked
        assertEquals(1, connListener.actionCountByDataStore.size());
        assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
    }

    private void init() throws Exception {
        connListener = new MockConnectionLifecycleListener();

        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName("ex", "ConnectionUsageParent");
        assertNotNull(typeInfo);

        FeatureSource fs = typeInfo.getFeatureSource(new NullProgressListener(), null);
        initMappingFS(fs);

        FeatureSource sourceFs = mappingFs.getMapping().getSource();

        // The test only makes sense if we have a databae backend and joining is enabled
        assumeTrue(
                sourceFs.getDataStore() instanceof JDBCDataStore
                        && AppSchemaDataAccessConfigurator.isJoining());

        sourceDataStore = (JDBCDataStore) sourceFs.getDataStore();

        ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(mappingFs.getMapping().getNamespaces());

        // retrieve one feature to trigger all necessary initialization steps so they don't
        // interfere
        // with the test's outcome
        PropertyIsEqualTo equals = ff.equals(ff.property("gml:name"), ff.literal("C_parent"));

        try (FeatureIterator fIt = mappingFs.getFeatures(equals).features()) {
            assertTrue(fIt.hasNext());
            assertNotNull(fIt.next());
        }

        // register connection listener
        sourceDataStore.getConnectionLifecycleListeners().add(connListener);
    }

    @SuppressWarnings("unchecked")
    private void initMappingFS(FeatureSource fs) {
        if (fs instanceof DecoratingFeatureSource) {
            mappingFs =
                    ((DecoratingFeatureSource<FeatureType, Feature>) fs)
                            .unwrap(MappingFeatureSource.class);
        } else {
            assertTrue(fs instanceof MappingFeatureSource);
            mappingFs = (MappingFeatureSource) fs;
        }
    }

    private void testNestedIterators(FeatureIterator iterator) throws IOException {
        assertTrue(iterator instanceof DataAccessMappingFeatureIterator);
        DataAccessMappingFeatureIterator mappingIt = (DataAccessMappingFeatureIterator) iterator;
        assertTrue(iterator.hasNext());

        // fetch one feature to trigger opening of nested iterators
        Feature f = iterator.next();
        assertNotNull(f);

        FeatureSource mappedSource = mappingIt.getMappedSource();
        assertSame(mappedSource.getDataStore(), sourceDataStore);
        assertNotNull(mappingIt.getTransaction());
        transaction = mappingIt.getTransaction();

        testNestedIteratorsRecursively(mappingFs.getMapping(), mappingIt);
    }

    private void testNestedIteratorsRecursively(
            FeatureTypeMapping mapping, DataAccessMappingFeatureIterator mappingIt)
            throws IOException {
        List<AttributeMapping> attrs = mapping.getAttributeMappings();
        assertNotNull(attrs);
        assertFalse(attrs.isEmpty());

        for (AttributeMapping attr : attrs) {
            if (attr instanceof JoiningNestedAttributeMapping) {
                nestedFeaturesCount++;

                JoiningNestedAttributeMapping joiningNestedAttr =
                        (JoiningNestedAttributeMapping) attr;
                Map<Name, DataAccessMappingFeatureIterator> nestedFeatureIterators =
                        joiningNestedAttr.getNestedFeatureIterators(mappingIt);
                assertNotNull(nestedFeatureIterators);

                if (!nestedFeatureIterators.isEmpty()) {
                    assertEquals(1, nestedFeatureIterators.size());

                    FeatureTypeMapping nestedMapping =
                            joiningNestedAttr.getFeatureTypeMapping(null);

                    try (DataAccessMappingFeatureIterator nestedIt =
                            nestedFeatureIterators.values().iterator().next()) {

                        FeatureSource nestedMappedSource = nestedIt.getMappedSource();
                        assertEquals(sourceDataStore, nestedMappedSource.getDataStore());
                        assertEquals(transaction, nestedIt.getTransaction());

                        testNestedIteratorsRecursively(nestedMapping, nestedIt);
                    }
                }
            }
        }
    }
}
