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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ToggleConfiguration;
import org.geoserver.cluster.events.ToggleEvent;
import org.geoserver.cluster.events.ToggleType;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * This class make it possible to enable and disable the Event producer/consumer using Applications
 * Events.
 *
 * <p>This is used at the GeoServer startup to disable the producer until the initial configuration
 * is loaded.
 *
 * <p>It can also be used to enable/disable the producer in a Master+Slave configuration to avoid
 * recursive event production.
 *
 * @see {@link ToggleEvent}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = Logging.getLogger(JMSApplicationListener.class);

    protected final ToggleType type;

    /**
     * This will be set to false:<br>
     * - until the GeoServer context is initialized<br>
     * - if this instance of geoserver act as pure slave
     */
    private volatile Boolean status = false;

    @Autowired public JMSConfiguration config;

    public JMSApplicationListener(ToggleType type) {
        this.type = type;
    }

    @PostConstruct
    private void init() {
        setStatus(getStatus(type, config));
    }

    public static boolean getStatus(final ToggleType type, JMSConfiguration config) {
        Object statusObj;
        if (type.equals(ToggleType.SLAVE)) {
            statusObj = config.getConfiguration(ToggleConfiguration.TOGGLE_SLAVE_KEY);
            if (statusObj == null) {
                statusObj = ToggleConfiguration.DEFAULT_SLAVE_STATUS;
            }
        } else {
            statusObj = config.getConfiguration(ToggleConfiguration.TOGGLE_MASTER_KEY);
            if (statusObj == null) {
                statusObj = ToggleConfiguration.DEFAULT_MASTER_STATUS;
            }
        }
        return Boolean.parseBoolean(statusObj.toString());
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Incoming event of type " + event.getClass().getSimpleName());
        }

        if (event instanceof ToggleEvent) {

            // enable/disable the producer
            final ToggleEvent tEv = (ToggleEvent) event;
            if (tEv.getType().equals(this.type)) {
                setStatus(tEv.toggleTo());
            }

        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Incoming application event of type " + event.getClass().getSimpleName());
            }
        }
    }

    /** @return the status */
    public final boolean isEnabled() {
        return status;
    }

    /**
     * @param status enable or disable producer
     * @note thread safe
     */
    public final void setStatus(final boolean producerEnabled) {
        if (producerEnabled) {
            // if produce is disable -> enable it
            if (!this.status) {
                synchronized (this.status) {
                    if (!this.status) {
                        this.status = true;
                    }
                }
            }
        } else {
            // if produce is Enabled -> disable
            if (this.status) {
                synchronized (this.status) {
                    if (this.status) {
                        this.status = false;
                    }
                }
            }
        }
    }
}
