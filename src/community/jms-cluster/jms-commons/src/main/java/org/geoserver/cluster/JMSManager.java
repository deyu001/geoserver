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

package org.geoserver.cluster;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class used to handle JMS extensions. Here we define a set of functions to perform resource lookup
 * into the Spring context.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSManager {
    private static final Logger LOGGER = Logging.getLogger(JMSManager.class);

    @Autowired private Map<String, JMSEventHandlerSPI> beans;

    /**
     * Method to make lookup using the type of the passed eventType.
     *
     * @param <S>
     * @param <O>
     * @return the handler
     */
    public <S extends Serializable, O> JMSEventHandler<S, O> getHandler(final O eventType)
            throws IllegalArgumentException {
        final Set<?> beanSet = beans.entrySet();
        // declare a tree set to define the handler priority
        final Set<JMSEventHandlerSPI<S, O>> candidates =
                new TreeSet<JMSEventHandlerSPI<S, O>>(
                        new Comparator<JMSEventHandlerSPI<S, O>>() {
                            @Override
                            public int compare(
                                    JMSEventHandlerSPI<S, O> o1, JMSEventHandlerSPI<S, O> o2) {
                                if (o1.getPriority() < o2.getPriority()) return -1;
                                else if (o1.getPriority() == o2.getPriority()) {
                                    return 0;
                                    // } else if (o1.getPriority()>o2.getPriority()){
                                } else {
                                    return 1;
                                }
                            }
                        });
        // for each handler check if it 'canHandle' the incoming object if so
        // add it to the tree
        for (final Iterator<?> it = beanSet.iterator(); it.hasNext(); ) {
            final Map.Entry<String, ?> entry = (Entry<String, ?>) it.next();

            final JMSEventHandlerSPI<S, O> spi = (JMSEventHandlerSPI) entry.getValue();
            if (spi != null) {
                if (spi.canHandle(eventType)) {
                    if (LOGGER.isLoggable(Level.INFO))
                        LOGGER.info("Creating an instance of: " + spi.getClass());
                    candidates.add(spi);
                }
            }
        }
        // TODO return the entire tree leaving choice to the caller (useful to
        // build a failover list)
        // return the first available handler
        final Iterator<JMSEventHandlerSPI<S, O>> it = candidates.iterator();
        while (it.hasNext()) {
            try {
                final JMSEventHandler<S, O> handler = it.next().createHandler();
                if (handler != null) return handler;
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }
        final String message =
                "Unable to find the needed Handler SPI for event of type: "
                        + eventType.getClass().getCanonicalName();
        if (LOGGER.isLoggable(Level.WARNING)) LOGGER.warning(message);
        throw new IllegalArgumentException(message);
    }

    public <S extends Serializable, O> JMSEventHandler<S, O> getHandlerByClassName(
            final String clazzName) throws IllegalArgumentException {
        final Object spiBean = beans.get(clazzName);
        if (spiBean != null) {
            JMSEventHandlerSPI<S, O> spi = JMSEventHandlerSPI.class.cast(spiBean);
            if (spi != null) {
                return spi.createHandler();
            }
        }

        final String message = "Unable to find the Handler SPI called: " + clazzName;
        if (LOGGER.isLoggable(Level.WARNING)) LOGGER.warning(message);
        throw new IllegalArgumentException(message);
    }
}
