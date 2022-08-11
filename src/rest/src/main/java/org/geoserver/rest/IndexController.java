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

import static org.geoserver.template.TemplateUtils.FM_VERSION;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleHash;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The IndexController lists the paths available for the Spring MVC RequestMappingHandler
 * Specifically, it auto-generates an index page containing all non-templated paths relative to the
 * router root.
 */
@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH,
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class IndexController extends RestBaseController {

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @GetMapping(
        value = {"", "index"},
        produces = {MediaType.TEXT_HTML_VALUE}
    )
    public RestWrapper get() {

        SimpleHash model = new SimpleHash(new DefaultObjectWrapper(FM_VERSION));
        model.put("links", getLinks());
        model.put("page", RequestInfo.get());

        return wrapObject(model, SimpleHash.class);
    }

    protected Set<String> getLinks() {

        // Ensure sorted, unique keys
        Set<String> s = new TreeSet<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                this.requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> item : handlerMethods.entrySet()) {
            RequestMappingInfo mapping = item.getKey();

            // Only list "get" endpoints
            if (mapping.getMethodsCondition().getMethods().contains(RequestMethod.GET)) {
                for (String pattern : mapping.getPatternsCondition().getPatterns()) {

                    if (!pattern.contains("{")) {

                        String path = pattern;
                        // exclude other rest apis, like gwc/rest
                        final int rootSize = RestBaseController.ROOT_PATH.length() + 1;
                        if (path.startsWith(RestBaseController.ROOT_PATH)
                                && path.length() > rootSize) {
                            // trim root path
                            path = path.substring(rootSize);

                            if (path.endsWith("/**")) {
                                path = path.substring(0, path.length() - 3);
                            }
                            if (path.length() > 0) {
                                s.add(path);
                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    @Override
    public String getTemplateName(Object o) {
        return "index";
    }
}
