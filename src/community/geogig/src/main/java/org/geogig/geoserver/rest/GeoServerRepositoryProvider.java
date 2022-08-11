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

package org.geogig.geoserver.rest;

import static org.geogig.geoserver.config.GeoServerGeoGigRepositoryResolver.getURI;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.rest.repository.InitRequestUtil;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.springframework.http.HttpStatus;

/**
 * {@link RepositoryProvider} that looks up the coresponding {@link GeoGIG} instance to a given
 * {@link HttpServletRequest} by asking the geoserver's {@link RepositoryManager}
 */
public class GeoServerRepositoryProvider implements RepositoryProvider {

    /** Init request command string. */
    public static final String INIT_CMD = "init";

    /** Import Existing Repository command string. */
    public static final String IMPORT_CMD = "importExistingRepo";

    public List<RepositoryInfo> getRepositoryInfos() {
        return RepositoryManager.get().getAll();
    }

    private String getRepoIdForName(String repoName) {
        // get the list of Repos the Manager knows about
        // loop and return the id if we find one
        for (RepositoryInfo repo : getRepositoryInfos()) {
            if (repo.getRepoName().equals(repoName)) {
                return repo.getId();
            }
        }
        return null;
    }

    @Override
    public void delete(String repoName) {
        Optional<Repository> geogig = getGeogig(repoName);
        Preconditions.checkState(geogig.isPresent(), "No repository to delete.");

        final String repoId = getRepoIdForName(repoName);
        Repository ggig = geogig.get();
        Optional<URI> repoUri = ggig.command(ResolveGeogigURI.class).call();
        Preconditions.checkState(repoUri.isPresent(), "No repository to delete.");

        ggig.close();
        try {
            GeoGIG.delete(repoUri.get());
            RepositoryManager manager = RepositoryManager.get();
            manager.delete(repoId);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void invalidate(String repoName) {
        final String repoId = getRepoIdForName(repoName);
        if (repoId != null) {
            RepositoryManager manager = RepositoryManager.get();
            manager.invalidate(repoId);
        }
    }

    @Override
    public Iterator<String> findRepositories() {
        List<RepositoryInfo> infos = getRepositoryInfos();
        return Iterators.transform(
                infos.iterator(),
                new Function<RepositoryInfo, String>() {
                    @Override
                    public String apply(RepositoryInfo input) {
                        return input.getRepoName();
                    }
                });
    }

    @Override
    public Repository createGeogig(String repositoryName, Map<String, String> parameters) {
        if (repositoryName != null && RepositoryManager.get().repoExistsByName(repositoryName)) {
            // repo already exists
            throw new CommandSpecException(
                    "The specified repository name is already in use, please try a different name",
                    HttpStatus.CONFLICT);
        }

        Optional<Repository> initRepo =
                AddRepoRequestHandler.createGeoGIG(repositoryName, parameters);
        if (initRepo.isPresent()) {
            // init request was sufficient
            return initRepo.get();
        }
        return null;
    }

    public Repository importExistingGeogig(String repositoryName, Map<String, String> parameters) {
        Optional<Repository> importRepo =
                AddRepoRequestHandler.importGeogig(repositoryName, parameters);
        if (importRepo.isPresent()) {
            // import request was sufficient
            return importRepo.get();
        }
        return null;
    }

    public Optional<Repository> getGeogig(String repositoryName) {
        Repository geogig = findRepository(repositoryName);
        return Optional.fromNullable(geogig);
    }

    private Repository findRepository(String repositoryName) {

        RepositoryManager manager = RepositoryManager.get();
        String repoId = getRepoIdForName(repositoryName);
        if (null == repoId) {
            return null;
        }
        try {
            return manager.getRepository(repoId);
        } catch (IOException e) {
            throw new CommandSpecException(
                    "Error accessing datastore " + repositoryName,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private static class AddRepoRequestHandler {

        private static Optional<Repository> createGeoGIG(
                String repositoryName, Map<String, String> parameters) {
            try {
                final Hints hints =
                        InitRequestUtil.createHintsFromParameters(repositoryName, parameters);
                final Repository repository = RepositoryManager.get().createRepo(hints);
                return Optional.fromNullable(repository);
            } catch (Exception ex) {
                Throwables.propagate(ex);
            }
            return Optional.absent();
        }

        private static Optional<Repository> importGeogig(
                String repositoryName, Map<String, String> parameters) {
            // if the request is a POST, and the request path ends in "importExistingRepo"

            try {
                final Hints hints =
                        InitRequestUtil.createHintsFromParameters(repositoryName, parameters);

                // now build the repo with the Hints
                RepositoryInfo repoInfo = new RepositoryInfo();

                // set the repo location from the URI
                if (!hints.get(Hints.REPOSITORY_URL).isPresent()) {
                    return Optional.absent();
                }
                URI uri = new URI(hints.get(Hints.REPOSITORY_URL).get().toString());
                repoInfo.setLocation(uri);

                // check to see if repo is initialized
                RepositoryResolver repoResolver = RepositoryResolver.lookup(uri);
                if (!repoResolver.repoExists(uri)) {
                    return Optional.absent();
                }

                // save the repo, this will set a UUID
                RepositoryManager.get().save(repoInfo);

                return Optional.of(RepositoryManager.get().getRepository(repoInfo.getId()));
            } catch (IOException | URISyntaxException e) {
                Throwables.propagate(e);
            } catch (RepositoryConnectionException e) {
                e.printStackTrace();
            }
            return Optional.absent();
        }
    }

    @Override
    public boolean hasGeoGig(String repositoryName) {
        if (null != repositoryName) {
            Iterator<String> findRepositories = findRepositories();
            while (findRepositories.hasNext()) {
                String next = findRepositories.next();
                if (next.equals(repositoryName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getMaskedLocationString(Repository repo, String repoName) {
        // need to mask the location as "geoserver://<repoName>"
        return getURI(repoName);
    }

    @Override
    public String getRepositoryId(String repoName) {
        return this.getRepoIdForName(repoName);
    }
}
