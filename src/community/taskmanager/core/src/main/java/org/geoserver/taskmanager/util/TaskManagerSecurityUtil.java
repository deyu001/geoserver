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

package org.geoserver.taskmanager.util;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** @author Niels Charlier */
@Service
public class TaskManagerSecurityUtil {

    @Autowired
    @Qualifier("rawCatalog")
    private Catalog catalog;

    @Autowired private SecureCatalogImpl secureCatalog;

    @Autowired GeoServerSecurityManager secManager;

    private WorkspaceInfo getWorkspace(String workspaceName) {
        if (workspaceName == null) {
            return catalog.getDefaultWorkspace();
        } else {
            return catalog.getWorkspaceByName(workspaceName);
        }
    }

    public boolean isReadable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) { // lack of default workspace (allow) versus incorrect workspace (deny
            // unless admin)
            return config.getWorkspace() == null
                    || secManager.checkAuthenticationForAdminRole(user);
        } else {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isReadable();
        }
    }

    public boolean isReadable(Authentication user, Batch batch) {
        WorkspaceInfo wi = null;
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) {
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
            if (batch.getWorkspace() != null) { // otherwise ignore this, don't use default ws
                wi = getWorkspace(batch.getWorkspace());
            }
        } else {
            wi = getWorkspace(batch.getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isReadable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check1 =
                    batch.getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        if (wif != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isReadable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check2 =
                    batch.getConfiguration() == null
                            || batch.getConfiguration().getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
            ;
        }
        return check1 && check2;
    }

    public boolean isWritable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) { // lack of default workspace (allow) versus incorrect workspace (deny
            // unless admin)
            return config.getWorkspace() == null
                    || secManager.checkAuthenticationForAdminRole(user);
        } else {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isWritable();
        }
    }

    public boolean isWritable(Authentication user, Batch batch) {
        WorkspaceInfo wi = null;
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) {
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
            if (batch.getWorkspace() != null) { // otherwise ignore this, don't use default ws
                wi = getWorkspace(batch.getWorkspace());
            }
        } else {
            wi = getWorkspace(batch.getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isWritable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check1 =
                    batch.getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        if (wif != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isWritable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check2 =
                    batch.getConfiguration() == null
                            || batch.getConfiguration().getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        return check1 && check2;
    }

    public boolean isAdminable(Authentication user, Configuration config) {
        WorkspaceInfo wi = getWorkspace(config.getWorkspace());
        if (wi == null) { // lack of default workspace (allow) versus incorrect workspace (deny
            // unless admin)
            return config.getWorkspace() == null
                    || secManager.checkAuthenticationForAdminRole(user);
        } else {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            return limits == null || limits.isAdminable();
        }
    }

    public boolean isAdminable(Authentication user, Batch batch) {
        WorkspaceInfo wi = null;
        WorkspaceInfo wif = null;
        if (batch.getConfiguration() != null) { // otherwise ignore this, don't use default ws
            wif = getWorkspace(batch.getConfiguration().getWorkspace());
            if (batch.getWorkspace() != null) {
                wi = getWorkspace(batch.getWorkspace());
            }
        } else {
            wi = getWorkspace(batch.getWorkspace());
        }
        boolean check1, check2;
        if (wi != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wi);
            check1 = limits == null || limits.isAdminable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check1 =
                    batch.getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        if (wif != null) {
            WorkspaceAccessLimits limits =
                    secureCatalog.getResourceAccessManager().getAccessLimits(user, wif);
            check2 = limits == null || limits.isAdminable();
        } else { // lack of default workspace (allow) versus incorrect workspace (deny unless admin)
            check2 =
                    batch.getConfiguration() == null
                            || batch.getConfiguration().getWorkspace() == null
                            || secManager.checkAuthenticationForAdminRole(user);
        }
        return check1 && check2;
    }

    public boolean isAdminable(Authentication user, WorkspaceInfo ws) {
        WorkspaceAccessLimits limits =
                secureCatalog.getResourceAccessManager().getAccessLimits(user, ws);
        return limits == null || limits.isAdminable();
    }

    public boolean isWritable(Authentication user, WorkspaceInfo ws) {
        WorkspaceAccessLimits limits =
                secureCatalog.getResourceAccessManager().getAccessLimits(user, ws);
        return limits == null || limits.isWritable();
    }
}
