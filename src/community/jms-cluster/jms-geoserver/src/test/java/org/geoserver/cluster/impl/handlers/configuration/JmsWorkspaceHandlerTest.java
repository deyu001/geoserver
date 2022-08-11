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

package org.geoserver.cluster.impl.handlers.configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.cluster.impl.events.configuration.JMSEventType;
import org.geoserver.cluster.impl.events.configuration.JMSSettingsModifyEvent;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JmsWorkspaceHandlerTest extends GeoServerSystemTestSupport {

    private WorkspaceInfo testWorkspace;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void setup() {
        // create a test workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("jms-test-workspace");
        workspace.setName("jms-test-workspace");
        getCatalog().add(workspace);
        testWorkspace = workspace;
    }

    @After
    public void clean() {
        // remove test workspace
        getCatalog().remove(getCatalog().getWorkspace("jms-test-workspace"));
    }

    @Test
    public void testSettingsSimpleCrud() throws Exception {
        // settings events handler
        JMSSettingsHandler handler = createHandler();
        // create a new settings
        handler.synchronize(createNewSettingsEvent("settings1", "settings1"));
        checkSettingsExists("settings1");
        // update settings
        handler.synchronize(createModifySettingsEvent("settings2"));
        checkSettingsExists("settings2");
        // delete settings
        handler.synchronize(createRemoveSettings());
        assertThat(getGeoServer().getSettings(testWorkspace), nullValue());
    }

    private void checkSettingsExists(String settingsTile) {
        SettingsInfo settingsInfo = getGeoServer().getSettings(testWorkspace);
        assertThat(settingsInfo, notNullValue());
        assertThat(settingsInfo.getTitle(), is(settingsTile));
    }

    private JMSSettingsModifyEvent createNewSettingsEvent(String settingsId, String settingsTitle) {
        // our settings information
        SettingsInfoImpl settingsInfo = new SettingsInfoImpl();
        settingsInfo.setId(settingsId);
        settingsInfo.setTitle(settingsTitle);
        settingsInfo.setWorkspace(testWorkspace);
        // create jms settings modify event
        return new JMSSettingsModifyEvent(settingsInfo, JMSEventType.ADDED);
    }

    private JMSSettingsModifyEvent createModifySettingsEvent(String newSettingsTitle) {
        // settings information
        SettingsInfo settingsInfo = getGeoServer().getSettings(testWorkspace);
        String oldSettingsTitle = settingsInfo.getTitle();
        settingsInfo.setTitle(newSettingsTitle);
        // create jms settings modify event
        return new JMSSettingsModifyEvent(
                settingsInfo,
                Collections.singletonList("title"),
                Collections.singletonList(oldSettingsTitle),
                Collections.singletonList(newSettingsTitle),
                JMSEventType.MODIFIED);
    }

    private JMSSettingsModifyEvent createRemoveSettings() {
        // our settings information
        SettingsInfo settingsInfo = getGeoServer().getSettings(testWorkspace);
        // create jms settings modify event
        return new JMSSettingsModifyEvent(settingsInfo, JMSEventType.REMOVED);
    }

    private JMSSettingsHandler createHandler() {
        JMSSettingsHandlerSPI handlerSpi = GeoServerExtensions.bean(JMSSettingsHandlerSPI.class);
        return (JMSSettingsHandler) handlerSpi.createHandler();
    }
}
