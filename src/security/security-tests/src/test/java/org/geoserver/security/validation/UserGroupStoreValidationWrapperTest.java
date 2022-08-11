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

import static org.geoserver.security.validation.UserGroupServiceException.GROUPNAME_REQUIRED;
import static org.geoserver.security.validation.UserGroupServiceException.GROUP_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.UserGroupServiceException.GROUP_NOT_FOUND_$1;
import static org.geoserver.security.validation.UserGroupServiceException.USERNAME_REQUIRED;
import static org.geoserver.security.validation.UserGroupServiceException.USER_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.UserGroupServiceException.USER_NOT_FOUND_$1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.MemoryUserGroupService;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class UserGroupStoreValidationWrapperTest extends GeoServerSecurityTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    protected UserGroupStoreValidationWrapper createStore(String name) throws IOException {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();
        config.setName(name);
        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        GeoServerUserGroupService service = new MemoryUserGroupService();
        service.setSecurityManager(getSecurityManager());
        service.initializeFromConfig(config);
        return new UserGroupStoreValidationWrapper(service.createStore());
    }

    protected void assertSecurityException(IOException ex, String id, Object... params) {
        assertTrue(ex.getCause() instanceof AbstractSecurityException);
        AbstractSecurityException secEx = (AbstractSecurityException) ex.getCause();
        assertEquals(id, secEx.getId());
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], secEx.getArgs()[i]);
        }
    }

    @Test
    public void testUserGroupStoreWrapper() throws Exception {
        boolean failed;
        UserGroupStoreValidationWrapper store = createStore("test");

        failed = false;
        try {
            store.addUser(store.createUserObject("", "", true));
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_REQUIRED);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.addGroup(store.createGroupObject(null, true));
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_REQUIRED);
            failed = true;
        }
        assertTrue(failed);

        store.addUser(store.createUserObject("user1", "abc", true));
        store.addGroup(store.createGroupObject("group1", true));
        assertEquals(1, store.getUsers().size());
        assertEquals(1, store.getUserCount());
        assertEquals(1, store.getUserGroups().size());
        assertEquals(1, store.getGroupCount());

        failed = false;
        try {
            store.addUser(store.createUserObject("user1", "abc", true));
        } catch (IOException ex) {
            assertSecurityException(ex, USER_ALREADY_EXISTS_$1, "user1");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.addGroup(store.createGroupObject("group1", true));
        } catch (IOException ex) {
            assertSecurityException(ex, GROUP_ALREADY_EXISTS_$1, "group1");
            failed = true;
        }
        assertTrue(failed);

        store.updateUser(store.createUserObject("user1", "abc", false));
        store.updateGroup(store.createGroupObject("group1", false));

        failed = false;
        try {
            store.updateUser(store.createUserObject("user1xxxx", "abc", true));
        } catch (IOException ex) {
            assertSecurityException(ex, USER_NOT_FOUND_$1, "user1xxxx");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.updateGroup(store.createGroupObject("group1xxx", true));
        } catch (IOException ex) {
            assertSecurityException(ex, GROUP_NOT_FOUND_$1, "group1xxx");
            failed = true;
        }
        assertTrue(failed);

        GeoServerUser user1 = store.getUserByUsername("user1");
        GeoServerUserGroup group1 = store.getGroupByGroupname("group1");
        failed = false;
        try {
            store.associateUserToGroup(store.createUserObject("xxx", "abc", true), group1);
        } catch (IOException ex) {
            assertSecurityException(ex, USER_NOT_FOUND_$1, "xxx");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.associateUserToGroup(user1, store.createGroupObject("yyy", true));
        } catch (IOException ex) {
            assertSecurityException(ex, GROUP_NOT_FOUND_$1, "yyy");
            failed = true;
        }
        assertTrue(failed);

        store.associateUserToGroup(user1, group1);
        assertEquals(1, store.getUsersForGroup(group1).size());
        assertEquals(1, store.getGroupsForUser(user1).size());

        failed = false;
        try {
            store.getGroupsForUser(store.createUserObject("xxx", "abc", true));
        } catch (IOException ex) {
            assertSecurityException(ex, USER_NOT_FOUND_$1, "xxx");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.getUsersForGroup(store.createGroupObject("yyy", true));
        } catch (IOException ex) {
            assertSecurityException(ex, GROUP_NOT_FOUND_$1, "yyy");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.disAssociateUserFromGroup(store.createUserObject("xxx", "abc", true), group1);
        } catch (IOException ex) {
            assertSecurityException(ex, USER_NOT_FOUND_$1, "xxx");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.disAssociateUserFromGroup(user1, store.createGroupObject("yyy", true));
        } catch (IOException ex) {
            assertSecurityException(ex, GROUP_NOT_FOUND_$1, "yyy");
            failed = true;
        }
        assertTrue(failed);

        store.disAssociateUserFromGroup(user1, group1);
        store.removeUser(user1);
        store.removeGroup(group1);
    }
}
