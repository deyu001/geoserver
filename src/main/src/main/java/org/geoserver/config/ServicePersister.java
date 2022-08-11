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

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public class ServicePersister extends ConfigurationListenerAdapter {

    static Logger LOGGER = Logging.getLogger("org.geoserver");

    List<XStreamServiceLoader<ServiceInfo>> loaders;
    GeoServer geoServer;
    GeoServerResourceLoader resourceLoader;

    public ServicePersister(List<XStreamServiceLoader<ServiceInfo>> loaders, GeoServer geoServer) {
        this.loaders = loaders;
        this.geoServer = geoServer;
        this.resourceLoader = geoServer.getCatalog().getResourceLoader();
    }

    @Override
    public void handleServiceChange(
            ServiceInfo service,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {

        XStreamServiceLoader loader = findServiceLoader(service);

        // handle the case of a service changing workspace and move the file
        int i = propertyNames.indexOf("workspace");
        if (i != -1) {
            // TODO: share code with GeoServerPersister
            WorkspaceInfo old = (WorkspaceInfo) oldValues.get(i);
            if (old != null) {
                WorkspaceInfo ws = (WorkspaceInfo) newValues.get(i);
                Resource f;
                try {
                    f = dir(ws).get(loader.getFilename());
                    f.renameTo(dir(ws).get(loader.getFilename()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void handlePostServiceChange(ServiceInfo service) {
        XStreamServiceLoader<ServiceInfo> loader = findServiceLoader(service);

        try {
            // TODO: handle workspace move, factor this class out into
            // separate persister class
            Resource directory =
                    service.getWorkspace() != null ? dir(service.getWorkspace()) : null;
            loader.save(service, geoServer, directory);
        } catch (Throwable t) {
            throw new RuntimeException(t);
            // LOGGER.log(Level.SEVERE, "Error occurred while saving configuration", t);
        }
    }

    public void handleServiceRemove(ServiceInfo service) {
        XStreamServiceLoader loader = findServiceLoader(service);
        try {
            Resource dir =
                    service.getWorkspace() != null
                            ? dir(service.getWorkspace())
                            : resourceLoader.get(Paths.BASE);
            dir.get(loader.getFilename()).delete();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    <T extends ServiceInfo> XStreamServiceLoader<T> findServiceLoader(T service) {
        XStreamServiceLoader loader = null;
        for (XStreamServiceLoader<ServiceInfo> l : loaders) {
            if (l.getServiceClass().isInstance(service)) {
                loader = l;
                break;
            }
        }

        if (loader == null) {
            throw new IllegalArgumentException("No loader for " + service.getName());
        }
        @SuppressWarnings("unchecked")
        XStreamServiceLoader<T> result = loader;
        return result;
    }

    Resource dir(WorkspaceInfo ws) throws IOException {
        return resourceLoader.get(Paths.path("workspaces", ws.getName()));
    }
}
