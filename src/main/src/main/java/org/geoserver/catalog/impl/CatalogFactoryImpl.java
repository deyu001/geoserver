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

package org.geoserver.catalog.impl;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

public class CatalogFactoryImpl implements CatalogFactory {

    Catalog catalog;

    public CatalogFactoryImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    public CoverageInfo createCoverage() {
        return new CoverageInfoImpl(catalog);
    }

    public CoverageDimensionInfo createCoverageDimension() {
        return new CoverageDimensionImpl();
    }

    public CoverageStoreInfo createCoverageStore() {
        return new CoverageStoreInfoImpl(catalog);
    }

    public DataStoreInfo createDataStore() {
        return new DataStoreInfoImpl(catalog);
    }

    public WMSStoreInfo createWebMapServer() {
        return new WMSStoreInfoImpl(catalog);
    }

    @Override
    public WMTSStoreInfo createWebMapTileServer() {
        return new WMTSStoreInfoImpl(catalog);
    }

    public AttributeTypeInfo createAttribute() {
        return new AttributeTypeInfoImpl();
    }

    public FeatureTypeInfo createFeatureType() {
        return new FeatureTypeInfoImpl(catalog);
    }

    public WMSLayerInfo createWMSLayer() {
        return new WMSLayerInfoImpl(catalog);
    }

    @Override
    public WMTSLayerInfo createWMTSLayer() {
        return new WMTSLayerInfoImpl(catalog);
    }

    public AttributionInfo createAttribution() {
        return new AttributionInfoImpl();
    }

    public LayerInfo createLayer() {
        return new LayerInfoImpl();
    }

    public MapInfo createMap() {
        return new MapInfoImpl();
    }

    public LayerGroupInfo createLayerGroup() {
        return new LayerGroupInfoImpl();
    }

    public LegendInfo createLegend() {
        return new LegendInfoImpl();
    }

    public MetadataLinkInfo createMetadataLink() {
        return new MetadataLinkInfoImpl();
    }

    public DataLinkInfo createDataLink() {
        return new DataLinkInfoImpl();
    }

    public NamespaceInfo createNamespace() {
        return new NamespaceInfoImpl();
    }

    public WorkspaceInfo createWorkspace() {
        return new WorkspaceInfoImpl();
    }

    public StyleInfo createStyle() {
        return new StyleInfoImpl(catalog);
    }

    public <T extends Object> T create(Class<T> clazz) {
        return null;
    }
}
