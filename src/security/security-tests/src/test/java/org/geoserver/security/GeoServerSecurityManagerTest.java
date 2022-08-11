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

package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Category(SystemTest.class)
public class GeoServerSecurityManagerTest extends GeoServerSecurityTestSupport {

    @Test
    public void testAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        TestingAuthenticationToken auth =
                new TestingAuthenticationToken(
                        "admin", "geoserver", Arrays.asList(GeoServerRole.ADMIN_ROLE));
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));
    }

    @Test
    public void testMasterPasswordForMigration() throws Exception {

        // simulate no user.properties file
        GeoServerSecurityManager secMgr = getSecurityManager();
        char[] generatedPW = secMgr.extractMasterPasswordForMigration(null);
        assertEquals(8, generatedPW.length);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));
        // dumpPWInfoFile();

        Properties props = new Properties();
        String adminUser = "user1";
        String noAdminUser = "user2";

        // check all users with default password
        String defaultMasterePassword = new String(GeoServerSecurityManager.MASTER_PASSWD_DEFAULT);
        props.put(
                GeoServerUser.ADMIN_USERNAME,
                defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(adminUser, defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(noAdminUser, defaultMasterePassword + ",ROLE_WFS");

        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));
        assertFalse(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        assertFalse(masterPWInfoFileContains(adminUser));
        assertFalse(masterPWInfoFileContains(noAdminUser));
        // dumpPWInfoFile();

        // valid master password for noadminuser
        props.put(noAdminUser, "validPassword" + ",ROLE_WFS");
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));

        // password to short  for adminuser
        props.put(adminUser, "abc" + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));

        // valid password for user having admin role

        String validPassword = "validPassword";
        props.put(adminUser, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, new String(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(adminUser));
        // dumpPWInfoFile();

        // valid password for "admin" user
        props.put(GeoServerUser.ADMIN_USERNAME, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, new String(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        // dumpPWInfoFile();

        // assert configuration reload works properly
        secMgr.reload();
    }

    @Test
    public void testMasterPasswordDump() throws Exception {

        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        f.delete();
        try {
            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(
                            "admin", "geoserver", Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertTrue(secMgr.dumpMasterPassword(Files.asResource(f)));
            dumpPWInfoFile(f);
            assertTrue(masterPWInfoFileContains(f, new String(secMgr.getMasterPassword())));
        } finally {
            f.delete();
        }
    }

    @Test
    public void testMasterPasswordDumpNotAuthorized() throws Exception {

        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        try {
            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(
                            "admin", "geoserver", Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));
        } finally {
            f.delete();
        }
    }

    @Test
    public void testMasterPasswordDumpNotOverwrite() throws Exception {

        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write("This should not be overwritten!".getBytes(StandardCharsets.UTF_8));
        }
        try {
            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(
                            "admin", "geoserver", Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));
            dumpPWInfoFile(f);
            assertTrue(masterPWInfoFileContains(f, "This should not be overwritten!"));
            assertFalse(masterPWInfoFileContains(f, new String(secMgr.getMasterPassword())));
        } finally {
            f.delete();
        }
    }

    @SuppressWarnings("PMD.SystemPrintln")
    void dumpPWInfoFile(File infoFile) throws Exception {

        try (BufferedReader bf = new BufferedReader(new FileReader(infoFile))) {
            String line;
            while ((line = bf.readLine()) != null) {
                System.out.println(line);
            }
        }
    }

    void dumpPWInfoFile() throws Exception {
        dumpPWInfoFile(
                new File(
                        getSecurityManager().get("security").dir(),
                        GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME));
    }

    boolean masterPWInfoFileContains(File infoFile, String searchString) throws Exception {

        try (BufferedReader bf = new BufferedReader(new FileReader(infoFile))) {
            String line;
            while ((line = bf.readLine()) != null) {
                if (line.indexOf(searchString) != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean masterPWInfoFileContains(String searchString) throws Exception {
        return masterPWInfoFileContains(
                new File(
                        getSecurityManager().get("security").dir(),
                        GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME),
                searchString);
    }

    @Test
    public void testWebLoginChainSessionCreation() throws Exception {
        // GEOS-6077
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        RequestFilterChain chain =
                config.getFilterChain()
                        .getRequestChainByName(GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME);
        assertTrue(chain.isAllowSessionCreation());
    }

    @Test
    public void testGeoServerEnvParametrization() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();
        String oldRoleServiceName = config.getRoleServiceName();

        try {
            if (GeoServerEnvironment.allowEnvParametrization()) {
                System.setProperty("TEST_SYS_PROPERTY", oldRoleServiceName);

                config.setRoleServiceName("${TEST_SYS_PROPERTY}");
                secMgr.saveSecurityConfig(config);

                SecurityManagerConfig config1 = secMgr.loadSecurityConfig();
                assertEquals(config1.getRoleServiceName(), oldRoleServiceName);
            }
        } finally {
            config.setRoleServiceName(oldRoleServiceName);
            secMgr.saveSecurityConfig(config);
            System.clearProperty("TEST_SYS_PROPERTY");
        }
    }
}
