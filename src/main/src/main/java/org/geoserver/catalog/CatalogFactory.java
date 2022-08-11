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

package org.geoserver.catalog;

import java.util.Map;

/**
 * Factory used to create catalog objects.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface CatalogFactory {

    /** Creates a new data store. */
    DataStoreInfo createDataStore();

    /** Creates a new web map server connection */
    WMSStoreInfo createWebMapServer();

    /** Creates a new Web Map Tile Server connection. */
    WMTSStoreInfo createWebMapTileServer();

    /** Creats a new metadata link. */
    MetadataLinkInfo createMetadataLink();

    /** Creates a new data link. */
    DataLinkInfo createDataLink();

    /** Creates a new coverage store. */
    CoverageStoreInfo createCoverageStore();

    /** Creates a new attribute type. */
    AttributeTypeInfo createAttribute();

    /** Creates a new feature type. */
    FeatureTypeInfo createFeatureType();

    /** Creates a new coverage. */
    CoverageInfo createCoverage();

    /** Creates a new WMS layer */
    WMSLayerInfo createWMSLayer();

    /** creates a new WMTS layer */
    WMTSLayerInfo createWMTSLayer();

    /** Creates a new coverage dimension. */
    CoverageDimensionInfo createCoverageDimension();

    /** Creates a new legend. */
    LegendInfo createLegend();

    /** Creates a new attribution record. */
    AttributionInfo createAttribution();

    /** Creates a new layer. */
    LayerInfo createLayer();

    /** Creates a new map. */
    MapInfo createMap();

    /** Creates a new base map. */
    LayerGroupInfo createLayerGroup();

    /** Creates a new style. */
    StyleInfo createStyle();

    /** Creates new namespace. */
    NamespaceInfo createNamespace();

    /** Creates a new workspace. */
    WorkspaceInfo createWorkspace();

    /**
     * Extensible factory method.
     *
     * <p>This method should lookup the appropritae instance of {@link Extension} to create the
     * object. The lookup mechanism is specific to the runtime environement.
     *
     * @param clazz The class of object to create.
     * @return The new object.
     */
    <T extends Object> T create(Class<T> clazz);

    /** Factory extension. */
    interface Extension {

        /**
         * Determines if the extension can create objects of the specified class.
         *
         * @param clazz The class of object to create.
         */
        <T extends Object> boolean canCreate(Class<T> clazz);

        /**
         * Creates an instance of the specified class.
         *
         * <p>This method is only called if {@link #canCreate(Class)} returns <code>true</code>.
         *
         * @param clazz The class of object to create.
         * @param context A context to initialize the object.
         * @return The new object.
         */
        <T extends Object> T create(Class<T> clazz, Map<Object, Object> context);
    }
}
