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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.PropertyFileWatcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class GeoServerUserDaoTest {

    static class TestableUserDao extends GeoServerUserDao {

        public TestableUserDao(Properties p) throws IOException {
            userMap = loadUsersFromProperties(p);
        }

        @Override
        void checkUserMap() throws DataAccessResourceFailureException {
            // do nothing, for this test we don't write on the fs by default
        }

        void loadUserMap() {
            super.checkUserMap();
        }
    }

    Properties props;
    TestableUserDao dao;

    @Before
    public void setUp() throws Exception {
        props = new Properties();
        props.put("admin", "gs,ROLE_ADMINISTRATOR");
        props.put("wfs", "webFeatureService,ROLE_WFS_READ,ROLE_WFS_WRITE");
        props.put("disabledUser", "nah,ROLE_TEST,disabled");
        dao = new TestableUserDao(props);
    }

    @Test
    public void testGetUsers() throws Exception {
        List<User> users = dao.getUsers();
        assertEquals(3, users.size());
    }

    @Test
    public void testLoadUser() throws Exception {
        UserDetails admin = dao.loadUserByUsername("admin");
        assertEquals("admin", admin.getUsername());
        assertEquals("gs", admin.getPassword());
        assertEquals(1, admin.getAuthorities().size());
        assertEquals("ROLE_ADMINISTRATOR", admin.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    public void testMissingUser() throws Exception {
        try {
            dao.loadUserByUsername("notThere");
            fail("This user should not be there");
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testSetUser() throws Exception {
        dao.setUser(
                new User(
                        "wfs",
                        "pwd",
                        true,
                        true,
                        true,
                        true,
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_WFS_ALL"),
                                    new SimpleGrantedAuthority("ROLE_WMS_ALL")
                                })));
        UserDetails user = dao.loadUserByUsername("wfs");
        assertEquals("wfs", user.getUsername());
        assertEquals("pwd", user.getPassword());
        assertEquals(2, user.getAuthorities().size());
        Set<String> authorities = new HashSet<>();
        for (GrantedAuthority ga : user.getAuthorities()) {
            authorities.add(ga.getAuthority());
        }
        //  order independent
        assertTrue(authorities.contains("ROLE_WFS_ALL"));
        assertTrue(authorities.contains("ROLE_WMS_ALL"));
    }

    @Test
    public void testSetMissingUser() throws Exception {
        try {
            dao.setUser(
                    new User(
                            "notther",
                            "pwd",
                            true,
                            true,
                            true,
                            true,
                            Arrays.asList(
                                    new GrantedAuthority[] {
                                        new SimpleGrantedAuthority("ROLE_WFS_ALL")
                                    })));
            fail("The user is not there, setUser should fail");
        } catch (IllegalArgumentException e) {
            // cool
        }
    }

    @Test
    public void testAddUser() throws Exception {
        dao.putUser(
                new User(
                        "newuser",
                        "pwd",
                        true,
                        true,
                        true,
                        true,
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_WFS_ALL")
                                })));
        assertNotNull(dao.loadUserByUsername("newuser"));
    }

    @Test
    public void addExistingUser() throws Exception {
        try {
            dao.putUser(
                    new User(
                            "admin",
                            "pwd",
                            true,
                            true,
                            true,
                            true,
                            Arrays.asList(
                                    new GrantedAuthority[] {
                                        new SimpleGrantedAuthority("ROLE_WFS_ALL")
                                    })));
            fail("The user is already there, addUser should fail");
        } catch (IllegalArgumentException e) {
            // cool
        }
    }

    @Test
    public void testRemoveUser() throws Exception {
        assertFalse(dao.removeUser("notthere"));
        assertTrue(dao.removeUser("wfs"));
        try {
            dao.loadUserByUsername("wfs");
            fail("The user is not there, loadUserByName should fail");
        } catch (UsernameNotFoundException e) {
            // cool
        }
    }

    @Test
    public void testStoreReload() throws Exception {
        File temp = File.createTempFile("sectest", "", new File("target"));
        temp.delete();
        temp.mkdir();
        File propFile = new File(temp, "users.properties");
        try {
            dao.userDefinitionsFile = new PropertyFileWatcher(Files.asResource(propFile));
            dao.storeUsers();
            dao.userMap.clear();
            dao.loadUserMap();
        } finally {
            temp.delete();
        }

        assertEquals(3, dao.getUsers().size());
        testLoadUser();
    }
}
