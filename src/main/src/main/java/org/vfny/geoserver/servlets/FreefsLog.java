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

package org.vfny.geoserver.servlets;

import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Initializes all logging functions.
 *
 * @author Rob Hranac, Vision for New York
 * @author Chris Holmes, TOPP
 * @version $Id$
 */
public class FreefsLog extends HttpServlet {
    /** Standard logging instance for class */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    /** Initializes logging and config. */
    public void init() throws ServletException {
        // configure log4j, since console logging is configured elsewhere
        // we deny all logging, this is really just to prevent log4j
        // initilization warnings
        // TODO: this is a hack, log config should be cleaner

        // JD: Commenting out
        //    	ConsoleAppender appender = new ConsoleAppender(new PatternLayout());
        //    	appender.addFilter(new DenyAllFilter());
        //
        //    	BasicConfigurator.configure(appender);
        //
        // HACK: java.util.prefs are awful.  See
        // http://www.allaboutbalance.com/disableprefs.  When the site comes
        // back up we should implement their better way of fixing the problem.
        System.setProperty("java.util.prefs.syncInterval", "5000000");
    }

    /**
     * Initializes logging.
     *
     * @param req The servlet request object.
     * @param res The servlet response object.
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) {
        // BasicConfigurator.configure();
    }

    /**
     * Closes down the zserver if it is running, and frees up resources.
     *
     * @task REVISIT: what we should consider is having geotools provide a nicer way to clean up
     *     datastores's resources, something like a close, so that we could just iterate through all
     *     the datastores calling close. Once that's done we can clean up this method a bit.
     */
    public void destroy() {
        super.destroy();
        // ConnectionPoolManager.getInstance().closeAll();

        /*
          HACK: we must get a standard API way for releasing resources...
        */
        try {
            Class<?> sdepfClass = Class.forName("org.geotools.data.arcsde.ConnectionPoolFactory");

            LOGGER.fine("SDE datasource found, releasing resources");

            java.lang.reflect.Method m = sdepfClass.getMethod("getInstance", new Class[0]);
            Object pfInstance = m.invoke(sdepfClass, new Object[0]);

            LOGGER.fine("got sde connection pool factory instance: " + pfInstance);

            java.lang.reflect.Method closeMethod =
                    pfInstance.getClass().getMethod("closeAll", new Class[0]);

            closeMethod.invoke(pfInstance, new Object[0]);
            LOGGER.info("just asked SDE datasource to release connections");
        } catch (ClassNotFoundException cnfe) {
            LOGGER.fine("No SDE datasource found");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
