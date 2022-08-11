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

package org.geoserver.geofence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.config.GeoFencePropertyPlaceholderConfigurer;
import org.geoserver.geofence.utils.GeofenceTestUtils;
import org.geoserver.geofence.web.GeofencePage;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.UrlResource;

public class GeofencePageTest extends GeoServerWicketTestSupport {

    static GeoFencePropertyPlaceholderConfigurer configurer;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // get the beans we use for testing
        configurer =
                (GeoFencePropertyPlaceholderConfigurer)
                        applicationContext.getBean("geofence-configurer");
        configurer.setLocation(
                new UrlResource(this.getClass().getResource("/test-config.properties")));
    }

    @Before
    public void before() {
        login();
        tester.startPage(GeofencePage.class);
    }

    /** @FIXME This test fails in 2.6 */
    @Test
    public void testSave() throws URISyntaxException, IOException {
        GeofenceTestUtils.emptyFile("test-config.properties");
        FormTester ft = tester.newFormTester("form");
        ft.submit("submit");
        tester.assertRenderedPage(GeoServerHomePage.class);

        File configFile = configurer.getConfigFile().file();
        LOGGER.info("Config file is " + configFile);

        assertTrue(GeofenceTestUtils.readConfig(configFile).length() > 0);
    }

    /** @FIXME This test fails in 2.6 */
    @Test
    public void testCancel() throws URISyntaxException, IOException {
        GeofenceTestUtils.emptyFile("test-config.properties");
        // GeofenceTestUtils.emptyFile("test-cache-config.properties");
        FormTester ft = tester.newFormTester("form");
        ft.submit("cancel");
        tester.assertRenderedPage(GeoServerHomePage.class);
        assertEquals(0, GeofenceTestUtils.readConfig("test-config.properties").length());
        // assertTrue(GeofenceTestUtils.readConfig("test-cache-config.properties").length() == 0);
    }

    @Test
    public void testErrorEmptyInstance() {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("instanceName", "");
        ft.submit("submit");
        tester.assertRenderedPage(GeofencePage.class);

        tester.assertContains("is required");
    }

    @Test
    public void testErrorEmptyURL() {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("servicesUrl", "");
        ft.submit("submit");
        tester.assertRenderedPage(GeofencePage.class);

        tester.assertContains("is required");
    }

    @Test
    public void testErrorWrongURL() {
        @SuppressWarnings("unchecked")
        TextField<String> servicesUrl =
                ((TextField<String>) tester.getComponentFromLastRenderedPage("form:servicesUrl"));
        servicesUrl.setDefaultModel(new Model<>("fakeurl"));

        tester.clickLink("form:test", true);

        tester.assertContains("RemoteAccessException");
    }

    @Test
    public void testErrorEmptyCacheSize() {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("cacheSize", "");
        ft.submit("submit");
        tester.assertRenderedPage(GeofencePage.class);

        tester.assertContains("is required");
    }

    @Test
    public void testErrorWrongCacheSize() {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("cacheSize", "A");
        ft.submit("submit");
        tester.assertRenderedPage(GeofencePage.class);

        tester.assertContains("long");
    }

    @Test
    public void testInvalidateCache() {
        tester.clickLink("form:invalidate", true);
        String success =
                new StringResourceModel(GeofencePage.class.getSimpleName() + ".cacheInvalidated")
                        .getObject();
        tester.assertInfoMessages(new String[] {success});
    }
}
