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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.security.AccessMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DataAccessRuleDAOTest {

    DataAccessRuleDAO dao;
    Properties props;

    @Before
    public void setUp() throws Exception {
        // make a nice little catalog that does always tell us stuff is there
        Catalog catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspaceByName(anyObject()))
                .andReturn(new WorkspaceInfoImpl())
                .anyTimes();
        expect(catalog.getLayerByName((String) anyObject()))
                .andReturn(new LayerInfoImpl())
                .anyTimes();
        replay(catalog);

        // prepare some base rules
        props = new Properties();
        props.put("mode", "CHALLENGE");
        props.put("topp.states.w", "ROLE_TSW");
        props.put("topp.*.w", "ROLE_TW");
        props.put("*.*.r", "*");
        props.put("group.r", "ROLE_GROUP");

        dao = new MemoryDataAccessRuleDAO(catalog, props);
    }

    @Test
    public void testRulesForRole() {

        Assert.assertEquals(0, dao.getRulesAssociatedWithRole("CHALLENGE").size());
        Assert.assertEquals(0, dao.getRulesAssociatedWithRole("NOTEXISTEND").size());
        Assert.assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_TSW").size());
        Assert.assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_TW").size());
        Assert.assertEquals(1, dao.getRulesAssociatedWithRole("ROLE_GROUP").size());
    }

    @Test
    public void testParseGlobalLayerGroupRule() {
        DataAccessRule r = dao.parseDataAccessRule("group.r", "ROLE_GROUP_OWNER");
        Assert.assertEquals(r.getRoot(), "group");
        Assert.assertNull(r.getLayer());
        Assert.assertTrue(r.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, r.getAccessMode());
    }

    @Test
    public void testParse() {
        Assert.assertEquals(4, dao.getRules().size());

        // check the first rule
        DataAccessRule rule = dao.getRules().get(0);
        Assert.assertEquals("*.*.r", rule.getKey());
        Assert.assertEquals(1, rule.getRoles().size());
        Assert.assertEquals("*", rule.getRoles().iterator().next());
    }

    @Test
    public void testAdd() {
        Assert.assertEquals(4, dao.getRules().size());
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        Assert.assertTrue(dao.addRule(newRule));
        Assert.assertEquals(5, dao.getRules().size());
        Assert.assertEquals(newRule, dao.getRules().get(1));
        Assert.assertFalse(dao.addRule(newRule));
    }

    @Test
    public void testRemove() {
        Assert.assertEquals(4, dao.getRules().size());
        DataAccessRule newRule = dao.parseDataAccessRule("*.*.w", "ROLE_GENERIC_W");
        Assert.assertFalse(dao.removeRule(newRule));
        DataAccessRule first = dao.getRules().get(0);
        Assert.assertTrue(dao.removeRule(first));
        Assert.assertFalse(dao.removeRule(first));
        Assert.assertEquals(3, dao.getRules().size());
    }

    @Test
    public void testStore() {
        Properties newProps = dao.toProperties();

        // properties equality does not seem to work...
        Assert.assertEquals(newProps.size(), props.size());
        for (Object key : newProps.keySet()) {
            Object newValue = newProps.get(key);
            Object oldValue = newProps.get(key);
            Assert.assertEquals(newValue, oldValue);
        }
    }

    @Test
    public void testParsePlain() {
        DataAccessRule rule = dao.parseDataAccessRule("a.b.r", "ROLE_WHO_CARES");
        Assert.assertEquals("a", rule.getRoot());
        Assert.assertEquals("b", rule.getLayer());
        Assert.assertFalse(rule.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testParseSpaces() {
        DataAccessRule rule = dao.parseDataAccessRule(" a  . b . r ", "ROLE_WHO_CARES");
        Assert.assertEquals("a", rule.getRoot());
        Assert.assertEquals("b", rule.getLayer());
        Assert.assertFalse(rule.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testParseEscapedDots() {
        DataAccessRule rule = dao.parseDataAccessRule("w. a\\.b . r ", "ROLE_WHO_CARES");
        Assert.assertEquals("w", rule.getRoot());
        Assert.assertEquals("a.b", rule.getLayer());
        Assert.assertFalse(rule.isGlobalGroupRule());
        Assert.assertEquals(AccessMode.READ, rule.getAccessMode());
    }

    @Test
    public void testStoreEscapedDots() throws Exception {
        dao.clear();
        dao.addRule(
                new DataAccessRule(
                        "it.geosolutions",
                        "layer.dots",
                        AccessMode.READ,
                        Collections.singleton("ROLE_ABC")));
        Properties ps = dao.toProperties();

        Assert.assertEquals(2, ps.size());
        Assert.assertEquals("ROLE_ABC", ps.getProperty("it\\.geosolutions.layer\\.dots.r"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ps.store(bos, null);
    }
}
