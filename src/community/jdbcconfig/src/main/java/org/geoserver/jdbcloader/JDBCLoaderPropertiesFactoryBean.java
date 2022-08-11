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

package org.geoserver.jdbcloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geoserver.config.GeoServerPluginConfigurator;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.web.context.ServletContextAware;

public abstract class JDBCLoaderPropertiesFactoryBean extends PropertiesFactoryBean
        implements GeoServerPluginConfigurator, ServletContextAware {

    private static final Logger LOGGER = Logging.getLogger(JDBCGeoServerLoader.class);

    protected static final String CONFIG_FILE = "${prefix}.properties";

    protected static final String CONFIG_SYSPROP = "${prefix}.properties";

    protected static final String JDBCURL_SYSPROP = "${prefix}.jdbcurl";

    protected static final String INITDB_SYSPROP = "${prefix}.initdb";

    protected static final String IMPORT_SYSPROP = "${prefix}.import";

    protected ResourceStore resourceStore;

    protected String prefix;

    protected String dataDirectory;

    public JDBCLoaderPropertiesFactoryBean(ResourceStore resourceStore, String prefix) {
        this.resourceStore = resourceStore;
        this.prefix = prefix;
    }

    protected abstract JDBCLoaderProperties createConfig() throws IOException;

    protected abstract String[] getScripts();

    protected abstract String[] getSampleConfigurations();

    protected String replacePrefix(String s) {
        return s.replace("${prefix}", prefix);
    }

    @Override
    public Properties createProperties() throws IOException {
        JDBCLoaderProperties config = loadConfig();
        if (!config.isEnabled()) {
            LOGGER.info("jdbcloader is disabled");
            return config;
        }
        return config;
    }

    private JDBCLoaderProperties loadConfig() throws IOException {
        // copy over sample scripts
        JDBCLoaderProperties config = loadDefaultConfig();

        /*
         * Find configuration, lookup heuristic is as follows.
         * 1. check system property "jdbcconfig.properties" for path/url to properties file
         * 2. check system properties jdbconfig.jdbcurl, jdbconfig.initdb, jdbcimport.import
         * 3. look for <GEOSERVER_DATA_DIR>/jdbcconfig/jdbcconfig.properties
         * 4. use built in defaults
         */
        if (loadConfigFromURL(config)) {
            return config;
        }

        if (loadConfigFromSysProps(config)) {
            return config;
        }

        if (loadConfigFromDataDir(config)) {
            return config;
        }

        LOGGER.info("Configuring jdbcloader from defaults");

        // copy over default config to data dir
        saveConfig(
                config,
                "Default GeoServer JDBC loader driver and connection pool options."
                        + " Edit as appropriate.");
        copySampleConfigsToDataDir();
        copyScriptsToDataDir();

        return config;
    }

    protected JDBCLoaderProperties loadDefaultConfig() throws IOException {
        JDBCLoaderProperties config = createConfig();
        config.load(getClass().getResourceAsStream("/" + replacePrefix(CONFIG_FILE)));
        return config;
    }

    protected boolean loadConfigFromSysProps(JDBCLoaderProperties config) throws IOException {
        String jdbcUrl = System.getProperty(replacePrefix(JDBCURL_SYSPROP));
        if (jdbcUrl != null) {
            config.setJdbcUrl(jdbcUrl);

            config.setInitDb(Boolean.getBoolean(replacePrefix(INITDB_SYSPROP)));
            config.setImport(Boolean.getBoolean(replacePrefix(IMPORT_SYSPROP)));

            if (LOGGER.isLoggable(Level.INFO)) {
                StringBuilder msg =
                        new StringBuilder("Configuring jdbcloader from system properties:\n");
                msg.append("  ")
                        .append(replacePrefix(JDBCURL_SYSPROP))
                        .append("=")
                        .append(jdbcUrl)
                        .append("\n");
                msg.append("  ")
                        .append(replacePrefix(INITDB_SYSPROP))
                        .append("=")
                        .append(config.isInitDb())
                        .append("\n");
                msg.append("  ")
                        .append(replacePrefix(IMPORT_SYSPROP))
                        .append("=")
                        .append(config.isImport())
                        .append("\n");
                LOGGER.info(msg.toString());
            }
            return true;
        }
        return false;
    }

    private boolean loadConfigFromURL(JDBCLoaderProperties config) throws IOException {
        String propUrl = System.getProperty(replacePrefix(CONFIG_SYSPROP));
        if (propUrl == null) {
            return false;
        }

        URL url = null;
        try {
            // try to parse directly as url
            try {
                url = new URL(propUrl);
            } catch (MalformedURLException e) {
                // failed, try as a file path
                File f = new File(propUrl);
                if (f.canRead() && f.exists()) {
                    url = URLs.fileToUrl(f);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error trying to read " + propUrl, e);
        }

        if (url != null) {
            LOGGER.info("Configuring jdbcloader from " + url.toString());
            InputStream in = url.openStream();
            try {
                config.load(in);
            } finally {
                in.close();
            }
            return true;
        }

        LOGGER.severe(
                "System property "
                        + replacePrefix(CONFIG_SYSPROP)
                        + " specified "
                        + propUrl
                        + " but could not be read, ignoring.");
        return false;
    }

    private boolean loadConfigFromDataDir(JDBCLoaderProperties config) throws IOException {
        Resource propFile = getBaseDir().get(replacePrefix(CONFIG_FILE));
        if (Resources.exists(propFile)) {
            LOGGER.info("Loading jdbcloader properties from " + propFile.path());
            InputStream stream = propFile.in();
            try {
                config.load(stream);
                return true;
            } finally {
                stream.close();
            }
        }
        return false;
    }

    void saveConfig(JDBCLoaderProperties config) throws IOException {
        saveConfig(config, "");
    }

    private void saveConfig(JDBCLoaderProperties config, String comment) throws IOException {
        Resource propFile = getBaseDir().get(replacePrefix(CONFIG_FILE));

        try {
            OutputStream out = propFile.out();
            try {
                config.store(out, comment);
            } finally {
                out.close();
            }

        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error saving jdbc loader properties to file " + propFile.path(),
                    e);
            propFile.delete();
        }
    }

    private void copyScriptsToDataDir() throws IOException {
        final Resource scriptsDir = getScriptDir();
        Class<?> scope = getClass();
        for (String scriptName : getScripts()) {
            Resource target = scriptsDir.get(scriptName);
            if (!Resources.exists(target)) {
                IOUtils.copy(scope.getResourceAsStream(scriptName), target.out());
            }
        }
    }

    private void copySampleConfigsToDataDir() throws IOException {
        final Resource baseDirectory = getBaseDir();
        for (String sampleConfig : getSampleConfigurations()) {
            Resource target = baseDirectory.get(sampleConfig);
            if (!Resources.exists(target)) {
                IOUtils.copy(
                        Thread.currentThread()
                                .getContextClassLoader()
                                .getResourceAsStream(sampleConfig),
                        target.out());
            }
        }
    }

    protected String getDataDirStr() {
        if (dataDirectory == null) {
            if (resourceStore instanceof GeoServerResourceLoader) {
                dataDirectory =
                        ((GeoServerResourceLoader) resourceStore)
                                .getBaseDirectory()
                                .getAbsolutePath();
            } else {
                throw new IllegalStateException("Data directory could not be determined.");
            }
        }
        return dataDirectory;
    }

    protected Resource getDataDir() {
        return resourceStore.get("");
    }

    protected Resource getBaseDir() {
        return resourceStore.get(prefix);
    }

    protected Resource getScriptDir() {
        return resourceStore.get(Paths.path(prefix, "scripts"));
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        dataDirectory = GeoServerResourceLoader.lookupGeoServerDataDirectory(servletContext);
    }
}
