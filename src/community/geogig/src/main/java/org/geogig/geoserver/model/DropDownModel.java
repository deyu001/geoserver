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

package org.geogig.geoserver.model;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.locationtech.geogig.repository.RepositoryResolver;

/**
 * Data model for the drop-down choice for GeoGig repository configuration. Currently, either a
 * Directory backend or a PostgreSQL backend are supported.
 */
public class DropDownModel implements IModel<Serializable> {

    private static final long serialVersionUID = 1L;
    static final String NO_DEFAULT_AVAILABLE = "No available repository types";

    public static final String PG_CONFIG = "PostgreSQL";
    public static final String DIRECTORY_CONFIG = "Directory";
    static String DEFAULT_CONFIG;
    public static final List<String> CONFIG_LIST = new ArrayList<>(2);

    static {
        if (RepositoryResolver.resolverAvailableForURIScheme("file")) {
            CONFIG_LIST.add(DIRECTORY_CONFIG);
        }
        if (RepositoryResolver.resolverAvailableForURIScheme("postgresql")) {
            CONFIG_LIST.add(PG_CONFIG);
        }
        if (!CONFIG_LIST.isEmpty()) {
            DEFAULT_CONFIG = CONFIG_LIST.get(0);
        } else {
            DEFAULT_CONFIG = NO_DEFAULT_AVAILABLE;
        }
    }

    private final IModel<RepositoryInfo> repoModel;
    private String type;

    public DropDownModel(IModel<RepositoryInfo> repoModel) {
        this.repoModel = repoModel;
        if (null == repoModel
                || null == repoModel.getObject()
                || null == repoModel.getObject().getLocation()) {
            type = DEFAULT_CONFIG;
        }
    }

    @Override
    public Serializable getObject() {
        if (type == null) {
            // get the type from the model
            RepositoryInfo repo = repoModel.getObject();
            URI location = repo != null ? repo.getLocation() : null;
            type = getType(location);
        }
        return type;
    }

    @Override
    public void setObject(Serializable object) {
        type = object.toString();
    }

    @Override
    public void detach() {
        if (repoModel != null) {
            repoModel.detach();
        }
        type = null;
    }

    public static String getType(URI location) {
        if (location != null) {
            if (null != location.getScheme()) {
                switch (location.getScheme()) {
                    case "postgresql":
                        return PG_CONFIG;
                    case "file":
                        return DIRECTORY_CONFIG;
                }
            }
        }
        return DEFAULT_CONFIG;
    }

    @VisibleForTesting
    static void setConfigList(List<String> configs, String defaultConfig) {
        // clear the existing list
        CONFIG_LIST.clear();
        // re-populate with provided configs
        CONFIG_LIST.addAll(configs);
        // set the default
        if (null != defaultConfig) {
            DEFAULT_CONFIG = defaultConfig;
        } else {
            DEFAULT_CONFIG = NO_DEFAULT_AVAILABLE;
        }
    }
}
