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

import java.util.Arrays;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.AuthenticationScheme;

/**
 * Base OAuth2 Configuration Class. Each OAuth2 specific Extension must implement its own {@link
 * OAuth2RestTemplate}
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public abstract class GeoServerOAuth2SecurityConfiguration implements OAuth2SecurityConfiguration {

    @Autowired protected Environment env;

    @Resource
    @Qualifier("accessTokenRequest")
    private AccessTokenRequest accessTokenRequest;

    /**
     * Returns the resource bean containing the Access Token Request info.
     *
     * @return the accessTokenRequest
     */
    public AccessTokenRequest getAccessTokenRequest() {
        return accessTokenRequest;
    }

    /**
     * Set the accessTokenRequest property.
     *
     * @param accessTokenRequest the accessTokenRequest to set
     */
    public void setAccessTokenRequest(AccessTokenRequest accessTokenRequest) {
        this.accessTokenRequest = accessTokenRequest;
    }

    /** Details for an OAuth2-protected resource. */
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
        details.setId(getDetailsId());

        details.setGrantType("authorization_code");
        details.setAuthenticationScheme(AuthenticationScheme.header);
        details.setClientAuthenticationScheme(AuthenticationScheme.form);

        return details;
    }

    /** Returns the details id for the AuthorizationCodeResourceDetails. */
    protected String getDetailsId() {
        return "oauth2-client";
    }

    /**
     * Rest template that is able to make OAuth2-authenticated REST requests with the credentials of
     * the provided resource.
     */
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {

        OAuth2RestTemplate oAuth2RestTemplate = getOAuth2RestTemplate();

        AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider =
                new AuthorizationCodeAccessTokenProvider();
        authorizationCodeAccessTokenProvider.setStateMandatory(false);

        AccessTokenProvider accessTokenProviderChain =
                new AccessTokenProviderChain(
                        Arrays.<AccessTokenProvider>asList(
                                authorizationCodeAccessTokenProvider,
                                new ImplicitAccessTokenProvider(),
                                new ResourceOwnerPasswordAccessTokenProvider(),
                                new ClientCredentialsAccessTokenProvider()));

        oAuth2RestTemplate.setAccessTokenProvider(accessTokenProviderChain);

        return oAuth2RestTemplate;
    }

    /**
     * Allows subclasses to return a custom {@link OAuth2RestTemplate} subclass
     *
     * @return
     */
    protected OAuth2RestTemplate getOAuth2RestTemplate() {
        return new OAuth2RestTemplate(
                geoServerOAuth2Resource(), new DefaultOAuth2ClientContext(getAccessTokenRequest()));
    }
}
