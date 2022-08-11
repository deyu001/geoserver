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

package org.geogig.geoserver.spring.controller;

import static org.locationtech.geogig.rest.repository.RepositoryProvider.BASE_REPOSITORY_ROUTE;
import static org.locationtech.geogig.rest.repository.RepositoryProvider.GEOGIG_ROUTE_PREFIX;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.bind.annotation.RequestMethod.TRACE;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geogig.geoserver.spring.dto.RepositoryImportRepo;
import org.geogig.geoserver.spring.service.ImportRepoService;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.spring.controller.AbstractController;
import org.locationtech.geogig.spring.dto.InitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for importing an existing repository. */
@RestController
@RequestMapping(
    path = GEOGIG_ROUTE_PREFIX + "/" + BASE_REPOSITORY_ROUTE + "/{repoName}/importExistingRepo",
    produces = {APPLICATION_XML_VALUE, APPLICATION_JSON_VALUE}
)
public class ImportRepoCommandController extends AbstractController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportRepoCommandController.class);

    @Autowired private ImportRepoService importRepoService;

    @RequestMapping(method = {GET, PUT, DELETE, PATCH, TRACE, OPTIONS})
    public void catchAll() {
        // if we hit this controller, it's a 405
        supportedMethods(Sets.newHashSet(POST.toString()));
    }

    @PostMapping
    public void importRepositoryNoBody(
            @PathVariable(name = "repoName") String repoName,
            HttpServletRequest request,
            HttpServletResponse response)
            throws RepositoryConnectionException {
        RepositoryImportRepo repo = importRepo(request, repoName);
        encode(repo, request, response);
    }

    @PostMapping(consumes = {APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE})
    public void importRepositoryFromJsonOrXml(
            @PathVariable(name = "repoName") String repoName,
            @RequestBody InitRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response)
            throws RepositoryConnectionException {

        RepositoryImportRepo repo = importRepo(request, repoName, requestBody);
        encode(repo, request, response);
    }

    @PostMapping(consumes = {APPLICATION_FORM_URLENCODED_VALUE})
    public void importRepositoryFromForm(
            @PathVariable(name = "repoName") String repoName,
            @RequestBody MultiValueMap<String, String> requestBody,
            HttpServletRequest request,
            HttpServletResponse response)
            throws RepositoryConnectionException {
        RepositoryImportRepo repo = importRepo(request, repoName, requestBody);
        encode(repo, request, response);
    }

    private RepositoryImportRepo importRepo(HttpServletRequest request, String repoName)
            throws RepositoryConnectionException {
        Optional<RepositoryProvider> repoProvider = getRepoProvider(request);
        if (repoProvider.isPresent()) {
            return importRepoService.importRepository(
                    repoProvider.get(), repoName, Maps.newHashMap());
        } else {
            throw NO_PROVIDER;
        }
    }

    private RepositoryImportRepo importRepo(
            HttpServletRequest request, String repoName, InitRequest requestBody)
            throws RepositoryConnectionException {
        Optional<RepositoryProvider> repoProvider = getRepoProvider(request);
        if (repoProvider.isPresent()) {
            return importRepoService.importRepository(
                    repoProvider.get(),
                    repoName,
                    (requestBody == null) ? Maps.newHashMap() : requestBody.getParameters());
        } else {
            throw NO_PROVIDER;
        }
    }

    private RepositoryImportRepo importRepo(
            HttpServletRequest request, String repoName, MultiValueMap<String, String> requestBody)
            throws RepositoryConnectionException {
        Optional<RepositoryProvider> repoProvider = getRepoProvider(request);
        if (repoProvider.isPresent()) {
            return importRepoService.importRepository(
                    repoProvider.get(),
                    repoName,
                    (requestBody == null) ? Maps.newHashMap() : requestBody.toSingleValueMap());
        } else {
            throw NO_PROVIDER;
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
