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

package org.geoserver.wfs;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.Join;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class JoinExtractingVisitorTest {

    private FeatureTypeInfo lakes;

    private FeatureTypeInfo forests;

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    private FeatureTypeInfo buildings;

    @Before
    public void setup() {
        lakes = createNiceMock(FeatureTypeInfo.class);
        expect(lakes.prefixedName()).andReturn("gs:Lakes").anyTimes();
        expect(lakes.getNativeName()).andReturn("Lakes").anyTimes();
        expect(lakes.getName()).andReturn("Lakes").anyTimes();

        forests = createNiceMock(FeatureTypeInfo.class);
        expect(forests.prefixedName()).andReturn("gs:Forests").anyTimes();
        expect(forests.getNativeName()).andReturn("Forests").anyTimes();
        expect(forests.getName()).andReturn("Forests").anyTimes();

        buildings = createNiceMock(FeatureTypeInfo.class);
        expect(buildings.prefixedName()).andReturn("gs:Buildings").anyTimes();
        expect(buildings.getNativeName()).andReturn("Buildings").anyTimes();
        expect(buildings.getName()).andReturn("Buildings").anyTimes();

        replay(lakes, forests, buildings);
    }

    @Test
    public void testTwoWayJoin() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(Arrays.asList(lakes, forests), Arrays.asList("a", "b"));
        Filter f = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        f.accept(visitor, null);

        assertEquals("a", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertNull(primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(1, joins.size());
        Join join = joins.get(0);
        assertEquals("Forests", join.getTypeName());
        assertEquals("b", join.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), join.getJoinFilter());
    }

    @Test
    public void testThreeWayJoinWithAliases() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(
                        Arrays.asList(lakes, forests, buildings), Arrays.asList("a", "b", "c"));
        Filter f1 = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        Filter f2 = ff.equals(ff.property("b/FID"), ff.property("c/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        testThreeWayJoin(visitor, f);
    }

    @Test
    public void testThreeWayJoinNoAliasesUnqualified() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(Arrays.asList(lakes, forests, buildings), null);
        Filter f1 = ff.equals(ff.property("Lakes/FID"), ff.property("Forests/FID"));
        Filter f2 = ff.equals(ff.property("Forests/FID"), ff.property("Buildings/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        testThreeWayJoin(visitor, f);
    }

    @Test
    public void testThreeWayJoinNoAliasesQualified() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(Arrays.asList(lakes, forests, buildings), null);
        Filter f1 = ff.equals(ff.property("gs:Lakes/FID"), ff.property("gs:Forests/FID"));
        Filter f2 = ff.equals(ff.property("gs:Forests/FID"), ff.property("gs:Buildings/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        testThreeWayJoin(visitor, f);
    }

    private void testThreeWayJoin(JoinExtractingVisitor visitor, Filter f) {
        f.accept(visitor, null);

        assertEquals("b", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertNull(primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(2, joins.size());

        Join j1 = joins.get(0);
        assertEquals("Lakes", j1.getTypeName());
        assertEquals("a", j1.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), j1.getJoinFilter());

        Join j2 = joins.get(1);
        assertEquals("Buildings", j2.getTypeName());
        assertEquals("c", j2.getAlias());
        assertEquals(ff.equals(ff.property("b.FID"), ff.property("c.FID")), j2.getJoinFilter());
    }

    @Test
    public void testThreeWayJoinPrimaryFilters() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(
                        Arrays.asList(lakes, forests, buildings), Arrays.asList("a", "b", "c"));
        Filter fj1 = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        Filter fj2 = ff.equals(ff.property("b/FID"), ff.property("c/FID"));
        Filter f1 = ff.equals(ff.property("a/FID"), ff.literal("Lakes.10"));
        Filter f2 = ff.equals(ff.property("b/FID"), ff.literal("Forests.10"));
        Filter f3 = ff.equals(ff.property("c/FID"), ff.literal("Buildings.10"));
        Filter f = ff.and(Arrays.asList(f1, f2, f3, fj1, fj2));
        f.accept(visitor, null);

        assertEquals("b", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertEquals(ff.equals(ff.property("FID"), ff.literal("Forests.10")), primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(2, joins.size());

        Join j1 = joins.get(0);
        assertEquals("Lakes", j1.getTypeName());
        assertEquals("a", j1.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), j1.getJoinFilter());
        assertEquals(ff.equals(ff.property("FID"), ff.literal("Lakes.10")), j1.getFilter());

        Join j2 = joins.get(1);
        assertEquals("Buildings", j2.getTypeName());
        assertEquals("c", j2.getAlias());
        assertEquals(ff.equals(ff.property("b.FID"), ff.property("c.FID")), j2.getJoinFilter());
        assertEquals(ff.equals(ff.property("FID"), ff.literal("Buildings.10")), j2.getFilter());
    }

    @Test
    public void testThreeWayJoinWithSelf() {
        JoinExtractingVisitor visitor =
                new JoinExtractingVisitor(
                        Arrays.asList(forests, lakes, lakes), Arrays.asList("a", "b", "c"));
        Filter f1 = ff.equals(ff.property("a/FID"), ff.property("b/FID"));
        Filter f2 = ff.equals(ff.property("b/FID"), ff.property("c/FID"));
        Filter f = ff.and(Arrays.asList(f1, f2));
        f.accept(visitor, null);

        assertEquals("b", visitor.getPrimaryAlias());

        Filter primary = visitor.getPrimaryFilter();
        assertNull(primary);

        List<Join> joins = visitor.getJoins();
        assertEquals(2, joins.size());

        Join j1 = joins.get(0);
        assertEquals("Forests", j1.getTypeName());
        assertEquals("a", j1.getAlias());
        assertEquals(ff.equals(ff.property("a.FID"), ff.property("b.FID")), j1.getJoinFilter());

        Join j2 = joins.get(1);
        assertEquals("Lakes", j2.getTypeName());
        assertEquals("c", j2.getAlias());
        assertEquals(ff.equals(ff.property("b.FID"), ff.property("c.FID")), j2.getJoinFilter());
    }
}
