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
import java.util.logging.Level;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.cluster.impl.events.configuration.JMSSettingsModifyEvent;
import org.geoserver.cluster.impl.utils.BeanUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;

/** */
public class JMSSettingsHandler extends JMSConfigurationHandler<JMSSettingsModifyEvent> {

    private final GeoServer geoServer;
    private final ToggleSwitch producer;

    public JMSSettingsHandler(GeoServer geo, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.geoServer = geo;
        this.producer = producer;
    }

    @Override
    protected void omitFields(final XStream xstream) {
        xstream.omitField(GeoServer.class, "geoServer");
    }

    @Override
    public boolean synchronize(JMSSettingsModifyEvent event) throws Exception {
        if (event == null) {
            throw new NullPointerException("Incoming event is NULL.");
        }
        try {
            // disable the message producer to avoid recursion
            producer.disable();
            // let's see which type of event we have and handle it
            switch (event.getEventType()) {
                case MODIFIED:
                    handleModifiedSettings(event);
                    break;
                case ADDED:
                    handleAddedSettings(event);
                    break;
                case REMOVED:
                    handleRemovedSettings(event);
                    break;
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Error handling settings event.", exception);
            throw exception;
        } finally {
            // enabling the events producer again
            producer.enable();
        }
        return true;
    }

    private void handleModifiedSettings(JMSSettingsModifyEvent event) {
        // let's extract some useful information from the event
        WorkspaceInfo workspace = event.getSource().getWorkspace();
        // settings are global or specific to a certain workspace
        SettingsInfo settingsInfo =
                workspace == null ? geoServer.getSettings() : geoServer.getSettings(workspace);
        // if not settings were found this means that a user just deleted this workspace
        // or deleted this workspace settings on this GeoServer instance or that a previously
        // synchronization problem happened
        if (settingsInfo == null) {
            throw new IllegalArgumentException(
                    String.format(
                            "No settings for workspace '%s' found on this instance.",
                            workspace.getName()));
        }
        // well let's update our settings updating only the modified properties
        try {
            BeanUtils.smartUpdate(settingsInfo, event.getPropertyNames(), event.getNewValues());
        } catch (Exception exception) {
            String message =
                    workspace == null
                            ? "Error updating GeoServer global settings."
                            : "Error updating workspace '%s' settings.";
            throw new RuntimeException(String.format(message, workspace), exception);
        }
        // save the updated settings
        geoServer.save(settingsInfo);
    }

    private void handleAddedSettings(JMSSettingsModifyEvent event) {
        // we only need to save the new settings, if the workspace associated
        // with this settings doesn't exists or this settings already exists
        // GeoServer will complain about it with a proper exception
        geoServer.add(event.getSource());
    }

    private void handleRemovedSettings(JMSSettingsModifyEvent event) {
        // we only need to remove the new settings
        geoServer.remove(event.getSource());
    }
}
