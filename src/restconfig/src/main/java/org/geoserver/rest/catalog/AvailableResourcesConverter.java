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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Outputs a named list of strings, as represented by {@link AvailableResources}.
 *
 * <p>This is used for WMS output.
 *
 * @author Kevin Smith (Boundless)
 */
// TODO: This is a duplicate of StringsListConverter
@Component
public class AvailableResourcesConverter extends BaseMessageConverter<AvailableResources> {

    // static final List<MediaType> MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_XML,
    // MediaType.APPLICATION_JSON);

    public AvailableResourcesConverter() {
        super(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return AvailableResources.class.isAssignableFrom(clazz);
    }

    @Override
    protected AvailableResources readInternal(
            Class<? extends AvailableResources> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException(
                "AvailableResourceConverter does not support deserialization", inputMessage);
    }

    @Override
    protected void writeInternal(
            AvailableResources availableResources, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();

        if (MediaType.APPLICATION_XML.isCompatibleWith(contentType)) {
            writeXML(availableResources, outputMessage);
        } else if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            writeJSON(availableResources, outputMessage);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void writeJSON(AvailableResources t, HttpOutputMessage outputMessage)
            throws IOException {
        JSONArray names = new JSONArray();
        names.addAll(t);
        JSONObject string = new JSONObject();
        string.put("string", names);
        JSONObject root = new JSONObject();
        root.put("list", string);
        try (OutputStream os = outputMessage.getBody();
                Writer writer = new OutputStreamWriter(os)) {

            root.write(writer);
        }
    }

    protected void writeXML(AvailableResources t, HttpOutputMessage outputMessage)
            throws IOException {
        Element root = new Element("list");
        final Document doc = new Document(root);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

        t.stream().map(name -> new Element(t.getName()).addContent(name)).forEach(root::addContent);

        try (OutputStream os = outputMessage.getBody()) {
            outputter.output(doc, os);
        }
    }
}
