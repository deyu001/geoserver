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

package org.geoserver.security.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig.J2EERoleSource;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerDigestAuthenticationFilter;
import org.geoserver.security.filter.GeoServerExceptionTranslationFilter;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;
import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.filter.GeoServerSecurityInterceptorFilter;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Test;

public class FilterConfigValidatorTest extends GeoServerMockTestSupport {

    @Test
    public void testDigestConfigValidation() throws Exception {
        DigestAuthenticationFilterConfig config = new DigestAuthenticationFilterConfig();
        config.setClassName(GeoServerDigestAuthenticationFilter.class.getName());
        config.setName("testDigest");

        GeoServerSecurityManager secMgr = getSecurityManager();

        FilterConfigValidator validator = new FilterConfigValidator(secMgr);

        try {
            validator.validateFilterConfig(config);
            fail("no user group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.USER_GROUP_SERVICE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserGroupServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown user group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setUserGroupServiceName(XMLUserGroupService.DEFAULT_NAME);
        config.setNonceValiditySeconds(-1);

        try {
            validator.validateFilterConfig(config);
            fail("invalid nonce should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.INVALID_SECONDS, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setNonceValiditySeconds(100);
        validator.validateFilterConfig(config);
    }

    @Test
    public void testRoleFilterConfigValidation() throws Exception {
        RoleFilterConfig config = new RoleFilterConfig();
        config.setClassName(GeoServerRoleFilter.class.getName());
        config.setName("testRoleFilter");

        GeoServerSecurityManager secMgr = getSecurityManager();
        FilterConfigValidator validator = new FilterConfigValidator(secMgr);
        try {
            validator.validateFilterConfig(config);
            fail("no header attribute should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.HEADER_ATTRIBUTE_NAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
        config.setHttpResponseHeaderAttrForIncludedRoles("roles");
        config.setRoleConverterName("unknown");

        try {
            validator.validateFilterConfig(config);
            fail("unkonwn role converter should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_CONVERTER, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }

        config.setRoleConverterName(null);
        validator.validateFilterConfig(config);
    }

    @Test
    public void testSecurityInterceptorFilterConfigValidation() throws Exception {
        SecurityInterceptorFilterConfig config = new SecurityInterceptorFilterConfig();
        config.setClassName(GeoServerSecurityInterceptorFilter.class.getName());
        config.setName("testInterceptFilter");

        GeoServerSecurityManager secMgr = getSecurityManager();
        FilterConfigValidator validator = new FilterConfigValidator(secMgr);
        try {
            validator.validateFilterConfig(config);
            fail("no metadata source should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.SECURITY_METADATA_SOURCE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setSecurityMetadataSource("unknown");
        try {
            validator.validateFilterConfig(config);
            fail("unknown metadata source should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_SECURITY_METADATA_SOURCE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }
    }

    @Test
    public void testX509FilterConfigValidation() throws Exception {
        X509CertificateAuthenticationFilterConfig config =
                new X509CertificateAuthenticationFilterConfig();
        config.setClassName(GeoServerX509CertificateAuthenticationFilter.class.getName());
        config.setName("testX509");

        check(config);
    }

    @Test
    public void testUsernamePasswordFilterConfigValidation() throws Exception {
        UsernamePasswordAuthenticationFilterConfig config =
                new UsernamePasswordAuthenticationFilterConfig();
        config.setClassName(GeoServerUserNamePasswordAuthenticationFilter.class.getName());
        config.setName("testUsernamePassword");

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        try {
            validator.validateFilterConfig(config);
            fail("no user should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.USER_PARAMETER_NAME_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUsernameParameterName("user");
        try {
            validator.validateFilterConfig(config);
            fail("no password should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.PASSWORD_PARAMETER_NAME_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPasswordParameterName("password");
        validator.validateFilterConfig(config);
    }

    @Test
    public void testJ2eeFilterConfigValidation() throws Exception {
        J2eeAuthenticationFilterConfig config = new J2eeAuthenticationFilterConfig();
        config.setClassName(GeoServerJ2eeAuthenticationFilter.class.getName());
        config.setName("testJ2ee");

        check(config);
    }

    @Test
    public void testExceptionTranslationFilterConfigValidation() throws Exception {
        ExceptionTranslationFilterConfig config = new ExceptionTranslationFilterConfig();
        config.setClassName(GeoServerExceptionTranslationFilter.class.getName());
        config.setName("testEx");

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        config.setAuthenticationFilterName("unknown");

        try {
            validator.validateFilterConfig(config);
            fail("invalid entry point should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.INVALID_ENTRY_POINT, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }

        config.setAuthenticationFilterName(
                GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);

        try {
            validator.validateFilterConfig(config);
            fail("no auth entry point should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.NO_AUTH_ENTRY_POINT, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals(GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR, ex.getArgs()[0]);
        }

        config.setAuthenticationFilterName(null);
        validator.validateFilterConfig(config);
    }

    public void check(PreAuthenticatedUserNameFilterConfig config) throws Exception {

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        try {
            validator.validateFilterConfig(config);
            fail("no role source should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.ROLE_SOURCE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource
                        .UserGroupService);
        try {
            validator.validateFilterConfig(config);
            fail("no user group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.USER_GROUP_SERVICE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserGroupServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown group service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_USER_GROUP_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setUserGroupServiceName(XMLUserGroupService.DEFAULT_NAME);

        config.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource
                        .RoleService);
        config.setRoleServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown role service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setRoleServiceName(XMLRoleService.DEFAULT_NAME);
        config.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header);

        try {
            validator.validateFilterConfig(config);
            fail("no roles header attribute should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.ROLES_HEADER_ATTRIBUTE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setRolesHeaderAttribute("roles");
        config.setRoleConverterName("unknown");

        try {
            validator.validateFilterConfig(config);
            fail("unknown role converter should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_CONVERTER, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("unknown", ex.getArgs()[0]);
        }

        config.setRoleConverterName(null);
        validator.validateFilterConfig(config);
    }

    public void check(J2eeAuthenticationBaseFilterConfig config) throws Exception {
        check((PreAuthenticatedUserNameFilterConfig) config);
        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());

        config.setRoleSource(J2EERoleSource.J2EE);
        config.setRoleServiceName("blabla");
        try {
            validator.validateFilterConfig(config);
            fail("unknown role service should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.UNKNOWN_ROLE_SERVICE, ex.getId());
            assertEquals(1, ex.getArgs().length);
            assertEquals("blabla", ex.getArgs()[0]);
        }

        config.setRoleServiceName(XMLRoleService.DEFAULT_NAME);
    }

    @Test
    public void testRequestHeaderFilterConfigValidation() throws Exception {
        RequestHeaderAuthenticationFilterConfig config =
                new RequestHeaderAuthenticationFilterConfig();
        config.setClassName(GeoServerRequestHeaderAuthenticationFilter.class.getName());
        config.setName("testRequestHeader");

        FilterConfigValidator validator = new FilterConfigValidator(getSecurityManager());
        try {
            validator.validateFilterConfig(config);
            fail("no principal header attribute should fail");
        } catch (FilterConfigException ex) {
            assertEquals(FilterConfigException.PRINCIPAL_HEADER_ATTRIBUTE_NEEDED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPrincipalHeaderAttribute("user");
        check(config);
    }
}
