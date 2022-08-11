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

package org.geoserver.cluster.impl.rest;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.configuration.BrokerConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.configuration.ToggleConfiguration;
import org.geoserver.cluster.events.ToggleType;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/cluster")
public class ClusterController extends AbstractCatalogController {

    @Autowired private Controller controller;

    @Autowired private JMSConfiguration config;

    public ClusterController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public RestWrapper<Properties> getClusterConfiguration() {
        return wrapObject(config.getConfigurations(), Properties.class);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public ResponseEntity<String> postClusterConfiguration(
            @RequestBody Properties props, UriComponentsBuilder builder) throws IOException {
        for (Object key : props.keySet()) {
            String k = key.toString();
            final String value = props.get(key).toString();
            // store config
            config.putConfiguration(key.toString(), value);
            final Object oldValue = config.getConfiguration(k);
            if (props.get(k).equals(oldValue)) {
                continue;
            }
            if (key.equals(ConnectionConfiguration.CONNECTION_KEY)) {
                // CONNECTION
                controller.connectClient(Boolean.getBoolean(value));
            } else if (key.equals(ToggleConfiguration.TOGGLE_MASTER_KEY)) {
                // toggle MASTER
                controller.toggle(Boolean.getBoolean(value), ToggleType.MASTER);
            } else if (key.equals(ToggleConfiguration.TOGGLE_SLAVE_KEY)) {
                // toggle SLAVE
                controller.toggle(Boolean.getBoolean(value), ToggleType.SLAVE);
            } else if (key.equals(JMSConfiguration.INSTANCE_NAME_KEY)) {
                // InstanceName
                controller.setInstanceName(value);
            } else if (key.equals(BrokerConfiguration.BROKER_URL_KEY)) {
                // BROKER_URL
                controller.setBrokerURL(value);
            } else if (key.equals(ReadOnlyConfiguration.READ_ONLY_KEY)) {
                // ReadOnly
                controller.setReadOnly(Boolean.getBoolean(value));
            } else if (key.equals(JMSConfiguration.GROUP_KEY)) {
                // group
                controller.setGroup(value);
            }
        }
        // SAVE to disk
        controller.save();
        UriComponents uriComponents = builder.path("/cluster").build();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<>(props.toString(), headers, HttpStatus.CREATED);
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return Properties.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {
        template.setObjectWrapper(
                new ObjectToMapWrapper<Properties>(Properties.class) {
                    @Override
                    protected void wrapInternal(
                            Map properties, SimpleHash model, Properties props) {
                        properties.putAll(props);
                    }
                });
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        persister.getXStream().allowTypes(new Class[] {Properties.class});
    }
}
