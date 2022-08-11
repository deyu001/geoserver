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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.storage.PageStats;
import org.geowebcache.diskquota.storage.PageStatsPayload;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.TilePage;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.diskquota.storage.TileSet;
import org.geowebcache.diskquota.storage.TileSetVisitor;

public class DummyQuotaStore implements QuotaStore {

    private static final Quota EMPTY_QUOTA = new Quota(BigInteger.valueOf(0));

    TilePageCalculator calculator;

    public DummyQuotaStore(TilePageCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public void createLayer(String layerName) throws InterruptedException {}

    @Override
    public Quota getGloballyUsedQuota() throws InterruptedException {
        return EMPTY_QUOTA;
    }

    @Override
    public Quota getUsedQuotaByTileSetId(String tileSetId) throws InterruptedException {
        return EMPTY_QUOTA;
    }

    @Override
    public void deleteLayer(String layerName) {}

    @Override
    public void renameLayer(String oldLayerName, String newLayerName) throws InterruptedException {}

    @Override
    public Quota getUsedQuotaByLayerName(String layerName) throws InterruptedException {
        return EMPTY_QUOTA;
    }

    @Override
    public long[][] getTilesForPage(TilePage page) throws InterruptedException {
        TileSet tileSet = getTileSetById(page.getTileSetId());
        long[][] gridCoverage = calculator.toGridCoverage(tileSet, page);
        return gridCoverage;
    }

    @Override
    public Set<TileSet> getTileSets() {
        return Collections.emptySet();
    }

    @Override
    public TileSet getTileSetById(String tileSetId) throws InterruptedException {
        return null;
    }

    @Override
    public void accept(TileSetVisitor visitor) {}

    @Override
    public TilePageCalculator getTilePageCalculator() {
        return calculator;
    }

    @Override
    public void addToQuotaAndTileCounts(
            TileSet tileSet, Quota quotaDiff, Collection<PageStatsPayload> tileCountDiffs)
            throws InterruptedException {}

    @Override
    public Future<List<PageStats>> addHitsAndSetAccesTime(
            Collection<PageStatsPayload> statsUpdates) {
        return new Future<List<PageStats>>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return true;
            }

            @Override
            public boolean isCancelled() {
                return true;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public List<PageStats> get() throws InterruptedException, ExecutionException {
                return Collections.emptyList();
            }

            @Override
            public List<PageStats> get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return Collections.emptyList();
            }
        };
    }

    @Override
    public TilePage getLeastFrequentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return null;
    }

    @Override
    public TilePage getLeastRecentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return null;
    }

    @Override
    public PageStats setTruncated(TilePage tilePage) throws InterruptedException {
        return null;
    }

    @Override
    public void deleteGridSubset(String layerName, String gridSetId) {}

    @Override
    public void close() throws Exception {}

    @Override
    public void deleteParameters(String layerName, String parametersId) {}
}
