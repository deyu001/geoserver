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

package org.geoserver.ows.util;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class OwsUtilsTest {

    @Test
    public void testSimple() throws Exception {
        Foo foo = new Foo();
        foo.setA("a");

        Assert.assertEquals("a", OwsUtils.get(foo, "a"));
        Assert.assertNull(OwsUtils.get(foo, "b"));

        OwsUtils.set(foo, "b", 5);
        Assert.assertEquals(5, OwsUtils.get(foo, "b"));

        Assert.assertEquals(0f, OwsUtils.get(foo, "c"));
        OwsUtils.set(foo, "c", 5f);
        Assert.assertEquals(5f, OwsUtils.get(foo, "c"));
    }

    @Test
    public void testExtended() throws Exception {
        Bar bar = new Bar();
        Assert.assertNull(OwsUtils.get(bar, "foo"));
        Assert.assertNull(OwsUtils.get(bar, "foo.a"));

        Foo foo = new Foo();
        bar.setFoo(foo);
        Assert.assertEquals(foo, OwsUtils.get(bar, "foo"));
        Assert.assertNull(OwsUtils.get(bar, "foo.a"));

        foo.setA("abc");
        Assert.assertEquals("abc", OwsUtils.get(bar, "foo.a"));

        OwsUtils.set(bar, "foo.b", 123);
        Assert.assertEquals(123, OwsUtils.get(bar, "foo.b"));
    }

    @Test
    public void testPut() throws Exception {
        Baz baz = new Baz();
        try {
            OwsUtils.put(baz, "map", "k", "v");
            Assert.fail("null map should cause exception");
        } catch (NullPointerException e) {
        }

        baz.map = new HashMap();
        try {
            OwsUtils.put(baz, "xyz", "k", "v");
            Assert.fail("bad property should cause exception");
        } catch (IllegalArgumentException e) {
        }

        Assert.assertTrue(baz.map.isEmpty());
        OwsUtils.put(baz, "map", "k", "v");
        Assert.assertEquals("v", baz.map.get("k"));
    }

    class Foo {
        String a;
        Integer b;
        float c;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public Integer getB() {
            return b;
        }

        public void setB(Integer b) {
            this.b = b;
        }

        public float getC() {
            return c;
        }

        public void setC(float c) {
            this.c = c;
        }
    }

    class Bar {
        Foo foo;
        Double d;

        public Foo getFoo() {
            return foo;
        }

        public void setFoo(Foo foo) {
            this.foo = foo;
        }

        public Double getD() {
            return d;
        }

        public void setD(Double d) {
            this.d = d;
        }
    }

    class Baz {
        Map map;

        public Map getMap() {
            return map;
        }
    }
}
