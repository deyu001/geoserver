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

package org.geoserver.wfs3.response;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.response.WFSResponse;
import org.geoserver.wfs3.BaseRequest;

/** Response encoding outputs in JSON/YAML using Jackson */
public abstract class JacksonResponse extends WFSResponse {

    public JacksonResponse(GeoServer gs, Class targetClass) {
        this(
                gs,
                targetClass,
                new LinkedHashSet<>(
                        Arrays.asList(
                                BaseRequest.JSON_MIME,
                                BaseRequest.YAML_MIME,
                                BaseRequest.XML_MIME)));
    }

    protected JacksonResponse(GeoServer gs, Class targetClass, Set<String> formats) {
        super(gs, targetClass, formats);
    }

    @Override
    public boolean canHandle(Operation operation) {
        return isJsonFormat(operation) || isYamlFormat(operation) || isXMLFormat(operation);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (isJsonFormat(operation)) {
            return BaseRequest.JSON_MIME;
        } else if (isYamlFormat(operation)) {
            return BaseRequest.YAML_MIME;
        } else if (isXMLFormat(operation)) {
            return BaseRequest.XML_MIME;
        } else {
            throw new ServiceException("Unknown format requested " + getFormat(operation));
        }
    }

    /**
     * Returns the requested format from the operation, falls back to {@link BaseRequest#JSON_MIME}
     * if none was requested
     */
    protected String getFormat(Operation operation) {
        BaseRequest request = (BaseRequest) operation.getParameters()[0];
        Optional<String> format = Optional.ofNullable(request.getOutputFormat());
        return format.orElse(BaseRequest.JSON_MIME);
    }

    /** Checks if the operation requested the JSON format */
    protected boolean isJsonFormat(Operation operation) {
        return BaseRequest.JSON_MIME.equalsIgnoreCase(getFormat(operation));
    }

    /** Checks if the operation requested the YAML format */
    protected boolean isYamlFormat(Operation operation) {
        return BaseRequest.YAML_MIME.equalsIgnoreCase(getFormat(operation));
    }

    /** Checks if the operation requested the XML format */
    protected boolean isXMLFormat(Operation operation) {
        return BaseRequest.XML_MIME.equalsIgnoreCase(getFormat(operation));
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        ObjectMapper mapper;
        if (isYamlFormat(operation)) {
            mapper = Yaml.mapper();
        } else if (isXMLFormat(operation)) {
            mapper = new XmlMapper();
            // using a custom annotation introspector to set the desired namespace
            mapper.setAnnotationIntrospector(
                    new JacksonXmlAnnotationIntrospector() {
                        @Override
                        public String findNamespace(Annotated ann) {
                            String ns = super.findNamespace(ann);
                            if (ns == null || ns.isEmpty()) {
                                return "http://www.opengis.net/wfs/3.0";
                            } else {
                                return ns;
                            }
                        }
                    });

        } else {
            mapper = Json.mapper();
            mapper.writer(new DefaultPrettyPrinter());
        }

        mapper.writeValue(output, value);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return getFileName(value, operation) + (isJsonFormat(operation) ? ".json" : ".yaml");
    }

    /** Just the name of the file to be returned (no extension) */
    protected abstract String getFileName(Object value, Operation operation);
}
