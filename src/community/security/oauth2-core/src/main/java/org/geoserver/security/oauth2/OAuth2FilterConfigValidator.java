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

import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.springframework.util.StringUtils;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *     <p>Validates {@link OAuth2FilterConfig} objects.
 */
public class OAuth2FilterConfigValidator extends FilterConfigValidator {

    public OAuth2FilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {

        if (config instanceof OAuth2FilterConfig) {
            validateOAuth2FilterConfig((OAuth2FilterConfig) config);
        } else {
            super.validateFilterConfig(config);
        }
    }

    public void validateOAuth2FilterConfig(OAuth2FilterConfig filterConfig)
            throws FilterConfigException {
        if (StringUtils.hasLength(filterConfig.getLogoutUri())) {
            try {
                new URL(filterConfig.getLogoutUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_URL_IN_LOGOUT_URI_MALFORMED);
            }
        }
        super.validateFilterConfig((SecurityNamedServiceConfig) filterConfig);

        if (StringUtils.hasLength(filterConfig.getCheckTokenEndpointUrl()) == false)
            throw createFilterException(
                    OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED);

        try {
            new URL(filterConfig.getCheckTokenEndpointUrl());
        } catch (MalformedURLException ex) {
            throw createFilterException(
                    OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED);
        }

        if (StringUtils.hasLength(filterConfig.getAccessTokenUri())) {
            URL accessTokenUri = null;
            try {
                accessTokenUri = new URL(filterConfig.getAccessTokenUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED);
            }
            if (filterConfig.getForceAccessTokenUriHttps()
                    && "https".equalsIgnoreCase(accessTokenUri.getProtocol()) == false)
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_NOT_HTTPS);
        }

        if (StringUtils.hasLength(filterConfig.getUserAuthorizationUri())) {
            URL userAuthorizationUri = null;
            try {
                userAuthorizationUri = new URL(filterConfig.getUserAuthorizationUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED);
            }
            if (filterConfig.getForceUserAuthorizationUriHttps()
                    && "https".equalsIgnoreCase(userAuthorizationUri.getProtocol()) == false)
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_USERAUTHURI_NOT_HTTPS);
        }

        if (StringUtils.hasLength(filterConfig.getRedirectUri())) {
            try {
                new URL(filterConfig.getRedirectUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_REDIRECT_URI_MALFORMED);
            }
        }

        if (!StringUtils.hasLength(filterConfig.getCliendId())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED);
        }

        if (!StringUtils.hasLength(filterConfig.getClientSecret())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_CLIENT_SECRET_REQUIRED);
        }

        if (!StringUtils.hasLength(filterConfig.getScopes())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_SCOPE_REQUIRED);
        }
    }

    protected OAuth2FilterConfigException createFilterException(String errorid, Object... args) {
        return new OAuth2FilterConfigException(errorid, args);
    }
}
