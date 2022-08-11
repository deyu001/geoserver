/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerOAuth2FilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig, OAuth2FilterConfig {

    private static final long serialVersionUID = -8581346584859849804L;

    protected String cliendId;

    protected String clientSecret;

    protected String accessTokenUri;

    protected String userAuthorizationUri;

    protected String redirectUri = "http://localhost:8080/geoserver";

    protected String checkTokenEndpointUrl;

    protected String logoutUri;

    protected String scopes;

    protected Boolean enableRedirectAuthenticationEntryPoint;

    protected Boolean forceAccessTokenUriHttps;

    protected Boolean forceUserAuthorizationUriHttps;

    protected String loginEndpoint;

    protected String logoutEndpoint;

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    /** @return the cliendId */
    public String getCliendId() {
        return cliendId;
    }

    /** @param cliendId the cliendId to set */
    public void setCliendId(String cliendId) {
        this.cliendId = cliendId;
    }

    /** @return the clientSecret */
    public String getClientSecret() {
        return clientSecret;
    }

    /** @param clientSecret the clientSecret to set */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /** @return the accessTokenUri */
    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    /** @param accessTokenUri the accessTokenUri to set */
    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    /** @return the userAuthorizationUri */
    public String getUserAuthorizationUri() {
        return userAuthorizationUri;
    }

    /** @param userAuthorizationUri the userAuthorizationUri to set */
    public void setUserAuthorizationUri(String userAuthorizationUri) {
        this.userAuthorizationUri = userAuthorizationUri;
    }

    /** @return the redirectUri */
    public String getRedirectUri() {
        return redirectUri;
    }

    /** @param redirectUri the redirectUri to set */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /** @return the checkTokenEndpointUrl */
    public String getCheckTokenEndpointUrl() {
        return checkTokenEndpointUrl;
    }

    /** @param checkTokenEndpointUrl the checkTokenEndpointUrl to set */
    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl) {
        this.checkTokenEndpointUrl = checkTokenEndpointUrl;
    }

    /** @return the logoutUri */
    public String getLogoutUri() {
        return logoutUri;
    }

    /** @param logoutUri the logoutUri to set */
    public void setLogoutUri(String logoutUri) {
        this.logoutUri = logoutUri;
    }

    /** @return the scopes */
    public String getScopes() {
        return scopes;
    }

    /** @param scopes the scopes to set */
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    /** @return the enableRedirectAuthenticationEntryPoint */
    public Boolean getEnableRedirectAuthenticationEntryPoint() {
        return enableRedirectAuthenticationEntryPoint;
    }

    /**
     * @param enableRedirectAuthenticationEntryPoint the enableRedirectAuthenticationEntryPoint to
     *     set
     */
    public void setEnableRedirectAuthenticationEntryPoint(
            Boolean enableRedirectAuthenticationEntryPoint) {
        this.enableRedirectAuthenticationEntryPoint = enableRedirectAuthenticationEntryPoint;
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new AuthenticationEntryPoint() {

            @Override
            public void commence(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    AuthenticationException authException)
                    throws IOException, ServletException {
                final StringBuilder loginUri = new StringBuilder(getUserAuthorizationUri());
                loginUri.append("?")
                        .append("response_type=code")
                        .append("&")
                        .append("client_id=")
                        .append(getCliendId())
                        .append("&")
                        .append("scope=")
                        .append(getScopes().replace(",", "%20"))
                        .append("&")
                        .append("redirect_uri=")
                        .append(getRedirectUri());

                if (getEnableRedirectAuthenticationEntryPoint()
                        || request.getRequestURI().endsWith(getLoginEndpoint())) {
                    response.sendRedirect(loginUri.toString());
                }
            }
        };
    }

    @Override
    public Boolean getForceAccessTokenUriHttps() {
        return forceAccessTokenUriHttps;
    }

    @Override
    public void setForceAccessTokenUriHttps(Boolean forceAccessTokenUriHttps) {
        this.forceAccessTokenUriHttps = forceAccessTokenUriHttps;
    }

    @Override
    public Boolean getForceUserAuthorizationUriHttps() {
        return forceUserAuthorizationUriHttps;
    }

    @Override
    public void setForceUserAuthorizationUriHttps(Boolean forceUserAuthorizationUriHttps) {
        this.forceUserAuthorizationUriHttps = forceUserAuthorizationUriHttps;
    }

    @Override
    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    @Override
    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    @Override
    public void setLoginEndpoint(String loginEndpoint) {
        this.loginEndpoint = loginEndpoint;
    }

    @Override
    public void setLogoutEndpoint(String logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }
}
