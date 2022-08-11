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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class RoleHierarchyHelperTest {

    protected Map<String, String> createFromArray(String[][] array) {
        Map<String, String> mappings = new HashMap<>();
        for (String[] strings : array) {
            mappings.put(strings[0], strings[1]);
        }
        return mappings;
    }

    @Test
    public void testValidTree() throws Exception {
        Map<String, String> map =
                createFromArray(
                        new String[][] {
                            {"node1", null},
                            {"node11", "node1"},
                            {"node12", "node1"},
                            {"node111", "node11"},
                            {"node112", "node11"},
                        });
        RoleHierarchyHelper helper = new RoleHierarchyHelper(map);

        assertFalse(helper.containsRole("abc"));
        assertTrue(helper.containsRole("node11"));
        assertFalse(helper.isRoot("node11"));
        assertTrue(helper.isRoot("node1"));
        assertEquals(1, helper.getRootRoles().size());
        assertTrue(helper.getRootRoles().contains("node1"));

        assertEquals(3, helper.getLeafRoles().size());
        assertTrue(helper.getLeafRoles().contains("node111"));
        assertTrue(helper.getLeafRoles().contains("node112"));
        assertTrue(helper.getLeafRoles().contains("node12"));

        assertEquals("node1", helper.getParent("node11"));
        assertNull(helper.getParent("node1"));

        assertEquals(0, helper.getAncestors("node1").size());
        assertEquals(1, helper.getAncestors("node12").size());
        assertTrue(helper.getAncestors("node12").contains("node1"));
        assertEquals(2, helper.getAncestors("node112").size());
        assertTrue(helper.getAncestors("node112").contains("node11"));
        assertTrue(helper.getAncestors("node112").contains("node1"));

        assertEquals(2, helper.getChildren("node1").size());
        assertTrue(helper.getChildren("node1").contains("node11"));
        assertTrue(helper.getChildren("node1").contains("node12"));

        assertEquals(0, helper.getChildren("node12").size());
        assertEquals(2, helper.getChildren("node11").size());
        assertTrue(helper.getChildren("node11").contains("node111"));
        assertTrue(helper.getChildren("node11").contains("node112"));

        assertEquals(4, helper.getDescendants("node1").size());
        assertTrue(helper.getDescendants("node1").contains("node11"));
        assertTrue(helper.getDescendants("node1").contains("node12"));
        assertTrue(helper.getDescendants("node1").contains("node111"));
        assertTrue(helper.getDescendants("node1").contains("node112"));

        assertEquals(0, helper.getDescendants("node12").size());

        assertEquals(2, helper.getDescendants("node11").size());
        assertTrue(helper.getDescendants("node11").contains("node111"));
        assertTrue(helper.getDescendants("node11").contains("node112"));

        assertTrue(helper.isValidParent("node11", null));
        assertTrue(helper.isValidParent("node11", "node12"));
        assertFalse(helper.isValidParent("node11", "node11"));
        assertFalse(helper.isValidParent("node1", "node111"));

        boolean fail = true;
        try {
            helper.isRoot("abc");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");
    }

    @Test
    public void testInValidTree1() throws Exception {
        Map<String, String> map = createFromArray(new String[][] {{"node1", "node1"}});
        RoleHierarchyHelper helper = new RoleHierarchyHelper(map);
        boolean fail;

        fail = true;
        try {
            helper.getParent("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        fail = true;
        try {
            helper.getAncestors("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        fail = true;
        try {
            helper.getChildren("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        fail = true;
        try {
            helper.getDescendants("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");
    }

    @Test
    public void testInValidTree2() throws Exception {
        Map<String, String> map =
                createFromArray(
                        new String[][] {
                            {"node1", "node2"},
                            {"node2", "node1"}
                        });
        RoleHierarchyHelper helper = new RoleHierarchyHelper(map);
        boolean fail;

        helper.getParent("node1"); // ok

        fail = true;
        try {
            helper.getAncestors("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");

        helper.getChildren("node1"); // ok

        fail = true;
        try {
            helper.getDescendants("node1");
        } catch (RuntimeException e) {
            fail = false;
        }
        if (fail) Assert.fail("No Exception");
    }
}
