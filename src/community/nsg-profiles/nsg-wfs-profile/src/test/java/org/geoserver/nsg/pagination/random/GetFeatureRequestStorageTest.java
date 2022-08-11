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

package org.geoserver.nsg.pagination.random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.wicket.util.file.File;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;

public class GetFeatureRequestStorageTest extends WFS20TestSupport {

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        IndexConfigurationManager ic = applicationContext.getBean(IndexConfigurationManager.class);
        DataStore dataStore = ic.getCurrentDataStore();
        dataStore.dispose();
        super.onTearDown(testData);
    }

    @Test
    public void testCleanOldRequest() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=cdf:Fifteen&resultType=index");
        String resultSetId = doc.getDocumentElement().getAttribute("resultSetID");
        IndexConfigurationManager ic = applicationContext.getBean(IndexConfigurationManager.class);
        SimpleFeature feature =
                getFeatureStore(ic)
                        .getFeatures(CQL.toFilter("ID='" + resultSetId + "'"))
                        .features()
                        .next();
        assertNotNull(feature);

        // Change timeout from default to 5 seconds
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        GeoServerDataDirectory dd = new GeoServerDataDirectory(loader);
        Properties properties = new Properties();
        Resource resource =
                dd.get(
                        IndexConfigurationManager.MODULE_DIR
                                + "/"
                                + IndexConfigurationManager.PROPERTY_FILENAME);
        InputStream is = resource.in();
        properties.load(is);
        is.close();
        properties.put("resultSets.timeToLive", "1");
        OutputStream out = resource.out();
        properties.store(out, null);
        out.close();
        final CountDownLatch done1 = new CountDownLatch(1);
        new Thread(
                        () -> {
                            while (true) {
                                try {
                                    Long ttl = ic.getTimeToLiveInSec();
                                    if (ttl == 1) {
                                        done1.countDown();
                                        break;
                                    }
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                            }
                        })
                .start();
        done1.await(10, TimeUnit.SECONDS);
        assertEquals(Long.valueOf(1), ic.getTimeToLiveInSec());
        // Check that feature not used is deleted
        final CountDownLatch done2 = new CountDownLatch(1);
        new Thread(
                        () -> {
                            while (true) {
                                try {

                                    boolean exists =
                                            getFeatureStore(ic)
                                                    .getFeatures(
                                                            CQL.toFilter(
                                                                    "ID='" + resultSetId + "'"))
                                                    .features()
                                                    .hasNext();
                                    if (!exists) {
                                        done2.countDown();
                                        break;
                                    }
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                }
                            }
                        })
                .start();
        done2.await(10, TimeUnit.SECONDS);
        boolean exists =
                getFeatureStore(ic)
                        .getFeatures(CQL.toFilter("ID='" + resultSetId + "'"))
                        .features()
                        .hasNext();
        assertFalse(exists);
    }

    private SimpleFeatureStore getFeatureStore(IndexConfigurationManager ic) {
        DataStore dataStore = ic.getCurrentDataStore();
        try {
            return (SimpleFeatureStore)
                    dataStore.getFeatureSource(IndexConfigurationManager.STORE_SCHEMA_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConcurrentChangeDatabaseParameters() throws Exception {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        GeoServerDataDirectory dd = new GeoServerDataDirectory(loader);
        Resource resource =
                dd.get(
                        IndexConfigurationManager.MODULE_DIR
                                + "/"
                                + IndexConfigurationManager.PROPERTY_FILENAME);
        Properties properties = new Properties();
        InputStream is = resource.in();
        properties.load(is);
        is.close();
        properties.put(
                IndexConfigurationManager.PROPERTY_DB_PREFIX + JDBCDataStoreFactory.DATABASE.key,
                dd.root().getPath() + "/nsg-profile/db/resultSets2");
        IndexConfigurationManager ic = applicationContext.getBean(IndexConfigurationManager.class);
        ExecutorCompletionService<Object> es =
                new ExecutorCompletionService<>(
                        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        final int REQUESTS = 100;
        for (int i = 0; i < REQUESTS; i++) {
            int count = i;
            es.submit(
                    () -> {
                        getAsDOM(
                                "ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=cdf:Fifteen&resultType=index");
                        if (count == REQUESTS / 2) {
                            // Change database
                            OutputStream out = resource.out();
                            properties.store(out, null);
                            out.close();
                        }
                        return null;
                    });
        }
        // just check there are no exceptions
        for (int i = 0; i < REQUESTS; i++) {
            es.take().get();
        }
        // wait for listener receive database notification changes
        File dbDataFile = new File(dd.root().getPath() + "/nsg-profile/db/resultSets2.data.db");
        final CountDownLatch done = new CountDownLatch(1);
        new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                while (true) {
                                    Map<String, Object> params = ic.getCurrentDataStoreParams();
                                    boolean condition =
                                            (dd.root().getPath() + "/nsg-profile/db/resultSets2")
                                                            .equals(
                                                                    params.get(
                                                                            JDBCDataStoreFactory
                                                                                    .DATABASE
                                                                                    .key))
                                                    && dbDataFile.exists();
                                    if (condition) {
                                        done.countDown();
                                        break;
                                    }
                                    try {
                                        Thread.sleep(100);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        })
                .start();
        done.await(20, TimeUnit.SECONDS);

        DataStore dataStore = ic.getCurrentDataStore();
        assertTrue(dbDataFile.exists());

        Map<String, Object> params = ic.getCurrentDataStoreParams();
        assertEquals(
                dd.root().getPath() + "/nsg-profile/db/resultSets2",
                params.get(JDBCDataStoreFactory.DATABASE.key));

        SimpleFeatureStore featureStore =
                (SimpleFeatureStore)
                        dataStore.getFeatureSource(IndexConfigurationManager.STORE_SCHEMA_NAME);
        assertEquals(REQUESTS, featureStore.getFeatures().size());
    }
}
