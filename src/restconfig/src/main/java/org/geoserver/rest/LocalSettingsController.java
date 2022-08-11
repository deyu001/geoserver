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

package org.geoserver.rest;

import freemarker.template.ObjectWrapper;
import java.lang.reflect.Type;
import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local Settings controller
 *
 * <p>Provides access to workspace-specific settings
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/settings")
public class LocalSettingsController extends AbstractGeoServerController {

    @Autowired
    public LocalSettingsController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<SettingsInfo> localSettingsGet(@PathVariable String workspaceName) {

        WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspaceName);
        SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
        if (settingsInfo == null) {
            settingsInfo = new SettingsInfoImpl();
            settingsInfo.setVerbose(false);
        }
        return wrapObject(settingsInfo, SettingsInfo.class);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        },
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public String localSettingsCreate(
            @PathVariable String workspaceName, @RequestBody SettingsInfo settingsInfo) {
        String name = "";
        if (workspaceName != null) {
            Catalog catalog = geoServer.getCatalog();
            WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspaceName);
            settingsInfo.setWorkspace(workspaceInfo);
            geoServer.add(settingsInfo);
            geoServer.save(geoServer.getSettings(workspaceInfo));
            name = settingsInfo.getWorkspace().getName();
        }
        return name;
    }

    @PutMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void localSettingsPut(
            @PathVariable String workspaceName, @RequestBody SettingsInfo settingsInfo) {
        if (workspaceName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            SettingsInfo original = geoServer.getSettings(workspaceInfo);
            if (original == null) {
                settingsInfo.setWorkspace(workspaceInfo);
                geoServer.add(settingsInfo);
                geoServer.save(geoServer.getSettings(workspaceInfo));
            } else {
                OwsUtils.copy(settingsInfo, original, SettingsInfo.class);
                original.setWorkspace(workspaceInfo);
                geoServer.save(original);
            }
        }
    }

    @DeleteMapping
    public void localSetingsDelete(@PathVariable String workspaceName) {
        if (workspaceName != null) {
            WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName(workspaceName);
            SettingsInfo settingsInfo = geoServer.getSettings(workspaceInfo);
            geoServer.remove(settingsInfo);
        }
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return SettingsInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected String getTemplateName(Object object) {
        return "localSettings";
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Collections.singletonList(WorkspaceInfo.class));
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setHideFeatureTypeAttributes();
        persister.getXStream().alias("contact", ContactInfo.class);
    }
}
