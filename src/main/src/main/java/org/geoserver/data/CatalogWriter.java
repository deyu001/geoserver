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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Writes the GeoServer catalog.xml file.
 *
 * <p>Usage:
 *
 * <pre>
 *         <code>
 *
 *                  Map dataStores = ...
 *                  Map nameSpaces = ...
 *
 *                  CatalogWriter writer = new CatalogWriter();
 *                  writer.dataStores( dataStores );
 *                  writer.nameSpaces( nameSpaces );
 *
 *                  File catalog = new File( &quot;.../catalog.xml&quot; );
 *                  writer.write( catalog );
 *
 *
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class CatalogWriter {
    /** The xml document */
    Document document;

    /** Root catalog element. */
    Element catalog;

    /** The coverage type key (aka format name) */
    public static final String COVERAGE_TYPE_KEY = "coverageType";

    /** The coverage url key (the actual coverage data location) */
    public static final String COVERAGE_URL_KEY = "coverageUrl";

    public CatalogWriter() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(false);
            builderFactory.setValidating(false);

            document = builderFactory.newDocumentBuilder().newDocument();
            catalog = document.createElement("catalog");
            document.appendChild(catalog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes "datastore" elements to the catalog.xml file.
     *
     * @param dataStores map of id to connection parameter map
     * @param namespaces map of id to namespace prefix map
     */
    public void dataStores(
            Map /* <String,Map> */ dataStores,
            Map /*<String,String>*/ namespaces,
            Set /*<String>*/ disabled) {
        Element dataStoresElement = document.createElement("datastores");
        catalog.appendChild(dataStoresElement);

        for (Object item : dataStores.entrySet()) {
            Map.Entry dataStore = (Map.Entry) item;
            String id = (String) dataStore.getKey();
            Map params = (Map) dataStore.getValue();

            Element dataStoreElement = document.createElement("datastore");
            dataStoresElement.appendChild(dataStoreElement);

            // set the datastore id
            dataStoreElement.setAttribute("id", id);
            dataStoreElement.setAttribute("enabled", Boolean.toString(!disabled.contains(id)));

            // set the namespace
            dataStoreElement.setAttribute("namespace", (String) namespaces.get(id));

            // encode hte ocnnection paramters
            Element connectionParamtersElement = document.createElement("connectionParams");
            dataStoreElement.appendChild(connectionParamtersElement);

            for (Object o : params.entrySet()) {
                Map.Entry param = (Map.Entry) o;
                String name = (String) param.getKey();
                Object value = param.getValue();

                // skip null values
                if (value == null) {
                    continue;
                }

                Element parameterElement = document.createElement("parameter");
                connectionParamtersElement.appendChild(parameterElement);

                parameterElement.setAttribute("name", name);
                parameterElement.setAttribute("value", value.toString());
            }
        }
    }

    /** Writers the "formats" element of the catalog.xml file */
    public void coverageStores(HashMap coverageStores, HashMap namespaces, Set disabled) {
        Element formatsElement = document.createElement("formats");
        catalog.appendChild(formatsElement);

        for (Object o : coverageStores.entrySet()) {
            Map.Entry dataStore = (Map.Entry) o;
            String id = (String) dataStore.getKey();
            Map params = (Map) dataStore.getValue();

            Element formatElement = document.createElement("format");
            formatsElement.appendChild(formatElement);

            // set the datastore id
            formatElement.setAttribute("id", id);
            formatElement.setAttribute("enabled", Boolean.toString(!disabled.contains(id)));

            // set the namespace
            formatElement.setAttribute("namespace", (String) namespaces.get(id));

            // encode type and url
            Element typeElement = document.createElement("type");
            formatElement.appendChild(typeElement);
            typeElement.setTextContent((String) params.get(COVERAGE_TYPE_KEY));
            Element urlElement = document.createElement("url");
            formatElement.appendChild(urlElement);
            urlElement.setTextContent((String) params.get(COVERAGE_URL_KEY));
        }
    }

    /**
     * Writes "namespace" elements to the catalog.xml file.
     *
     * @param namespaces map of <prefix,uri>, default uri is located under the empty string key.
     */
    public void namespaces(Map namespaces) {
        namespaces(namespaces, Collections.emptyList());
    }

    /**
     * Writes namespaces elements to the catalog.xml file.
     *
     * @param namespaces map containing namespaces prefix and URIs
     * @param isolatedNamespaces list containing the prefix of isolated namespaces
     */
    public void namespaces(Map namespaces, List<String> isolatedNamespaces) {
        Element namespacesElement = document.createElement("namespaces");
        catalog.appendChild(namespacesElement);

        for (Object o : namespaces.entrySet()) {
            Map.Entry namespace = (Map.Entry) o;
            String prefix = (String) namespace.getKey();
            String uri = (String) namespace.getValue();

            // dont write out default prefix
            if ("".equals(prefix)) {
                continue;
            }

            Element namespaceElement = document.createElement("namespace");
            namespacesElement.appendChild(namespaceElement);

            namespaceElement.setAttribute("uri", uri);
            namespaceElement.setAttribute("prefix", prefix);

            // check for default
            if (uri.equals(namespaces.get(""))) {
                namespaceElement.setAttribute("default", "true");
            }

            // check if is an isolated workspace
            if (isolatedNamespaces.contains(prefix)) {
                // mark this namespace as isolated
                namespaceElement.setAttribute("isolated", "true");
            }
        }
    }

    /**
     * Writes "style" elements to the catalog.xml file.
     *
     * @param styles map of <id,filename>
     */
    public void styles(Map styles) {
        Element stylesElement = document.createElement("styles");
        catalog.appendChild(stylesElement);

        for (Object o : styles.entrySet()) {
            Map.Entry style = (Map.Entry) o;
            String id = (String) style.getKey();
            String filename = (String) style.getValue();

            Element styleElement = document.createElement("style");
            stylesElement.appendChild(styleElement);

            styleElement.setAttribute("id", id);
            styleElement.setAttribute("filename", filename);
        }
    }

    /**
     * WRites the catalog.xml file.
     *
     * <p>This method *must* be called after any other methods.
     *
     * @param file The catalog.xml file.
     * @throws IOException In event of a writing error.
     */
    public void write(File file) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            Transformer tx = TransformerFactory.newInstance().newTransformer();
            tx.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(os);

            tx.transform(source, result);
        } catch (Exception e) {
            String msg = "Could not write catalog to " + file;
            throw (IOException) new IOException(msg).initCause(e);
        }
    }
}
