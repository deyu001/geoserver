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

package org.geoserver.security.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;

/**
 * An {@link AuthenticationCache} implementation using a {@link LRUCache} for caching authentication
 * tokens.
 *
 * <p>For an explanation of the time parameters, see {@link AuthenticationCacheEntry}
 *
 * <p>The class uses a {@link ReentrantReadWriteLock} object to synchronize access from multiple
 * threads
 *
 * <p>Additionally, a {@link TimerTask} is started to remove expired entries.
 *
 * @author christian
 */
public class LRUAuthenticationCacheImpl implements AuthenticationCache {

    protected LRUCache<AuthenticationCacheKey, AuthenticationCacheEntry> cache;
    int timeToIdleSeconds, timeToLiveSeconds, maxEntries;

    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** Timer task to remove unused entries */
    public LRUAuthenticationCacheImpl(int maxEntries) {
        this(DEFAULT_IDLE_TIME, DEFAULT_LIVE_TIME, maxEntries);
    }

    public LRUAuthenticationCacheImpl(
            int timeToIdleSeconds, int timeToLiveSeconds, int maxEntries) {
        super();
        this.timeToIdleSeconds = timeToIdleSeconds;
        this.timeToLiveSeconds = timeToLiveSeconds;
        this.maxEntries = maxEntries;
        cache = new LRUCache<>(maxEntries);
    }

    public int getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    @Override
    public void removeAll() {
        writeLock.lock();
        try {
            cache.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeAll(String filterName) {
        if (filterName == null) return;
        writeLock.lock();
        try {
            Set<AuthenticationCacheKey> toBeRemoved = new HashSet<>();
            for (AuthenticationCacheKey key : cache.keySet()) {
                if (filterName.equals(key.getFilterName())) toBeRemoved.add(key);
            }
            for (AuthenticationCacheKey key : toBeRemoved) cache.remove(key);

        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(String filterName, String cacheKey) {
        writeLock.lock();
        try {
            cache.remove(new AuthenticationCacheKey(filterName, cacheKey));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Authentication get(String filterName, String cacheKey) {
        readLock.lock();
        boolean hasTobeRemoved = false;
        try {
            long currentTime = System.currentTimeMillis();
            AuthenticationCacheEntry entry =
                    cache.get(new AuthenticationCacheKey(filterName, cacheKey));
            if (entry == null) return null;
            if (entry.hasExpired(currentTime)) {
                hasTobeRemoved = true;
                return null;
            }
            entry.setLastAccessed(currentTime);
            return entry.getAuthentication();

        } finally {
            readLock.unlock();
            if (hasTobeRemoved) remove(filterName, cacheKey);
        }
    }

    @Override
    public void put(
            String filterName,
            String cacheKey,
            Authentication auth,
            Integer timeToIdleSeconds,
            Integer timeToLiveSeconds) {

        timeToIdleSeconds = timeToIdleSeconds != null ? timeToIdleSeconds : this.timeToIdleSeconds;
        timeToLiveSeconds = timeToLiveSeconds != null ? timeToLiveSeconds : this.timeToLiveSeconds;

        writeLock.lock();
        try {
            cache.put(
                    new AuthenticationCacheKey(filterName, cacheKey),
                    new AuthenticationCacheEntry(auth, timeToIdleSeconds, timeToLiveSeconds));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth) {
        put(filterName, cacheKey, auth, timeToIdleSeconds, timeToLiveSeconds);
    }
}
