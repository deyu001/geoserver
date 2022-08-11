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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractRoleServiceTest extends AbstractSecurityServiceTest {

    protected GeoServerRoleService service;
    protected GeoServerRoleStore store;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        service = createRoleService("test");
    }

    @Before
    public void init() throws IOException {
        service = getSecurityManager().loadRoleService("test");
        store = createStore(service);
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

        GeoServerRole role = store.createRoleObject("ROLE_DUMMY");
        GeoServerRole role_parent = store.createRoleObject("ROLE_PARENT");

        assertFalse(store.isModified());

        // add,remove,update
        store.addRole(role);
        store.addRole(role_parent);
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.updateRole(role);
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.removeRole(role);
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.associateRoleToGroup(role, "agroup");
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.disAssociateRoleFromGroup(role, "agroup");
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.associateRoleToUser(role, "auser");
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.disAssociateRoleFromUser(role, "auser");
        assertTrue(store.isModified());
        store.load();

        assertFalse(store.isModified());
        store.setParentRole(role, role_parent);
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.setParentRole(role, null);
        assertTrue(store.isModified());
        store.store();

        assertFalse(store.isModified());
        store.clear();
        assertTrue(store.isModified());
        store.load();
    }

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
}
