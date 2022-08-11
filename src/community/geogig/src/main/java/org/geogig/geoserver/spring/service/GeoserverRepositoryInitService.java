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

package org.geogig.geoserver.spring.service;

import static org.locationtech.geogig.porcelain.ConfigOp.ConfigAction.CONFIG_SET;
import static org.locationtech.geogig.porcelain.ConfigOp.ConfigScope.LOCAL;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.File;
import java.net.URI;
import java.util.Map;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.spring.dto.InitRequest;
import org.locationtech.geogig.spring.dto.RepositoryInitRepo;
import org.locationtech.geogig.spring.service.RepositoryInitService;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Replace the default {@link RepositoryInitService} with one that saves the repository info. */
@Service("repositoryInitService")
public class GeoserverRepositoryInitService extends RepositoryInitService {

    @Override
    public RepositoryInitRepo initRepository(
            RepositoryProvider provider, String repositoryName, Map<String, String> parameters)
            throws RepositoryConnectionException {
        if (provider.hasGeoGig(repositoryName)) {
            throw new CommandSpecException(
                    "The specified repository name is already in use, please try a different name",
                    HttpStatus.CONFLICT);
        }

        Repository newRepo = provider.createGeogig(repositoryName, parameters);

        if (newRepo.isOpen()) {
            throw new CommandSpecException(
                    "Cannot run init on an already initialized repository.", HttpStatus.CONFLICT);
        }

        InitOp command = newRepo.command(InitOp.class);

        newRepo = command.call();

        // set author inof, if provided in request parameters
        String authorName = parameters.get(InitRequest.AUTHORNAME);
        String authorEmail = parameters.get(InitRequest.AUTHOREMAIL);
        if (authorName != null || authorEmail != null) {
            ConfigOp configOp = newRepo.command(ConfigOp.class);
            configOp.setAction(CONFIG_SET)
                    .setScope(LOCAL)
                    .setName("user.name")
                    .setValue(authorName)
                    .call();
            configOp.setAction(CONFIG_SET)
                    .setScope(LOCAL)
                    .setName("user.email")
                    .setValue(authorEmail)
                    .call();
        }
        Optional<URI> repoUri = newRepo.command(ResolveGeogigURI.class).call();
        Preconditions.checkState(
                repoUri.isPresent(), "Unable to resolve URI of newly created repository.");

        final String repoName =
                RepositoryResolver.load(repoUri.get()).command(ResolveRepositoryName.class).call();
        RepositoryInitRepo info = new RepositoryInitRepo();
        info.setName(repoName);
        // set the Web API Atom Link, not the repository URI link
        info.setLink(RepositoryProvider.BASE_REPOSITORY_ROUTE + "/" + repoName);
        saveRepository(newRepo);
        return info;
    }

    private RepositoryInfo saveRepository(Repository geogig) {
        // repo was just created, need to register it with an ID in the manager
        // create a RepositoryInfo object
        RepositoryInfo repoInfo = new RepositoryInfo();
        URI location = geogig.getLocation().normalize();
        if ("file".equals(location.getScheme())) {
            // need the parent
            File parentDir = new File(location).getParentFile();
            location = parentDir.toURI().normalize();
        }
        // set the URI
        repoInfo.setLocation(location);
        // save the repo, this will set a UUID
        return RepositoryManager.get().save(repoInfo);
    }
}
