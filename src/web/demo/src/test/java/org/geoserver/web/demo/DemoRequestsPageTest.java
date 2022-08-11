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

package org.geoserver.web.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.platform.resource.Files;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.test.TestData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.SerializationUtils;

/**
 * @author Gabriel Roldan
 * @verion $Id$
 */
public class DemoRequestsPageTest extends GeoServerWicketTestSupport {

    private File demoDir;

    @Before
    public void setUp() throws Exception {
        demoDir = TestData.file(this, "demo-requests");
        tester.startPage(new DemoRequestsPage(Files.asResource(demoDir)));
    }

    /** Kind of smoke test to make sure the page structure was correctly set up once loaded */
    @Test
    public void testStructure() {
        // print(tester.getLastRenderedPage(), true, true);

        assertTrue(tester.getLastRenderedPage() instanceof DemoRequestsPage);

        tester.assertComponent("demoRequestsForm", Form.class);
        tester.assertComponent("demoRequestsForm:demoRequestsList", DropDownChoice.class);
        tester.assertComponent("demoRequestsForm:url", TextField.class);
        tester.assertComponent(
                "demoRequestsForm:body:editorContainer:editorParent:editor", TextArea.class);
        tester.assertComponent("demoRequestsForm:username", TextField.class);
        tester.assertComponent("demoRequestsForm:password", PasswordTextField.class);
        tester.assertComponent("demoRequestsForm:submit", AjaxSubmitLink.class);

        tester.assertComponent("responseWindow", ModalWindow.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDemoListLoaded() {
        // print(tester.getLastRenderedPage(), true, true);

        /*
         * Expected choices are the file names in the demo requests dir
         * (/src/test/resources/test-data/demo-requests in this case)
         */
        final List<String> expectedList =
                Arrays.asList(new String[] {"WFS_getFeature-1.1.xml", "WMS_describeLayer.url"});

        DropDownChoice dropDown =
                (DropDownChoice)
                        tester.getComponentFromLastRenderedPage(
                                "demoRequestsForm:demoRequestsList");
        List choices = dropDown.getChoices();
        assertEquals(expectedList, choices);
    }

    @Test
    public void testUrlLinkUnmodified() {
        // print(tester.getLastRenderedPage(), true, true);

        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        final String requestName = "WMS_describeLayer.url";
        requestFormTester.select("demoRequestsList", 1);

        /*
         * There's an AjaxFormSubmitBehavior attached to onchange so force it
         */
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

        final boolean isAjax = true;
        tester.clickLink("demoRequestsForm:submit", isAjax);

        tester.assertVisible("responseWindow");

        IModel model = tester.getLastRenderedPage().getDefaultModel();
        assertTrue(model.getObject() instanceof DemoRequest);
        DemoRequest req = (DemoRequest) model.getObject();

        assertEquals(Files.asResource(demoDir).path(), req.getDemoDir());
        String requestFileName = req.getRequestFileName();
        String requestUrl = req.getRequestUrl();
        String requestBody = req.getRequestBody();

        assertEquals(requestName, requestFileName);
        assertNotNull(requestUrl);
        assertNull(requestBody);
    }

    @Test
    public void testUrlLinkSelected() {
        // print(tester.getLastRenderedPage(), true, true);

        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        final String requestName = "WMS_describeLayer.url";

        requestFormTester.select("demoRequestsList", 1);

        /*
         * There's an AjaxFormSubmitBehavior attached to onchange so force it
         */
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

        final boolean isAjax = true;
        tester.clickLink("demoRequestsForm:submit", isAjax);

        tester.assertVisible("responseWindow");

        IModel model = tester.getLastRenderedPage().getDefaultModel();
        assertTrue(model.getObject() instanceof DemoRequest);
        DemoRequest req = (DemoRequest) model.getObject();

        assertEquals(Files.asResource(demoDir).path(), req.getDemoDir());
        String requestFileName = req.getRequestFileName();
        String requestUrl = req.getRequestUrl();
        String requestBody = req.getRequestBody();

        assertEquals(requestName, requestFileName);
        assertNotNull(requestUrl);
        assertNull(requestBody);
    }

    @Test
    public void testUrlLinkModified() {
        // print(tester.getLastRenderedPage(), true, true);

        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        final String requestName = "WMS_describeLayer.url";

        requestFormTester.select("demoRequestsList", 1);

        /*
         * There's an AjaxFormSubmitBehavior attached to onchange so force it
         */
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

        final String modifiedUrl = "http://modified/url";

        TextField url = (TextField) tester.getComponentFromLastRenderedPage("demoRequestsForm:url");
        url.setModelValue(new String[] {modifiedUrl});

        assertEquals(modifiedUrl, url.getValue());

        final boolean isAjax = true;
        tester.clickLink("demoRequestsForm:submit", isAjax);

        tester.assertVisible("responseWindow");

        IModel model = tester.getLastRenderedPage().getDefaultModel();
        assertTrue(model.getObject() instanceof DemoRequest);
        DemoRequest req = (DemoRequest) model.getObject();

        String requestUrl = req.getRequestUrl();
        assertEquals(modifiedUrl, requestUrl);
    }

    @Test
    public void testProxyBaseUrl() {
        // setup the proxy base url
        GeoServerInfo global = getGeoServer().getGlobal();
        String proxyBaseUrl = "http://www.geoserver.org/test_gs";
        global.getSettings().setProxyBaseUrl(proxyBaseUrl);
        try {
            getGeoServer().save(global);

            final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");
            final String requestName = "WMS_describeLayer.url";
            requestFormTester.select("demoRequestsList", 1);

            /*
             * There's an AjaxFormSubmitBehavior attached to onchange so force it
             */
            tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");
            tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

            final boolean isAjax = true;
            tester.clickLink("demoRequestsForm:submit", isAjax);

            tester.assertVisible("responseWindow");

            IModel model = tester.getLastRenderedPage().getDefaultModel();
            assertTrue(model.getObject() instanceof DemoRequest);
            DemoRequest req = (DemoRequest) model.getObject();

            assertEquals(Files.asResource(demoDir).path(), req.getDemoDir());
            String requestFileName = req.getRequestFileName();
            String requestUrl = req.getRequestUrl();

            assertEquals(requestName, requestFileName);
            assertTrue(requestUrl.startsWith(proxyBaseUrl + "/wms"));
        } finally {
            global.getSettings().setProxyBaseUrl(null);
            getGeoServer().save(global);
        }
    }

    @Test
    public void testAuthentication() {
        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        final String requestName = "WMS_describeLayer.url";
        requestFormTester.select("demoRequestsList", 1);

        /*
         * There's an AjaxFormSubmitBehavior attached to onchange so force it
         */
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");
        tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

        String username = "admin";
        String password = "geoserver";

        requestFormTester.setValue("username", username);
        requestFormTester.setValue("password", password);

        final boolean isAjax = true;
        tester.clickLink("demoRequestsForm:submit", isAjax);

        tester.assertVisible("responseWindow");

        IModel model = tester.getLastRenderedPage().getDefaultModel();
        assertTrue(model.getObject() instanceof DemoRequest);

        assertEquals(
                username,
                tester.getLastRequest()
                        .getPostParameters()
                        .getParameterValue("username")
                        .toString());
        assertEquals(
                password,
                tester.getLastRequest()
                        .getPostParameters()
                        .getParameterValue("password")
                        .toString());
    }

    @Test
    public void testSerializable() {
        DemoRequestsPage page = new DemoRequestsPage();
        DemoRequestsPage page2 =
                (DemoRequestsPage)
                        SerializationUtils.deserialize(SerializationUtils.serialize(page));
        assertEquals(page.demoDir, page2.demoDir);
    }
}
