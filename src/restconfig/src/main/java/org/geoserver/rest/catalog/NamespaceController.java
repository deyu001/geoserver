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
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/namespaces",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class NamespaceController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(NamespaceController.class);

    @Autowired
    public NamespaceController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        value = "/{namespaceName}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper<NamespaceInfo> namespaceGet(@PathVariable String namespaceName) {

        NamespaceInfo namespace = catalog.getNamespaceByPrefix(namespaceName);
        if (namespace == null) {
            throw new ResourceNotFoundException("No such namespace: '" + namespaceName + "' found");
        }

        LOGGER.info("GET " + namespaceName);
        LOGGER.info("got " + namespace.getName());

        return wrapObject(namespace, NamespaceInfo.class);
    }

    @GetMapping
    public RestWrapper getNamespaces() {

        List<NamespaceInfo> wkspaces = catalog.getNamespaces();
        return wrapList(wkspaces, NamespaceInfo.class);
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
    public ResponseEntity<String> namespacePost(
            @RequestBody NamespaceInfo namespace, UriComponentsBuilder builder) {

        catalog.add(namespace);
        String name = namespace.getName();
        LOGGER.info("Added namespace " + name);

        // JD: we need to keep namespace and workspace in sync, so create a worksapce
        // if one does not already exists, we can remove this once we get to a point
        // where namespace is just an attribute on a layer, and not a containing element
        if (catalog.getWorkspaceByName(namespace.getPrefix()) == null) {
            WorkspaceInfo ws = catalog.getFactory().createWorkspace();
            ws.setName(namespace.getPrefix());
            ws.setIsolated(namespace.isIsolated());
            catalog.add(ws);
        }

        // build the new path
        UriComponents uriComponents = getUriComponents(name, builder);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(name, headers, HttpStatus.CREATED);
    }

    @PutMapping(
        value = "/{prefix}",
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE
        }
    )
    public void namespacePut(
            @RequestBody NamespaceInfo namespace,
            @PathVariable String prefix,
            UriComponentsBuilder builder) {

        if ("default".equals(prefix)) {
            catalog.setDefaultNamespace(namespace);
        } else {
            // name must exist
            NamespaceInfo nsi = catalog.getNamespaceByPrefix(prefix);
            if (nsi == null) {
                throw new RestException(
                        "Can't change a non existant namespace (" + prefix + ")",
                        HttpStatus.NOT_FOUND);
            }

            String infoName = namespace.getName();
            if (infoName != null && !prefix.equals(infoName)) {
                throw new RestException("Can't change name of namespace", HttpStatus.FORBIDDEN);
            }

            new CatalogBuilder(catalog).updateNamespace(nsi, namespace);
            catalog.save(nsi);
        }
    }

    @DeleteMapping(path = "/{prefix}")
    protected void namespaceDelete(@PathVariable String prefix) {

        NamespaceInfo ns = catalog.getNamespaceByPrefix(prefix);
        if (prefix.equals("default")) {
            throw new RestException(
                    "Can't delete the default namespace", HttpStatus.METHOD_NOT_ALLOWED);
        }
        if (ns == null) {
            throw new RestException("Namespace '" + prefix + "' not found", HttpStatus.NOT_FOUND);
        }
        if (!catalog.getResourcesByNamespace(ns, ResourceInfo.class).isEmpty()) {
            throw new RestException("Namespace not empty", HttpStatus.UNAUTHORIZED);
        }
        catalog.remove(ns);
    }

    private UriComponents getUriComponents(String name, UriComponentsBuilder builder) {
        UriComponents uriComponents;

        uriComponents = builder.path("/namespaces/{id}").buildAndExpand(name);

        return uriComponents;
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<NamespaceInfo>(NamespaceInfo.class) {
            @Override
            protected void wrapInternal(
                    Map<String, Object> properties, SimpleHash model, NamespaceInfo namespace) {
                if (properties == null) {
                    properties = hashToProperties(model);
                }

                NamespaceInfo def = catalog.getDefaultNamespace();
                if (def.equals(namespace)) {
                    properties.put("isDefault", Boolean.TRUE);
                } else {
                    properties.put("isDefault", Boolean.FALSE);
                }
                List<Map<String, Map<String, String>>> resources = new ArrayList<>();
                List<ResourceInfo> res =
                        catalog.getResourcesByNamespace(namespace, ResourceInfo.class);
                for (ResourceInfo r : res) {
                    HashMap<String, String> props = new HashMap<>();
                    props.put("name", r.getName());
                    props.put("description", r.getDescription());
                    resources.add(Collections.singletonMap("properties", props));
                }

                properties.put("resources", resources);
            }

            @Override
            protected void wrapInternal(
                    SimpleHash model, @SuppressWarnings("rawtypes") Collection object) {

                for (Object w : object) {
                    NamespaceInfo ns = (NamespaceInfo) w;
                    wrapInternal(null, model, ns);
                }
            }
        };
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return NamespaceInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.setCallback(
                new XStreamPersister.Callback() {
                    @Override
                    protected Class<NamespaceInfo> getObjectClass() {
                        return NamespaceInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = getURITemplateVariables();
                        String prefix = uriTemplateVars.get("namespaceName");

                        if (prefix == null) {
                            return null;
                        }
                        return catalog.getNamespaceByPrefix(prefix);
                    }

                    @Override
                    protected void postEncodeNamespace(
                            NamespaceInfo cs,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {}

                    @Override
                    protected void postEncodeReference(
                            Object obj,
                            String ref,
                            String prefix,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {
                        if (obj instanceof NamespaceInfo) {
                            converter.encodeLink("/namespaces/" + converter.encode(ref), writer);
                        }
                    }
                });
    }
}
