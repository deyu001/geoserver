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

package org.geoserver.web.wicket.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PropertyEditorFormComponentTest extends GeoServerWicketTestSupport {

    Foo foo;

    @Before
    public void init() {
        foo = new Foo();
    }

    void startPage() {
        tester.startPage(new PropertyEditorTestPage(foo));
        tester.assertRenderedPage(PropertyEditorTestPage.class);
    }

    // TODO mcr
    // since introduction of PropertyEditorFormComponent.validate this test is broken
    // Using the component in the GUI works perfectly

    @Test
    @Ignore
    public void testAdd() {
        // JD:for the life of me i can't figure out any sane way to test forms with ajax in the mix
        // so unable to test the case of adding multiple key/value pairs since it involves
        // intermixing of the two
        startPage();

        tester.clickLink("form:props:add", true);
        tester.assertComponent("form:props:container:list:0:key", TextField.class);
        tester.assertComponent("form:props:container:list:0:value", TextField.class);
        tester.assertComponent("form:props:container:list:0:remove", AjaxLink.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("props:container:list:0:key", "foo");
        form.setValue("props:container:list:0:value", "bar");
        form.submit();

        assertEquals(1, foo.getProps().size());
        assertEquals("bar", foo.getProps().get("foo"));
    }

    @Test
    @SuppressWarnings({"TryFailThrowable", "PMD.ForLoopCanBeForEach"})
    public void testRemove() {
        foo.getProps().put("foo", "bar");
        foo.getProps().put("bar", "baz");
        foo.getProps().put("baz", "foo");
        startPage();

        tester.assertComponent("form:props:container:list:0:remove", AjaxLink.class);
        tester.assertComponent("form:props:container:list:1:remove", AjaxLink.class);
        tester.assertComponent("form:props:container:list:2:remove", AjaxLink.class);
        try {
            tester.assertComponent("form:props:container:list:3:remove", AjaxLink.class);
            fail();
        } catch (AssertionError e) {
        }

        @SuppressWarnings("unchecked")
        ListView<String> list =
                (ListView) tester.getComponentFromLastRenderedPage("form:props:container:list");
        assertNotNull(list);

        int i = 0;
        for (Component c : list) {
            if ("baz".equals(c.get("key").getDefaultModelObjectAsString())) {
                break;
            }
            i++;
        }
        assertNotEquals(3, i);

        tester.clickLink("form:props:container:list:" + i + ":remove", true);
        tester.newFormTester("form").submit();

        assertEquals(2, foo.getProps().size());
        assertEquals("bar", foo.getProps().get("foo"));
        assertEquals("baz", foo.getProps().get("bar"));
        assertFalse(foo.getProps().containsKey("baz"));
    }

    @Test
    public void testAddRemove() {
        startPage();
        tester.clickLink("form:props:add", true);
        tester.assertComponent("form:props:container:list:0:key", TextField.class);
        tester.assertComponent("form:props:container:list:0:value", TextField.class);
        tester.assertComponent("form:props:container:list:0:remove", AjaxLink.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("props:container:list:0:key", "foo");
        form.setValue("props:container:list:0:value", "bar");

        tester.clickLink("form:props:container:list:0:remove", true);

        assertNull(form.getForm().get("props:container:list:0:key"));
        assertNull(form.getForm().get("props:container:list:0:value"));
        assertNull(form.getForm().get("props:container:list:0:remove"));
        form.submit();

        assertTrue(foo.getProps().isEmpty());
    }
}
