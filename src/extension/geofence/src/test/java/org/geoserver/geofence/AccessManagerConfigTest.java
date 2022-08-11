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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.config.GeoFencePropertyPlaceholderConfigurer;
import org.geoserver.geofence.utils.GeofenceTestUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.springframework.core.io.UrlResource;

public class AccessManagerConfigTest extends GeoServerTestSupport {

    // protected GeofenceAccessManager manager;
    // protected RuleReaderService geofenceService;
    GeoFencePropertyPlaceholderConfigurer configurer;

    GeoFenceConfigurationManager manager;

    @Override
    protected void oneTimeSetUp() throws Exception {
        try {
            super.oneTimeSetUp();
        } catch (Exception e) {
            LOGGER.severe(
                    "Error in OneTimeSetup: it may be due to GeoFence not running, please check the logs -- "
                            + e.getMessage());
            LOGGER.log(
                    Level.FINE,
                    "Error in OneTimeSetup: it may be due to GeoFence not running, please check the logs",
                    e);
        }

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        // get the beans we use for testing
        // manager = (GeofenceAccessManager)
        // applicationContext.getBean("geofenceRuleAccessManager");
        // geofenceService = (RuleReaderService) applicationContext.getBean("ruleReaderService");
        manager =
                (GeoFenceConfigurationManager)
                        applicationContext.getBean("geofenceConfigurationManager");

        configurer =
                (GeoFencePropertyPlaceholderConfigurer)
                        applicationContext.getBean("geofence-configurer");
        configurer.setLocation(
                new UrlResource(this.getClass().getResource("/test-config.properties")));
    }

    @Test
    public void testSave() throws IOException, URISyntaxException {
        GeofenceTestUtils.emptyFile("test-config.properties");

        GeoFenceConfiguration config = new GeoFenceConfiguration();
        config.setInstanceName("TEST_INSTANCE");
        config.setServicesUrl("http://fakeservice");
        config.setAllowRemoteAndInlineLayers(true);
        config.setGrantWriteToWorkspacesToAuthenticatedUsers(true);
        config.setUseRolesToFilter(true);
        config.setAcceptedRoles("A,B");

        manager.setConfiguration(config);

        Resource configurationFile = configurer.getConfigFile();

        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(configurationFile.out()))) {
            writer.write("newUserProperty=custom_property_value\n");
        }

        manager.storeConfiguration();

        File configFile = configurer.getConfigFile().file();
        LOGGER.info("Config file is " + configFile);

        String content = GeofenceTestUtils.readConfig(configFile);
        assertTrue(content.contains("fakeservice"));
        assertTrue(content.contains("TEST_INSTANCE"));
        assertFalse(content.contains("custom_property_value"));
    }
}
