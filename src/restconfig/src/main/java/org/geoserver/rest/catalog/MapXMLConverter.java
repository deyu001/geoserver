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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class MapXMLConverter extends BaseMessageConverter<Map<?, ?>> {

    public MapXMLConverter() {
        super(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz) && !Properties.class.isAssignableFrom(clazz);
    }

    //
    // reading
    //
    @Override
    public Map<?, ?> readInternal(Class<? extends Map<?, ?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        Object result;
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog.getResourcePool().getEntityResolver());
        Document doc;
        try {
            doc = builder.build(inputMessage.getBody());
        } catch (JDOMException e) {
            throw new IOException("Error building document", e);
        }

        Element elem = doc.getRootElement();
        result = convert(elem);
        return (Map<?, ?>) result;
    }

    //
    // writing
    //
    @Override
    public void writeInternal(Map<?, ?> map, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Element root = new Element(getMapName(map));
        final Document doc = new Document(root);
        insert(root, map);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(doc, outputMessage.getBody());
    }

    protected String getMapName(Map<?, ?> map) {
        if (map instanceof NamedMap) {
            return ((NamedMap<?, ?>) map).getName();
        } else {
            return "root";
        }
    }

    /**
     * Interpret XML and convert it back to a Java collection.
     *
     * @param elem a JDOM element
     * @return the Object produced by interpreting the XML
     */
    protected Object convert(Element elem) {
        List<?> children = elem.getChildren();
        if (children.isEmpty()) {
            if (elem.getContent().isEmpty()) {
                return null;
            } else {
                return elem.getText();
            }
        } else if (children.get(0) instanceof Element) {
            Element child = (Element) children.get(0);
            if (child.getName().equals("entry")) {
                List<Object> l = new ArrayList<>();
                for (Object o : elem.getChildren("entry")) {
                    Element curr = (Element) o;
                    l.add(convert(curr));
                }
                return l;
            } else {
                Map<String, Object> m = new NamedMap<>(child.getName());
                for (Object aChildren : children) {
                    Element curr = (Element) aChildren;
                    m.put(curr.getName(), convert(curr));
                }
                return m;
            }
        }
        throw new RuntimeException("Unable to parse XML");
    }

    /**
     * Generate the JDOM element needed to represent an object and insert it into the parent element
     * given.
     *
     * @todo This method is recursive and could cause stack overflow errors for large input maps.
     * @param elem the parent Element into which to insert the created JDOM element
     * @param object the Object to be converted
     */
    protected void insert(Element elem, Object object) {
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Element newElem = new Element(entry.getKey().toString());
                insert(newElem, entry.getValue());
                elem.addContent(newElem);
            }
        } else if (object instanceof Collection) {
            Collection<?> collection = (Collection<?>) object;

            for (Object entry : collection) {
                Element newElem = new Element("entry");
                insert(newElem, entry);
                elem.addContent(newElem);
            }
        } else {
            elem.addContent(object == null ? "" : object.toString());
        }
    }
}
