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

import java.util.Properties;
import java.util.Set;
import org.geoserver.security.AccessMode;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests parsing of the property file into a security tree, and the functionality of the tree as
 * well (building the tree by hand is tedious)
 *
 * @author Andrea Aime - TOPP
 */
public class DefaultDataAccessManagerTreeTest extends AbstractAuthorizationTest {

    @Before
    public void setupCatalog() {
        populateCatalog();
    }

    private SecureTreeNode buildTree(String propertyFile) throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(propertyFile));
        return new DefaultResourceAccessManager(
                        new MemoryDataAccessRuleDAO(catalog, props), catalog)
                .root;
    }

    @Test
    public void testWideOpen() throws Exception {
        SecureTreeNode root = buildTree("wideOpen.properties");
        assertEquals(0, root.children.size());
        // we have he "*" rules
        assertEquals(1, root.getAuthorizedRoles(AccessMode.READ).size());
        assertEquals(1, root.getAuthorizedRoles(AccessMode.WRITE).size());
        assertTrue(root.canAccess(anonymous, AccessMode.READ));
        assertTrue(root.canAccess(anonymous, AccessMode.WRITE));
    }

    @Test
    public void testLockedDown() throws Exception {
        SecureTreeNode root = buildTree("lockedDown.properties");
        assertEquals(0, root.children.size());
        final Set<String> readRoles = root.getAuthorizedRoles(AccessMode.READ);
        assertEquals(1, readRoles.size());
        assertTrue(readRoles.contains("WRITER"));
        final Set<String> writeRoles = root.getAuthorizedRoles(AccessMode.WRITE);
        assertEquals(1, writeRoles.size());
        assertTrue(writeRoles.contains("WRITER"));
        assertFalse(root.canAccess(anonymous, AccessMode.READ));
        assertFalse(root.canAccess(anonymous, AccessMode.WRITE));
        assertFalse(root.canAccess(roUser, AccessMode.READ));
        assertFalse(root.canAccess(roUser, AccessMode.WRITE));
        assertTrue(root.canAccess(rwUser, AccessMode.READ));
        assertTrue(root.canAccess(rwUser, AccessMode.WRITE));
    }

    @Test
    public void testPublicRead() throws Exception {
        SecureTreeNode root = buildTree("publicRead.properties");
        assertEquals(0, root.children.size());
        assertEquals(SecureTreeNode.EVERYBODY, root.getAuthorizedRoles(AccessMode.READ));
        final Set<String> writeRoles = root.getAuthorizedRoles(AccessMode.WRITE);
        assertEquals(1, writeRoles.size());
        assertTrue(writeRoles.contains("WRITER"));
        assertTrue(root.canAccess(anonymous, AccessMode.READ));
        assertFalse(root.canAccess(anonymous, AccessMode.WRITE));
        assertTrue(root.canAccess(roUser, AccessMode.READ));
        assertFalse(root.canAccess(roUser, AccessMode.WRITE));
        assertTrue(root.canAccess(rwUser, AccessMode.READ));
        assertTrue(root.canAccess(rwUser, AccessMode.WRITE));
    }

    @Test
    public void testComplex() throws Exception {
        SecureTreeNode root = buildTree("complex.properties");

        // first off, evaluate tree structure
        assertEquals(2, root.children.size());
        SecureTreeNode topp = root.getChild("topp");
        assertNotNull(topp);
        assertEquals(3, topp.children.size());
        SecureTreeNode states = topp.getChild("states");
        SecureTreeNode landmarks = topp.getChild("landmarks");
        SecureTreeNode bases = topp.getChild("bases");
        assertNotNull(states);
        assertNotNull(landmarks);
        assertNotNull(bases);

        // perform some checks with anonymous access
        assertFalse(root.canAccess(anonymous, AccessMode.READ));
        assertFalse(root.canAccess(anonymous, AccessMode.WRITE));
        assertTrue(topp.canAccess(anonymous, AccessMode.READ));
        assertFalse(states.canAccess(anonymous, AccessMode.READ));
        assertTrue(landmarks.canAccess(anonymous, AccessMode.READ));
        assertFalse(landmarks.canAccess(anonymous, AccessMode.WRITE));
        assertFalse(bases.canAccess(anonymous, AccessMode.READ));

        // perform some checks with read only access
        assertTrue(root.canAccess(roUser, AccessMode.READ));
        assertFalse(root.canAccess(roUser, AccessMode.WRITE));
        assertTrue(topp.canAccess(roUser, AccessMode.READ));
        assertTrue(states.canAccess(roUser, AccessMode.READ));
        assertTrue(landmarks.canAccess(roUser, AccessMode.READ));
        assertFalse(landmarks.canAccess(roUser, AccessMode.WRITE));
        assertFalse(bases.canAccess(roUser, AccessMode.READ));

        // perform some checks with read write access
        assertTrue(root.canAccess(rwUser, AccessMode.READ));
        assertFalse(root.canAccess(rwUser, AccessMode.WRITE));
        assertTrue(topp.canAccess(rwUser, AccessMode.READ));
        assertTrue(states.canAccess(rwUser, AccessMode.WRITE));
        assertTrue(landmarks.canAccess(rwUser, AccessMode.READ));
        assertTrue(landmarks.canAccess(rwUser, AccessMode.WRITE));
        assertFalse(bases.canAccess(rwUser, AccessMode.READ));

        // military access... just access the one layer, for the rest he's like anonymous
        assertFalse(root.canAccess(milUser, AccessMode.READ));
        assertFalse(root.canAccess(milUser, AccessMode.WRITE));
        assertTrue(topp.canAccess(milUser, AccessMode.READ));
        assertFalse(states.canAccess(milUser, AccessMode.WRITE));
        assertTrue(landmarks.canAccess(milUser, AccessMode.READ));
        assertFalse(landmarks.canAccess(milUser, AccessMode.WRITE));
        assertTrue(bases.canAccess(milUser, AccessMode.READ));
        assertTrue(bases.canAccess(milUser, AccessMode.WRITE));
    }
}
