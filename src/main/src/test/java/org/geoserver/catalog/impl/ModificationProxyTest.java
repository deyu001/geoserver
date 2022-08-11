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

package org.geoserver.catalog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

public class ModificationProxyTest {

    @Test
    public void testRewrapNoProxyIdentity() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");

        TestBean result = ModificationProxy.rewrap(bean, b -> b, TestBean.class);

        assertThat(result, sameInstance(bean));
    }

    @Test
    public void testRewrapNoProxyInnerChange() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");

        TestBean result = ModificationProxy.rewrap(bean, b -> newBean, TestBean.class);

        assertThat(result, sameInstance(newBean));
    }

    @Test
    public void testRewrapEmptyProxyIdentity() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        TestBean result = ModificationProxy.rewrap(proxy, b -> b, TestBean.class);
        assertThat(result, modProxy(sameInstance(bean)));

        assertThat(result.getValue(), equalTo("Mr. Bean"));
        assertThat(result.getListValue(), contains("Uhh", "Bean"));
    }

    @Test
    public void testRewrapChangedProxyIdentity() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        proxy.setValue("Edmond Blackadder");
        proxy.setListValue(Arrays.asList("Cunning", "Plan"));

        TestBean result = ModificationProxy.rewrap(proxy, b -> b, TestBean.class);

        // Should be a new wrapper
        assertThat(result, not(sameInstance(proxy)));
        // Wrapping the same object
        assertThat(result, modProxy(sameInstance(bean)));

        // With the same changes
        assertThat(result.getValue(), equalTo("Edmond Blackadder"));
        assertThat(result.getListValue(), contains("Cunning", "Plan"));

        // The changes should not have been committed
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
    }

    @Test
    public void testRewrapChangedProxyInnerChange() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        proxy.setValue("Edmond Blackadder");
        proxy.setListValue(Arrays.asList("Cunning", "Plan"));

        // Swap the old bean for the new one
        TestBean result = ModificationProxy.rewrap(proxy, b -> newBean, TestBean.class);

        // Should be a new wrapper
        assertThat(result, not(sameInstance(proxy)));
        // Wrapping the new object
        assertThat(result, modProxy(sameInstance(newBean)));

        // With the same changes
        assertThat(result.getValue(), equalTo("Edmond Blackadder"));
        assertThat(result.getListValue(), contains("Cunning", "Plan"));

        // The changes should not have been committed to either bean
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
        assertThat(newBean.getValue(), equalTo("Johnny English"));
        assertThat(newBean.getListValue(), contains("Not", "Bond"));
    }

    @Test
    public void testRewrapEmptyProxyInnerChange() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        // Swap the old bean for the new one
        TestBean result = ModificationProxy.rewrap(proxy, b -> newBean, TestBean.class);

        // Should be a new wrapper
        assertThat(result, not(sameInstance(proxy)));
        // Wrapping the new object
        assertThat(result, modProxy(sameInstance(newBean)));

        // Should show the properties of the new bean
        assertThat(result.getValue(), equalTo("Johnny English"));
        assertThat(result.getListValue(), contains("Not", "Bond"));

        // No changes should not have been committed to either bean
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
        assertThat(newBean.getValue(), equalTo("Johnny English"));
        assertThat(newBean.getListValue(), contains("Not", "Bond"));
    }

    @Test
    public void testRewrapCommitToNew() throws Exception {
        TestBean bean = new TestBeanImpl("Mr. Bean", "Uhh", "Bean");
        TestBean newBean = new TestBeanImpl("Johnny English", "Not", "Bond");
        TestBean proxy = ModificationProxy.create(bean, TestBean.class);

        proxy.setValue("Edmond Blackadder");
        proxy.setListValue(Arrays.asList("Cunning", "Plan"));

        // Swap the old bean for the new one
        TestBean result = ModificationProxy.rewrap(proxy, b -> newBean, TestBean.class);

        // Commit the changes
        ModificationProxy.handler(result).commit();

        // The changes should not have been committed to either bean
        assertThat(bean.getValue(), equalTo("Mr. Bean"));
        assertThat(bean.getListValue(), contains("Uhh", "Bean"));
        assertThat(newBean.getValue(), equalTo("Edmond Blackadder"));
        assertThat(newBean.getListValue(), contains("Cunning", "Plan"));
    }

    /** Matches a modification proxy wrapping an object matching the given matcher */
    public static <T> Matcher<T> modProxy(Matcher<T> objectMatcher) {
        return new BaseMatcher<T>() {

            @Override
            public boolean matches(Object item) {
                ModificationProxy handler = ModificationProxy.handler(item);
                if (handler == null) {
                    return false;
                }
                return objectMatcher.matches(handler.getProxyObject());
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("ModificationProxy wrapping ")
                        .appendDescriptionOf(objectMatcher);
            }
        };
    }

    static interface TestBean {
        public String getValue();

        public void setValue(String value);

        public List<String> getListValue();

        public void setListValue(List<String> listValue);
    }

    static class TestBeanImpl implements TestBean {
        String value;
        List<String> listValue;

        public TestBeanImpl(String value, List<String> listValue) {
            super();
            this.value = value;
            this.listValue = new ArrayList<>(listValue);
        }

        public TestBeanImpl(String value, String... listValues) {
            this(value, Arrays.asList(listValues));
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public List<String> getListValue() {
            return listValue;
        }

        public void setListValue(List<String> listValue) {
            this.listValue = listValue;
        }
    }
}
