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

package org.geoserver.rest.service;

import freemarker.template.ObjectWrapper;
import java.util.Collections;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.AbstractGeoServerController;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestException;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Service Settings controller */
public abstract class ServiceSettingsController<T extends ServiceInfo>
        extends AbstractGeoServerController {
    private Class<T> clazz;

    @Autowired
    public ServiceSettingsController(@Qualifier("geoServer") GeoServer geoServer, Class<T> clazz) {
        super(geoServer);
        this.clazz = clazz;
    }

    @GetMapping(
        value = {"/settings", "/workspaces/{workspaceName}/settings"},
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper serviceSettingsGet(@PathVariable(required = false) String workspaceName) {
        T service;
        if (workspaceName != null) {
            WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            if (ws == null) {
                throw new RestException(
                        "Workspace " + workspaceName + " does not exist", HttpStatus.NOT_FOUND);
            }
            service = geoServer.getService(ws, clazz);
        } else {
            service = geoServer.getService(clazz);
        }
        if (service == null) {
            String errorMessage =
                    "Service does not exist"
                            + (workspaceName == null ? "" : " for workspace " + workspaceName);
            throw new RestException(errorMessage, HttpStatus.NOT_FOUND);
        }

        return wrapObject(service, clazz);
    }

    public void serviceSettingsPut(T info, String workspaceName) {
        WorkspaceInfo ws = null;
        if (workspaceName != null) ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);

        T originalInfo;
        if (ws != null) {
            originalInfo = geoServer.getService(ws, clazz);
        } else {
            originalInfo = geoServer.getService(clazz);
        }
        if (originalInfo != null) {
            OwsUtils.copy(info, originalInfo, clazz);
            geoServer.save(originalInfo);
        } else {
            if (ws != null) {
                info.setWorkspace(ws);
            }
            geoServer.add(info);
        }
    }

    @DeleteMapping(value = "/workspaces/{workspaceName}/settings")
    public void serviceDelete(@PathVariable String workspaceName) {
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new RestException(
                    "Workspace " + workspaceName + " does not exist", HttpStatus.NOT_FOUND);
        }
        ServiceInfo serviceInfo = geoServer.getService(ws, clazz);
        if (serviceInfo != null) {
            geoServer.remove(serviceInfo);
        }
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Collections.singletonList(WorkspaceInfo.class));
    }
}
