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

package org.geoserver.wps.web;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.web.wicket.KeywordsEditor;
import org.geoserver.wps.WPSInfo;
import org.junit.Test;

public class WPSAdminPageTest extends WPSPagesTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        wps.setMaxAsynchronousExecutionTime(600);
        wps.setMaxSynchronousExecutionTime(60);
        wps.setMaxSynchronousProcesses(16);
        wps.setMaxAsynchronousProcesses(16);
        getGeoServer().save(wps);
    }

    @Test
    public void test() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSAdminPage());
        // print(tester.getLastRenderedPage(), true, true);

        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        wps.setMaxAsynchronousTotalTime(6000);
        wps.setMaxSynchronousTotalTime(120);
        getGeoServer().save(wps);

        // test that components have been filled as expected
        tester.assertComponent("form:keywords", KeywordsEditor.class);
        tester.assertModelValue("form:keywords", wps.getKeywords());
        tester.assertModelValue("form:maxSynchronousProcesses:", 16);
        tester.assertModelValue("form:maxAsynchronousProcesses:", 16);
        tester.assertModelValue("form:maxSynchronousExecutionTime:", 60);
        tester.assertModelValue("form:maxAsynchronousExecutionTime:", 600);
        tester.assertModelValue("form:maxSynchronousTotalTime:", 120);
        tester.assertModelValue("form:maxAsynchronousTotalTime:", 6000);
    }

    @Test
    public void testUpgrade() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSAdminPage());

        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        getGeoServer().save(wps);

        // test that components have been filled as expected
        tester.assertModelValue("form:maxSynchronousExecutionTime:", 60);
        tester.assertModelValue("form:maxAsynchronousExecutionTime:", 600);
        tester.assertModelValue("form:maxSynchronousTotalTime:", 60);
        tester.assertModelValue("form:maxAsynchronousTotalTime:", 600);
    }

    @Test
    public void testWorkspace() throws Exception {
        GeoServer geoServer = getGeoServerApplication().getGeoServer();
        WPSInfo wps = geoServer.getService(WPSInfo.class);
        WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();
        WPSInfo wps2 = geoServer.getFactory().create(WPSInfo.class);
        OwsUtils.copy(wps, wps2, WPSInfo.class);
        ((ServiceInfoImpl) wps2).setId(null);
        wps2.setWorkspace(defaultWs);
        wps2.setMaintainer("TestMaintainer");
        geoServer.add(wps2);

        // start the page with the custom workspace
        login();
        tester.startPage(
                WPSAdminPage.class, new PageParameters().add("workspace", defaultWs.getName()));
        // print(tester.getLastRenderedPage(), true, true, true);

        // test that components have been filled as expected
        tester.assertModelValue("form:maintainer", "TestMaintainer");
    }
}
