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

package org.geoserver.platform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.core.Constants;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * Utility class uses to process GeoServer configuration workflow through external environment
 * variables.
 *
 * <p>This class must be used everytime we need to resolve a configuration placeholder at runtime.
 *
 * <p>An instance of this class needs to be registered in spring context as follows.
 *
 * <pre>
 * <code>
 *         &lt;bean id="geoserverEnvironment" class="org.geoserver.GeoServerEnvironment" depends-on="extensions"/&gt;
 * </code>
 * </pre>
 *
 * It must be a singleton, and must not be loaded lazily. Furthermore, this bean must be loaded
 * before any beans that use it.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class GeoServerEnvironment {

    /** logger */
    protected static final Logger LOGGER = Logging.getLogger("org.geoserver.platform");

    private static final Constants constants = new Constants(PlaceholderConfigurerSupport.class);

    /**
     * Variable set via System Environment in order to instruct GeoServer to make use or not of the
     * config placeholders translation.
     *
     * <p>Default to FALSE
     */
    private static volatile boolean allowEnvParametrization =
            Boolean.valueOf(System.getProperty("ALLOW_ENV_PARAMETRIZATION", "false"));

    /**
     * Returns the variable set via System Environment in order to instruct GeoServer to make use or
     * not of the config placeholders translation.
     */
    public static boolean allowEnvParametrization() {
        return allowEnvParametrization;
    }

    /**
     * Reloads the variable set via System Environment in order to instruct GeoServer to make use or
     * not of the config placeholders translation. Use this synchronized method only for testing
     * purposes.
     */
    public static synchronized void reloadAllowEnvParametrization() {
        allowEnvParametrization =
                Boolean.valueOf(System.getProperty("ALLOW_ENV_PARAMETRIZATION", "false"));
    }

    private static final String PROPERTYFILENAME = "geoserver-environment.properties";

    private static final String nullValue = "null";

    private final PropertyPlaceholderHelper helper =
            new PropertyPlaceholderHelper(
                    constants.asString("DEFAULT_PLACEHOLDER_PREFIX"),
                    constants.asString("DEFAULT_PLACEHOLDER_SUFFIX"),
                    constants.asString("DEFAULT_VALUE_SEPARATOR"),
                    true);

    private final PlaceholderResolver resolver =
            new PlaceholderResolver() {

                @Override
                public String resolvePlaceholder(String placeholderName) {
                    return GeoServerEnvironment.this.resolvePlaceholder(placeholderName);
                }
            };

    private FileWatcher<Properties> configFile;

    private Properties props;

    /**
     * Internal "props" getter method.
     *
     * @return the props
     */
    public Properties getProps() {
        return props;
    }

    public GeoServerEnvironment() {
        try {
            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            configFile =
                    new FileWatcher<Properties>(loader.get(PROPERTYFILENAME)) {

                        @Override
                        protected Properties parseFileContents(InputStream in) throws IOException {
                            Properties p = new Properties();
                            p.load(in);
                            return p;
                        }
                    };

            props = configFile.read();
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not find any '" + PROPERTYFILENAME + "' property file.",
                    e);
            props = new Properties();
        }
    }

    protected String resolvePlaceholder(String placeholder) {
        String propVal = null;
        propVal = resolveSystemProperty(placeholder);

        if (configFile != null && configFile.isModified()) {
            try {
                props = configFile.read();
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not find any '" + PROPERTYFILENAME + "' property file.",
                        e);
                props = new Properties();
            }
        }

        if (props != null && propVal == null) {
            propVal = props.getProperty(placeholder);
        }

        return propVal;
    }

    protected String resolveSystemProperty(String key) {
        try {
            String value = System.getProperty(key);
            if (value == null) {
                value = System.getenv(key);
            }
            return value;
        } catch (Throwable ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Could not access system property '" + key + "': " + ex);
            }
            return null;
        }
    }

    protected String resolveStringValue(String strVal) throws BeansException {
        String resolved = this.helper.replacePlaceholders(strVal, this.resolver);

        return (resolved.equals(nullValue) ? null : resolved);
    }

    /**
     * Translates placeholders in the form of Spring Property placemark ${...} into their real
     * values.
     *
     * <p>The method first looks for System variables which take precedence on local ones, then into
     * internal props loaded from configuration file 'geoserver-environment.properties'.
     */
    public Object resolveValue(Object value) {
        if (value != null) {
            if (value instanceof String) {
                return resolveStringValue((String) value);
            }
        }

        return value;
    }

    /**
     * Returns 'false' whenever the configuration file 'geoserver-environment.properties' has
     * changed.
     */
    public boolean isStale() {
        return this.configFile != null && this.configFile.isModified();
    }
}
