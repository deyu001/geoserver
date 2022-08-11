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

package org.geoserver.gwc.layer;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.layer.ExpirationRule;

/**
 * Delegate for {@link GeoServerTileLayer} configuration, for serialization.
 *
 * @see TileLayerInfoUtil#loadOrCreate(CatalogInfo, GWCConfig)
 * @see TileLayerInfoUtil#loadOrCreate(LayerInfo, GWCConfig)
 * @see TileLayerInfoUtil#loadOrCreate(LayerGroupInfo, GWCConfig)
 */
public interface GeoServerTileLayerInfo extends Serializable, Cloneable {

    /** @return The id of the corresponding {@link LayerInfo} or {@link LayerGroupInfo} */
    String getId();

    /**
     * Sets the id.
     *
     * @param id The id of the corresponding {@link LayerInfo} or {@link LayerGroupInfo}
     */
    void setId(String id);

    /** @return The name of the corresponding {@link GeoServerTileLayer} */
    String getName();

    /**
     * Sets the name
     *
     * @param name The name of the corresponding {@link GeoServerTileLayer}
     */
    void setName(String name);

    /**
     * @return The {@link BlobStoreInfo#getId() blob store id} for this layer's tiles, or {@code
     *     null} if whatever the default blob store is shall be used
     */
    @Nullable
    String getBlobStoreId();

    /**
     * @param blobStoreId the {@link BlobStoreInfo#getId() blob store id} for this layer's tiles, or
     *     {@code null} if whatever the default blob store is shall be used
     */
    void setBlobStoreId(@Nullable String blobStoreId);

    /** @return the X metatiling factor */
    int getMetaTilingX();

    /** @return the Y metatiling factor */
    int getMetaTilingY();

    /** @param metaTilingY the Y metatiling factor */
    void setMetaTilingY(int metaTilingY);

    /** @param metaTilingX the X metatiling factor */
    void setMetaTilingX(int metaTilingX);

    /**
     * Gets the default expiration time for tiles in the cache.
     *
     * @return the expiration time for tiles in the cache
     */
    int getExpireCache();

    /**
     * Sets the default expiration time for tiles in the cache.
     *
     * @param expireCache the expiration time
     */
    void setExpireCache(int expireCache);

    /**
     * Gets a list of {@link ExpirationRule} defining expiration time by zoom level
     *
     * @return list expiration rules for tiles in the cache
     */
    List<ExpirationRule> getExpireCacheList();

    /**
     * Sets the {@link ExpirationRule}s for expiring tiles in the cache
     *
     * @param expireCacheList the list of expiration rules
     */
    void setExpireCacheList(List<ExpirationRule> expireCacheList);

    /**
     * Gets the expiration time to be declared to clients
     *
     * @return the expiration time, in seconds
     */
    int getExpireClients();

    /**
     * Sets the expiration time to be declared to clients
     *
     * @param seconds the expiration time, in seconds
     */
    void setExpireClients(int seconds);

    /**
     * Derived property from {@link #getParameterFilters()}, returns the configured allowable values
     * for a parameter filter over the {@code STYLE} key, if exists, or the empty set.
     *
     * <p>The returned set is immutable and dettached from this object's internal state
     *
     * <p>The returned set shall not return the default style for the layer
     */
    ImmutableSet<String> cachedStyles();

    /**
     * @see GeoServerTileLayer#getMimeTypes()
     * @return set of MIME types supported by the tile layer
     */
    Set<String> getMimeFormats();

    /**
     * Get the list of cached {@link org.geowebcache.grid.GridSubset}s for the {@link
     * GeoServerTileLayer}
     *
     * @return The grid subsets
     */
    Set<XMLGridSubset> getGridSubsets();

    /**
     * Set the list of cached {@link org.geowebcache.grid.GridSubset}s for the {@link
     * GeoServerTileLayer}
     *
     * @param gridSubsets list of grid subsets to cache
     */
    void setGridSubsets(Set<XMLGridSubset> gridSubsets);

    /**
     * Sets whether the tile layer is enabled
     *
     * @param enabled if the tile layer is enabled
     */
    void setEnabled(boolean enabled);

    /** @return true if the tile layer is enabled */
    boolean isEnabled();

    /**
     * Sets the size of the gutter surrounding MetaTiles, in pixels.
     *
     * @see org.geowebcache.layer.MetaTile
     * @param gutter the size of the gutter, in pixels
     */
    void setGutter(int gutter);

    /**
     * Sets the size of the gutter surrounding MetaTiles, in pixels.
     *
     * @see org.geowebcache.layer.MetaTile
     * @return the size of the gutter, in pixels
     */
    int getGutter();

    /**
     * Is the tile layer configured to automatically cache all styles
     *
     * @return true if the tile layer automatically caches all styles
     */
    boolean isAutoCacheStyles();

    /**
     * Set whether the tile layer should automaticaly cache all styles
     *
     * @param autoCacheStyles if the tile layer should automatically cache all styles
     */
    void setAutoCacheStyles(boolean autoCacheStyles);

    /** @return the parameterFilters */
    Set<ParameterFilter> getParameterFilters();

    /** Replace the set of parameter filters */
    void setParameterFilters(Set<ParameterFilter> parameterFilters);

    /**
     * Add a parameter filter, replacing any existing filter with the same key.
     *
     * @return true if an existing filter was replaced, false otherwise.
     */
    boolean addParameterFilter(ParameterFilter parameterFilter);

    /**
     * Remove the filter with the specified key
     *
     * @return true if the filter existed, false otherwise
     */
    boolean removeParameterFilter(String key);

    GeoServerTileLayerInfo clone();

    /** Get the ParameterFilter with the specified key */
    ParameterFilter getParameterFilter(String key);

    /**
     * Is the layer cached in-memory
     *
     * @return true if the layer is cached in-memory
     */
    boolean isInMemoryCached();

    /**
     * Set whether or not the layer is cached in-memory
     *
     * @param inMemoryCached is the layer cached in-memory
     */
    void setInMemoryCached(boolean inMemoryCached);
}
