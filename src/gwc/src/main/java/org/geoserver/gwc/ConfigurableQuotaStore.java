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

package org.geoserver.gwc;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.storage.PageStats;
import org.geowebcache.diskquota.storage.PageStatsPayload;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.TilePage;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.diskquota.storage.TileSet;
import org.geowebcache.diskquota.storage.TileSetVisitor;

/**
 * A {@link QuotaStore} delegating to another instance of {@link QuotaStore}, and allowing the
 * delegate to be changed at runtime.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ConfigurableQuotaStore implements QuotaStore {

    static final Logger LOGGER = Logging.getLogger(ConfigurableQuotaStore.class);

    private QuotaStore delegate;

    public void setStore(QuotaStore delegate) {
        this.delegate = delegate;
    }

    public QuotaStore getStore() {
        return delegate;
    }

    public ConfigurableQuotaStore(QuotaStore delegate) {
        this.delegate = delegate;
    }

    public TilePageCalculator getTilePageCalculator() {
        return delegate.getTilePageCalculator();
    }

    public void createLayer(String layerName) throws InterruptedException {
        delegate.createLayer(layerName);
    }

    public Quota getGloballyUsedQuota() throws InterruptedException {
        return delegate.getGloballyUsedQuota();
    }

    public Quota getUsedQuotaByTileSetId(String tileSetId) throws InterruptedException {
        return delegate.getUsedQuotaByTileSetId(tileSetId);
    }

    public void deleteLayer(String layerName) {
        delegate.deleteLayer(layerName);
    }

    public void renameLayer(String oldLayerName, String newLayerName) throws InterruptedException {
        delegate.renameLayer(oldLayerName, newLayerName);
    }

    public Quota getUsedQuotaByLayerName(String layerName) throws InterruptedException {
        return delegate.getUsedQuotaByLayerName(layerName);
    }

    public long[][] getTilesForPage(TilePage page) throws InterruptedException {
        return delegate.getTilesForPage(page);
    }

    public Set<TileSet> getTileSets() {
        return delegate.getTileSets();
    }

    public TileSet getTileSetById(String tileSetId) throws InterruptedException {
        return delegate.getTileSetById(tileSetId);
    }

    public void accept(TileSetVisitor visitor) {
        delegate.accept(visitor);
    }

    public void addToQuotaAndTileCounts(
            TileSet tileSet, Quota quotaDiff, Collection<PageStatsPayload> tileCountDiffs)
            throws InterruptedException {
        delegate.addToQuotaAndTileCounts(tileSet, quotaDiff, tileCountDiffs);
    }

    public Future<List<PageStats>> addHitsAndSetAccesTime(
            Collection<PageStatsPayload> statsUpdates) {
        return delegate.addHitsAndSetAccesTime(statsUpdates);
    }

    public TilePage getLeastFrequentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return delegate.getLeastFrequentlyUsedPage(layerNames);
    }

    public TilePage getLeastRecentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return delegate.getLeastRecentlyUsedPage(layerNames);
    }

    public PageStats setTruncated(TilePage tilePage) throws InterruptedException {
        return delegate.setTruncated(tilePage);
    }

    public void deleteGridSubset(String layerName, String gridSetId) {
        delegate.deleteGridSubset(layerName, gridSetId);
    }

    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public void deleteParameters(String layerName, String parametersId) {
        delegate.deleteParameters(layerName, parametersId);
    }
}
