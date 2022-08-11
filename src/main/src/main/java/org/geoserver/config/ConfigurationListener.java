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

package org.geoserver.config;

import java.util.List;

/** @author Justin Deoliveira, The Open Planning Project */
public interface ConfigurationListener {

    /**
     * Handles a change to the global configuration.
     *
     * @param global The global config object.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleGlobalChange(
            GeoServerInfo global,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /**
     * Handles the event fired post change to global configuration.
     *
     * @param global The global config object.
     */
    void handlePostGlobalChange(GeoServerInfo global);

    /**
     * Handles the event fired when a settings configuration is added.
     *
     * @param settings The settings.
     */
    void handleSettingsAdded(SettingsInfo settings);

    /**
     * Handles the event fired when a settings configuration is changed.
     *
     * @param settings The settings.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleSettingsModified(
            SettingsInfo settings,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /**
     * Handles the event fired post change to a settings configuration.
     *
     * @param settings The settings.
     */
    void handleSettingsPostModified(SettingsInfo settings);

    /**
     * Handles the event fired when a settings configuration is removed.
     *
     * @param settings The settings.
     */
    void handleSettingsRemoved(SettingsInfo settings);

    /**
     * Handles a change to the logging configuration.
     *
     * @param logging The logging config object.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleLoggingChange(
            LoggingInfo logging,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /** Handles the event fired post change to logging configuration. */
    void handlePostLoggingChange(LoggingInfo logging);

    /**
     * Handles a change to a service configuration.
     *
     * @param service The service config object.
     * @param propertyNames The names of the properties that were changed.
     * @param oldValues The old values for the properties that were changed.
     * @param newValues The new values for the properties that were changed.
     */
    void handleServiceChange(
            ServiceInfo service,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues);

    /**
     * Handles the event fired post change to service configuration.
     *
     * @param service The service config object.
     */
    void handlePostServiceChange(ServiceInfo service);

    /**
     * Handles the event fired when a service configuration is removed.
     *
     * @param service The service config object.
     */
    void handleServiceRemove(ServiceInfo service);

    /** A callback notifying when GeoServer configuration has been reloaded. */
    void reloaded();
}
