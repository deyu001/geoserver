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

package org.geoserver.security.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractListPageTest<T> extends AbstractSecurityWicketTestSupport {

    public static final String ITEMS_PATH = "table:listContainer:items";
    public static final String FIRST_COLUM_PATH = "itemProperties:0:component:link";

    @Before
    public void setUp() throws Exception {
        login();
    }

    @Test
    public void testRenders() throws Exception {
        initializeForXML();
        tester.startPage(listPage(null));
        tester.assertRenderedPage(listPage(null).getClass());
    }

    protected abstract Page listPage(PageParameters params);

    protected abstract Page newPage(Object... params);

    protected abstract Page editPage(Object... params);

    protected abstract String getSearchString() throws Exception;

    protected abstract Property<T> getEditProperty();

    protected abstract boolean checkEditForm(String search);

    @Test
    public void testEdit() throws Exception {
        // the name link for the first user
        initializeForXML();
        // insertValues();

        tester.startPage(listPage(null));

        String search = getSearchString();
        assertNotNull(search);
        Component c = getFromList(FIRST_COLUM_PATH, search, getEditProperty());
        assertNotNull(c);
        tester.clickLink(c.getPageRelativePath());

        tester.assertRenderedPage(editPage().getClass());
        assertTrue(checkEditForm(search));
    }

    protected Component getFromList(String columnPath, Object columnValue, Property<T> property) {
        MarkupContainer listView = (MarkupContainer) tester.getLastRenderedPage().get(ITEMS_PATH);

        @SuppressWarnings("unchecked")
        Iterator<Component> it = listView.iterator();

        while (it.hasNext()) {
            Component container = it.next();
            Component c = container.get(columnPath);
            @SuppressWarnings("unchecked")
            T modelObject = (T) c.getDefaultModelObject();
            if (columnValue.equals(property.getPropertyValue(modelObject))) return c;
        }
        return null;
    }

    @Test
    public void testNew() throws Exception {
        initializeForXML();
        tester.startPage(listPage(null));
        tester.clickLink("headerPanel:addNew");
        Page newPage = tester.getLastRenderedPage();
        tester.assertRenderedPage(newPage.getClass());
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        insertValues();
        addAdditonalData();
        doRemove("headerPanel:removeSelected");
    }

    protected void doRemove(String pathForLink) throws Exception {
        tester.startPage(listPage(null));

        String selectAllPath = "table:listContainer:selectAllContainer:selectAll";
        tester.assertComponent(selectAllPath, CheckBox.class);
        CheckBox selectAllComponent =
                (CheckBox) tester.getComponentFromLastRenderedPage(selectAllPath);

        setFormComponentValue(selectAllComponent, "true");
        tester.executeAjaxEvent(selectAllPath, "click");

        ModalWindow w = (ModalWindow) tester.getLastRenderedPage().get("dialog:dialog");
        assertNull(w.getTitle()); // window was not opened
        tester.executeAjaxEvent(pathForLink, "click");
        assertNotNull(w.getTitle()); // window was opened
        simulateDeleteSubmit();
        executeModalWindowCloseButtonCallback(w);
    }

    protected abstract void simulateDeleteSubmit() throws Exception;

    protected Component getRemoveLink() {
        Component result = tester.getLastRenderedPage().get("headerPanel:removeSelected");
        assertNotNull(result);
        return result;
    }

    protected Component getRemoveLinkWithRoles() {
        Component result = tester.getLastRenderedPage().get("headerPanel:removeSelectedWithRoles");
        assertNotNull(result);
        return result;
    }

    protected Component getAddLink() {
        Component result = tester.getLastRenderedPage().get("headerPanel:addNew");
        assertNotNull(result);
        return result;
    }
}
