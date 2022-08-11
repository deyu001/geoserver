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

package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.CatalogUtil;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetadataSyncTaskTypeImpl implements TaskType {

    private static final Logger LOGGER = Logging.getLogger(MetadataSyncTaskTypeImpl.class);

    public static final String NAME = "MetadataSync";

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_WORKSPACE = "workspace";

    public static final String PARAM_LAYER = "layer";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @Autowired protected Catalog catalog;

    @Autowired protected ExtTypes extTypes;

    @Autowired protected CatalogUtil catalogUtil;

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_EXT_GS, new ParameterInfo(PARAM_EXT_GS, extTypes.extGeoserver, true));
        ParameterInfo paramWorkspace =
                new ParameterInfo(PARAM_WORKSPACE, extTypes.workspace, false);
        paramInfo.put(PARAM_WORKSPACE, paramWorkspace);
        paramInfo.put(
                PARAM_LAYER,
                new ParameterInfo(PARAM_LAYER, extTypes.internalLayer, true)
                        .dependsOn(false, paramWorkspace));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final ExternalGS extGS = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
        final LayerInfo layer = (LayerInfo) ctx.getParameterValues().get(PARAM_LAYER);
        final ResourceInfo resource = layer.getResource();
        final StoreInfo store = resource.getStore();
        final StoreType storeType =
                store instanceof CoverageStoreInfo
                        ? StoreType.COVERAGESTORES
                        : StoreType.DATASTORES;
        final String ws = store.getWorkspace().getName();

        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }

        if (!restManager.getReader().existGeoserver()) {
            throw new TaskException("Failed to connect to geoserver " + extGS.getUrl());
        }

        RESTLayer restLayer = restManager.getReader().getLayer(ws, layer.getName());

        if (restLayer == null) {
            throw new TaskException("Layer does not exist on destination " + layer.getName());
        }
        String storeName;

        Pattern pattern =
                Pattern.compile("rest/workspaces/" + ws + "/" + storeType.toString() + "/([^/]*)/");
        Matcher matcher = pattern.matcher(restLayer.getResourceUrl());
        if (matcher.find()) {
            storeName = matcher.group(1);
        } else {
            throw new TaskException("Couldn't determine store name for " + layer.getName());
        }
        // sync resource
        GSResourceEncoder re = catalogUtil.syncMetadata(resource);
        if (!restManager.getPublisher().configureResource(ws, storeType, storeName, re)) {
            throw new TaskException(
                    "Failed to configure resource " + ws + ":" + resource.getName());
        }

        // sync styles
        final Set<String> createWorkspaces = new HashSet<String>();
        final Set<StyleInfo> styles = new HashSet<StyleInfo>(layer.getStyles());
        styles.add(layer.getDefaultStyle());
        for (StyleInfo si : styles) {
            if (si != null) {
                String wsStyle = CatalogUtil.wsName(si.getWorkspace());
                if (!restManager.getReader().existsStyle(wsStyle, si.getName())) {
                    if (wsStyle != null && !restManager.getReader().existsWorkspace(wsStyle)) {
                        createWorkspaces.add(wsStyle);
                    }
                }
            }
        }
        for (String newWs : createWorkspaces) { // workspace doesn't exist yet, publish
            LOGGER.log(
                    Level.INFO,
                    "Workspace doesn't exist: " + newWs + " on " + extGS.getName() + ", creating.");
            try {
                if (!restManager
                        .getPublisher()
                        .createWorkspace(
                                newWs, new URI(catalog.getNamespaceByPrefix(newWs).getURI()))) {
                    throw new TaskException("Failed to create workspace " + newWs);
                }
            } catch (URISyntaxException e) {
                throw new TaskException("Failed to create workspace " + newWs, e);
            }
        }

        for (StyleInfo si : styles) {
            LOGGER.log(Level.INFO, "Synchronizing style : " + si.getName());
            String wsName = CatalogUtil.wsName(si.getWorkspace());
            if (!(restManager.getStyleManager().existsStyle(wsName, si.getName())
                    ? restManager
                            .getStyleManager()
                            .updateStyleZippedInWorkspace(
                                    wsName, catalogUtil.createStyleZipFile(si), si.getName())
                    : restManager
                            .getStyleManager()
                            .publishStyleZippedInWorkspace(
                                    wsName, catalogUtil.createStyleZipFile(si), si.getName()))) {
                throw new TaskException("Failed to create style " + si.getName());
            }
            if (!restManager
                    .getStyleManager()
                    .updateStyleInWorkspace(
                            CatalogUtil.wsName(si.getWorkspace()),
                            catalogUtil.syncStyle(si),
                            si.getName())) {
                throw new TaskException("Failed to sync style " + si.getName());
            }
        }

        // sync layer
        final GSLayerEncoder layerEncoder = new GSLayerEncoder();
        layerEncoder.setDefaultStyle(
                layer.getDefaultStyle().getWorkspace() == null
                        ? null
                        : layer.getDefaultStyle().getWorkspace().getName(),
                layer.getDefaultStyle().getName());
        for (StyleInfo si : layer.getStyles()) {
            layerEncoder.addStyle(
                    si.getWorkspace() != null
                            ? CatalogUtil.wsName(si.getWorkspace()) + ":" + si.getName()
                            : si.getName());
        }

        if (!restManager.getPublisher().configureLayer(ws, layer.getName(), layerEncoder)) {
            throw new TaskException("Failed to configure layer " + ws + ":" + resource.getName());
        }

        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                // do nothing, it's already done
            }

            @Override
            public void rollback() throws TaskException {
                throw new TaskException("Cannot roll-back metadata synchronisation task");
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        throw new TaskException("unsupported");
    }

    @Override
    public boolean supportsCleanup() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
