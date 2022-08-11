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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for freemarker templates.
 *
 * <ul>
 *   <li>templates
 *   <li>templates/{templateName}.ftl
 *   <li>workspaces/{workspaceName}/templates
 *   <li>workspaces/{workspaceName}/templates/{template>.ftl
 *   <li>workspaces/{workspaceName}/datastores/{storeName}/templates
 *   <li>workspaces/{workspaceName}/datastores/{storeName}/templates/{templateName}.ftl
 *   <li>workspaces/{workspaceName}/datastores/{storeName}/featuretypes/{featureTypeName}/templates
 *   <li>workspaces/{workspaceName}/datastores/{storeName}/teaturetypes/{featureTypeName}/templates/{templateName}.ftl
 *   <li>workspaces/{workspaceName}/coveragestores/{storeName}/coverage/{featureTypeName}/templates
 *   <li>workspaces/{workspaceName}/coveragestores/{storeName}/coverage/{featureTypeName}/templates/{templateName}.ftl
 * </ul>
 *
 * @author Jody Garnett (Boundless)
 */
@RestController
@ControllerAdvice
@RequestMapping(
    path = {
        RestBaseController.ROOT_PATH + "/templates",
        RestBaseController.ROOT_PATH + "/workspaces/{workspaceName}/templates",
        RestBaseController.ROOT_PATH
                + "/workspaces/{workspaceName}/datastores/{storeName}/templates",
        RestBaseController.ROOT_PATH
                + "/workspaces/{workspaceName}/datastores/{storeName}/featuretypes/{featureTypeName}/templates",
        RestBaseController.ROOT_PATH
                + "/workspaces/{workspaceName}/coveragestores/{storeName}/templates",
        RestBaseController.ROOT_PATH
                + "/workspaces/{workspaceName}/coveragestores/{storeName}/coverages/{featureTypeName}/templates"
    }
)
public class TemplateController extends AbstractCatalogController {

    private GeoServerResourceLoader resources;
    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");

    @Autowired
    public TemplateController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
        resources = catalog.getResourceLoader();
    }

    /** Template definition. */
    @DeleteMapping(value = "/{templateName}")
    public void templateDelete(
            HttpServletResponse response,
            @PathVariable(required = false) String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable(required = false) String featureTypeName,
            @PathVariable String templateName) {

        String filename = templateName + "." + MediaTypeExtensions.FTL_EXTENSION;
        String path = Paths.path(path(workspaceName, storeName, featureTypeName), filename);
        Resource resource = resources.get(path);

        if (resource.getType() != Type.RESOURCE) {
            throw new ResourceNotFoundException("Template not found: '" + path + "'");
        }
        boolean removed = resource.delete();
        if (!removed) {
            throw new RestException(
                    "Template '" + path + "' not removed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Template definition. */
    @GetMapping(
        value = "/{templateName}",
        produces = {MediaTypeExtensions.TEXT_FTL_VALUE}
    )
    public void templateGet(
            @PathVariable(required = false) String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable(required = false) String featureTypeName,
            @PathVariable String templateName,
            HttpServletResponse response) {

        String filename = templateName + "." + MediaTypeExtensions.FTL_EXTENSION;
        String path = Paths.path(path(workspaceName, storeName, featureTypeName), filename);
        Resource resource = resources.get(path);

        if (resource.getType() != Type.RESOURCE) {
            throw new ResourceNotFoundException("Template not found: '" + path + "'");
        }
        byte[] bytes;
        try {
            bytes = resource.getContents();

            response.setContentType(MediaTypeExtensions.TEXT_FTL_VALUE);
            response.setContentLength(bytes.length);

            try (ServletOutputStream output = response.getOutputStream()) {
                output.write(bytes);
                output.flush();
            }
        } catch (IOException problem) {
            throw new RestException(
                    problem.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, problem);
        }
    }

    /** All templates as JSON, XML or HTML. */
    @PutMapping(
        value = "/{templateName}",
        consumes = {MediaTypeExtensions.TEXT_FTL_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    public void templatePut(
            @PathVariable(required = false) String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable(required = false) String featureTypeName,
            @PathVariable String templateName,
            HttpServletRequest request) {

        String filename = templateName + "." + MediaTypeExtensions.FTL_EXTENSION;
        String path = path(workspaceName, storeName, featureTypeName);
        Resource directory = resources.get(path);

        Resource resource = fileUpload(directory, filename, request);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("PUT template: " + resource.path());
        }
    }

    //
    // List Templates
    //
    // These endpoints return a list of FreeMarkerTemplateInfo, that is converted to the appropriate
    // output.
    //
    /**
     * All templates as JSON, XML or HTML.
     *
     * @return All templates
     */
    @GetMapping(
        produces = {
            MediaType.TEXT_HTML_VALUE, // this is the default
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper<TemplateInfo> templatesGet(
            @PathVariable(required = false) String workspaceName,
            @PathVariable(required = false) String storeName,
            @PathVariable(required = false) String featureTypeName) {

        String path = path(workspaceName, storeName, featureTypeName);
        Resource directory = resources.get(path);
        switch (directory.getType()) {
            case RESOURCE:
            case UNDEFINED:
                throw new ResourceNotFoundException("Directory not found: '" + path + "'");
            default:
                List<Resource> files =
                        Resources.list(directory, new Resources.ExtensionFilter("FTL"), false);
                List<TemplateInfo> list = new ArrayList<>();
                for (Resource file : files) {
                    list.add(new TemplateInfo(file));
                }
                return wrapList(list, TemplateInfo.class);
        }
    }
    /** Verifies mime type */
    private Resource fileUpload(Resource directory, String filename, HttpServletRequest request) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(
                    "PUT file: mimetype="
                            + request.getContentType()
                            + ", path="
                            + directory.path());
        }
        try {
            return RESTUtils.handleBinUpload(filename, directory, false, request);
        } catch (IOException problem) {
            throw new RestException(
                    problem.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, problem);
        }
    }

    /**
     * Construct "get directory path"
     *
     * @param workspace Workspace, optional
     * @param store DataStore or Coverage store, requires workspace
     * @param type FeatureType or Coverage, requires store
     * @return template path
     */
    public static String path(String workspace, String store, String type) {
        List<String> path = new ArrayList<>();
        path.add("workspaces");

        if (workspace != null) {
            path.add(workspace);
            if (store != null) {
                path.add(store);
                if (type != null) {
                    path.add(type);
                }
            }
        }
        return Paths.path(path.toArray(new String[] {}));
    }
}
