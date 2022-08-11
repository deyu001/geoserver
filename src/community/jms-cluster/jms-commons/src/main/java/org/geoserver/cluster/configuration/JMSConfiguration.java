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

package org.geoserver.cluster.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.WebUtils;

/**
 * Abstract class to store and load configuration from global var or temp webapp directory
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public final class JMSConfiguration {
    public static final String DEFAULT_GROUP = "geoserver-cluster";

    protected static final java.util.logging.Logger LOGGER =
            Logging.getLogger(JMSConfiguration.class);

    @Autowired public List<JMSConfigurationExt> exts;

    public static final String INSTANCE_NAME_KEY = "instanceName";

    public static final String GROUP_KEY = "group";

    /** This file contains the configuration */
    public static final String CONFIG_FILE_NAME = "cluster.properties";

    /**
     * This variable stores the configuration path dir. the default initialization will set this to
     * the webapp temp dir. If you need to store it to a new path use the setter to change it.
     */
    private static Resource configPathDir = Files.asResource(getTempDir());

    public static void setConfigPathDir(Resource dir) {
        configPathDir = dir;
    }

    public static final Resource getConfigPathDir() {
        return configPathDir;
    }

    // the configuration
    protected Properties configuration = new Properties();

    public Properties getConfigurations() {
        return configuration;
    }

    public <T> T getConfiguration(String key) {
        return (T) configuration.get(key);
    }

    public void putConfiguration(String key, String o) {
        configuration.put(key, o);
    }

    /** Initialize configuration */
    @PostConstruct
    private void init() throws IOException {
        try {
            loadConfig();
            if (configuration.isEmpty()) {
                initDefaults();
            }
            // avoid ActiveMQ matching all pattern has virtual topic name
            String topicName = configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY);
            if (topicName.equalsIgnoreCase("VirtualTopic.>")) {
                // override topic name with the default topic name
                configuration.put(
                        TopicConfiguration.TOPIC_NAME_KEY, TopicConfiguration.DEFAULT_TOPIC_NAME);
                storeConfig();
            }
            // if configuration is changed (since last boot) store changes
            // on disk
            boolean override = override();
            if (exts != null) {
                for (JMSConfigurationExt ext : exts) {
                    override |= ext.override(this);
                }
            }
            if (override) {
                storeConfig();
            }

        } catch (IOException e) {
            LOGGER.severe("Unable to load properties: using defaults");
            initDefaults();
        }

        try {
            storeConfig();
        } catch (IOException e) {
            LOGGER.severe("Unable to store properties");
        }
    }

    /** Initialize configuration with default parameters */
    public void initDefaults() throws IOException {
        // set the group
        configuration.put(GROUP_KEY, DEFAULT_GROUP);

        // set the name
        configuration.put(INSTANCE_NAME_KEY, UUID.randomUUID().toString());
        if (exts != null) {
            for (JMSConfigurationExt ext : exts) {
                ext.initDefaults(this);
            }
        }
    }

    /**
     * check if instance name is changed since last application boot, if so set the overridden value
     * into configuration and returns true
     *
     * @return true if some parameter is overridden by an Extension property
     */
    public boolean override() {
        return override(INSTANCE_NAME_KEY, UUID.randomUUID().toString());
    }

    /**
     * check if instance name is changed since last application boot, if so set the overridden value
     * into configuration and returns true
     */
    public final boolean override(String nameKey, Object defaultVal) {
        boolean override = false;
        final String ovrName = getOverride(nameKey);
        if (ovrName != null) {
            final String name = configuration.getProperty(nameKey);
            if (name != null && !name.equals(ovrName)) {
                override = true;
            }
            configuration.put(nameKey, ovrName);
        } else {
            final String name = configuration.getProperty(nameKey);
            // no configuration found setup defaults and return override==true
            if (name == null) {
                override = true;
                configuration.put(nameKey, defaultVal);
            }
        }
        return override;
    }

    /**
     * @param theName the key name of the configuration parameter
     * @return the overridden value if override is set, null otherwise
     */
    public static <T> T getOverride(final String theName) {
        return (T) ApplicationProperties.getProperty(theName);
    }

    public void loadConfig() throws IOException {
        Resource config = configPathDir.get(CONFIG_FILE_NAME);
        try (InputStream fis = config.in()) {
            this.configuration.load(fis);
        }
    }

    public void storeConfig() throws IOException {
        Resource config = configPathDir.get(CONFIG_FILE_NAME);
        try (OutputStream fos = config.out()) {
            this.configuration.store(fos, "");
        }
    }

    public static final File getTempDir() {
        String tempPath =
                (ApplicationProperties.getProperty(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE) != null
                        ? ApplicationProperties.getProperty(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE)
                        : System.getProperty("java.io.tmpdir"));
        if (tempPath == null) {
            return null;
        }
        File tempDir = new File(tempPath);
        if (tempDir.exists() == false) return null;
        if (tempDir.isDirectory() == false) return null;
        if (tempDir.canWrite() == false) return null;
        return tempDir;
    }
}
