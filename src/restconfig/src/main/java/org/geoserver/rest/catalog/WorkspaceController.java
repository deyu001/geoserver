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
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/workspaces",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class WorkspaceController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(WorkspaceController.class);

    @Autowired
    public WorkspaceController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping
    public RestWrapper workspacesGet() {

        List<WorkspaceInfo> wkspaces = catalog.getWorkspaces();
        return wrapList(wkspaces, WorkspaceInfo.class);
    }

    @GetMapping(value = "/{workspaceName}")
    public RestWrapper<WorkspaceInfo> workspaceGet(@PathVariable String workspaceName) {

        WorkspaceInfo wkspace = catalog.getWorkspaceByName(workspaceName);
        if (wkspace == null) {
            throw new ResourceNotFoundException("No such workspace: '" + workspaceName + "' found");
        }

        LOGGER.info("GET " + workspaceName);
        LOGGER.info("got " + wkspace.getName());

        return wrapObject(wkspace, WorkspaceInfo.class);
    }

    @PostMapping(
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> workspacePost(
            @RequestBody WorkspaceInfo workspace,
            @RequestParam(defaultValue = "false", name = "default") boolean makeDefault,
            UriComponentsBuilder builder) {

        if (catalog.getWorkspaceByName(workspace.getName()) != null) {
            throw new RestException(
                    "Workspace '" + workspace.getName() + "' already exists",
                    HttpStatus.UNAUTHORIZED);
        }
        catalog.add(workspace);
        String name = workspace.getName();
        LOGGER.info("Added workspace " + name);
        if (makeDefault) {
            catalog.setDefaultWorkspace(workspace);
            LOGGER.info("made workspace " + name + " default");
        }
        LOGGER.info("POST workspace " + name);

        // create a namespace corresponding to the workspace if one does not
        // already exist
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            LOGGER.fine("Automatically creating namespace for workspace " + workspace.getName());

            namespace = catalog.getFactory().createNamespace();
            namespace.setPrefix(workspace.getName());
            namespace.setURI("http://" + workspace.getName());
            namespace.setIsolated(workspace.isIsolated());
            catalog.add(namespace);
        }

        // build the new path
        UriComponents uriComponents = getUriComponents(name, builder);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(name, headers, HttpStatus.CREATED);
    }

    @PutMapping(
        value = "/{workspaceName}",
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public void workspacePut(
            @RequestBody WorkspaceInfo workspace,
            @PathVariable String workspaceName,
            UriComponentsBuilder builder) {

        if ("default".equals(workspaceName)) {
            catalog.setDefaultWorkspace(workspace);
        } else {
            // name must exist
            WorkspaceInfo wks = catalog.getWorkspaceByName(workspaceName);
            if (wks == null) {
                throw new RestException(
                        "Can't change a non existant workspace (" + workspaceName + ")",
                        HttpStatus.NOT_FOUND);
            }

            String infoName = workspace.getName();
            if (infoName != null && infoName.isEmpty()) {
                throw new RestException("The workspace name cannot be empty", HttpStatus.FORBIDDEN);
            }

            new CatalogBuilder(catalog).updateWorkspace(wks, workspace);
            catalog.save(wks);
        }
    }

    @DeleteMapping(path = "/{workspaceName}")
    protected void workspaceDelete(
            @PathVariable String workspaceName,
            @RequestParam(defaultValue = "false", name = "recurse") boolean recurse) {

        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);
        if (ws == null) {
            throw new RestException(
                    "Workspace '" + workspaceName + "' not found", HttpStatus.NOT_FOUND);
        }
        if (!recurse) {
            if (!catalog.getStoresByWorkspace(ws, StoreInfo.class).isEmpty()) {
                throw new RestException("Workspace not empty", HttpStatus.FORBIDDEN);
            }

            // check for "linked" workspace
            NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
            if (ns != null) {
                if (!catalog.getFeatureTypesByNamespace(ns).isEmpty()) {
                    throw new RestException(
                            "Namespace for workspace not empty.", HttpStatus.FORBIDDEN);
                }
                catalog.remove(ns);
            }

            catalog.remove(ws);
        } else {
            // recursive delete
            new CascadeDeleteVisitor(catalog).visit(ws);
        }

        LOGGER.info("DELETE workspace " + ws);
    }

    private UriComponents getUriComponents(String name, UriComponentsBuilder builder) {
        UriComponents uriComponents;

        uriComponents = builder.path("/workspaces/{id}").buildAndExpand(name);

        return uriComponents;
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<WorkspaceInfo>(WorkspaceInfo.class) {
            @Override
            protected void wrapInternal(
                    Map<String, Object> properties, SimpleHash model, WorkspaceInfo wkspace) {
                if (properties == null) {
                    properties = hashToProperties(model);
                }

                collectSources(DataStoreInfo.class, "dataStores", properties, wkspace);
                collectSources(CoverageStoreInfo.class, "coverageStores", properties, wkspace);
                collectSources(WMSStoreInfo.class, "wmsStores", properties, wkspace);
                collectSources(WMTSStoreInfo.class, "wmtsStores", properties, wkspace);

                WorkspaceInfo def = catalog.getDefaultWorkspace();
                if (def.equals(wkspace)) {
                    properties.put("isDefault", Boolean.TRUE);
                } else {
                    properties.put("isDefault", Boolean.FALSE);
                }
            }

            protected <T extends StoreInfo> void collectSources(
                    Class<T> clazz,
                    String propsName,
                    Map<String, Object> properties,
                    WorkspaceInfo wkspace) {

                List<Map<String, Map<String, String>>> dsProps = new ArrayList<>();

                List<T> wmssources = catalog.getStoresByWorkspace(wkspace, clazz);
                for (StoreInfo ds : wmssources) {
                    Map<String, String> names = new HashMap<>();
                    names.put("name", ds.getName());
                    dsProps.add(Collections.singletonMap("properties", names));
                }
                if (!dsProps.isEmpty()) properties.putIfAbsent(propsName, dsProps);
            }

            @Override
            protected void wrapInternal(
                    SimpleHash model, @SuppressWarnings("rawtypes") Collection object) {
                for (Object w : object) {
                    WorkspaceInfo wk = (WorkspaceInfo) w;
                    wrapInternal(null, model, wk);
                }
            }
        };
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return WorkspaceInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<WorkspaceInfo> getObjectClass() {
                        return WorkspaceInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String workspace = uriTemplateVars.get("workspaceName");

                        if (workspace == null) {
                            return null;
                        }
                        return catalog.getWorkspaceByName(workspace);
                    }

                    @Override
                    protected void postEncodeWorkspace(
                            WorkspaceInfo cs,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {

                        // add a link to the datastores
                        writer.startNode("dataStores");
                        converter.encodeCollectionLink("datastores", writer);
                        writer.endNode();

                        writer.startNode("coverageStores");
                        converter.encodeCollectionLink("coveragestores", writer);
                        writer.endNode();

                        writer.startNode("wmsStores");
                        converter.encodeCollectionLink("wmsstores", writer);
                        writer.endNode();

                        writer.startNode("wmtsStores");
                        converter.encodeCollectionLink("wmtsstores", writer);
                        writer.endNode();
                    }

                    @Override
                    protected void postEncodeReference(
                            Object obj,
                            String ref,
                            String prefix,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        if (obj instanceof WorkspaceInfo) {
                            converter.encodeLink("/workspaces/" + converter.encode(ref), writer);
                        }
                    }
                });
    }

    @Override
    protected String getTemplateName(Object object) {
        if (object instanceof WorkspaceInfo) {
            return "WorkspaceInfo";
        }
        return null;
    }
}
