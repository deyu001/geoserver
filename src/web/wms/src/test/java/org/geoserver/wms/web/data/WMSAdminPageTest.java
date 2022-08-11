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

package org.geoserver.wms.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.web.WMSAdminPage;
import org.junit.Before;
import org.junit.Test;

public class WMSAdminPageTest extends GeoServerWicketTestSupport {

    private WMSInfo wms;

    @Before
    public void setUp() throws Exception {
        wms = getGeoServerApplication().getGeoServer().getService(WMSInfo.class);
        login();
    }

    @Test
    public void testValues() throws Exception {
        tester.startPage(WMSAdminPage.class);
        tester.assertModelValue("form:keywords", wms.getKeywords());
        tester.assertModelValue("form:srs", new ArrayList<String>());
    }

    @Test
    public void testFormSubmit() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }

    @Test
    public void testWatermarkLocalFile() throws Exception {
        File f = new File(getClass().getResource("GeoServer_75.png").toURI());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("watermark.uRL", f.getAbsolutePath());
        ft.submit("submit");
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
    }

    @Test
    public void testFormInvalid() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("srs", "bla");
        ft.submit("submit");
        List errors = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, errors.size());
        assertTrue(
                ((ValidationErrorFeedback) errors.get(0)).getMessage().toString().contains("bla"));
        tester.assertRenderedPage(WMSAdminPage.class);
    }

    @Test
    public void testBBOXForEachCRS() throws Exception {
        assertFalse(wms.isBBOXForEachCRS());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("bBOXForEachCRS", true);
        ft.submit("submit");
        assertTrue(wms.isBBOXForEachCRS());
    }

    @Test
    public void testRootLayerRemove() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("rootLayerEnabled", false);
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getMetadata().get(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY), false);
    }

    @Test
    public void testRootLayerTitle() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("rootLayerTitle", "test");
        ft.setValue("rootLayerAbstract", "abstract test");
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getRootLayerTitle(), "test");
        assertEquals(wms.getRootLayerAbstract(), "abstract test");
    }

    @Test
    public void testDensification() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("aph.densify", true);
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getMetadata().get(WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY), true);
    }

    @Test
    public void testDisableWrappingHeuristic() throws Exception {
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("aph.dlh", true);
        ft.submit("submit");
        tester.assertNoErrorMessage();
        assertEquals(wms.getMetadata().get(WMS.DATELINE_WRAPPING_HEURISTIC_KEY), true);
    }

    @Test
    public void testDynamicStylingDisabled() throws Exception {
        assertFalse(wms.isDynamicStylingDisabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("dynamicStyling.disabled", true);
        ft.submit("submit");
        assertTrue(wms.isDynamicStylingDisabled());
    }

    @Test
    public void testCacheConfiguration() throws Exception {
        assertFalse(wms.getCacheConfiguration().isEnabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("cacheConfiguration.enabled", true);
        ft.submit("submit");
        assertTrue(wms.getCacheConfiguration().isEnabled());
    }

    @Test
    public void testFeaturesReprojectionDisabled() throws Exception {
        assertFalse(wms.isFeaturesReprojectionDisabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("disableFeaturesReproject", true);
        ft.submit("submit");
        assertTrue(wms.isFeaturesReprojectionDisabled());
    }

    @Test
    public void testIncludeDefaultGroupStyleInCapabilitiesDisabled() throws Exception {
        assertTrue(wms.isDefaultGroupStyleEnabled());
        tester.startPage(WMSAdminPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("defaultGroupStyleEnabled", false);
        ft.submit("submit");
        assertFalse(wms.isDefaultGroupStyleEnabled());
    }
}
