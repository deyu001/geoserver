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

package org.geoserver.wcs2_0.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.util.MapEntry;
import org.geotools.util.logging.Logging;

/**
 * De/encode a workspace and a resource name into a single string.
 *
 * <p>Some external formats do not allow to use semicolons in some strings. This class offers
 * methods to encode and decode workspace and names into a single string without using semicolons.
 *
 * <p>We simply use a "__" as separator. This should reduce the conflicts with existing underscores.
 * This encoding is not unique, so the {@link #decode(java.lang.String) decode} method return a list
 * of possible workspace,name combinations. You'll need to check which workspace is really existing.
 *
 * <p>You may use the {@link #getLayer(org.geoserver.catalog.Catalog, java.lang.String) getLayer()}
 * method to just retrieve the matching layers.
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class NCNameResourceCodec {
    protected static Logger LOGGER = Logging.getLogger(NCNameResourceCodec.class);

    private static final String DELIMITER = "__";

    public static String encode(ResourceInfo resource) {
        return encode(resource.getNamespace().getPrefix(), resource.getName());
    }

    public static String encode(String workspaceName, String resourceName) {
        return workspaceName + DELIMITER + resourceName;
    }

    public static LayerInfo getCoverage(Catalog catalog, String encodedCoverageId)
            throws WCS20Exception {
        List<LayerInfo> layers = NCNameResourceCodec.getLayers(catalog, encodedCoverageId);
        if (layers == null) return null;

        LayerInfo ret = null;

        for (LayerInfo layer : layers) {
            if (layer != null && layer.getType() == PublishedType.RASTER) {
                if (ret == null) {
                    ret = layer;
                } else {
                    LOGGER.warning(
                            "Multiple coverages found for NSName '"
                                    + encodedCoverageId
                                    + "': "
                                    + ret.prefixedName()
                                    + " is selected, "
                                    + layer.prefixedName()
                                    + " will be ignored");
                }
            }
        }

        return ret;
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
        List<MapEntry<String, String>> decodedList = decode(encodedResourceId);
        if (decodedList.isEmpty()) {
            LOGGER.info("Could not decode id '" + encodedResourceId + "'");
            return null;
        }

        List<LayerInfo> ret = new ArrayList<>();

        LOGGER.info(" Examining encoded name " + encodedResourceId);

        for (MapEntry<String, String> mapEntry : decodedList) {

            String namespace = mapEntry.getKey();
            String covName = mapEntry.getValue();

            if (namespace == null || namespace.isEmpty()) {
                LOGGER.info(" Checking coverage name " + covName);

                LayerInfo layer = catalog.getLayerByName(covName);
                if (layer != null) {
                    LOGGER.info(" - Collecting layer " + layer.prefixedName());
                    ret.add(layer);
                } else {
                    LOGGER.info(" - Ignoring layer " + covName);
                }
            } else {
                LOGGER.info(" Checking pair " + namespace + " : " + covName);

                String fullName = namespace + ":" + covName;
                NamespaceInfo nsInfo = catalog.getNamespaceByPrefix(namespace);
                if (nsInfo != null) {
                    LOGGER.info(" - Namespace found " + namespace);
                    LayerInfo layer = catalog.getLayerByName(fullName);
                    if (layer != null) {
                        LOGGER.info(" - Collecting layer " + layer.prefixedName());
                        ret.add(layer);
                    } else {
                        LOGGER.info(" - Ignoring layer " + fullName);
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
        List<MapEntry<String, String>> ret = new ArrayList<>();

        if (lastPos == -1) {
            ret.add(new MapEntry<>(null, qualifiedName));
            return ret;
        }

        while (lastPos > -1) {
            String ws = qualifiedName.substring(0, lastPos);
            String name = qualifiedName.substring(lastPos + DELIMITER.length());
            ret.add(new MapEntry<>(ws, name));
            lastPos = qualifiedName.lastIndexOf(DELIMITER, lastPos - 1);
        }
        return ret;
    }
}
