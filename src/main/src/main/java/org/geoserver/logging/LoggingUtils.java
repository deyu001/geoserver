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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.vfny.geoserver.global.ConfigurationException;

public class LoggingUtils {

    public static final String RELINQUISH_LOG4J_CONTROL = "RELINQUISH_LOG4J_CONTROL";
    public static final String GT2_LOGGING_REDIRECTION = "GT2_LOGGING_REDIRECTION";
    public static final String GEOSERVER_LOG_LOCATION = "GEOSERVER_LOG_LOCATION";

    public static enum GeoToolsLoggingRedirection {
        JavaLogging,
        CommonsLogging,
        Log4J;

        /**
         * Returns the enum value corresponding to the name (using case insensitive comparison) or
         * Log4j if no match is found
         */
        public static GeoToolsLoggingRedirection findValue(String name) {
            for (GeoToolsLoggingRedirection value : values()) {
                if (value.name().equalsIgnoreCase(name)) return value;
            }
            return Log4J;
        }
    }

    public static void configureGeoServerLogging(
            GeoServerResourceLoader loader,
            InputStream loggingConfigStream,
            boolean suppressStdOutLogging,
            boolean suppressFileLogging,
            String logFileName)
            throws FileNotFoundException, IOException, ConfigurationException {
        // JD: before we wipe out the logging configuration, save any appenders that are not
        // console or file based. This allows for other types of appenders to remain in tact
        // when geoserver is reloaded.
        List<Appender> appenders = new ArrayList<>();
        Enumeration a = LogManager.getRootLogger().getAllAppenders();
        while (a.hasMoreElements()) {
            Appender appender = (Appender) a.nextElement();
            if (!(appender instanceof ConsoleAppender || appender instanceof FileAppender)) {
                // save it
                appenders.add(appender);
            }
        }

        Properties lprops = new Properties();
        lprops.load(loggingConfigStream);
        LogManager.resetConfiguration();
        //        LogLog.setQuietMode(true);
        PropertyConfigurator.configure(lprops);
        //        LogLog.setQuietMode(false);

        // configuring the log4j file logger
        if (!suppressFileLogging) {
            Appender gslf = org.apache.log4j.Logger.getRootLogger().getAppender("geoserverlogfile");
            if (gslf instanceof org.apache.log4j.FileAppender) {
                if (logFileName == null) {
                    logFileName = loader.get("logs").get("geoserver.log").file().getAbsolutePath();
                } else {
                    if (!new File(logFileName).isAbsolute()) {
                        logFileName =
                                new File(loader.getBaseDirectory(), logFileName).getAbsolutePath();
                        LoggingInitializer.LOGGER.fine(
                                "Non-absolute pathname detected for logfile.  Setting logfile relative to data dir.");
                    }
                }
                lprops.setProperty("log4j.appender.geoserverlogfile.File", logFileName);
                PropertyConfigurator.configure(lprops);
                LoggingInitializer.LOGGER.fine("Logging output to file '" + logFileName + "'");
            } else if (gslf != null) {
                LoggingInitializer.LOGGER.warning(
                        "'log4j.appender.geoserverlogfile' appender is defined, but isn't a FileAppender.  GeoServer won't control the file-based logging.");
            } else {
                LoggingInitializer.LOGGER.warning(
                        "'log4j.appender.geoserverlogfile' appender isn't defined.  GeoServer won't control the file-based logging.");
            }
        }

        // ... and the std output logging too
        if (suppressStdOutLogging) {
            LoggingInitializer.LOGGER.info(
                    "Suppressing StdOut logging.  If you want to see GeoServer logs, be sure to look in '"
                            + logFileName
                            + "'");
            Enumeration allAppenders = org.apache.log4j.Logger.getRootLogger().getAllAppenders();
            Appender curApp;
            while (allAppenders.hasMoreElements()) {
                curApp = (Appender) allAppenders.nextElement();
                if (curApp instanceof org.apache.log4j.ConsoleAppender) {
                    org.apache.log4j.Logger.getRootLogger().removeAppender(curApp);
                }
            }
        }

        // add the appenders we saved above
        for (Appender appender : appenders) {
            LogManager.getRootLogger().addAppender(appender);
        }
        LoggingInitializer.LOGGER.fine(
                "FINISHED CONFIGURING GEOSERVER LOGGING -------------------------");
    }

    public static void initLogging(
            GeoServerResourceLoader resourceLoader,
            String configFileName,
            boolean suppressStdOutLogging,
            String logFileName)
            throws Exception {
        // to initialize logging we need to do a couple of things:
        // 1)  Figure out whether the user has 'overridden' some configuration settings
        // in the logging system (not using log4j in commons-logging.properties or perhaps
        // has set up their own 'custom' log4j.properties file.
        // 2)  If they *have*, then we don't worry about configuring logging
        // 3)  If they haven't, then we configure logging to use the log4j config file
        // specified, and remove console appenders if the suppressstdoutlogging is true.
        LoggingInitializer.LOGGER.fine("CONFIGURING GEOSERVER LOGGING -------------------------");

        if (configFileName == null) {
            configFileName = "DEFAULT_LOGGING.properties";
            LoggingInitializer.LOGGER.warning(
                    "No log4jConfigFile defined in services.xml:  using 'DEFAULT_LOGGING.properties'");
        }
        Resource resource = resourceLoader.get(Paths.path("logs", configFileName));
        if (resource == null || resource.getType() == Type.UNDEFINED) {
            // hmm, well, we don't have a log4j config file and this could be due to the fact
            // that this is a data-dir upgrade.  We can count on the DEFAULT_LOGGING.properties file
            // being present on the classpath, so we'll upgrade their data_dir and then use the
            // default DEFAULT_LOGGING.properties configuration.
            LoggingInitializer.LOGGER.warning(
                    "log4jConfigFile '"
                            + configFileName
                            + "' couldn't be found in the data dir, so GeoServer will "
                            + "install the various logging config file into the data dir, and then try to find it again.");

            Resource logs = resourceLoader.get("logs");
            File lcdir = logs.dir();

            // now we copy in the various logging config files from the base repo location on the
            // classpath
            final String[] lcfiles =
                    new String[] {
                        "DEFAULT_LOGGING.properties",
                        "GEOSERVER_DEVELOPER_LOGGING.properties",
                        "GEOTOOLS_DEVELOPER_LOGGING.properties",
                        "PRODUCTION_LOGGING.properties",
                        "QUIET_LOGGING.properties",
                        "TEST_LOGGING.properties",
                        "VERBOSE_LOGGING.properties"
                    };

            for (String lcfile : lcfiles) {
                File target = new File(lcdir.getAbsolutePath(), lcfile);
                if (!target.exists()) {
                    resourceLoader.copyFromClassPath(lcfile, target);
                }
            }

            // ok, the possibly-new 'logs' directory is in-place, with all the various configs
            // there.
            // Is the originally configured log4jconfigfile there now?
            if (resource == null || resource.getType() != Type.RESOURCE) {
                LoggingInitializer.LOGGER.warning(
                        "Still couldn't find log4jConfigFile '"
                                + configFileName
                                + "'.  Using DEFAULT_LOGGING.properties instead.");
            }

            resource = resourceLoader.get(Paths.path("logs", "DEFAULT_LOGGING.properties"));
        }

        if (resource == null || resource.getType() != Type.RESOURCE) {
            throw new ConfigurationException(
                    "Unable to load logging configuration '"
                            + configFileName
                            + "'.  In addition, an attempt "
                            + "was made to create the 'logs' directory in your data dir, and to use the DEFAULT_LOGGING configuration, but"
                            + "this failed as well.  Is your data dir writeable?");
        }

        // reconfiguring log4j logger levels by resetting and loading a new set of configuration
        // properties
        try (InputStream loggingConfigStream = resource.in()) {
            if (loggingConfigStream == null) {
                LoggingInitializer.LOGGER.warning(
                        "Couldn't open Log4J configuration file '" + resource);
                return;
            } else {
                LoggingInitializer.LOGGER.fine(
                        "GeoServer logging profile '" + resource.name() + "' enabled.");
            }

            configureGeoServerLogging(
                    resourceLoader, loggingConfigStream, suppressStdOutLogging, false, logFileName);
        }
    }

    /**
     * Finds the log location in the "context" (system variable, env variable, servlet context) or
     * uses the provided base location otherwise
     */
    public static String getLogFileLocation(String baseLocation) {
        return getLogFileLocation(baseLocation, null);
    }

    /**
     * Finds the log location in the "context" (system variable, env variable, servlet context) or
     * uses the provided base location otherwise.
     *
     * <p>This method accepts a servlet context directly for cases where the logging location must
     * be known but the spring application context may not be initialized yet.
     */
    public static String getLogFileLocation(String baseLocation, ServletContext context) {
        // accept a servlet context directly in the case of startup where the application context
        // is not yet available, in other cases (like a logging change) we can fall back on the
        // app context and dervive the servlet context from that
        String location =
                context != null
                        ? GeoServerExtensions.getProperty(
                                LoggingUtils.GEOSERVER_LOG_LOCATION, context)
                        : GeoServerExtensions.getProperty(LoggingUtils.GEOSERVER_LOG_LOCATION);
        if (location == null) {
            return baseLocation;
        } else {
            return location;
        }
    }
}
