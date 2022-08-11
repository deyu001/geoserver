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

package org.geoserver.gwc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.MaxSizeConfig.MaxSizePolicy;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.logging.Logging;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.MemoryBlobStore;
import org.geowebcache.storage.blobstore.memory.NullBlobStore;
import org.geowebcache.storage.blobstore.memory.distributed.HazelcastCacheProvider;
import org.geowebcache.storage.blobstore.memory.distributed.HazelcastLoader;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests the functionalities of the {@link ConfigurableBlobStore} class.
 *
 * @author Nicola Lagomarsini Geosolutions
 */
public class HazelcastTest extends GeoServerSystemTestSupport {

    /** {@link Logger} used for reporting exceptions */
    private static final Logger LOGGER = Logging.getLogger(HazelcastTest.class);

    /** Name of the test directory */
    public static final String TEST_BLOB_DIR_NAME = "gwcTestBlobs";

    /** {@link CacheProvider} object used for testing purposes */
    private static CacheProvider cache;

    /** {@link ConfigurableBlobStore} object to test */
    private static ConfigurableBlobStore blobStore;

    /** Directory containing files for the {@link FileBlobStore} */
    private File directory;

    @BeforeClass
    public static void initialSetup() {
        cache = new GuavaCacheProvider(new CacheConfiguration());
    }

    @Before
    public void setup() throws IOException {
        // Setup the fileBlobStore
        File dataDirectoryRoot = getTestData().getDataDirectoryRoot();

        MemoryBlobStore mbs = new MemoryBlobStore();

        NullBlobStore nbs = new NullBlobStore();

        directory = new File(dataDirectoryRoot, "testConfigurableBlobStore");
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
        directory.mkdirs();

        BlobStore defaultStore = new FileBlobStore(directory.getAbsolutePath());
        blobStore = new ConfigurableBlobStore(defaultStore, mbs, nbs);
        blobStore.setCache(cache);
    }

    @After
    public void after() throws IOException {
        // Delete the created directory
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
    }

    @SuppressWarnings("serial")
    @Test
    public void testHazelcast() throws Exception {
        // Configuring hazelcast caching
        Config config = new Config();
        MapConfig mapConfig = new MapConfig(HazelcastCacheProvider.HAZELCAST_MAP_DEFINITION);
        MaxSizeConfig maxSizeConf = new MaxSizeConfig(16, MaxSizePolicy.USED_HEAP_SIZE);
        mapConfig.setMaxSizeConfig(maxSizeConf);
        mapConfig.setEvictionPolicy(EvictionPolicy.LRU);
        config.addMapConfig(mapConfig);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
        config.getNetworkConfig()
                .getJoin()
                .getTcpIpConfig()
                .setMembers(
                        new ArrayList<String>() {
                            {
                                add("127.0.0.1");
                            }
                        });
        config.getNetworkConfig().getInterfaces().addInterface("127.0.0.1");
        HazelcastInstance instance1 = Hazelcast.newHazelcastInstance(config);
        HazelcastLoader loader1 = new HazelcastLoader();
        loader1.setInstance(instance1);
        loader1.afterPropertiesSet();

        // Creating another cacheprovider for ensuring hazelcast is behaving correctly
        HazelcastCacheProvider cacheProvider1 = new HazelcastCacheProvider(loader1);

        HazelcastInstance instance2 = Hazelcast.newHazelcastInstance(config);
        HazelcastLoader loader2 = new HazelcastLoader();
        loader2.setInstance(instance2);
        loader2.afterPropertiesSet();

        HazelcastCacheProvider cacheProvider2 = new HazelcastCacheProvider(loader2);

        // Configure the blobstore
        GWCConfig gwcConfig = new GWCConfig();
        gwcConfig.setInnerCachingEnabled(true);
        gwcConfig.setEnabledPersistence(false);
        blobStore.setChanged(gwcConfig, false);
        blobStore.setCache(cacheProvider1);

        assertTrue(blobStore.getDelegate() instanceof MemoryBlobStore);
        assertTrue(((MemoryBlobStore) blobStore.getDelegate()).getStore() instanceof NullBlobStore);

        // Put a TileObject
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = {1L, 2L, 3L};
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        TileObject to =
                TileObject.createCompleteTileObject(
                        "test:123123 112", xyz, "EPSG:4326", "image/jpeg", parameters, bytes);

        blobStore.put(to);
        // Try to get the Tile Object
        TileObject to2 =
                TileObject.createQueryTileObject(
                        "test:123123 112", xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.get(to2);

        // Check formats
        assertEquals(to.getBlobFormat(), to2.getBlobFormat());

        // Check if the resources are equals
        InputStream is = to.getBlob().getInputStream();
        InputStream is2 = to2.getBlob().getInputStream();
        checkInputStreams(is, is2);

        // Ensure Caches contain the result

        // cache1
        TileObject to3 = cacheProvider1.getTileObj(to);
        assertNotNull(to3);

        is = to.getBlob().getInputStream();
        InputStream is3 = to3.getBlob().getInputStream();
        checkInputStreams(is, is3);

        // cache2
        TileObject to4 = cacheProvider2.getTileObj(to);
        assertNotNull(to4);

        is = to.getBlob().getInputStream();
        InputStream is4 = to4.getBlob().getInputStream();
        checkInputStreams(is, is4);

        // DELETE

        // Remove TileObject
        TileObject to5 =
                TileObject.createQueryTileObject(
                        "test:123123 112", xyz, "EPSG:4326", "image/jpeg", parameters);
        blobStore.delete(to5);

        // Ensure TileObject is no more present
        TileObject to6 =
                TileObject.createQueryTileObject(
                        "test:123123 112", xyz, "EPSG:4326", "image/jpeg", parameters);
        assertFalse(blobStore.get(to6));

        // Ensure that each cache provider does not contain the tile object
        assertNull(cacheProvider1.getTileObj(to6));
        assertNull(cacheProvider2.getTileObj(to6));

        // At the end, destroy the caches
        cacheProvider1.destroy();
        cacheProvider2.destroy();
    }

    /** Checks if the streams are equals, note that the {@link InputStream}s are also closed. */
    private void checkInputStreams(InputStream is, InputStream is2) throws IOException {
        try {
            assertTrue(IOUtils.contentEquals(is, is2));
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                assertTrue(false);
            }
            try {
                is2.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                assertTrue(false);
            }
        }
    }
}
