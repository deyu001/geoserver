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

package org.geoserver.security;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerRestRoleServiceConfig extends BaseSecurityNamedServiceConfig
        implements SecurityRoleServiceConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = -8380244566532287415L;

    private static final int defaultCacheConcurrencyLevel = 4;

    private static final long defaultCacheMaximumSize = 10000;

    private static final long defaultCacheExpirationTime = 30000;

    private String adminGroup;

    private String groupAdminGroup;

    private String baseUrl;

    private String rolesRESTEndpoint = "/api/roles";

    private String adminRoleRESTEndpoint = "/api/adminRole";

    private String usersRESTEndpoint = "/api/users";

    private String rolesJSONPath = "$.groups";

    private String adminRoleJSONPath = "$.adminRole";

    private String usersJSONPath = "$.users[?(@.username=='${username}')].groups";

    private int cacheConcurrencyLevel = defaultCacheConcurrencyLevel;

    private long cacheMaximumSize = defaultCacheMaximumSize;

    private long cacheExpirationTime = defaultCacheExpirationTime;

    private String authApiKey;

    @Override
    public String getAdminRoleName() {
        return adminGroup;
    }

    @Override
    public void setAdminRoleName(String adminRoleName) {
        this.adminGroup = adminRoleName;
    }

    @Override
    public String getGroupAdminRoleName() {
        return groupAdminGroup;
    }

    @Override
    public void setGroupAdminRoleName(String adminRoleName) {
        this.groupAdminGroup = adminRoleName;
    }

    /** @return the baseUrl */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** @param baseUrl the baseUrl to set */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** @return the rolesRESTEndpoint */
    public String getRolesRESTEndpoint() {
        return rolesRESTEndpoint;
    }

    /** @param rolesRESTEndpoint the rolesRESTEndpoint to set */
    public void setRolesRESTEndpoint(String rolesRESTEndpoint) {
        this.rolesRESTEndpoint = rolesRESTEndpoint;
    }

    /** @return the adminRoleRESTEndpoint */
    public String getAdminRoleRESTEndpoint() {
        return adminRoleRESTEndpoint;
    }

    /** @param adminRoleRESTEndpoint the adminRoleRESTEndpoint to set */
    public void setAdminRoleRESTEndpoint(String adminRoleRESTEndpoint) {
        this.adminRoleRESTEndpoint = adminRoleRESTEndpoint;
    }

    /** @return the usersRESTEndpoint */
    public String getUsersRESTEndpoint() {
        return usersRESTEndpoint;
    }

    /** @param usersRESTEndpoint the usersRESTEndpoint to set */
    public void setUsersRESTEndpoint(String usersRESTEndpoint) {
        this.usersRESTEndpoint = usersRESTEndpoint;
    }

    /** @return the rolesJSONPath */
    public String getRolesJSONPath() {
        return rolesJSONPath;
    }

    /** @param rolesJSONPath the rolesJSONPath to set */
    public void setRolesJSONPath(String rolesJSONPath) {
        this.rolesJSONPath = rolesJSONPath;
    }

    /** @return the adminRoleJSONPath */
    public String getAdminRoleJSONPath() {
        return adminRoleJSONPath;
    }

    /** @param adminRoleJSONPath the adminRoleJSONPath to set */
    public void setAdminRoleJSONPath(String adminRoleJSONPath) {
        this.adminRoleJSONPath = adminRoleJSONPath;
    }

    /** @return the usersJSONPath */
    public String getUsersJSONPath() {
        return usersJSONPath;
    }

    /** @param usersJSONPath the usersJSONPath to set */
    public void setUsersJSONPath(String usersJSONPath) {
        this.usersJSONPath = usersJSONPath;
    }

    /** @return the cacheConcurrencyLevel */
    public int getCacheConcurrencyLevel() {
        if (cacheConcurrencyLevel > 1) {
            return cacheConcurrencyLevel;
        } else {
            return defaultCacheConcurrencyLevel;
        }
    }

    /** @param cacheConcurrencyLevel the cacheConcurrencyLevel to set */
    public void setCacheConcurrencyLevel(int cacheConcurrencyLevel) {
        this.cacheConcurrencyLevel = cacheConcurrencyLevel;
    }

    /** @return the cacheMaximumSize */
    public long getCacheMaximumSize() {
        if (cacheMaximumSize > 0) {
            return cacheMaximumSize;
        } else {
            return defaultCacheMaximumSize;
        }
    }

    /** @param cacheMaximumSize the cacheMaximumSize to set */
    public void setCacheMaximumSize(long cacheMaximumSize) {
        this.cacheMaximumSize = cacheMaximumSize;
    }

    /** @param cacheExpirationTime the cacheExpirationTime to set */
    public void setCacheExpirationTime(long cacheExpirationTime) {
        this.cacheExpirationTime = cacheExpirationTime;
    }

    /** @return */
    public long getCacheExpirationTime() {
        if (cacheExpirationTime > 0) {
            return cacheExpirationTime;
        } else {
            return defaultCacheExpirationTime;
        }
    }

    /**
     * @return the authApiKey if set, the rest client will create an "X-AUTH" custom header in order
     *     to send authentication to the backend.
     */
    public String getAuthApiKey() {
        return authApiKey;
    }

    /** @param authApiKey the authApiKey to set */
    public void setAuthApiKey(String authApiKey) {
        this.authApiKey = authApiKey;
    }
}
