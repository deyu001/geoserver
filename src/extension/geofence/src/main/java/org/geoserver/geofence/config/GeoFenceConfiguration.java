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

package org.geoserver.geofence.config;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration object for GeofenceAccessManager.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class GeoFenceConfiguration implements Serializable, Cloneable {

    public static final String URL_INTERNAL = "internal:/";

    private static final long serialVersionUID = 3L;

    private String servicesUrl;

    private String instanceName;

    private boolean allowRemoteAndInlineLayers;

    private boolean grantWriteToWorkspacesToAuthenticatedUsers;

    private boolean useRolesToFilter;

    private String acceptedRoles = "";

    private List<String> roles = new ArrayList<>();

    private String gwcContextSuffix;

    private String defaultUserGroupServiceName;

    /** Remote GeoFence services url. */
    public String getServicesUrl() {
        return servicesUrl;
    }

    /** Remote GeoFence services url. */
    public void setServicesUrl(String servicesUrl) {
        this.servicesUrl = servicesUrl;
    }

    /**
     * Name of this GeoServer instance for GeoFence rule configuration.
     *
     * @param instanceName the instanceName to set
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /**
     * Name of this GeoServer instance for GeoFence rule configuration.
     *
     * @return the instanceName
     */
    public String getInstanceName() {
        return instanceName;
    }

    /** Flag to allow usage of remote and inline layers in SLDs. */
    public void setAllowRemoteAndInlineLayers(boolean allowRemoteAndInlineLayers) {
        this.allowRemoteAndInlineLayers = allowRemoteAndInlineLayers;
    }

    /** Flag to allow usage of remote and inline layers in SLDs. */
    public boolean isAllowRemoteAndInlineLayers() {
        return allowRemoteAndInlineLayers;
    }

    /**
     * Allows write access to resources to authenticated users, if false only ADMINs have write
     * access.
     *
     * @return the grantWriteToWorkspacesToAuthenticatedUsers
     */
    public boolean isGrantWriteToWorkspacesToAuthenticatedUsers() {
        return grantWriteToWorkspacesToAuthenticatedUsers;
    }

    /**
     * Allows write access to resources to authenticated users, if false only ADMINs have write
     * access.
     *
     * @param grantWriteToWorkspacesToAuthenticatedUsers the
     *     grantWriteToWorkspacesToAuthenticatedUsers to set
     */
    public void setGrantWriteToWorkspacesToAuthenticatedUsers(
            boolean grantWriteToWorkspacesToAuthenticatedUsers) {
        this.grantWriteToWorkspacesToAuthenticatedUsers =
                grantWriteToWorkspacesToAuthenticatedUsers;
    }

    /**
     * Use authenticated users roles to match rules, instead of username.
     *
     * @return the useRolesToFilter
     */
    public boolean isUseRolesToFilter() {
        return useRolesToFilter;
    }

    /**
     * Use authenticated users roles to match rules, instead of username.
     *
     * @param useRolesToFilter the useRolesToFilter to set
     */
    public void setUseRolesToFilter(boolean useRolesToFilter) {
        this.useRolesToFilter = useRolesToFilter;
    }

    /**
     * List of mutually exclusive roles used for rule matching when useRolesToFilter is true.
     *
     * @return the acceptedRoles
     */
    public String getAcceptedRoles() {
        return acceptedRoles;
    }

    /**
     * List of mutually exclusive roles used for rule matching when useRolesToFilter is true.
     *
     * @param acceptedRoles the acceptedRoles to set
     */
    public void setAcceptedRoles(String acceptedRoles) {
        if (acceptedRoles == null) {
            acceptedRoles = "";
        }

        this.acceptedRoles = acceptedRoles;

        // from comma delimited to list
        roles = Lists.newArrayList(acceptedRoles.split(","));
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isInternal() {
        return servicesUrl.startsWith(URL_INTERNAL);
    }

    /** @return */
    public String getGwcContextSuffix() {
        return gwcContextSuffix;
    }

    /** @param gwcContextSuffix the gwcContextSuffix to set */
    public void setGwcContextSuffix(String gwcContextSuffix) {
        this.gwcContextSuffix = gwcContextSuffix;
    }

    /** @param defaultUserGroupServiceName the defaultUserGroupServiceName to set */
    public void setDefaultUserGroupServiceName(String defaultUserGroupServiceName) {
        this.defaultUserGroupServiceName = defaultUserGroupServiceName;
    }

    /** @return */
    public String getDefaultUserGroupServiceName() {
        return defaultUserGroupServiceName;
    }

    /** Creates a copy of the configuration object. */
    @Override
    public GeoFenceConfiguration clone() {
        try {
            GeoFenceConfiguration clone = (GeoFenceConfiguration) super.clone();
            clone.setAcceptedRoles(
                    acceptedRoles); // make sure the computed list is properly initted
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new UnknownError("Unexpected exception: " + ex.getMessage());
        }
    }
}
