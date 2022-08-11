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

package org.geoserver.rest.system.status;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import freemarker.template.ObjectWrapper;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ObjectToMapWrapper;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.system.status.MetricValue;
import org.geoserver.system.status.Metrics;
import org.geoserver.system.status.SystemInfoCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint that return the available system information.
 *
 * <p>Every time this endpoint is hitted the informations are retrieved from the system, no cached
 * information is used.
 *
 * <p>HTML, XML and JSON are supported.
 *
 * @author sandr
 */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/about/system-status")
public class MonitorRest extends RestBaseController {

    @Autowired SystemInfoCollector systemInfoCollector;

    @GetMapping(
        value = "",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    @ResponseStatus(HttpStatus.OK)
    public RestWrapper<Metrics> getData(HttpServletRequest request, HttpServletResponse response) {
        Metrics si = systemInfoCollector.retrieveAllSystemInfo();
        return wrapObject(si, Metrics.class);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        xs.alias("metric", MetricValue.class);
        xs.alias("metrics", Metrics.class);
        xs.omitField(MetricValue.class, "value");
        xs.registerConverter(new ValueHolderConverter());
        xs.aliasField("value", MetricValue.class, "holder");
        xs.addImplicitCollection(Metrics.class, "metrics");
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        return new ObjectToMapWrapper<>(clazz, Arrays.asList(MetricValue.class));
    }

    /** Will convert a metric value to is correct representation. */
    private static final class ValueHolderConverter implements Converter {

        @Override
        public boolean canConvert(Class type) {
            return MetricValue.ValueHolder.class.isAssignableFrom(type);
        }

        @Override
        public void marshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Object value = ((MetricValue.ValueHolder) source).getValue();
            writer.setValue(value != null ? value.toString() : "");
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            return null;
        }
    }
}
