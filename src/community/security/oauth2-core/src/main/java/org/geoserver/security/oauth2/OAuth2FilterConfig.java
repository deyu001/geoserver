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

package org.geoserver.security.oauth2;

import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * The GeoServer OAuth2 Filter Configuration. This POJO contains the properties needed to correctly
 * configure the Spring Auth Filter.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public interface OAuth2FilterConfig {

    /** @return the cliendId */
    public String getCliendId();

    /** @param cliendId the cliendId to set */
    public void setCliendId(String cliendId);

    /** @return the clientSecret */
    public String getClientSecret();

    /** @param clientSecret the clientSecret to set */
    public void setClientSecret(String clientSecret);

    /** @return */
    public Boolean getForceAccessTokenUriHttps();

    /** @param forceAccessTokenUriHttps */
    public void setForceAccessTokenUriHttps(Boolean forceAccessTokenUriHttps);

    /** @return the accessTokenUri */
    public String getAccessTokenUri();

    /** @param accessTokenUri the accessTokenUri to set */
    public void setAccessTokenUri(String accessTokenUri);

    /** @return */
    public Boolean getForceUserAuthorizationUriHttps();

    /** @param forceAccessTokenUriHttps */
    public void setForceUserAuthorizationUriHttps(Boolean forceUserAuthorizationUriHttps);

    /** @return the userAuthorizationUri */
    public String getUserAuthorizationUri();

    /** @param userAuthorizationUri the userAuthorizationUri to set */
    public void setUserAuthorizationUri(String userAuthorizationUri);

    /** @return the redirectUri */
    public String getRedirectUri();

    /** @param redirectUri the redirectUri to set */
    public void setRedirectUri(String redirectUri);

    /** @return the checkTokenEndpointUrl */
    public String getCheckTokenEndpointUrl();

    /** @param checkTokenEndpointUrl the checkTokenEndpointUrl to set */
    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl);

    /** @return the logoutUri */
    public String getLogoutUri();

    /** @param logoutUri the logoutUri to set */
    public void setLogoutUri(String logoutUri);

    /** @return the scopes */
    public String getScopes();

    /** @param scopes the scopes to set */
    public void setScopes(String scopes);

    /** **THIS MUST** be different for every OAuth2 Plugin */
    public String getLoginEndpoint();

    /** **THIS MUST** be different for every OAuth2 Plugin */
    public String getLogoutEndpoint();

    /** @param loginEndpoint */
    public void setLoginEndpoint(String loginEndpoint);

    /** @param logoutEndpoint */
    public void setLogoutEndpoint(String logoutEndpoint);

    /** @return the enableRedirectAuthenticationEntryPoint */
    public Boolean getEnableRedirectAuthenticationEntryPoint();

    /**
     * @param enableRedirectAuthenticationEntryPoint the enableRedirectAuthenticationEntryPoint to
     *     set
     */
    public void setEnableRedirectAuthenticationEntryPoint(
            Boolean enableRedirectAuthenticationEntryPoint);

    /**
     * Returns filter {@link AuthenticationEntryPoint} actual implementation
     *
     * @return {@link AuthenticationEntryPoint}
     */
    public AuthenticationEntryPoint getAuthenticationEntryPoint();
}
