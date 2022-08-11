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

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

/**
 * Utility class
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime, The Open Planning Project
 */
public class ApplicationProperties implements ApplicationContextAware {

    /** logger */
    public static Logger LOGGER = Logging.getLogger("org.geoserver");

    /** A static application context */
    static ApplicationContext context;

    /**
     * Sets the web application context to be used for looking up extensions.
     *
     * <p>This method is called by the spring container, and should never be called by client code.
     * If client needs to supply a particular context, methods which take a context are available.
     *
     * <p>This is the context that is used for methods which dont supply their own context.
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        ApplicationProperties.context = context;
    }

    /** Checks the context, if null will issue a warning. */
    static void checkContext(ApplicationContext context) {
        if (context == null) {
            LOGGER.severe("Extension lookup occured, but ApplicationContext is unset.");
        }
    }

    /**
     * Looks up for a named string property in the order defined by {@link #getProperty(String,
     * ApplicationContext)} using the internally cached spring application context.
     *
     * <p>Care should be taken when using this method. It should not be called during startup or
     * from tests cases as the internal context will not have been set.
     *
     * @param propertyName The property name to lookup.
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName) {
        return getProperty(propertyName, context);
    }

    /**
     * Looks up for a named string property into the following contexts (in order):
     *
     * <ul>
     *   <li>System Property
     *   <li>web.xml init parameters (only works if the context is a {@link WebApplicationContext}
     *   <li>Environment variable
     * </ul>
     *
     * and returns the first non null, non empty value found.
     *
     * @param propertyName The property name to be searched
     * @param context The Spring context (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ApplicationContext context) {
        if (context instanceof WebApplicationContext) {
            return getProperty(propertyName, ((WebApplicationContext) context).getServletContext());
        } else {
            return getProperty(propertyName, (ServletContext) null);
        }
    }

    /**
     * Looks up for a named string property into the following contexts (in order):
     *
     * <ul>
     *   <li>System Property
     *   <li>web.xml init parameters
     *   <li>Environment variable
     * </ul>
     *
     * and returns the first non null, non empty value found.
     *
     * @param propertyName The property name to be searched
     * @param context The servlet context used to look into web.xml (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ServletContext context) {
        // TODO: this code comes from the data directory lookup and it's useful
        // until we provide a way for the user to manually inspect the three contexts
        // (when trying to debug why the variable they thing they've set, and so on, see also
        // https://osgeo-org.atlassian.net/browse/GEOS-2343
        // Once that is fixed, we can remove the logging code that makes this method more complex
        // than strictly necessary

        final String[] typeStrs = {
            "Java environment variable ",
            "Servlet context parameter ",
            "System environment variable "
        };

        String result = null;
        for (int j = 0; j < typeStrs.length; j++) {
            // Lookup section
            switch (j) {
                case 0:
                    result = System.getProperty(propertyName);
                    break;
                case 1:
                    if (context != null) {
                        result = context.getInitParameter(propertyName);
                    }
                    break;
                case 2:
                    result = System.getenv(propertyName);
                    break;
            }

            if (result == null || result.equalsIgnoreCase("")) {
                LOGGER.finer("Found " + typeStrs[j] + ": '" + propertyName + "' to be unset");
            } else {
                break;
            }
        }

        return result;
    }
}
