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

package org.geoserver.logging;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.logging.LoggingUtils.GeoToolsLoggingRedirection;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.CommonsLoggerFactory;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;

/**
 * Listens for GeoServer startup and tries to configure logging redirection to LOG4J, then
 * configures LOG4J according to the GeoServer configuration files (provided logging control hasn't
 * been disabled)
 */
public class LoggingStartupContextListener implements ServletContextListener {
    private static Logger LOGGER;

    public void contextDestroyed(ServletContextEvent event) {}

    public void contextInitialized(ServletContextEvent event) {
        // setup GeoTools logging redirection (to log4j by default, but so that it can be
        // overridden)
        final ServletContext context = event.getServletContext();
        GeoToolsLoggingRedirection logging =
                GeoToolsLoggingRedirection.findValue(
                        GeoServerExtensions.getProperty(
                                LoggingUtils.GT2_LOGGING_REDIRECTION, context));
        try {
            if (logging == GeoToolsLoggingRedirection.CommonsLogging) {
                Logging.ALL.setLoggerFactory(CommonsLoggerFactory.getInstance());
            } else if (logging != GeoToolsLoggingRedirection.JavaLogging) {
                Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not configure log4j logging redirection", e);
        }

        String relinquishLoggingControl =
                GeoServerExtensions.getProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, context);
        if (Boolean.valueOf(relinquishLoggingControl)) {
            getLogger()
                    .info(
                            "RELINQUISH_LOG4J_CONTROL on, won't attempt to reconfigure LOG4J loggers");
        } else {
            try {
                File baseDir =
                        new File(GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
                GeoServerResourceLoader loader = new GeoServerResourceLoader(baseDir);

                LoggingInfo loginfo = getLogging(loader);
                if (loginfo != null) {
                    final String location =
                            LoggingUtils.getLogFileLocation(
                                    loginfo.getLocation(), event.getServletContext());
                    LoggingUtils.initLogging(
                            loader, loginfo.getLevel(), !loginfo.isStdOutLogging(), location);
                } else {
                    // check for old style data directory
                    File f = loader.find("services.xml");
                    if (f != null) {
                        LegacyLoggingImporter loggingImporter = new LegacyLoggingImporter();
                        loggingImporter.imprt(baseDir);
                        final String location =
                                LoggingUtils.getLogFileLocation(loggingImporter.getLogFile(), null);
                        LoggingUtils.initLogging(
                                loader,
                                loggingImporter.getConfigFileName(),
                                loggingImporter.getSuppressStdOutLogging(),
                                location);
                    } else {
                        getLogger()
                                .log(
                                        Level.WARNING,
                                        "Could not find configuration file for logging");
                    }
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Could not configure log4j overrides", e);
            }
        }
    }

    /**
     * Get the LoggingInfo used at startup before the regular configuration system is available.
     *
     * <p>You probably want {@link org.geoserver.config.GeoServer#getLogging} instead
     *
     * @return LoggingInfo loaded directly from logging.xml. Returns null if logging.xml does not
     *     exist
     */
    public static @Nullable LoggingInfo getLogging(ResourceStore store) throws IOException {
        // Exposing this is a hack to provide JDBCConfig with the information it needs to compute
        // the "change" between logging.xml and the versions stored in JDBC. KS
        // TODO find a better solution than re-initializing on JDBCCOnfig startup.
        Resource f = store.get("logging.xml");
        if (f != null) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            try (BufferedInputStream in = new BufferedInputStream(f.in())) {
                LoggingInfo loginfo = xp.load(in, LoggingInfo.class);
                return loginfo;
            }
        } else {
            return null;
        }
    }

    Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = Logging.getLogger("org.geoserver.logging");
        }
        return LOGGER;
    }
}
