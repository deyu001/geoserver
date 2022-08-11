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

package org.geoserver.geofence.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.geofence.GeofenceAccessManager;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceConfigurationManager {

    private static final Logger LOGGER = Logging.getLogger(GeofenceAccessManager.class);

    private GeoFencePropertyPlaceholderConfigurer configurer;

    private GeoFenceConfiguration geofenceConfiguration;

    private CacheConfiguration cacheConfiguration;

    public GeoFenceConfiguration getConfiguration() {
        return geofenceConfiguration;
    }

    /** Updates the configuration. */
    public void setConfiguration(GeoFenceConfiguration configuration) {

        this.geofenceConfiguration = configuration;

        LOGGER.log(
                Level.INFO,
                "GeoFence configuration: instance name is {0}",
                configuration.getInstanceName());
    }

    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    public void storeConfiguration() throws IOException {
        Resource configurationFile = configurer.getConfigFile();

        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(configurationFile.out()))) {
            writer.write("### GeoFence Module configuration file\n");
            writer.write("### \n");
            writer.write("### GeoServer will read this file at boot time.\n");
            writer.write(
                    "### This file may be automatically regenerated by GeoServer, so any changes beside the property values may be lost.\n\n");

            saveConfiguration(writer, geofenceConfiguration);
            saveConfiguration(writer, cacheConfiguration);
        }
    }

    /** Saves current configuration to disk. */
    protected void saveConfiguration(Writer writer, GeoFenceConfiguration configuration)
            throws IOException {

        writer.write("### GeoFence main configuration\n\n");

        saveConfig(writer, "instanceName", configuration.getInstanceName());
        saveConfig(writer, "servicesUrl", configuration.getServicesUrl());
        saveConfig(
                writer, "allowRemoteAndInlineLayers", configuration.isAllowRemoteAndInlineLayers());
        saveConfig(
                writer,
                "grantWriteToWorkspacesToAuthenticatedUsers",
                configuration.isGrantWriteToWorkspacesToAuthenticatedUsers());
        saveConfig(writer, "useRolesToFilter", configuration.isUseRolesToFilter());
        saveConfig(writer, "acceptedRoles", configuration.getAcceptedRoles());
        saveConfig(writer, "gwc.context.suffix", configuration.getGwcContextSuffix());
        saveConfig(
                writer,
                "org.geoserver.rest.DefaultUserGroupServiceName",
                configuration.getDefaultUserGroupServiceName());
    }

    protected void saveConfig(Writer writer, String name, Object value) throws IOException {
        writer.write(name + "=" + String.valueOf(value) + "\n");
    }

    public void saveConfiguration(Writer writer, CacheConfiguration params) throws IOException {

        writer.write("\n\n### Cache configuration\n\n");

        saveConfig(writer, "cacheSize", params.getSize());
        saveConfig(writer, "cacheRefresh", params.getRefreshMilliSec());
        saveConfig(writer, "cacheExpire", params.getExpireMilliSec());
    }

    /** Returns a copy of the configuration. */
    public void setConfigurer(GeoFencePropertyPlaceholderConfigurer configurer) {
        this.configurer = configurer;
    }
}
