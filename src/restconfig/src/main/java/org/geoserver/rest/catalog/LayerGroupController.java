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

package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/** Layer group controller */
@RestController
@ControllerAdvice
@RequestMapping(
    path = {
        RestBaseController.ROOT_PATH + "/layergroups",
        RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/layergroups"
    },
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class LayerGroupController extends AbstractCatalogController {
    private static final Logger LOGGER = Logging.getLogger(LayerGroupController.class);

    @Autowired
    public LayerGroupController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping
    public RestWrapper<?> getLayerGroups(@PathVariable(required = false) String workspaceName) {

        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        List<LayerGroupInfo> layerGroupInfos =
                workspaceName != null
                        ? catalog.getLayerGroupsByWorkspace(workspaceName)
                        : catalog.getLayerGroupsByWorkspace(CatalogFacade.NO_WORKSPACE);
        return wrapList(layerGroupInfos, LayerGroupInfo.class);
    }

    @GetMapping(value = "{layerGroupName}")
    public RestWrapper<?> getLayerGroup(
            @PathVariable String layerGroupName,
            @PathVariable(required = false) String workspaceName) {

        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }

        LayerGroupInfo layerGroupInfo =
                workspaceName != null
                        ? catalog.getLayerGroupByName(workspaceName, layerGroupName)
                        : catalog.getLayerGroupByName(layerGroupName);

        if (layerGroupInfo == null) {
            throw new ResourceNotFoundException(
                    "No such layer group "
                            + layerGroupName
                            + (workspaceName == null ? "" : " in workspace " + workspaceName));
        }
        return wrapObject(layerGroupInfo, LayerGroupInfo.class);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public ResponseEntity<String> layerGroupPost(
            @RequestBody LayerGroupInfo lg,
            @PathVariable(required = false) String workspaceName,
            UriComponentsBuilder builder)
            throws Exception {

        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        checkFullAdminRequired(workspaceName);

        if (lg.getLayers().isEmpty()) {
            throw new RestException("layer group must not be empty", HttpStatus.BAD_REQUEST);
        }

        // force resolve layers because catalog may leave null references
        for (int i = 0; i < lg.getLayers().size(); i++) {
            lg.getLayers().set(i, ResolvingProxy.resolve(catalog, lg.getLayers().get(i)));
        }

        if (lg.getBounds() == null) {
            LOGGER.fine("Auto calculating layer group bounds");
            new CatalogBuilder(catalog).calculateLayerGroupBounds(lg);
        }

        if (workspaceName != null) {
            lg.setWorkspace(catalog.getWorkspaceByName(workspaceName));
        }

        if (lg.getMode() == null) {
            LOGGER.fine("Setting layer group mode SINGLE");
            lg.setMode(LayerGroupInfo.Mode.SINGLE);
        }

        catalog.validate(lg, true).throwIfInvalid();
        catalog.add(lg);

        String layerGroupName = lg.getName();
        LOGGER.info("POST layer group " + layerGroupName);
        UriComponents uriComponents =
                builder.path("/layergroups/{layerGroupName}").buildAndExpand(layerGroupName);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(uriComponents.toUri());
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(layerGroupName, httpHeaders, HttpStatus.CREATED);
    }

    @PutMapping(
        value = "{layerGroupName}",
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void layerGroupPut(
            @RequestBody LayerGroupInfo lg,
            @PathVariable(required = false) String workspaceName,
            @PathVariable String layerGroupName)
            throws Exception {

        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
        checkFullAdminRequired(workspaceName);

        LOGGER.info(
                "PUT layer group "
                        + layerGroupName
                        + (workspaceName != null ? ", workspace " + workspaceName : ""));
        LayerGroupInfo original =
                workspaceName != null
                        ? catalog.getLayerGroupByName(workspaceName, layerGroupName)
                        : catalog.getLayerGroupByName(layerGroupName);

        // ensure not a name change
        if (lg.getName() != null && !lg.getName().equals(original.getName())) {
            throw new RestException("Can't change name of a layer group", HttpStatus.FORBIDDEN);
        }

        // ensure not a workspace change
        if (lg.getWorkspace() != null) {
            if (!lg.getWorkspace().equals(original.getWorkspace())) {
                throw new RestException(
                        "Can't change the workspace of a layer group, instead "
                                + "DELETE from existing workspace and POST to new workspace",
                        HttpStatus.FORBIDDEN);
            }
        }

        new CatalogBuilder(catalog).updateLayerGroup(original, lg);
        catalog.save(original);
    }

    @DeleteMapping(value = "{layerGroupName}")
    public void layerGroupDelete(
            @PathVariable(required = false) String workspaceName,
            @PathVariable String layerGroupName) {

        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }

        LOGGER.info("DELETE layer group " + layerGroupName);
        LayerGroupInfo lg =
                workspaceName == null
                        ? catalog.getLayerGroupByName(layerGroupName)
                        : catalog.getLayerGroupByName(workspaceName, layerGroupName);
        catalog.remove(lg);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return LayerGroupInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<LayerGroupInfo> getObjectClass() {
                        return LayerGroupInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String workspace = uriTemplateVars.get("workspaceName");
                        String layerGroup = uriTemplateVars.get("layerGroupName");

                        if (layerGroup == null) {
                            return null;
                        }
                        return catalog.getLayerGroupByName(workspace, layerGroup);
                    }

                    @Override
                    protected void postEncodeReference(
                            Object obj,
                            String ref,
                            String prefix,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        if (obj instanceof StyleInfo) {
                            StringBuilder link = new StringBuilder();
                            if (prefix != null) {
                                link.append("/workspaces/").append(converter.encode(prefix));
                            }
                            link.append("/styles/").append(converter.encode(ref));
                            converter.encodeLink(link.toString(), writer);
                        }
                        if (obj instanceof LayerInfo) {
                            converter.encodeLink(
                                    "/workspaces/" + prefix + "/layers/" + converter.encode(ref),
                                    writer);
                        } else if (obj instanceof LayerGroupInfo) {
                            LayerGroupInfo lg = (LayerGroupInfo) obj;
                            if (lg.getWorkspace() != null) {
                                converter.encodeLink(
                                        "/workspaces/"
                                                + lg.getWorkspace().getName()
                                                + "/layergroups/"
                                                + converter.encode(ref),
                                        writer);
                            } else {
                                converter.encodeLink(
                                        "/layergroups/" + converter.encode(ref), writer);
                            }
                        }
                    }
                });
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<LayerGroupInfo>(LayerGroupInfo.class) {
            @Override
            protected void wrapInternal(
                    Map<String, Object> properties, SimpleHash model, LayerGroupInfo layerGroup) {
                if (properties == null) {
                    properties = hashToProperties(model);
                }
                List<Map<String, Map<String, String>>> layerProps = new ArrayList<>();
                for (PublishedInfo info : layerGroup.getLayers()) {
                    Map<String, String> props = new HashMap<>();
                    if (info != null) {
                        props.put("name", info.getName());
                        props.put("prefixedName", info.prefixedName());
                    }
                    layerProps.add(Collections.singletonMap("properties", props));
                }
                properties.put("layers", layerProps);

                List<Map<String, Map<String, String>>> styleProps = new ArrayList<>();
                for (StyleInfo info : layerGroup.getStyles()) {
                    Map<String, String> props = new HashMap<>();
                    if (info != null) {
                        props.put("name", info.getName());
                        if (info.getWorkspace() != null) {
                            props.put("workspace", info.getWorkspace().getName());
                        }
                    }
                    styleProps.add(Collections.singletonMap("properties", props));
                }
                properties.put("styles", styleProps);
            }

            @Override
            protected void wrapInternal(
                    SimpleHash model, @SuppressWarnings("rawtypes") Collection object) {
                for (Object l : object) {
                    LayerGroupInfo lg = (LayerGroupInfo) l;
                    wrapInternal(null, model, lg);
                }
            }
        };
    }
}
