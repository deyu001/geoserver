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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.security.oauth2.services.GeoNodeTokenServices;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.MultiValueMap;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class OAuth2RestTemplateTest extends AbstractOAuth2RestTemplateTest {

    @Override
    public void open() throws Exception {
        configuration = new GeoNodeOAuth2SecurityConfiguration();
        configuration.setAccessTokenRequest(accessTokenRequest);
        resource = (AuthorizationCodeResourceDetails) configuration.geoServerOAuth2Resource();

        assertNotNull(resource);

        resource.setTokenName("bearer_token");
        restTemplate = configuration.geoServerOauth2RestTemplate();

        assertNotNull(restTemplate);

        request = mock(ClientHttpRequest.class);
        headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        HttpStatus statusCode = HttpStatus.OK;
        when(response.getStatusCode()).thenReturn(statusCode);
        when(request.execute()).thenReturn(response);
    }

    @Test(expected = AccessTokenRequiredException.class)
    public void testAccessDeneiedException() throws Exception {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        authenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
    }

    @Test
    public void testNonBearerToken() throws Exception {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        restTemplate.getOAuth2ClientContext().setAccessToken(token);
        authenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
        String auth = request.getHeaders().getFirst("Authorization");

        assertTrue(auth.startsWith("access_token "));
    }

    @Test
    public void testCustomAuthenticator() throws Exception {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        restTemplate.getOAuth2ClientContext().setAccessToken(token);
        OAuth2RequestAuthenticator customAuthenticator =
                new OAuth2RequestAuthenticator() {

                    @Override
                    public void authenticate(
                            OAuth2ProtectedResourceDetails resource,
                            OAuth2ClientContext clientContext,
                            ClientHttpRequest req) {
                        req.getHeaders()
                                .set(
                                        "X-Authorization",
                                        clientContext.getAccessToken().getTokenType()
                                                + " "
                                                + "Nah-nah-na-nah-nah");
                    }
                };

        customAuthenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
        String auth = request.getHeaders().getFirst("X-Authorization");

        assertEquals("access_token Nah-nah-na-nah-nah", auth);
    }

    @Test
    public void testBearerAccessTokenURLMangler() {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        token.setTokenType(OAuth2AccessToken.BEARER_TYPE);
        restTemplate.getOAuth2ClientContext().setAccessToken(token);
        authenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
        String auth = request.getHeaders().getFirst("Authorization");

        assertTrue(auth.startsWith(OAuth2AccessToken.BEARER_TYPE));

        OAuth2AccessTokenURLMangler urlMangler =
                new OAuth2AccessTokenURLMangler(getSecurityManager(), configuration, restTemplate);
        urlMangler.geoServerOauth2RestTemplate = restTemplate;

        assertNotNull(urlMangler);

        Authentication user =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "geoserver",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")
                                }));
        SecurityContextHolder.getContext().setAuthentication(user);

        StringBuilder baseURL = new StringBuilder("http://test.geoserver-org/wms");
        StringBuilder path = new StringBuilder();
        Map<String, String> kvp = new HashMap<String, String>();
        kvp.put("request", "GetCapabilities");

        urlMangler.mangleURL(baseURL, path, kvp, URLType.SERVICE);

        assertFalse(kvp.containsKey("access_token"));

        try {
            System.setProperty(OAuth2AccessTokenURLMangler.ALLOW_OAUTH2_URL_MANGLER, "true");

            urlMangler.mangleURL(baseURL, path, kvp, URLType.SERVICE);

            assertTrue(kvp.containsKey("access_token"));
            assertTrue("12345".equals(kvp.get("access_token")));
        } finally {
            System.clearProperty(OAuth2AccessTokenURLMangler.ALLOW_OAUTH2_URL_MANGLER);
        }
    }

    @Test
    public void testAccessTokenConverter() throws Exception {
        final String path = "http://foo.url";
        final String accessToken = "access_token Nah-nah-na-nah-nah";

        GeoNodeTokenServices accessTokenServices = new MockGeoNodeTokenServices(accessToken);

        accessTokenServices.checkTokenEndpointUrl = path;
        accessTokenServices.setClientId("1234");
        accessTokenServices.setClientSecret("56789-10");

        OAuth2Authentication auth = accessTokenServices.loadAuthentication(accessToken);
        assertEquals("1234", auth.getOAuth2Request().getClientId());
    }

    class MockGeoNodeTokenServices extends GeoNodeTokenServices {

        final String accessToken;

        public MockGeoNodeTokenServices(String accessToken) {
            this.accessToken = accessToken;
        }

        protected Map<String, Object> postForMap(
                String path, MultiValueMap<String, String> formData, HttpHeaders headers) {

            assertTrue(headers.containsKey("Authorization"));
            assertEquals(getAuthorizationHeader(accessToken), headers.get("Authorization").get(0));

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("client_id", clientId);
            return body;
        }
    }
}
