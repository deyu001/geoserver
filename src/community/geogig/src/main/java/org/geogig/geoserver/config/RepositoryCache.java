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

package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryResolver;

class RepositoryCache {

    private static final Logger LOGGER = Logging.getLogger(RepositoryCache.class);

    private final LoadingCache<String, Repository> repoCache;

    public RepositoryCache(final RepositoryManager repoManager) {

        RemovalListener<String, Repository> disposingListener =
                new RemovalListener<String, Repository>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, Repository> notification) {
                        String repoId = notification.getKey();
                        Repository repository = notification.getValue();
                        if (repository != null) {
                            try {
                                URI location = repository.getLocation();
                                LOGGER.fine(
                                        format(
                                                "Closing cached GeoGig repository instance %s",
                                                location != null ? location : repoId));
                                repository.close();
                                LOGGER.finer(
                                        format(
                                                "Closed cached GeoGig repository instance %s",
                                                location != null ? location : repoId));
                            } catch (RuntimeException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        format(
                                                "Error disposing GeoGig repository instance for id %s",
                                                repoId),
                                        e);
                            }
                        }
                    }
                };

        final CacheLoader<String, Repository> loader =
                new CacheLoader<String, Repository>() {
                    private final RepositoryManager manager = repoManager;

                    @Override
                    public Repository load(final String repoId) throws Exception {
                        try {
                            RepositoryInfo repoInfo = manager.get(repoId);
                            URI repoLocation = repoInfo.getLocation();
                            // RepositoryResolver.load returns an open repository or fails
                            Repository repo = RepositoryResolver.load(repoLocation);
                            checkState(repo.isOpen());

                            return repo;
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.WARNING,
                                    format(
                                            "Error loading GeoGig repository instance for id %s",
                                            repoId),
                                    e);
                            throw e;
                        }
                    }
                };

        repoCache =
                CacheBuilder.newBuilder() //
                        .softValues() //
                        .expireAfterAccess(5, TimeUnit.MINUTES) //
                        .removalListener(disposingListener) //
                        .build(loader);
    }

    /**
     * @implNote: the returned repository's close() method does nothing. Closing the repository
     *     happens when it's evicted from the cache or is removed. This avoids several errors as
     *     GeoSever can aggressively create and dispose DataStores, whose dispose() method would
     *     otherwise close the repository and produce unexpected exceptions for any other code using
     *     it.
     */
    public Repository get(final String repositoryId) throws IOException {
        try {
            return repoCache.get(repositoryId);
        } catch (Throwable e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            Throwable cause = e.getCause();
            throw new IOException(
                    "Error obtaining cached geogig instance for repo "
                            + repositoryId
                            + ": "
                            + cause.getMessage(),
                    cause);
        }
    }

    public void invalidate(final String repoId) {
        repoCache.invalidate(repoId);
    }

    public void invalidateAll() {
        repoCache.invalidateAll();
        repoCache.cleanUp();
    }
}
