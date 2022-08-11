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

package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractUserGroupServiceTest extends AbstractSecurityServiceTest {

    protected GeoServerUserGroupService service;
    protected GeoServerUserGroupStore store;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        service = createUserGroupService("test");
        // store  = createStore(service);
    }

    @Before
    public void setServiceAndStore() throws Exception {
        service = getSecurityManager().loadUserGroupService("test");
        store = createStore(service);
    }

    protected abstract SecurityUserGroupServiceConfig createConfigObject(String name);

    @Test
    public void testInsert() throws Exception {
        // all is empty
        checkEmpty(service);
        checkEmpty(store);

        // transaction has values ?
        insertValues(store);
        if (!isJDBCTest()) checkEmpty(service);
        checkValuesInserted(store);

        // rollback
        store.load();
        checkEmpty(store);
        checkEmpty(service);

        // commit
        insertValues(store);
        store.store();
        checkValuesInserted(store);
        checkValuesInserted(service);
    }

    @Test
    public void testModify() throws Exception {
        // all is empty
        checkEmpty(service);
        checkEmpty(store);

        insertValues(store);
        store.store();
        checkValuesInserted(store);
        checkValuesInserted(service);

        modifyValues(store);
        if (!isJDBCTest()) checkValuesInserted(service);
        checkValuesModified(store);

        store.load();
        checkValuesInserted(store);
        checkValuesInserted(service);

        modifyValues(store);
        store.store();
        checkValuesModified(store);
        checkValuesModified(service);
    }

    @Test
    public void testRemove() throws Exception {
        // all is empty
        checkEmpty(service);
        checkEmpty(store);

        insertValues(store);
        store.store();
        checkValuesInserted(store);
        checkValuesInserted(service);

        removeValues(store);
        if (!isJDBCTest()) checkValuesInserted(service);
        checkValuesRemoved(store);

        store.load();
        checkValuesInserted(store);
        checkValuesInserted(service);

        removeValues(store);
        store.store();
        checkValuesRemoved(store);
        checkValuesRemoved(service);
    }

    @Test
    public void testIsModified() throws Exception {
        assertFalse(store.isModified());

        insertValues(store);
        assertTrue(store.isModified());

        store.load();
        assertFalse(store.isModified());

        insertValues(store);
        store.store();
        assertFalse(store.isModified());

        GeoServerUser user = store.createUserObject("uuuu", "", true);
        GeoServerUserGroup group = store.createGroupObject("gggg", true);

        assertFalse(store.isModified());

        // add,remove,update
        store.addUser(user);
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.addGroup(group);
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.updateUser(user);
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.updateGroup(group);
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.removeUser(user);
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.removeGroup(group);
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.associateUserToGroup(user, group);
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.disAssociateUserFromGroup(user, group);
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.clear();
        assertTrue(store.isModified());
        store.load();
    }

    @Test
    public void testEmptyPassword() throws Exception {
        // all is empty
        checkEmpty(service);
        checkEmpty(store);

        GeoServerUser user = store.createUserObject("userNoPasswd", null, true);
        store.addUser(user);
        store.store();

        assertEquals(1, service.getUserCount());
        user = service.getUserByUsername("userNoPasswd");
        assertNull(user.getPassword());

        user = (GeoServerUser) service.loadUserByUsername("userNoPasswd");
        assertNull(user.getPassword());
    }

    @Test
    public void testEraseCredentials() throws Exception {

        GeoServerUser user = store.createUserObject("user", "foobar", true);
        store.addUser(user);
        store.store();

        user = store.getUserByUsername("user");
        assertNotNull(user.getPassword());
        user.eraseCredentials();

        user = store.getUserByUsername("user");
        assertNotNull(user.getPassword());
    }

    @Test
    public void testPasswordRecoding() throws Exception {

        SecurityUserGroupServiceConfig config =
                getSecurityManager().loadUserGroupServiceConfig(service.getName());
        config.setPasswordEncoderName(getPlainTextPasswordEncoder().getName());
        getSecurityManager().saveUserGroupService(config);
        service.initializeFromConfig(config);
        store = service.createStore();

        store.addUser(store.createUserObject("u1", "p1", true));
        store.addUser(store.createUserObject("u2", "p2", true));
        store.store();

        Util.recodePasswords(service.createStore());
        // no recoding
        assertTrue(
                service.loadUserByUsername("u1")
                        .getPassword()
                        .startsWith(getPlainTextPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u2")
                        .getPassword()
                        .startsWith(getPlainTextPasswordEncoder().getPrefix()));

        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        getSecurityManager().saveUserGroupService(config);
        service.initializeFromConfig(config);

        Util.recodePasswords(service.createStore());
        // recoding
        assertTrue(
                service.loadUserByUsername("u1")
                        .getPassword()
                        .startsWith(getPBEPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u2")
                        .getPassword()
                        .startsWith(getPBEPasswordEncoder().getPrefix()));

        config.setPasswordEncoderName(getDigestPasswordEncoder().getName());
        getSecurityManager().saveUserGroupService(config);
        service.initializeFromConfig(config);

        Util.recodePasswords(service.createStore());
        // recoding
        assertTrue(
                service.loadUserByUsername("u1")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u2")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));

        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        getSecurityManager().saveUserGroupService(config);
        service.initializeFromConfig(config);

        Util.recodePasswords(service.createStore());
        // recoding has no effect
        assertTrue(
                service.loadUserByUsername("u1")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u2")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));

        // add a user with pbe encoding
        store = service.createStore();
        store.addUser(store.createUserObject("u3", "p3", true));
        store.store();

        assertTrue(
                service.loadUserByUsername("u1")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u2")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u3")
                        .getPassword()
                        .startsWith(getPBEPasswordEncoder().getPrefix()));

        config.setPasswordEncoderName(getEmptyEncoder().getName());
        getSecurityManager().saveUserGroupService(config);
        service.initializeFromConfig(config);

        Util.recodePasswords(service.createStore());
        // recode u3 to empty
        assertTrue(
                service.loadUserByUsername("u1")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u2")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u3")
                        .getPassword()
                        .startsWith(getEmptyEncoder().getPrefix()));

        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        getSecurityManager().saveUserGroupService(config);
        service.initializeFromConfig(config);

        Util.recodePasswords(service.createStore());
        // recode has no effect
        assertTrue(
                service.loadUserByUsername("u1")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u2")
                        .getPassword()
                        .startsWith(getDigestPasswordEncoder().getPrefix()));
        assertTrue(
                service.loadUserByUsername("u3")
                        .getPassword()
                        .startsWith(getEmptyEncoder().getPrefix()));
    }
}
