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


package org.geoserver.netcdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.imageio.netcdf.NetCDFUnitFormat;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.logging.Logging;

/** Re-configures the {@link NetCDFUnitFormat} on config reload */
public class NetCDFUnitsConfigurator implements GeoServerLifecycleHandler {

    static final Logger LOGGER = Logging.getLogger(NetCDFUnitsConfigurator.class);

    public static String NETCDF_UNIT_ALIASES = "NETCDF_UNIT_ALIASES";

    public static String NETCDF_UNIT_REPLACEMENTS = "NETCDF_UNIT_REPLACEMENTS";
    private final GeoServerResourceLoader resourceLoader;

    public NetCDFUnitsConfigurator(GeoServerResourceLoader resourceLoader) throws IOException {
        this.resourceLoader = resourceLoader;
        configure();
    }

    private void configure() {
        try {
            LinkedHashMap<String, String> aliases =
                    getMapResource(NETCDF_UNIT_ALIASES, NetCDFUnitFormat.NETCDF_UNIT_ALIASES);
            if (aliases != null) {
                NetCDFUnitFormat.setAliases(aliases);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load NetCDF unit aliases", e);
        }

        try {
            LinkedHashMap<String, String> replacements =
                    getMapResource(
                            NETCDF_UNIT_REPLACEMENTS, NetCDFUnitFormat.NETCDF_UNIT_REPLACEMENTS);
            if (replacements != null) {
                NetCDFUnitFormat.setReplacements(replacements);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load NetCDF unit replacements", e);
        }
    }

    /**
     * Searches for a config file with an absolute path, or inside the NetCDF data dir, or inside
     * the GeoServer data dir. Will return a map with the contents of the property file, with the
     * same order as the file contents.
     */
    private LinkedHashMap<String, String> getMapResource(
            String absolutePathProperty, String defaultFileName) throws IOException {
        Resource aliasResource = getResource(absolutePathProperty, defaultFileName);
        if (aliasResource != null) {
            try (InputStream is = aliasResource.in()) {
                return NetCDFUnitFormat.loadPropertiesOrdered(is);
            }
        }

        return null;
    }

    /**
     * Searches for a config file with an absolute path, or inside the NetCDF data dir, or inside
     * the GeoServer data dir. Will return a resource for the searched file, but only if it was
     * found, null otherwise.
     */
    private Resource getResource(String absolutePathProperty, String defaultFileName) {
        String source = GeoServerExtensions.getProperty(absolutePathProperty);
        // check the in path provided by the user, if any (the method called is null safe)
        Resource resource = getResourceForPath(source);
        // if not found search the NetCDF Data Directory
        if (resource == null && NetCDFUtilities.EXTERNAL_DATA_DIR != null) {
            source = new File(NetCDFUtilities.EXTERNAL_DATA_DIR, defaultFileName).getPath();
            resource = getResourceForPath(source);
        }
        // if still not found search the GeoServer Data Directory
        if (resource == null) {
            resource = resourceLoader.get(NetCDFUnitFormat.NETCDF_UNIT_ALIASES);
            if (resource.getType() != Resource.Type.RESOURCE) {
                resource = null;
            }
        }

        return resource;
    }

    /**
     * Gets a Resource from file system building the necessary wrappers (if the file is found), or
     * returns null instead.
     */
    private Resource getResourceForPath(String path) {
        Resource resource = null;
        if (path != null) {
            File resourceFile = new File(path);
            if (resourceFile.exists()) {
                FileSystemResourceStore store =
                        new FileSystemResourceStore(resourceFile.getParentFile());
                resource = store.get(resourceFile.getName());
            } else {
                LOGGER.fine("Could not locate " + path + ", moving on");
            }
        }

        return resource;
    }

    @Override
    public void onReset() {
        NetCDFUtilities.clearCaches();
        configure();
    }

    @Override
    public void onDispose() {
        // nothing to do
    }

    @Override
    public void beforeReload() {
        NetCDFUtilities.clearCaches();
        // better reload the unit config before reloading the catalog
        configure();
    }

    @Override
    public void onReload() {
        // nothing to do
    }
}
