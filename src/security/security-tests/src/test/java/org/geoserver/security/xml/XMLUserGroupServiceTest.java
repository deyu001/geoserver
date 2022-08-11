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

package org.geoserver.security.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.AbstractUserGroupServiceTest;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.Util;
import org.geoserver.security.password.GeoServerMultiplexingPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class XMLUserGroupServiceTest extends AbstractUserGroupServiceTest {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        return createUserGroupService(serviceName, XMLConstants.FILE_UR);
    }

    @Before
    public void clearUserGroupService() throws Exception {
        store.clear();
        store.store();
    }

    @Override
    protected XMLUserGroupServiceConfig createConfigObject(String name) {
        XMLUserGroupServiceConfig config = new XMLUserGroupServiceConfig();
        config.setName(name);
        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        config.setClassName(XMLUserGroupService.class.getName());
        config.setCheckInterval(1000);
        config.setFileName("users.xml");
        config.setValidating(true);
        config.setPasswordEncoderName(getPlainTextPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        return config;
    }

    protected GeoServerUserGroupService createUserGroupService(
            String serviceName, String xmlFileName) throws Exception {
        XMLUserGroupServiceConfig ugConfig =
                (XMLUserGroupServiceConfig)
                        getSecurityManager().loadUserGroupServiceConfig(serviceName);

        if (ugConfig == null) {
            ugConfig = createConfigObject(serviceName);
            ugConfig.setName(serviceName);
        }

        ugConfig.setClassName(XMLUserGroupService.class.getName());
        ugConfig.setCheckInterval(1000);
        ugConfig.setFileName(xmlFileName);
        ugConfig.setValidating(true);
        ugConfig.setPasswordEncoderName(getDigestPasswordEncoder().getName());
        ugConfig.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        getSecurityManager().saveUserGroupService(ugConfig /*,isNewUGService(serviceName)*/);

        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(serviceName);
        service.initializeFromConfig(ugConfig); // create files
        return service;
    }

    @Test
    public void testCopyFrom() throws Exception {
        GeoServerUserGroupService service1 = createUserGroupService("copyFrom");
        GeoServerUserGroupService service2 = createUserGroupService("copyTo");
        GeoServerUserGroupStore store1 = createStore(service1);
        GeoServerUserGroupStore store2 = createStore(service2);

        store1.clear();
        checkEmpty(store1);
        insertValues(store1);

        Util.copyFrom(store1, store2);
        store1.clear();
        checkEmpty(store1);

        checkValuesInserted(store2);
    }

    @Test
    public void testDefault() throws Exception {
        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        assertEquals(1, service.getUsers().size());
        assertEquals(1, service.getUserCount());
        assertEquals(0, service.getUserGroups().size());
        assertEquals(0, service.getGroupCount());

        GeoServerUser admin = service.getUserByUsername(GeoServerUser.ADMIN_USERNAME);
        assertNotNull(admin);
        assertEquals(GeoServerUser.AdminEnabled, admin.isEnabled());

        GeoServerMultiplexingPasswordEncoder enc = getEncoder(service);
        assertTrue(
                enc.isPasswordValid(admin.getPassword(), GeoServerUser.DEFAULT_ADMIN_PASSWD, null));
        assertEquals(admin.getProperties().size(), 0);

        assertEquals(0, service.getGroupsForUser(admin).size());
    }

    @Test
    public void testLocking() throws Exception {
        File xmlFile = File.createTempFile("users", ".xml");
        try {
            FileUtils.copyURLToFile(getClass().getResource("usersTemplate.xml"), xmlFile);
            GeoServerUserGroupService service1 =
                    createUserGroupService("locking1", xmlFile.getCanonicalPath());
            GeoServerUserGroupService service2 =
                    createUserGroupService("locking2", xmlFile.getCanonicalPath());
            GeoServerUserGroupStore store1 = createStore(service1);
            GeoServerUserGroupStore store2 = createStore(service2);

            GeoServerUser user = store1.createUserObject("user", "ps", true);
            GeoServerUserGroup group = store2.createGroupObject("group", true);

            // obtain a lock
            store1.addUser(user);
            boolean fail;
            String failMessage = "Concurrent lock not allowed";
            fail = true;
            try {
                store2.clear();
            } catch (IOException ex) {
                fail = false;
            }
            if (fail) Assert.fail(failMessage);

            // release lock
            store1.load();
            // get lock
            store2.addUser(user);

            fail = true;
            try {
                store1.clear();
            } catch (IOException ex) {
                fail = false;
            }
            if (fail) Assert.fail(failMessage);

            // release lock
            store2.store();
            store1.clear();
            store1.store();

            // // end of part one, now check all modifying methods

            // obtain a lock
            store1.addUser(user);

            fail = true;
            try {
                store2.associateUserToGroup(user, group);
            } catch (IOException ex) {
                try {
                    store2.disAssociateUserFromGroup(user, group);
                } catch (IOException e) {
                    fail = false;
                }
            }
            if (fail) Assert.fail(failMessage);

            fail = true;
            try {
                store2.updateUser(user);
            } catch (IOException ex) {
                try {
                    store2.removeUser(user);
                } catch (IOException ex1) {
                    try {
                        store2.addUser(user);
                    } catch (IOException ex2) {
                        fail = false;
                    }
                }
            }
            if (fail) Assert.fail(failMessage);

            fail = true;
            try {
                store2.updateGroup(group);
            } catch (IOException ex) {
                try {
                    store2.removeGroup(group);
                } catch (IOException ex1) {
                    try {
                        store2.addGroup(group);
                    } catch (IOException ex2) {
                        fail = false;
                    }
                }
            }
            if (fail) Assert.fail(failMessage);

            fail = true;
            try {
                store2.clear();
            } catch (IOException ex) {
                try {
                    store2.store();
                } catch (IOException e) {
                    fail = false;
                }
            }
            if (fail) Assert.fail(failMessage);
        } finally {
            xmlFile.delete();
        }
    }

    @Test
    public void testDynamicReload() throws Exception {
        File xmlFile = File.createTempFile("users", ".xml");
        try {
            FileUtils.copyURLToFile(getClass().getResource("usersTemplate.xml"), xmlFile);
            GeoServerUserGroupService service1 =
                    createUserGroupService("reload1", xmlFile.getCanonicalPath());
            GeoServerUserGroupService service2 =
                    createUserGroupService("reload2", xmlFile.getCanonicalPath());

            GeoServerUserGroupStore store1 = createStore(service1);

            GeoServerUserGroup group = store1.createGroupObject("group", true);

            checkEmpty(service1);
            checkEmpty(service2);

            // prepare for syncing

            UserGroupLoadedListener listener =
                    new UserGroupLoadedListener() {

                        @Override
                        public void usersAndGroupsChanged(UserGroupLoadedEvent event) {
                            synchronized (this) {
                                this.notifyAll();
                            }
                        }
                    };

            service2.registerUserGroupLoadedListener(listener);

            // modifiy store1
            store1.addGroup(group);
            store1.store();
            assertEquals(1, service1.getUserGroups().size());
            assertEquals(1, service1.getGroupCount());

            // increment lastmodified adding a second manually, the test is too fast
            xmlFile.setLastModified(xmlFile.lastModified() + 2000);

            // wait for the listener to unlock when
            // service 2 triggers a load event
            synchronized (listener) {
                listener.wait();
            }

            // here comes the magic !!!
            assertEquals(1, service2.getUserGroups().size());
            assertEquals(1, service2.getGroupCount());
        } finally {
            xmlFile.delete();
        }
    }
}
