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

package org.geoserver.gwc.web;

import java.io.Serializable;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerBasePage;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;

/** Utility class used to lookup icons for various catalog objects */
@SuppressWarnings("serial")
public class GWCIconFactory implements Serializable {

    public static final PackageResourceReference UNKNOWN_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final PackageResourceReference DISABLED_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");

    public static final PackageResourceReference ENABLED_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/tick.png");

    public static final PackageResourceReference ADD_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/add.png");

    public static final PackageResourceReference DELETE_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/delete.png");

    public static final PackageResourceReference GRIDSET =
            new PackageResourceReference(GWCSettingsPage.class, "gridset.png");

    public static final PackageResourceReference GWC =
            new PackageResourceReference(GWCSettingsPage.class, "geowebcache-16.png");

    /**
     * Enum of tile layer type to aid in presenting a type column in the UI without incurring in
     * heavy resource lookups such as loading feature types from the geoserver catalog.
     */
    public static enum CachedLayerType {
        VECTOR(PublishedType.VECTOR.getCode()),
        RASTER(PublishedType.RASTER.getCode()),
        REMOTE(PublishedType.REMOTE.getCode()),
        WMS(PublishedType.WMS.getCode()),
        GROUP(PublishedType.GROUP.getCode()),
        WMTS(PublishedType.WMTS.getCode()),
        GWC(-1),
        UNKNOWN(-2);

        private final Integer code;

        CachedLayerType(Integer code) {
            this.code = code;
        }

        public Integer getCode() {
            return code;
        }

        public static CachedLayerType valueOf(Integer code) {
            return values()[code.intValue()];
        }
    }

    private GWCIconFactory() {
        // private constructor, this is a singleton
    }

    public static CachedLayerType getCachedLayerType(final TileLayer layer) {
        if (layer instanceof GeoServerTileLayer) {
            GeoServerTileLayer gsTileLayer = (GeoServerTileLayer) layer;
            PublishedInfo published = gsTileLayer.getPublishedInfo();
            PublishedType publishedType = published.getType();
            return CachedLayerType.valueOf(publishedType.getCode());
        }
        if (layer instanceof WMSLayer) {
            return CachedLayerType.GWC;
        }
        return CachedLayerType.UNKNOWN;
    }

    /** Returns the appropriate icon for the specified layer type. */
    public static PackageResourceReference getSpecificLayerIcon(final TileLayer layer) {
        if (layer instanceof GeoServerTileLayer) {
            GeoServerTileLayer gsTileLayer = (GeoServerTileLayer) layer;
            PublishedInfo published = gsTileLayer.getPublishedInfo();
            if (published instanceof LayerInfo) {
                return CatalogIconFactory.get().getSpecificLayerIcon((LayerInfo) published);
            }
            return CatalogIconFactory.GROUP_ICON;
        }
        if (layer instanceof WMSLayer) {
            return GWC;
        }
        return UNKNOWN_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate an enabled/properly configured
     * resource
     */
    public static PackageResourceReference getEnabledIcon() {
        return ENABLED_ICON;
    }

    /**
     * Returns a reference to a general purpose icon to indicate a
     * disabled/misconfigured/unreachable resource
     */
    public static PackageResourceReference getDisabledIcon() {
        return DISABLED_ICON;
    }

    public static PackageResourceReference getErrorIcon() {
        return UNKNOWN_ICON;
    }
}
