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

import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DDL_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_INVALID;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DML_FILE_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASSNAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.DRIVER_CLASS_NOT_FOUND_$1;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JDBCURL_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.JNDINAME_REQUIRED;
import static org.geoserver.security.jdbc.JDBCSecurityConfigException.USERNAME_REQUIRED;

import java.io.File;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;

public class JdbcSecurityConfigValidator extends SecurityConfigValidator {

    public JdbcSecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validate(SecurityRoleServiceConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;

        validateFileNames(
                jdbcConfig, JDBCRoleService.DEFAULT_DDL_FILE, JDBCRoleService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);

        if (jdbcConfig.isJndi()) validateJNDI(jdbcConfig);
        else validateJDBC(jdbcConfig);
    }

    @Override
    public void validate(SecurityUserGroupServiceConfig config) throws SecurityConfigException {
        super.validate(config);

        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;

        validateFileNames(
                jdbcConfig,
                JDBCUserGroupService.DEFAULT_DDL_FILE,
                JDBCUserGroupService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);

        if (jdbcConfig.isJndi()) validateJNDI(jdbcConfig);
        else validateJDBC(jdbcConfig);
    }

    protected void checkAutomaticTableCreation(JDBCSecurityServiceConfig config)
            throws SecurityConfigException {
        if (config.isCreatingTables()) {
            if (isNotEmpty(config.getPropertyFileNameDDL()) == false)
                throw createSecurityException(DDL_FILE_REQUIRED);
        }
    }

    protected void validateFileNames(
            JDBCSecurityServiceConfig config, String defaultDDL, String defaultDML)
            throws SecurityConfigException {

        String fileName = config.getPropertyFileNameDDL();
        // ddl may be null
        if (isNotEmpty(fileName)) {
            if (defaultDDL.equals(fileName) == false) {
                // not the default property file
                File file = new File(fileName);
                if (checkFile(file) == false) {
                    throw createSecurityException(DDL_FILE_INVALID, fileName);
                }
            }
        }

        fileName = config.getPropertyFileNameDML();
        if (isNotEmpty(fileName) == false) {
            // dml file is required
            throw createSecurityException(DML_FILE_REQUIRED);
        }

        if (defaultDML.equals(fileName) == false) {
            // not the default property file
            File file = new File(fileName);
            if (checkFile(file) == false) {
                throw createSecurityException(DML_FILE_INVALID, fileName);
            }
        }
    }

    protected void validateJNDI(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getJndiName()) == false)
            throw createSecurityException(JNDINAME_REQUIRED);
    }

    protected void validateJDBC(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getDriverClassName()) == false)
            throw createSecurityException(DRIVER_CLASSNAME_REQUIRED);
        if (isNotEmpty(config.getUserName()) == false)
            throw createSecurityException(USERNAME_REQUIRED);
        if (isNotEmpty(config.getConnectURL()) == false)
            throw createSecurityException(JDBCURL_REQUIRED);

        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(DRIVER_CLASS_NOT_FOUND_$1, config.getDriverClassName());
        }
    }

    @Override
    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCConnectAuthProviderConfig jdbcConfig = (JDBCConnectAuthProviderConfig) config;
        if (isNotEmpty(jdbcConfig.getDriverClassName()) == false)
            throw createSecurityException(DRIVER_CLASSNAME_REQUIRED);
        if (isNotEmpty(jdbcConfig.getConnectURL()) == false)
            throw createSecurityException(JDBCURL_REQUIRED);

        try {
            Class.forName(jdbcConfig.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(
                    DRIVER_CLASS_NOT_FOUND_$1, jdbcConfig.getDriverClassName());
        }
    }
}
