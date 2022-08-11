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


package org.geoserver.cluster.impl.rest;

import java.io.IOException;
import org.geoserver.cluster.client.JMSContainer;
import org.geoserver.cluster.configuration.BrokerConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.configuration.ToggleConfiguration;
import org.geoserver.cluster.events.ToggleEvent;
import org.geoserver.cluster.events.ToggleType;
import org.geoserver.cluster.impl.configuration.ConfigDirConfiguration;
import org.geoserver.config.ReadOnlyGeoServerLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/** @author carlo cancellieri - geosolutions SAS */
public class Controller {

    @Autowired JMSConfiguration config;

    @Autowired ReadOnlyGeoServerLoader loader;

    @Autowired ApplicationContext ctx;

    @Autowired JMSContainer container;

    public void setInstanceName(final String name) {
        Assert.notNull(name, "Unable to setup a null name");
        config.putConfiguration(JMSConfiguration.INSTANCE_NAME_KEY, name);
    }

    public void setGroup(final String group) {
        Assert.notNull(group, "Unable to setup a null group");
        config.putConfiguration(JMSConfiguration.GROUP_KEY, group);
    }

    public void setBrokerURL(final String url) {
        Assert.notNull(url, "Unable to setup a null Broker URL");
        config.putConfiguration(BrokerConfiguration.BROKER_URL_KEY, url);
    }

    public void setReadOnly(final boolean set) {
        loader.enable(set);
        config.putConfiguration(
                ReadOnlyConfiguration.READ_ONLY_KEY, Boolean.valueOf(set).toString());
    }

    public void setConfigDir(final String path) {
        Assert.notNull(path, "Unable to setup a null path");
        config.putConfiguration(ConfigDirConfiguration.CONFIGDIR_KEY, path);
    }

    public void toggle(final boolean switchTo, final ToggleType type) {

        ctx.publishEvent(new ToggleEvent(switchTo, type));

        final String switchToValue = Boolean.valueOf(switchTo).toString();
        if (type.equals(ToggleType.MASTER))
            config.putConfiguration(ToggleConfiguration.TOGGLE_MASTER_KEY, switchToValue);
        else config.putConfiguration(ToggleConfiguration.TOGGLE_MASTER_KEY, switchToValue);

        // if (switchTo) {
        // // LOGGER.info("The " + type + " toggle is now ENABLED");
        // } else {
        // // LOGGER.warn("The " + type
        // // +
        // //
        // " toggle is now DISABLED no event will be posted/received to/from the broker");
        // // fp.info("Note that the " + type
        // // + " is still registered to the topic destination");
        // }
    }

    public void connectClient(final boolean connect) throws IOException {
        if (connect) {

            if (!container.isRunning()) {
                // .info("Connecting...");
                if (container.connect()) {
                    // .info("Now GeoServer is registered with the destination");
                    config.putConfiguration(
                            ConnectionConfiguration.CONNECTION_KEY, Boolean.TRUE.toString());
                } else {
                    config.putConfiguration(
                            ConnectionConfiguration.CONNECTION_KEY, Boolean.FALSE.toString());
                    throw new IOException(
                            "Connection error: Registration aborted due to a connection problem");
                }
            }
        } else {
            if (container.isRunning()) {
                // LOGGER.info("Disconnecting...");
                if (container.disconnect()) {
                    config.putConfiguration(
                            ConnectionConfiguration.CONNECTION_KEY, Boolean.FALSE.toString());
                } else {
                    config.putConfiguration(
                            ConnectionConfiguration.CONNECTION_KEY, Boolean.TRUE.toString());
                    throw new IOException("Disconnection error");
                }
            }
        }
    }

    public void save() throws IOException {
        config.storeConfig();
    }
}
