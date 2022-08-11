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

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

/**
 * GitHub specific REST remplates for OAuth2 protocol.
 *
 * <p>GitHub Authorization APIs available at
 * <strong>https://developer.github.com/v3/#authentication</strong> and
 * <strong>https://developer.github.com/v3/oauth/</strong>
 *
 * <p>First of all the user must create an API key through the GitHub API Credentials <br>
 * See: <string>https://github.com/settings/applications/new</strong>
 *
 * <p>The procedure will provide a new <b>Client ID</b> and <b>Client Secret</b>
 *
 * <p>Also the user must specify the <b>Authorization callback URL</b> pointing to the GeoServer
 * instances <br>
 * Example:
 *
 * <ul>
 *   <li>http://localhost:8080/geoserver
 * </ul>
 *
 * <p>The GitHub OAuth2 Filter Endpoint will automatically redirect the users to an URL like the
 * following one at first login <br>
 * <br>
 * <code>
 * https://github.com/login/oauth/authorize?response_type=code&client_id=my_client_id&redirect_uri=http://localhost:8080/geoserver&scope=user
 * </code>
 *
 * <p>Tipically a correct configuration for the GitHub OAuth2 Provider is like the following:
 *
 * <ul>
 *   <li>Cliend Id: <b>my_client_id</b>
 *   <li>Cliend Secret: <b>my_client_secret</b>
 *   <li>Access Token URI: <b>https://github.com/login/oauth/access_token</b>
 *   <li>User Authorization URI: <b>https://github.com/login/oauth/authorize</b>
 *   <li>Redirect URI: <b>http://localhost:8080/geoserver</b>
 *   <li>Check Token Endpoint URL: <b>https://api.github.com/user</b>
 *   <li>Logout URI: <b>https://github.com/logout</b>
 *   <li>Scopes: <b>user</b>
 * </ul>
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
@Configuration(value = "githubOAuth2SecurityConfiguration")
@EnableOAuth2Client
class GitHubOAuth2SecurityConfiguration extends GeoServerOAuth2SecurityConfiguration {

    @Bean(name = "githubOAuth2Resource")
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        return super.geoServerOAuth2Resource();
    }

    @Override
    protected String getDetailsId() {
        return "github-oauth2-client";
    }

    /** Must have "session" scope */
    @Bean(name = "githubOauth2RestTemplate")
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {
        OAuth2RestTemplate template = super.geoServerOauth2RestTemplate();
        List<HttpMessageConverter<?>> messageConverters = template.getMessageConverters();
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        return template;
    }
}
