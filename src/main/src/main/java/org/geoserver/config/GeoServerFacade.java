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

import java.util.Collection;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Data access facade for geoserver configuration.
 *
 * @author ETj <etj at geo-solutions.it>
 * @author Justin Deoliveira, OpenGeo
 */
public interface GeoServerFacade {

    GeoServer getGeoServer();

    void setGeoServer(GeoServer geoServer);

    /** The global geoserver configuration. */
    GeoServerInfo getGlobal();

    /** Sets the global configuration. */
    void setGlobal(GeoServerInfo global);

    /** Saves the global geoserver configuration after modification. */
    void save(GeoServerInfo geoServer);

    /**
     * The settings configuration for the specified workspoace, or <code>null</code> if non exists.
     */
    SettingsInfo getSettings(WorkspaceInfo workspace);

    /** Adds a settings configuration for the specified workspace. */
    void add(SettingsInfo settings);

    /** Saves the settings configuration for the specified workspace. */
    void save(SettingsInfo settings);

    /** Removes the settings configuration for the specified workspace. */
    void remove(SettingsInfo settings);

    /** The logging configuration. */
    LoggingInfo getLogging();

    /** Sets logging configuration. */
    void setLogging(LoggingInfo logging);

    /** Saves the logging configuration. */
    void save(LoggingInfo logging);

    /** Adds a service to the configuration. */
    void add(ServiceInfo service);

    /** Removes a service from the configuration. */
    void remove(ServiceInfo service);

    /** Saves a service that has been modified. */
    void save(ServiceInfo service);

    /** GeoServer services. */
    Collection<? extends ServiceInfo> getServices();

    /** GeoServer services specific to the specified workspace. */
    Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace);

    /**
     * GeoServer global service filtered by class.
     *
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> T getService(Class<T> clazz);

    /**
     * GeoServer service specific to the specified workspace and filtered by class.
     *
     * @param workspace The workspace the service is specific to.
     * @param clazz The class of the service to return.
     */
    <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz);

    /**
     * Looks up a service by id.
     *
     * @param id The id of the service.
     * @param clazz The type of the service.
     * @return The service with the specified id, or <code>null</code> if no such service coud be
     *     found.
     */
    <T extends ServiceInfo> T getService(String id, Class<T> clazz);

    /**
     * Looks up a service by name.
     *
     * @param name The name of the service.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz);

    /**
     * Looks up a service by name, specific to the specified workspace.
     *
     * @param name The name of the service.
     * @param workspace The workspace the service is specific to.
     * @param clazz The type of the service.
     * @return The service with the specified name or <code>null</code> if no such service could be
     *     found.
     */
    <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz);

    /** Disposes the configuration. */
    void dispose();
}
