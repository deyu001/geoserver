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

package org.geoserver.cluster.server;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.geoserver.cluster.impl.events.RestDispatcherCallback;
import org.geoserver.platform.ContextLoadedEvent;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;

/**
 * JMS MASTER (Producer) Listener used to provide basic functionalities to the producer
 * implementations
 *
 * @see {@link JMSAbstractProducer}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class JMSAbstractGeoServerProducer extends JMSAbstractProducer {
    private static final java.util.logging.Logger LOGGER =
            Logging.getLogger(JMSAbstractGeoServerProducer.class);

    public JMSAbstractGeoServerProducer() {
        super();
        // disable producer until the application receive the ContextLoadedEvent
        setStatus(false);
    }

    /**
     * This should be called before each message send to add options (coming form the dispatcher
     * callback) to the message
     *
     * @return a copy of the configuration object updated with others options coming from the
     *     RestDispatcherCallback<br>
     *     TODO use also options coming from the the GUI DispatcherCallback
     */
    protected Properties getProperties() {
        // append options
        final Properties options = new Properties();
        for (Entry<Object, Object> e : config.getConfigurations().entrySet()) {
            options.put(e.getKey(), e.getValue());
        }
        // TODO not all options are needed: append only instance name when NOT debug mode

        // get options from rest callback
        final Map<String, String> p = RestDispatcherCallback.getParameters();
        if (p != null) {
            for (Map.Entry<String, String> entry : p.entrySet()) {
                options.put(entry.getKey(), entry.getValue());
            }
        }
        return options;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // event coming from the GeoServer application when the configuration
        // load process is complete
        if (event instanceof ContextLoadedEvent) {
            boolean status = getStatus(type, config);
            if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
                if (status) LOGGER.info("Activating JMS Catalog event publisher...");
                else LOGGER.info("JMS Catalog event publisher is disabled by configuration...");
            }
            // restore the status to the configured
            setStatus(status);
        } else {
            super.onApplicationEvent(event);
        }
    }
}
