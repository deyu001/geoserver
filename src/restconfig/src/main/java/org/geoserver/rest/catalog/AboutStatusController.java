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

import static org.geoserver.template.TemplateUtils.FM_VERSION;

import com.thoughtworks.xstream.XStream;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.platform.RenderingEngineStatus;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/about/status",
    produces = {
        MediaType.TEXT_HTML_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
    }
)
public class AboutStatusController extends RestBaseController {

    @GetMapping
    protected RestWrapper<ModuleStatus> statusGet() throws Exception {
        List<ModuleStatus> applicationStatus =
                GeoServerExtensions.extensions(ModuleStatus.class)
                        .stream()
                        .map(ModuleStatusImpl::new)
                        .collect(Collectors.toList());
        return wrapList(applicationStatus, ModuleStatus.class);
    }

    @GetMapping(value = "/{target}")
    protected RestWrapper<ModuleStatus> statusGet(@PathVariable String target) throws Exception {
        List<ModuleStatus> applicationStatus =
                GeoServerExtensions.extensions(ModuleStatus.class)
                        .stream()
                        .map(ModuleStatusImpl::new)
                        .filter(getModule(target))
                        .collect(Collectors.toList());
        if (applicationStatus.isEmpty()) {
            throw new RestException("No such module: " + target, HttpStatus.NOT_FOUND);
        }
        return wrapList(applicationStatus, ModuleStatus.class);
    }

    protected static Predicate<ModuleStatus> getModule(String target) {
        return m -> m.getModule().equalsIgnoreCase(target);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        xs.processAnnotations(ModuleStatus.class);
        xs.allowTypes(new Class[] {ModuleStatus.class});
        xs.alias("about", List.class);
        xs.alias("status", ModuleStatus.class);
        xs.addDefaultImplementation(ModuleStatusImpl.class, ModuleStatus.class);
        xs.addDefaultImplementation(RenderingEngineStatus.class, ModuleStatus.class);
    }

    @Override
    protected String getTemplateName(Object object) {
        return "ModuleStatusImpl";
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return ModuleStatus.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new BeansWrapper(FM_VERSION) {
            @Override
            public TemplateModel wrap(Object obj) throws TemplateModelException {
                if (obj instanceof List) { // we expect List of ModuleStatus
                    List<?> list = (List<?>) obj;
                    SimpleHash hash = new SimpleHash(new DefaultObjectWrapper(FM_VERSION));
                    hash.put(
                            "values",
                            new CollectionModel(
                                    list,
                                    new BeansWrapper(FM_VERSION) {
                                        public TemplateModel wrap(Object object)
                                                throws TemplateModelException {
                                            if (object instanceof ModuleStatus) {
                                                ModuleStatus status = (ModuleStatus) object;
                                                SimpleHash hash =
                                                        new SimpleHash(
                                                                new DefaultObjectWrapper(
                                                                        FM_VERSION));
                                                hash.put("module", status.getModule());
                                                hash.put("name", status.getName());
                                                hash.put(
                                                        "isAvailable",
                                                        Boolean.toString(status.isAvailable()));
                                                hash.put(
                                                        "isEnabled",
                                                        Boolean.toString(status.isEnabled()));
                                                status.getComponent()
                                                        .ifPresent(
                                                                component ->
                                                                        hash.put(
                                                                                "component",
                                                                                component));
                                                status.getVersion()
                                                        .ifPresent(
                                                                version ->
                                                                        hash.put(
                                                                                "version",
                                                                                version));
                                                // Make sure to escape the string, otherwise strange
                                                // chars here will bork the XML parser later

                                                status.getMessage()
                                                        .ifPresent(
                                                                message -> {
                                                                    String noControlChars =
                                                                            message.replaceAll(
                                                                                            "\u001b",
                                                                                            "ESC")
                                                                                    .replaceAll(
                                                                                            "\u0008",
                                                                                            "BACK")
                                                                                    .replaceAll(
                                                                                            "\u0007",
                                                                                            "BELL");
                                                                    String escaped =
                                                                            StringEscapeUtils
                                                                                    .escapeXml10(
                                                                                            noControlChars)
                                                                                    .replaceAll(
                                                                                            "\n",
                                                                                            "<br/>");
                                                                    hash.put("message", escaped);
                                                                });

                                                return hash;
                                            }
                                            return super.wrap(object);
                                        }
                                    }));
                    return hash;
                }
                return super.wrap(obj);
            }
        };
    }
}
