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

package org.geoserver.cluster.impl.handlers.configuration;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;

/**
 * JMS Handler is used to synchronize
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSLoggingHandler extends JMSConfigurationHandler<LoggingInfo> {
    private final GeoServer geoServer;

    private final ToggleSwitch producer;

    public JMSLoggingHandler(GeoServer geo, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.geoServer = geo;
        this.producer = producer;
    }

    @Override
    protected void omitFields(final XStream xstream) {
        // omit not serializable fields
        // NOTHING TO DO
    }

    @Override
    public boolean synchronize(LoggingInfo info) throws Exception {
        if (info == null) {
            throw new NullPointerException("Incoming object is null");
        }
        try {
            // LOCALIZE service
            final LoggingInfo localObject = geoServer.getLogging();
            // overwrite local object members with new incoming settings
            BeanUtils.copyProperties(localObject, info);

            // disable the message producer to avoid recursion
            producer.disable();

            // save the localized object
            geoServer.save(localObject);

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(
                        this.getClass() + " is unable to synchronize the incoming event: " + info);
            throw e;
        } finally {
            // enable message the producer
            producer.enable();
        }
        return true;
    }
}
