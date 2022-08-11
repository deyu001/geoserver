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

package org.geoserver.security.jdbc;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASSNAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASS_NOT_FOUND_$1;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JNDINAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.USERNAME_REQUIRED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidatorTest;
import org.geotools.util.logging.Logging;
import org.junit.Test;

public class JdbcSecurityConfigValidatorTest extends SecurityConfigValidatorTest {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    @Override
    protected SecurityUserGroupServiceConfig createUGConfig(
            String name, Class<?> aClass, String encoder, String policyName) {
        JDBCUserGroupServiceConfig config = new JDBCUserGroupServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setPasswordEncoderName(encoder);
        config.setPasswordPolicyName(policyName);
        config.setCreatingTables(false);
        return config;
    }

    @Override
    protected SecurityRoleServiceConfig createRoleConfig(
            String name, Class<?> aClass, String adminRole) {
        JDBCRoleServiceConfig config = new JDBCRoleServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setAdminRoleName(adminRole);
        config.setCreatingTables(false);
        return config;
    }

    @Override
    protected SecurityAuthProviderConfig createAuthConfig(
            String name, Class<?> aClass, String userGroupServiceName) {
        JDBCConnectAuthProviderConfig config = new JDBCConnectAuthProviderConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setUserGroupServiceName(userGroupServiceName);
        return config;
    }

    @Override
    @Test
    public void testRoleConfig() throws IOException {

        super.testRoleConfig();

        JDBCRoleServiceConfig config =
                (JDBCRoleServiceConfig)
                        createRoleConfig(
                                "jdbc",
                                JDBCRoleService.class,
                                AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);

        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        JDBCRoleServiceConfig configJNDI =
                (JDBCRoleServiceConfig)
                        createRoleConfig(
                                "jndi",
                                JDBCRoleService.class,
                                AbstractRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
        configJNDI.setJndi(true);
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        JdbcSecurityConfigValidator validator =
                new JdbcSecurityConfigValidator(getSecurityManager());
        try {
            configJNDI.setJndiName("");
            validator.validateAddRoleService(configJNDI);
            // getSecurityManager().saveRoleService(configJNDI);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JNDINAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        try {
            config.setDriverClassName("");
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASSNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setDriverClassName("a.b.c");
        try {
            config.setUserName("");
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(USERNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserName("user");
        try {
            config.setConnectURL(null);
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setConnectURL("jdbc:connect");
        try {
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASS_NOT_FOUND_$1, ex.getId());
            assertEquals("a.b.c", ex.getArgs()[0]);
        }

        config.setDriverClassName("java.lang.String");
        config.setPropertyFileNameDDL(null);
        try {
            validator.validateAddRoleService(config);
            // getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }

        GeoServerSecurityManager secMgr = createNiceMock(GeoServerSecurityManager.class);
        expect(secMgr.listRoleServices())
                .andReturn(new TreeSet<>(Arrays.asList("default", "jdbc")))
                .anyTimes();
        replay(secMgr);
        validator = new JdbcSecurityConfigValidator(secMgr);

        JDBCRoleServiceConfig oldConfig = new JDBCRoleServiceConfig(config);

        config.setPropertyFileNameDML(null);
        try {
            // getSecurityManager().saveRoleService(config);
            validator.validateModifiedRoleService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DML_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCRoleServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            try {
                // getSecurityManager().saveRoleService(config);
                validator.validateModifiedRoleService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DDL_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCRoleServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDML(invalidPath);
            try {
                // getSecurityManager().saveRoleService(config);
                validator.validateModifiedRoleService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DML_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        oldConfig = new JDBCRoleServiceConfig(config);

        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        try {
            // getSecurityManager().saveRoleService(config);
            validator.validateModifiedRoleService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DDL_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
    }

    @Override
    @Test
    public void testUserGroupConfig() throws IOException {

        super.testUserGroupConfig();

        JDBCUserGroupServiceConfig config =
                (JDBCUserGroupServiceConfig)
                        createUGConfig(
                                "jdbc",
                                JDBCUserGroupService.class,
                                getPlainTextPasswordEncoder().getName(),
                                PasswordValidator.DEFAULT_NAME);

        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        JDBCUserGroupServiceConfig configJNDI =
                (JDBCUserGroupServiceConfig)
                        createUGConfig(
                                "jdbc",
                                JDBCUserGroupService.class,
                                getPlainTextPasswordEncoder().getName(),
                                PasswordValidator.DEFAULT_NAME);
        configJNDI.setJndi(true);
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        JdbcSecurityConfigValidator validator =
                new JdbcSecurityConfigValidator(getSecurityManager());
        try {
            configJNDI.setJndiName("");
            // getSecurityManager().saveUserGroupService(configJNDI);
            validator.validateAddUserGroupService(configJNDI);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JNDINAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        try {
            config.setDriverClassName("");
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASSNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setDriverClassName("a.b.c");
        try {
            config.setUserName("");
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(USERNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setUserName("user");
        try {
            config.setConnectURL(null);
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setConnectURL("jdbc:connect");
        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASS_NOT_FOUND_$1, ex.getId());
            assertEquals("a.b.c", ex.getArgs()[0]);
        }

        config.setDriverClassName("java.lang.String");
        config.setPropertyFileNameDDL(null);
        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateAddUserGroupService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }

        GeoServerSecurityManager secMgr = createNiceMock(GeoServerSecurityManager.class);
        expect(secMgr.listUserGroupServices())
                .andReturn(new TreeSet<>(Arrays.asList("default", "jdbc")))
                .anyTimes();

        GeoServerPlainTextPasswordEncoder pwEncoder = getPlainTextPasswordEncoder();
        expect(secMgr.loadPasswordEncoder(pwEncoder.getName())).andReturn(pwEncoder).anyTimes();
        expect(secMgr.listPasswordValidators())
                .andReturn(new TreeSet<>(Arrays.asList(PasswordValidator.DEFAULT_NAME)))
                .anyTimes();
        replay(secMgr);

        validator = new JdbcSecurityConfigValidator(secMgr);

        JDBCUserGroupServiceConfig oldConfig = new JDBCUserGroupServiceConfig(config);

        config.setPropertyFileNameDML(null);
        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateModifiedUserGroupService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DML_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCUserGroupServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            try {
                // getSecurityManager().saveUserGroupService(config);
                validator.validateModifiedUserGroupService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DDL_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir() != null) {
            oldConfig = new JDBCUserGroupServiceConfig(config);
            String invalidPath = "abc" + File.separator + "def.properties";
            config.setPropertyFileNameDML(invalidPath);
            try {
                // getSecurityManager().saveUserGroupService(config);
                validator.validateModifiedUserGroupService(config, oldConfig);
                fail();
            } catch (SecurityConfigException ex) {
                assertEquals(DML_FILE_INVALID, ex.getId());
                assertEquals(invalidPath, ex.getArgs()[0]);
            }
        }

        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);

        try {
            // getSecurityManager().saveUserGroupService(config);
            validator.validateModifiedUserGroupService(config, oldConfig);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DDL_FILE_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
    }

    @Override
    @Test
    public void testAuthenticationProvider() throws IOException {
        super.testAuthenticationProvider();
        JDBCConnectAuthProviderConfig config =
                (JDBCConnectAuthProviderConfig)
                        createAuthConfig("jdbcprov", JDBCConnectAuthProvider.class, "default");

        config.setConnectURL("jdbc:connect");

        JdbcSecurityConfigValidator validator =
                new JdbcSecurityConfigValidator(getSecurityManager());

        try {
            config.setDriverClassName("");
            // getSecurityManager().saveAuthenticationProvider(config);
            validator.validateAddAuthProvider(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASSNAME_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }

        config.setDriverClassName("a.b.c");
        try {
            // getSecurityManager().saveAuthenticationProvider(config);
            validator.validateAddAuthProvider(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(DRIVER_CLASS_NOT_FOUND_$1, ex.getId());
            assertEquals("a.b.c", ex.getArgs()[0]);
        }

        try {
            config.setConnectURL(null);
            // getSecurityManager().saveAuthenticationProvider(config);
            validator.validateAddAuthProvider(config);
            fail();
        } catch (SecurityConfigException ex) {
            assertEquals(JDBCURL_REQUIRED, ex.getId());
            assertEquals(0, ex.getArgs().length);
        }
    }
}
