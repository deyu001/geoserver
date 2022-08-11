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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import java.net.URI;
import java.util.List;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.storage.ConfigDatabase;

/** Specialized RepositoryResolver for GeoServer manager Geogig Repositories. */
public class GeoServerGeoGigRepositoryResolver extends RepositoryResolver {

    public static final String GEOSERVER_URI_SCHEME = "geoserver";

    public static final int SCHEME_LENGTH = GEOSERVER_URI_SCHEME.length() + "://".length();

    public static String getURI(String repoName) {
        return String.format("%s://%s", GEOSERVER_URI_SCHEME, repoName);
    }

    @Override
    public boolean canHandle(URI repoURI) {
        return repoURI != null && canHandleURIScheme(repoURI.getScheme());
    }

    @Override
    public boolean canHandleURIScheme(String scheme) {
        return scheme != null && GEOSERVER_URI_SCHEME.equals(scheme);
    }

    @Override
    public boolean repoExists(URI repoURI) throws IllegalArgumentException {
        String name = getName(repoURI);
        RepositoryManager repoMgr = RepositoryManager.get();
        // get the repo by name
        RepositoryInfo repoInfo = repoMgr.getByRepoName(name);
        return repoInfo != null;
    }

    @Override
    public String getName(URI repoURI) {
        checkArgument(canHandle(repoURI), "Not a GeoServer GeoGig repository URI: %s", repoURI);
        // valid looking URI, strip the name part out and get everything after the scheme
        // "geoserver" and the "://"
        String name = repoURI.toString().substring(SCHEME_LENGTH);
        // if it's empty, they didn't provide a name or Id
        checkArgument(!Strings.isNullOrEmpty(name), "No GeoGig repository Name or ID specified");
        return name;
    }

    @Override
    public void initialize(URI repoURI, Context repoContext) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ConfigDatabase getConfigDatabase(URI repoURI, Context repoContext, boolean rootUri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Repository open(URI repositoryLocation) throws RepositoryConnectionException {
        String name = getName(repositoryLocation);
        // get a handle to the RepositoryManager
        RepositoryManager repoMgr = RepositoryManager.get();
        // get the repo by name
        RepositoryInfo info = repoMgr.getByRepoName(name);
        if (info != null) {
            // get the native RepositoryResolver for the location and open it directly
            // Using the RepositryManager to get the repo would cause the repo to be managed by the
            // RepositoryManager,
            // when this repo should be managed by the DataStore. The DataStore will close this repo
            // instance when
            // GeoServer decides to dispose the DataStore.
            Repository repo = RepositoryResolver.load(info.getLocation());
            checkState(
                    repo.isOpen(), "RepositoryManager returned a closed repository for %s", name);
            return repo;
        } else {
            // didn't find a repo
            RepositoryConnectionException rce =
                    new RepositoryConnectionException(
                            "No GeoGig repository found with NAME or ID: " + name);
            throw rce;
        }
    }

    @Override
    public boolean delete(URI repositoryLocation) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI buildRepoURI(URI rootRepoURI, String repoName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> listRepoNamesUnderRootURI(URI rootRepoURI) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
