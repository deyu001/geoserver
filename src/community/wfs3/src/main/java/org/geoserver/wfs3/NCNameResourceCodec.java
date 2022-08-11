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

package org.geoserver.wfs3;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.util.MapEntry;
import org.geotools.util.logging.Logging;

/**
 * Copy of NCNameResourceCodec found in WCS 2.0.
 *
 * <p>TODO: move the class in main and share (there is one static method there that's WCS 2.0
 * specific, needs first to be moved somewhere else)
 */
public class NCNameResourceCodec {
    protected static Logger LOGGER = Logging.getLogger(NCNameResourceCodec.class);

    private static final String DELIMITER = "__";

    public static String encode(ResourceInfo resource) {
        return encode(resource.getNamespace().getPrefix(), resource.getName());
    }

    public static String encode(String workspaceName, String resourceName) {
        final WorkspaceInfo workspace = LocalWorkspace.get();
        if (workspace != null && workspace.getName().equalsIgnoreCase(workspaceName)) {
            return resourceName;
        }
        return workspaceName + DELIMITER + resourceName;
    }

    /**
     * Search in the catalog the Layers matching the encoded id.
     *
     * <p>
     *
     * @return A possibly empty list of the matching layers, or null if the encoded id could not be
     *     decoded.
     */
    public static List<LayerInfo> getLayers(Catalog catalog, String encodedResourceId) {
        final WorkspaceInfo workspace = LocalWorkspace.get();
        if (workspace != null) {
            encodedResourceId = workspace.getName() + DELIMITER + encodedResourceId;
        }

        List<MapEntry<String, String>> decodedList = decode(encodedResourceId);
        if (decodedList.isEmpty()) {
            LOGGER.info("Could not decode id '" + encodedResourceId + "'");
            return null;
        }

        List<LayerInfo> ret = new ArrayList<LayerInfo>();

        LOGGER.info(" Examining encoded name " + encodedResourceId);

        for (MapEntry<String, String> mapEntry : decodedList) {

            String namespace = mapEntry.getKey();
            String localName = mapEntry.getValue();

            if (namespace == null || namespace.isEmpty()) {
                LOGGER.log(Level.FINE, " Checking coverage name {0}", localName);

                LayerInfo layer = catalog.getLayerByName(localName);
                if (layer != null) {
                    LOGGER.log(Level.FINE, " - Collecting layer {0}", layer.prefixedName());
                    ret.add(layer);
                } else {
                    LOGGER.log(Level.FINE, " - Ignoring layer {0}", localName);
                }
            } else {
                LOGGER.info(" Checking pair " + namespace + " : " + localName);

                String fullName = namespace + ":" + localName;
                NamespaceInfo nsInfo = catalog.getNamespaceByPrefix(namespace);
                if (nsInfo != null) {
                    LOGGER.log(Level.FINE, " - Namespace found {0}", namespace);
                    LayerInfo layer = catalog.getLayerByName(fullName);
                    if (layer != null) {
                        LOGGER.log(Level.FINE, " - Collecting layer {0} ", layer.prefixedName());
                        ret.add(layer);
                    } else {
                        LOGGER.log(Level.FINE, " - Ignoring layer {0} " + fullName);
                    }
                } else {
                    LOGGER.info(" - Namespace not found " + namespace);
                }
            }
        }

        return ret;
    }

    /**
     * @return a List of possible workspace/name pairs, possibly empty if the input could not be
     *     decoded;
     */
    public static List<MapEntry<String, String>> decode(String qualifiedName) {
        int lastPos = qualifiedName.lastIndexOf(DELIMITER);
        List<MapEntry<String, String>> ret = new ArrayList<MapEntry<String, String>>();

        if (lastPos == -1) {
            ret.add(new MapEntry<String, String>(null, qualifiedName));
            return ret;
        }

        while (lastPos > -1) {
            String ws = qualifiedName.substring(0, lastPos);
            String name = qualifiedName.substring(lastPos + DELIMITER.length());
            ret.add(new MapEntry<String, String>(ws, name));
            lastPos = qualifiedName.lastIndexOf(DELIMITER, lastPos - 1);
        }
        return ret;
    }
}
