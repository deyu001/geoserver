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

package org.geoserver.security.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.SortedSet;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** @author Niels Charlier */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(
    transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
    allowAnonymousAccess = true
)
@CreateDS(
    name = "myDS",
    partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)}
)
@ApplyLdifFiles({"data4.ldif"})
public class LDAPUserGroupServiceTest extends LDAPBaseTest {
    GeoServerUserGroupService service;

    @Override
    protected void createConfig() {
        config = new LDAPUserGroupServiceConfig();
    }

    @Before
    public void createUserGroupService() throws Exception {
        config.setGroupNameAttribute("cn");
        config.setUserSearchBase("ou=People");
        config.setUserNameAttribute("uid");
        config.setGroupSearchFilter("member={1},dc=example,dc=com");
        ((LDAPUserGroupServiceConfig) config)
                .setPopulatedAttributes("sn, givenName, telephoneNumber, mail");
        service = new LDAPUserGroupService(config);
    }

    @Test
    public void testUsers() throws Exception {
        SortedSet<GeoServerUser> users = service.getUsers();
        assertNotNull(users);
        assertEquals(4, users.size());
    }

    @Test
    public void testGroupByName() throws Exception {
        assertNotNull(service.getGroupByGroupname("extra"));
        assertNull(service.getGroupByGroupname("dummy"));
    }

    @Test
    public void testUserByName() throws Exception {
        GeoServerUser user = service.getUserByUsername("other");
        assertNotNull(user);
        assertEquals("other", user.getProperties().get("givenName"));
        assertEquals("dude", user.getProperties().get("sn"));
        assertEquals("2", user.getProperties().get("telephoneNumber"));
        assertNull(service.getUserByUsername("dummy"));
    }

    @Test
    public void testUsersForGroup() throws Exception {
        SortedSet<GeoServerUser> users =
                service.getUsersForGroup(service.getGroupByGroupname("other"));
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    public void testGroupsForUser() throws Exception {
        SortedSet<GeoServerUserGroup> groups =
                service.getGroupsForUser(service.getUserByUsername("other"));
        assertNotNull(groups);
        assertEquals(1, groups.size());
    }

    @Test
    public void testUserCount() throws Exception {
        assertEquals(4, service.getUserCount());
    }

    @Test
    public void testGroupCount() throws Exception {
        assertEquals(8, service.getGroupCount());
    }

    @Test
    public void testUsersHavingProperty() throws Exception {
        SortedSet<GeoServerUser> users = service.getUsersHavingProperty("mail");
        assertEquals(1, users.size());
        for (GeoServerUser user : users) {
            assertEquals("extra", user.getUsername());
        }
    }

    @Test
    public void testUsersNotHavingProperty() throws Exception {
        SortedSet<GeoServerUser> users = service.getUsersNotHavingProperty("telephoneNumber");
        assertEquals(1, users.size());
        for (GeoServerUser user : users) {
            assertEquals("extra", user.getUsername());
        }
    }

    @Test
    public void testCountUsersHavingProperty() throws Exception {
        assertEquals(1, service.getUserCountHavingProperty("mail"));
    }

    @Test
    public void testCountUsersNotHavingProperty() throws Exception {
        assertEquals(1, service.getUserCountNotHavingProperty("telephoneNumber"));
    }

    @Test
    public void testUsersHavingPropertyValue() throws Exception {
        SortedSet<GeoServerUser> users =
                service.getUsersHavingPropertyValue("telephoneNumber", "2");
        assertEquals(1, users.size());
        for (GeoServerUser user : users) {
            assertEquals("other", user.getUsername());
        }
    }

    @Test
    public void testUserCountHavingPropertyValue() throws Exception {
        assertEquals(1, service.getUserCountHavingPropertyValue("telephoneNumber", "2"));
    }

    /** Tests Users retrieval for a hierarchical parent group. */
    @Test
    public void testUsersForHierarchicalGroup() throws Exception {
        config.setUseNestedParentGroups(true);
        service = new LDAPUserGroupService(config);
        SortedSet<GeoServerUser> users =
                service.getUsersForGroup(service.getGroupByGroupname("extra"));
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(x -> "nestedUser".equals(x.getUsername())));
    }

    /** Tests Hierarchical LDAP groups retrieval for an user. */
    @Test
    public void testHierarchicalGroupsForUser() throws Exception {
        config.setUseNestedParentGroups(true);
        service = new LDAPUserGroupService(config);
        SortedSet<GeoServerUserGroup> groups =
                service.getGroupsForUser(service.getUserByUsername("nestedUser"));
        assertNotNull(groups);
        assertEquals(6, groups.size());
        assertTrue(groups.stream().anyMatch(x -> "extra".equals(x.getGroupname())));
    }
}
