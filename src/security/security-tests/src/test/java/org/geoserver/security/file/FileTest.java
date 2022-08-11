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

package org.geoserver.security.file;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.junit.Assert;
import org.junit.Test;

/** @author christian */
public class FileTest {
    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    int gaCounter = 0, ugCounter = 0;

    GeoServerRoleService gaService =
            new AbstractRoleService() {

                public String getName() {
                    return "TestGAService";
                };

                @Override
                protected void deserialize() throws IOException {
                    gaCounter++;
                }

                @Override
                public void initializeFromConfig(SecurityNamedServiceConfig config)
                        throws IOException {
                    super.initializeFromConfig(config);
                }
            };

    GeoServerUserGroupService ugService =
            new AbstractUserGroupService() {

                public String getName() {
                    return "TestUGService";
                };

                @Override
                protected void deserialize() throws IOException {
                    ugCounter++;
                }

                @Override
                public void initializeFromConfig(SecurityNamedServiceConfig config)
                        throws IOException {}
            };

    @Test
    public void testFileWatcher() throws Exception {
        Files.schedule(100, TimeUnit.MILLISECONDS);
        try {
            File ugFile = File.createTempFile("users", ".xml");
            ugFile.deleteOnExit();
            File gaFile = File.createTempFile("roles", ".xml");
            gaFile.deleteOnExit();

            RoleFileWatcher gaWatcher = new RoleFileWatcher(Files.asResource(gaFile), gaService);
            assertEquals(1, gaCounter);

            gaWatcher.start();

            UserGroupFileWatcher ugWatcher =
                    new UserGroupFileWatcher(Files.asResource(ugFile), ugService);
            assertEquals(1, ugCounter);
            ugWatcher.start();

            LOGGER.info(gaWatcher.toString());
            LOGGER.info(ugWatcher.toString());

            // now, modifiy last access
            ugFile.setLastModified(ugFile.lastModified() + 1000);
            gaFile.setLastModified(gaFile.lastModified() + 1000);

            // Try for two seconds
            int maxTries = 10;
            boolean failed = true;
            for (int i = 0; i < maxTries; i++) {
                if (ugCounter == 2 && gaCounter == 2) {
                    failed = false;
                    break;
                }
                Thread.sleep(100);
            }
            if (failed) {
                Assert.fail("FileWatchers not working");
            }
            ugWatcher.setTerminate(true);
            gaWatcher.setTerminate(true);
            ugFile.delete();
            gaFile.delete();
        } finally {
            Files.schedule(10, TimeUnit.SECONDS);
        }
    }

    //    @Test
    //    @Ignore
    //    public void testLockFile() {
    //
    //        try {
    //            File fileToLock = File.createTempFile("test", ".xml");
    //            fileToLock.deleteOnExit();
    //
    //            LockFile lf1 = new LockFile(fileToLock);
    //            LockFile lf2 = new LockFile(fileToLock);
    //
    //            assertFalse(lf1.hasWriteLock());
    //            assertFalse(lf1.hasForeignWriteLock());
    //
    //            lf2.writeLock();
    //
    //            assertFalse(lf1.hasWriteLock());
    //            assertTrue(lf1.hasForeignWriteLock());
    //            assertTrue(lf2.hasWriteLock());
    //            assertFalse(lf2.hasForeignWriteLock());
    //
    //            lf2.writeUnLock();
    //
    //            assertFalse(lf1.hasWriteLock());
    //            assertFalse(lf1.hasForeignWriteLock());
    //            assertFalse(lf2.hasWriteLock());
    //            assertFalse(lf2.hasForeignWriteLock());
    //
    //            lf2.writeLock();
    //
    //            boolean fail = true;
    //            try {
    //                lf1.writeLock();
    //            } catch (IOException ex) {
    //                fail = false;
    //                LOGGER.info(ex.getMessage());
    //            }
    //            if (fail) {
    //                Assert.fail("IOException not thrown for concurrent write lock" );
    //            }
    //
    //            lf2.writeUnLock();
    //            lf1.writeLock();
    //
    //            assertTrue(lf1.hasWriteLock());
    //            assertFalse(lf1.hasForeignWriteLock());
    //            assertFalse(lf2.hasWriteLock());
    //            assertTrue(lf2.hasForeignWriteLock());
    //
    //            lf1.finalize();
    //
    //            assertFalse(lf1.hasWriteLock());
    //            assertFalse(lf1.hasForeignWriteLock());
    //            assertFalse(lf2.hasWriteLock());
    //            assertFalse(lf2.hasForeignWriteLock());
    //
    //
    //        } catch (Throwable ex) {
    //            Assert.fail(ex.getMessage());
    //        }
    //    }
}
