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

package org.geoserver.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.util.ReaderUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads the GeoServer catalog.xml file.
 *
 * <p>Usage:
 *
 * <pre>
 *         <code>
 *                 File catalog = new File( ".../catalog.xml" );
 *                 CatalogReader reader = new CatalogReader();
 *                 reader.read( catalog );
 *                 List dataStores = reader.dataStores();
 *                 LIst nameSpaces = reader.nameSpaces();
 *         </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class CatalogReader {
    /** Root catalog element. */
    Element catalog;

    /**
     * Parses the catalog.xml file into a DOM.
     *
     * <p>This method *must* be called before any other methods.
     *
     * @param file The catalog.xml file.
     * @throws IOException In event of a parser error.
     */
    public void read(File file) throws IOException {

        try (FileReader reader = new FileReader(file)) {
            catalog = ReaderUtils.parse(reader);
        }
    }

    /**
     * Reads "datastore" elements from the catalog.xml file.
     *
     * <p>For each datastore element read, a map of the connection parameters is created.
     *
     * @return A list of Map objects containg the datastore connection parameters.
     * @throws Exception If error processing "datastores" element.
     */
    public List<Map<String, String>> dataStores() throws Exception {
        Element dataStoresElement = ReaderUtils.getChildElement(catalog, "datastores", true);

        NodeList dataStoreElements = dataStoresElement.getElementsByTagName("datastore");
        List<Map<String, String>> dataStores = new ArrayList<>();

        for (int i = 0; i < dataStoreElements.getLength(); i++) {
            Element dataStoreElement = (Element) dataStoreElements.item(i);

            try {
                Map<String, String> params = dataStoreParams(dataStoreElement);
                dataStores.add(params);
            } catch (Exception e) {
                // TODO: log this
                continue;
            }
        }

        return dataStores;
    }

    /**
     * Reads "namespace" elements from the catalog.xml file.
     *
     * <p>For each namespace element read, an entry of <prefix,uri> is created in a map. The default
     * uri is located under the empty string key.
     *
     * @return A map containing <prefix,uri> tuples.
     * @throws Exception If error processing "namespaces" element.
     */
    public Map<String, String> namespaces() throws Exception {
        Element namespacesElement = ReaderUtils.getChildElement(catalog, "namespaces", true);

        NodeList namespaceElements = namespacesElement.getElementsByTagName("namespace");
        Map<String, String> namespaces = new HashMap<>();

        for (int i = 0; i < namespaceElements.getLength(); i++) {
            Element namespaceElement = (Element) namespaceElements.item(i);

            try {
                Map.Entry<String, String> tuple = namespaceTuple(namespaceElement);
                namespaces.put(tuple.getKey(), tuple.getValue());

                // check for default
                if ("true".equals(namespaceElement.getAttribute("default"))) {
                    namespaces.put("", tuple.getValue());
                }
            } catch (Exception e) {
                // TODO: log this
                continue;
            }
        }

        return namespaces;
    }

    /**
     * Convenience method for reading connection parameters from a datastore element.
     *
     * @param dataStoreElement The "datastore" element.
     * @return The map of connection paramters.
     * @throws Exception If problem parsing any parameters.
     */
    protected Map<String, String> dataStoreParams(Element dataStoreElement) throws Exception {
        Element paramsElement =
                ReaderUtils.getChildElement(dataStoreElement, "connectionParameters", true);
        NodeList paramList = paramsElement.getElementsByTagName("parameter");

        Map<String, String> params = new HashMap<>();

        for (int i = 0; i < paramList.getLength(); i++) {
            Element paramElement = (Element) paramList.item(i);
            String key = ReaderUtils.getAttribute(paramElement, "name", true);
            String value = ReaderUtils.getAttribute(paramElement, "value", true);

            params.put(key, value);
        }

        return params;
    }

    /**
     * Convenience method for reading namespace prefix and uri from a namespace element.
     *
     * @param namespaceElement The "namespace" element.
     * @return A <prefix,uri> tuple.
     * @throws Exception If problem parsing any parameters.
     */
    protected Map.Entry<String, String> namespaceTuple(Element namespaceElement) throws Exception {
        final String pre = namespaceElement.getAttribute("prefix");
        final String uri = namespaceElement.getAttribute("uri");

        return new AbstractMap.SimpleEntry<>(pre, uri);
    }
}
