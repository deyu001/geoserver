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
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.DataAccessMappingFeatureIterator;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.MappingFeatureSource;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.joining.JoiningNestedAttributeMapping;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.PropertyIsLike;

public class FeatureChainingSharedConnectionTest extends AbstractAppSchemaTestSupport {

    private int nestedFeaturesCount;

    private Transaction mfTransaction;

    private Transaction guTransaction;

    private JDBCDataStore mfSourceDataStore;

    private JDBCDataStore guSourceDataStore;

    @Override
    protected FeatureChainingMockData createTestData() {
        return new FeatureChainingMockData();
    }

    @Before
    public void setUp() {

        nestedFeaturesCount = 0;
        mfTransaction = null;
        guTransaction = null;
        mfSourceDataStore = null;
        guSourceDataStore = null;
    }

    /**
     * Tests that connection is automatically shared among top feature iterators and nested feature
     * iterators, but only in the context of a single AppSchemaDataAccess instance.
     *
     * <p>What this means in practice is:
     *
     * <ul>
     *   <li><em>MappedFeature</em> and <em>GeologicUnit</em> belong to different
     *       AppSchemaDataAccess instances, so an iterator on MappedFeature will open a new database
     *       connection to retrieve the nested GeologicUnit features
     *   <li><em>GeologicUnit, CompositionPart, ControlledConcept, CGI_TermValue</em> belong to the
     *       same AppSchemaDataAccess instances, so an iterator on GeologicUnit will NOT open a new
     *       database connection to retrieve the nested <em>CompositionPart, ControlledConcept,
     *       CGI_TermValue</em> "features"
     * </ul>
     */
    @Test
    public void testSharedConnection() throws Exception {
        MockConnectionLifecycleListener connListener = new MockConnectionLifecycleListener();

        FeatureTypeInfo mfTypeInfo = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        assertNotNull(mfTypeInfo);
        FeatureTypeInfo guTypeInfo = getCatalog().getFeatureTypeByName("gsml", "GeologicUnit");
        assertNotNull(guTypeInfo);

        FeatureSource fs = mfTypeInfo.getFeatureSource(new NullProgressListener(), null);
        MappingFeatureSource mfFs = unwrap(fs);

        FeatureSource mfSourceFs = mfFs.getMapping().getSource();

        fs = guTypeInfo.getFeatureSource(new NullProgressListener(), null);
        MappingFeatureSource guFs = unwrap(fs);
        FeatureSource guSourceFs = guFs.getMapping().getSource();

        // The test only makes sense if we have a databae backend and joining is enabled
        assumeTrue(
                mfSourceFs.getDataStore() instanceof JDBCDataStore
                        && guSourceFs.getDataStore() instanceof JDBCDataStore
                        && AppSchemaDataAccessConfigurator.isJoining());

        mfSourceDataStore = (JDBCDataStore) mfSourceFs.getDataStore();
        guSourceDataStore = (JDBCDataStore) guSourceFs.getDataStore();

        // retrieve one feature to trigger all necessary initialization steps so they don't
        // interfere
        // with the test's outcome
        try (FeatureIterator fIt = mfFs.getFeatures().features()) {
            assertTrue(fIt.hasNext());
            assertNotNull(fIt.next());
        }

        // register connection listeners
        mfSourceDataStore.getConnectionLifecycleListeners().add(connListener);
        guSourceDataStore.getConnectionLifecycleListeners().add(connListener);

        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(mfFs.getMapping().getNamespaces());

        PropertyIsLike like =
                ff.like(
                        ff.property("gsml:specification/gsml:GeologicUnit/gml:description"),
                        "*sedimentary*");

        try (DataAccessMappingFeatureIterator mappingIt =
                (DataAccessMappingFeatureIterator) mfFs.getFeatures(like).features()) {
            assertTrue(mappingIt.hasNext());

            // fetch one feature to trigger opening of nested iterators
            Feature f = mappingIt.next();
            assertNotNull(f);

            FeatureSource mappedSource = mappingIt.getMappedSource();
            assertSame(mappedSource.getDataStore(), mfSourceDataStore);
            assertNotNull(mappingIt.getTransaction());
            mfTransaction = mappingIt.getTransaction();

            testSharedConnectionRecursively(
                    mfFs.getMapping(), mappingIt, mfSourceDataStore, mfTransaction);
        }

        assertEquals(2, connListener.actionCountByDataStore.size());
        assertTrue(connListener.actionCountByDataStore.containsKey(mfSourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(mfSourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(mfSourceDataStore).releaseCount);
        assertTrue(connListener.actionCountByDataStore.containsKey(guSourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(guSourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(guSourceDataStore).releaseCount);
        assertEquals(8, nestedFeaturesCount);
    }

    @SuppressWarnings("unchecked")
    private MappingFeatureSource unwrap(FeatureSource fs) {
        MappingFeatureSource mfFs;
        if (fs instanceof DecoratingFeatureSource) {
            mfFs =
                    ((DecoratingFeatureSource<FeatureType, Feature>) fs)
                            .unwrap(MappingFeatureSource.class);
        } else {
            assertTrue(fs instanceof MappingFeatureSource);
            mfFs = (MappingFeatureSource) fs;
        }
        return mfFs;
    }

    @SuppressWarnings("PMD.CloseResource") // weird dance with transaction fields, leaving it alone
    private void testSharedConnectionRecursively(
            FeatureTypeMapping mapping,
            DataAccessMappingFeatureIterator mappingIt,
            DataAccess parentDataStore,
            Transaction parentTx)
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

                    DataAccessMappingFeatureIterator nestedIt =
                            nestedFeatureIterators.values().iterator().next();

                    Transaction expectedTx = parentTx;
                    DataAccess expectedDataStore = parentDataStore;
                    String nestedFeatureName = nestedMapping.getTargetFeature().getLocalName();
                    if (nestedFeatureName.equals("GeologicUnit")) {
                        expectedDataStore = guSourceDataStore;
                        if (guTransaction == null) {
                            guTransaction = nestedIt.getTransaction();
                        }
                        expectedTx = guTransaction;
                    } else if (nestedFeatureName.equals("MappedFeature")) {
                        expectedDataStore = mfSourceDataStore;
                        if (mfTransaction == null) {
                            mfTransaction = nestedIt.getTransaction();
                        }
                        expectedTx = mfTransaction;
                    }

                    FeatureSource nestedMappedSource = nestedIt.getMappedSource();
                    assertEquals(expectedDataStore, nestedMappedSource.getDataStore());
                    assertEquals(expectedTx, nestedIt.getTransaction());

                    testSharedConnectionRecursively(
                            nestedMapping, nestedIt, expectedDataStore, expectedTx);
                }
            }
        }
    }
}
