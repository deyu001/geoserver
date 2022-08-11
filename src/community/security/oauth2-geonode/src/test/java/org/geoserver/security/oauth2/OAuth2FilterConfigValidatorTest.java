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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *     <p>Validates {@link OAuth2FilterConfig} objects.
 */
public class OAuth2FilterConfigValidatorTest extends GeoServerMockTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    OAuth2FilterConfigValidator validator;

    @Before
    public void setValidator() {
        validator = new OAuth2FilterConfigValidator(getSecurityManager());
    }

    @Test
    public void testOAuth2FilterConfigValidation() throws Exception {
        GeoNodeOAuth2FilterConfig config = new GeoNodeOAuth2FilterConfig();
        config.setClassName(GeoServerOAuthAuthenticationFilter.class.getName());
        config.setName("testOAuth2");

        check(config);
        validator.validateOAuth2FilterConfig(config);
    }

    public void check(GeoNodeOAuth2FilterConfig config) throws Exception {

        boolean failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, ex.getId());
            // assertEquals(FilterConfigException.ROLE_SOURCE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setRoleSource(PreAuthenticatedUserNameRoleSource.UserGroupService);
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, ex.getId());
            // assertEquals(FilterConfigException.USER_GROUP_SERVICE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setUserGroupServiceName("blabla");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (FilterConfigException ex) {
            // assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());

            failed = true;
        }
        assertTrue(failed);

        config.setRoleConverterName(null);

        config.setCheckTokenEndpointUrl(null);

        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(
                    OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setCheckTokenEndpointUrl("http://localhost/callback");

        config.setAccessTokenUri("blabal");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setAccessTokenUri("http://localhost/callback");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);

        config.setAccessTokenUri("https://localhost/callback");

        config.setUserAuthorizationUri("blabal");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(OAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setUserAuthorizationUri("https://oauth2server/case");

        config.setLogoutUri("blbla");
        failed = false;
        try {
            validator.validateOAuth2FilterConfig(config);
        } catch (OAuth2FilterConfigException ex) {
            assertEquals(
                    OAuth2FilterConfigException.OAUTH2_URL_IN_LOGOUT_URI_MALFORMED, ex.getId());
            assertEquals(0, ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            failed = true;
        }
        assertTrue(failed);
        config.setLogoutUri("http://localhost/gesoerver");

        config.setCliendId("oauth2clientid");
        config.setClientSecret("oauth2clientsecret");
        config.setScopes("email,profile");

        validator.validateOAuth2FilterConfig(config);
    }
}
