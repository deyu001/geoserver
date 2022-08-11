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

import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.ows.URLMangler;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * A URL Mangler checking for a "BEARER" type OAuth2 Access Token into the OAuth2 Security Context
 * and injecting it on the OWS URLs.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2AccessTokenURLMangler implements URLMangler {

    public static final String ALLOW_OAUTH2_URL_MANGLER = "ALLOW_OAUTH2_URL_MANGLER";

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(OAuth2AccessTokenURLMangler.class);

    GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration;

    OAuth2RestTemplate geoServerOauth2RestTemplate;

    private ApplicationContext context;

    public OAuth2AccessTokenURLMangler(
            GeoServerSecurityManager securityManager,
            String oauth2SecurityConfiguration,
            String geoServerOauth2RestTemplate) {

        assert securityManager != null;

        context = securityManager.getApplicationContext();

        assert context != null;

        this.oauth2SecurityConfiguration =
                (GeoServerOAuth2SecurityConfiguration) context.getBean(oauth2SecurityConfiguration);
        this.geoServerOauth2RestTemplate =
                (OAuth2RestTemplate) context.getBean(geoServerOauth2RestTemplate);
    }

    public OAuth2AccessTokenURLMangler(
            GeoServerSecurityManager securityManager,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestTemplate geoServerOauth2RestTemplate) {

        assert securityManager != null;

        context = securityManager.getApplicationContext();

        assert context != null;

        this.oauth2SecurityConfiguration = oauth2SecurityConfiguration;
        this.geoServerOauth2RestTemplate = geoServerOauth2RestTemplate;
    }

    /** @return the context */
    public ApplicationContext getContext() {
        return context;
    }

    /** @param context the context to set */
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        Boolean OAUTH2_URL_MANGLER_ENABLED =
                Boolean.valueOf(System.getProperty(ALLOW_OAUTH2_URL_MANGLER, "false"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            OAuth2AccessToken token =
                    geoServerOauth2RestTemplate.getOAuth2ClientContext().getAccessToken();
            if (OAUTH2_URL_MANGLER_ENABLED
                    && authentication != null
                    && authentication.isAuthenticated()
                    && token != null
                    && token.getTokenType().equalsIgnoreCase(OAuth2AccessToken.BEARER_TYPE)) {
                kvp.put("access_token", token.getValue());
            }
        } catch (Exception e) {
            // We are outside the session scope
            LOGGER.warning(e.getMessage());
        }
    }
}
