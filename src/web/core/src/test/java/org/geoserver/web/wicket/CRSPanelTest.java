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

package org.geoserver.web.wicket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CRSPanelTest extends GeoServerWicketTestSupport {

    @Test
    public void testStandloneUnset() throws Exception {
        tester.startPage(new CRSPanelTestPage());

        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:crs", CRSPanel.class);

        FormTester ft = tester.newFormTester("form");
        ft.submit();

        CRSPanel crsPanel = (CRSPanel) tester.getComponentFromLastRenderedPage("form:crs");
        assertNull(crsPanel.getCRS());
    }

    @Test
    public void testStandaloneUnchanged() throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        tester.startPage(new CRSPanelTestPage(crs));
        // print(new CRSPanelTestPage(crs), true, true);

        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:crs", CRSPanel.class);

        FormTester ft = tester.newFormTester("form", false);
        ft.submit();

        CRSPanel crsPanel = (CRSPanel) tester.getComponentFromLastRenderedPage("form:crs");
        assertTrue(CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, crsPanel.getCRS()));
    }

    @Test
    public void testPopupWindow() throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        tester.startPage(new CRSPanelTestPage(crs));

        ModalWindow window =
                (ModalWindow) tester.getComponentFromLastRenderedPage("form:crs:popup");
        assertFalse(window.isShown());

        tester.clickLink("form:crs:wkt", true);
        assertTrue(window.isShown());

        tester.assertModelValue("form:crs:popup:content:wkt", crs.toWKT());
    }

    @Test
    public void testPopupWindowNoCRS() throws Exception {
        // see GEOS-3207
        tester.startPage(new CRSPanelTestPage());

        ModalWindow window =
                (ModalWindow) tester.getComponentFromLastRenderedPage("form:crs:popup");
        assertFalse(window.isShown());

        GeoServerAjaxFormLink link =
                (GeoServerAjaxFormLink) tester.getComponentFromLastRenderedPage("form:crs:wkt");
        assertFalse(link.isEnabled());
    }

    @Test
    public void testStandaloneChanged() throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        tester.startPage(new CRSPanelTestPage(crs));

        @SuppressWarnings("unchecked")
        TextField<String> srs = (TextField) tester.getComponentFromLastRenderedPage("form:crs:srs");
        srs.setModelObject("EPSG:3005");

        FormTester ft = tester.newFormTester("form", false);
        ft.submit();

        CRSPanel crsPanel = (CRSPanel) tester.getComponentFromLastRenderedPage("form:crs");
        assertTrue(CRS.equalsIgnoreMetadata(CRS.decode("EPSG:3005"), crsPanel.getCRS()));
    }

    @Test
    public void testStandaloneChanged2() throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        tester.startPage(new CRSPanelTestPage(crs));
        // write down the text, submit the form
        FormTester ft = tester.newFormTester("form");
        ft.setValue("crs:srs", "EPSG:3005");
        ft.submit();
        tester.assertNoErrorMessage();
        CRSPanel crsPanel = (CRSPanel) tester.getComponentFromLastRenderedPage("form:crs");
        assertTrue(CRS.equalsIgnoreMetadata(CRS.decode("EPSG:3005"), crsPanel.getCRS()));
    }

    @Test
    public void testRequired() throws Exception {
        tester.startPage(new CRSPanelTestPage((CoordinateReferenceSystem) null));
        CRSPanel panel = (CRSPanel) tester.getComponentFromLastRenderedPage("form:crs");
        panel.setRequired(true);

        FormTester ft = tester.newFormTester("form");
        ft.submit();

        assertEquals(1, panel.getFeedbackMessages().size());
        // System.out.println(Session.get().getFeedbackMessages().messageForComponent(panel));
    }

    @Test
    public void testCompoundPropertyUnchanged() throws Exception {
        Foo foo = new Foo(DefaultGeographicCRS.WGS84);
        tester.startPage(new CRSPanelTestPage(foo));

        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:crs", CRSPanel.class);

        FormTester ft = tester.newFormTester("form");
        ft.submit();

        assertEquals(CRS.decode("EPSG:4326"), foo.crs);
    }

    @Test
    public void testCompoundPropertyChanged() throws Exception {
        Foo foo = new Foo(DefaultGeographicCRS.WGS84);
        tester.startPage(new CRSPanelTestPage(foo));

        @SuppressWarnings("unchecked")
        TextField<String> srs = (TextField) tester.getComponentFromLastRenderedPage("form:crs:srs");
        srs.setModelObject("EPSG:3005");

        FormTester ft = tester.newFormTester("form");
        ft.submit();

        assertEquals(CRS.decode("EPSG:3005"), foo.crs);
    }

    @Test
    public void testPropertyUnchanged() throws Exception {
        Foo foo = new Foo(DefaultGeographicCRS.WGS84);
        tester.startPage(new CRSPanelTestPage(new PropertyModel<>(foo, "crs")));

        tester.assertComponent("form", Form.class);
        tester.assertComponent("form:crs", CRSPanel.class);

        FormTester ft = tester.newFormTester("form");
        ft.submit();

        assertEquals(CRS.decode("EPSG:4326"), foo.crs);
    }

    @Test
    public void testPropertyChanged() throws Exception {
        Foo foo = new Foo(DefaultGeographicCRS.WGS84);
        tester.startPage(new CRSPanelTestPage(new PropertyModel<>(foo, "crs")));

        @SuppressWarnings("unchecked")
        TextField<String> srs = (TextField) tester.getComponentFromLastRenderedPage("form:crs:srs");
        srs.setModelObject("EPSG:3005");

        FormTester ft = tester.newFormTester("form");
        ft.submit();

        assertEquals(CRS.decode("EPSG:3005"), foo.crs);
    }

    static class Foo implements Serializable {
        public CoordinateReferenceSystem crs;

        Foo(CoordinateReferenceSystem crs) {
            this.crs = crs;
        }
    }
}
