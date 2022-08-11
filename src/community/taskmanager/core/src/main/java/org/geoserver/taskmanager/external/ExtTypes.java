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

package org.geoserver.taskmanager.external;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerSecurityUtil;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Parameter types refering to external resources.
 *
 * @author Niels Charlier
 */
@Service
public class ExtTypes {
    private static final Logger LOGGER = Logging.getLogger(ExtTypes.class);

    @Autowired private LookupService<DbSource> dbSources;

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private LookupService<FileService> fileServices;

    @Autowired private GeoServer geoServer;

    @Autowired private TaskManagerSecurityUtil secUtil;

    public final ParameterType dbName =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return new ArrayList<String>(dbSources.names());
                }

                @Override
                public DbSource parse(String value, List<String> dependsOnRawValues) {
                    return dbSources.get(value);
                }
            };

    public final ParameterType tableName =
            new ParameterType() {
                private Set<String> getTables(String databaseName) {
                    SortedSet<String> tables = new TreeSet<String>();
                    tables.add(""); // custom value is possible here
                    if (databaseName != null) {
                        DbSource ds = dbSources.get(databaseName);
                        if (ds != null) {
                            try {
                                try (Connection conn = ds.getDataSource().getConnection()) {
                                    DatabaseMetaData md = conn.getMetaData();
                                    try (ResultSet rs =
                                            md.getTables(
                                                    null,
                                                    ds.getSchema(),
                                                    "%",
                                                    new String[] {
                                                        "TABLE", "VIEW", "MATERIALIZED VIEW"
                                                    })) {
                                        while (rs.next()) {
                                            if (ds.getSchema() != null || rs.getString(2) == null) {
                                                tables.add(rs.getString(3));
                                            } else {
                                                tables.add(rs.getString(2) + "." + rs.getString(3));
                                            }
                                        }
                                    }
                                }
                            } catch (SQLException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Failed to retrieve tables from data source "
                                                + databaseName,
                                        e);
                            }
                        }
                    }
                    return tables;
                }

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return new ArrayList<String>(getTables(dependsOnRawValues.get(0)));
                }

                @Override
                public boolean validate(String value, List<String> dependsOnRawValues) {
                    if (dependsOnRawValues.size() < 1) {
                        throw new IllegalArgumentException(
                                "tableName parameter must be dependent on database.");
                    }
                    // since the table may not yet exist, do not validate its existence.
                    return true;
                }

                @Override
                public Object parse(String value, List<String> dependsOnRawValues) {
                    if (dependsOnRawValues.size() < 1) {
                        throw new IllegalArgumentException(
                                "tableName parameter must be dependent on database.");
                    }
                    return new DbTableImpl(dbSources.get(dependsOnRawValues.get(0)), value);
                }
            };

    public final ParameterType extGeoserver =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return new ArrayList<String>(extGeoservers.names());
                }

                @Override
                public ExternalGS parse(String value, List<String> dependsOnRawValues) {
                    return extGeoservers.get(value);
                }
            };

    public final ParameterType workspace =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    SortedSet<String> workspaces = new TreeSet<>();
                    for (WorkspaceInfo workspace : geoServer.getCatalog().getWorkspaces()) {
                        if (secUtil.isWritable(
                                SecurityContextHolder.getContext().getAuthentication(),
                                workspace)) {
                            workspaces.add(workspace.getName());
                        }
                    }
                    return new ArrayList<String>(workspaces);
                }

                @Override
                public WorkspaceInfo parse(String value, List<String> dependsOnRawValues) {
                    return geoServer.getCatalog().getWorkspaceByName(value);
                }
            };

    public final ParameterType internalLayer =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    SortedSet<String> layers = new TreeSet<>();
                    if (dependsOnRawValues.size() > 0
                            && !Strings.isEmpty(dependsOnRawValues.get(0))) {
                        NamespaceInfo ni =
                                geoServer
                                        .getCatalog()
                                        .getNamespaceByPrefix(dependsOnRawValues.get(0));
                        for (ResourceInfo resource :
                                geoServer
                                        .getCatalog()
                                        .getResourcesByNamespace(ni, ResourceInfo.class)) {
                            layers.add(resource.getName());
                        }
                    } else {
                        for (LayerInfo layer : geoServer.getCatalog().getLayers()) {
                            layers.add(layer.prefixedName());
                            if (layer.getResource()
                                    .getNamespace()
                                    .equals(geoServer.getCatalog().getDefaultNamespace())) {
                                layers.add(layer.getName());
                            }
                        }
                    }
                    return new ArrayList<String>(layers);
                }

                @Override
                public LayerInfo parse(String value, List<String> dependsOnRawValues) {
                    if (dependsOnRawValues.size() > 0
                            && !Strings.isEmpty(dependsOnRawValues.get(0))) {
                        NamespaceInfo ns =
                                geoServer
                                        .getCatalog()
                                        .getNamespaceByPrefix(dependsOnRawValues.get(0));
                        if (ns == null) {
                            return null;
                        }
                        return geoServer
                                .getCatalog()
                                .getLayerByName(new NameImpl(ns.getURI(), value));
                    } else {
                        return geoServer.getCatalog().getLayerByName(value);
                    }
                }

                @Override
                public List<String> getActions() {
                    return Collections.singletonList("LayerEdit");
                }
            };

    public final ParameterType name =
            new ParameterType() {

                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return null;
                }

                @Override
                public Name parse(String value, List<String> dependsOnRawValues) {
                    NamespaceInfo ni;
                    if (dependsOnRawValues.size() > 0
                            && !Strings.isEmpty(dependsOnRawValues.get(0))) {
                        ni = geoServer.getCatalog().getNamespaceByPrefix(dependsOnRawValues.get(0));
                    } else {
                        int colon = value.indexOf(':');
                        if (colon >= 0) {
                            ni =
                                    geoServer
                                            .getCatalog()
                                            .getNamespaceByPrefix(value.substring(0, colon));
                            value = value.substring(colon + 1);
                        } else {
                            ni = geoServer.getCatalog().getDefaultNamespace();
                        }
                    }
                    return new NameImpl(ni == null ? null : ni.getURI(), value);
                }

                @Override
                public boolean validate(String value, List<String> dependsOnRawValues) {
                    int colon = value.indexOf(':');
                    if (colon >= 0) {
                        return geoServer
                                        .getCatalog()
                                        .getNamespaceByPrefix(value.substring(0, colon))
                                != null;
                    }
                    return true;
                }
            };

    public final ParameterType fileService =
            new ParameterType() {
                @Override
                public List<String> getDomain(List<String> dependsOnRawValues) {
                    return new ArrayList<String>(fileServices.names());
                }

                @Override
                public FileService parse(String value, List<String> dependsOnRawValues) {
                    return fileServices.get(value);
                }
            };

    public final ParameterType file(boolean mustExist, boolean canUpload) {
        return new ParameterType() {
            @Override
            public List<String> getDomain(List<String> dependsOnRawValues) {
                return null;
            }

            @Override
            public FileReference parse(String value, List<String> dependsOnRawValues) {
                if (dependsOnRawValues.size() < 1) {
                    throw new IllegalArgumentException(
                            "file parameter must be dependent on file service.");
                }
                FileService fileService = fileServices.get(dependsOnRawValues.get(0));
                if (dependsOnRawValues.size() >= 2
                        && "true".equalsIgnoreCase(dependsOnRawValues.get(1))) {
                    value = FileService.versioned(value);
                }
                FileReference ref = fileService.getVersioned(value);
                if (mustExist) {
                    try {
                        if (!fileService.checkFileExists(ref.getLatestVersion())) {
                            return null;
                        }
                    } catch (IOException e) {
                        return null;
                    }
                }
                return ref;
            }

            @Override
            public boolean validate(String value, List<String> dependsOnRawValues) {
                if (dependsOnRawValues.size() < 1) {
                    throw new IllegalArgumentException(
                            "file parameter must be dependent on file service.");
                }
                // since the file may not yet exist at configuration
                // do not validate its existence
                return true;
            }

            @Override
            public List<String> getActions() {
                return canUpload
                        ? Collections.singletonList("FileUpload")
                        : Collections.emptyList();
            }
        };
    }
}
