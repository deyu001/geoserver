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

package org.geoserver.jdbcconfig.internal;

import static org.geoserver.jdbcconfig.JDBCConfigTestSupport.createTempDir;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.jdbcloader.JDBCLoaderPropertiesFactoryBean;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.URLs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JDBCConfigPropertiesTest {

    protected static final String CONFIG_FILE = "jdbcconfig.properties";

    protected static final String CONFIG_SYSPROP = "jdbcconfig.properties";

    protected static final String JDBCURL_SYSPROP = "jdbcconfig.jdbcurl";

    protected static final String INITDB_SYSPROP = "jdbcconfig.initdb";

    protected static final String IMPORT_SYSPROP = "jdbcconfig.import";

    GeoServerResourceLoader loader;

    @Before
    public void setUp() throws IOException {
        loader = new GeoServerResourceLoader(createTempDir());
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(loader.getBaseDirectory());
    }

    @Test
    public void testLoadDefaults() throws IOException {
        JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
        JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

        assertFalse(props.isEnabled());
        assertTrue(props.isInitDb());
        assertTrue(props.isImport());

        // assert files copied over
        assertNotNull(loader.find("jdbcconfig", "jdbcconfig.properties"));
        assertNotNull(loader.find("jdbcconfig", "scripts", "initdb.postgres.sql"));

        // assert file location are accessible
        assertNotNull(factory.getFileLocations());

        // assert configuration can be stored successfully on another resource loader
        File tmpDir = org.geoserver.jdbcconfig.JDBCConfigTestSupport.createTempDir();
        Resources.directory(Files.asResource(tmpDir).get("jdbcconfig"), true);

        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(tmpDir);
        factory.saveConfiguration(resourceLoader);

        assertEquals(
                factory.getFileLocations().size(),
                (resourceLoader.find("jdbcconfig").list().length - 1)
                        + (resourceLoader.find("jdbcconfig/scripts").list().length));
    }

    private File createDummyConfigFile() throws IOException {
        Properties p = new Properties();
        p.put("foo", "bar");
        p.put("initdb", "false");
        p.put("import", "false");

        File configFile = new File(loader.getBaseDirectory(), "foo.properties");
        FileOutputStream fout = new FileOutputStream(configFile);
        p.store(fout, "");
        fout.flush();
        fout.close();

        return configFile;
    }

    @Test
    public void testLoadFromFile() throws Exception {
        File configFile = createDummyConfigFile();

        System.setProperty(CONFIG_SYSPROP, configFile.getAbsolutePath());
        try {
            JDBCLoaderPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("bar", props.getProperty("foo"));
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        } finally {
            System.clearProperty(CONFIG_SYSPROP);
        }
    }

    @Test
    public void testLoadFromURL() throws Exception {
        File configFile = createDummyConfigFile();

        System.setProperty(CONFIG_SYSPROP, URLs.fileToUrl(configFile).toString());
        try {
            JDBCLoaderPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("bar", props.getProperty("foo"));
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        } finally {
            System.clearProperty(CONFIG_SYSPROP);
        }
    }

    @Test
    public void testLoadFromSysProps() throws Exception {
        System.setProperty(JDBCURL_SYSPROP, "jdbc:h2:nofile");
        System.setProperty(INITDB_SYSPROP, "false");
        System.setProperty(IMPORT_SYSPROP, "false");

        try {
            JDBCLoaderPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("jdbc:h2:nofile", props.getJdbcUrl().get());
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        } finally {
            System.clearProperty(JDBCURL_SYSPROP);
            System.clearProperty(INITDB_SYSPROP);
            System.clearProperty(IMPORT_SYSPROP);
        }
    }

    @Test
    public void testDataDirPlaceholder() throws Exception {
        JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
        JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();
        props.setJdbcUrl("jdbc:h2:file:${GEOSERVER_DATA_DIR}");

        assertThat(
                props.getJdbcUrl().get(),
                containsString(loader.getBaseDirectory().getAbsolutePath()));
    }
}
