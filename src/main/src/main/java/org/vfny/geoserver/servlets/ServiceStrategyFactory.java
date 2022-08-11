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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.OutputStrategyFactory;
import org.geoserver.ows.ServiceStrategy;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.vfny.geoserver.util.PartialBufferedOutputStream2;

public class ServiceStrategyFactory implements OutputStrategyFactory, ApplicationContextAware {
    /** Class logger */
    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    /** GeoServer configuratoin */
    GeoServer geoServer;

    /** The application context */
    ApplicationContext context;

    /** The default service strategy */
    String serviceStrategy;

    /** The default buffer size when the partial buffer strategy is used */
    int partialBufferSize = PartialBufferedOutputStream2.DEFAULT_BUFFER_SIZE;

    public ServiceStrategyFactory(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    public void setServiceStrategy(String serviceStrategy) {
        this.serviceStrategy = serviceStrategy;
    }

    public void setPartialBufferSize(int partialBufferSize) {
        this.partialBufferSize = partialBufferSize;
    }

    public ServletContext getServletContext() {
        return ((WebApplicationContext) context).getServletContext();
    }

    public ServiceStrategy createOutputStrategy(HttpServletResponse response) {
        // If verbose exceptions is on then lets make sure they actually get the
        // exception by using the file strategy.
        ServiceStrategy theStrategy = null;

        if (serviceStrategy == null) {
            // none set, look up in web application context
            serviceStrategy = GeoServerExtensions.getProperty("serviceStrategy");
        }

        // do a lookup
        if (serviceStrategy != null) {
            theStrategy = (ServiceStrategy) context.getBean(serviceStrategy);
        }

        if (theStrategy == null) {
            // default to partial buffer 2
            theStrategy = (ServiceStrategy) context.getBean("PARTIAL-BUFFER2");
        }

        // clone the strategy since at the moment the strategies are marked as singletons
        // in the web.xml file.
        try {
            theStrategy = (ServiceStrategy) theStrategy.clone();
        } catch (CloneNotSupportedException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Programming error found, service strategies should be cloneable, " + e,
                    e);
            throw new RuntimeException("Found a strategy that does not support cloning...", e);
        }

        // TODO: this hack should be removed once modules have their own config
        if (theStrategy instanceof PartialBufferStrategy2) {
            if (partialBufferSize == 0) {
                String size = getServletContext().getInitParameter("PARTIAL_BUFFER_STRATEGY_SIZE");

                if (size != null) {
                    try {
                        partialBufferSize = Integer.valueOf(size).intValue();

                        if (partialBufferSize <= 0) {
                            LOGGER.warning(
                                    "Invalid partial buffer size, defaulting to "
                                            + PartialBufferedOutputStream2.DEFAULT_BUFFER_SIZE
                                            + " (was "
                                            + partialBufferSize
                                            + ")");
                            partialBufferSize = 0;
                        }
                    } catch (NumberFormatException nfe) {
                        LOGGER.warning(
                                "Invalid partial buffer size, defaulting to "
                                        + PartialBufferedOutputStream2.DEFAULT_BUFFER_SIZE
                                        + " (was "
                                        + partialBufferSize
                                        + ")");
                        partialBufferSize = 0;
                    }
                }
            }

            ((PartialBufferStrategy2) theStrategy).setBufferSize(partialBufferSize);
        }

        return theStrategy;
    }
}
