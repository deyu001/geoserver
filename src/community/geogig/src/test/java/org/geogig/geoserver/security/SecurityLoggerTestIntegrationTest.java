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

package org.geogig.geoserver.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.config.LogEvent;
import org.geogig.geoserver.config.LogEvent.Severity;
import org.geogig.geoserver.config.LogStore;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;

// SecurityLogger has been disabled in the plugin, ignore.
@Ignore
@TestSetup(run = TestSetupFrequency.REPEAT)
public class SecurityLoggerTestIntegrationTest extends GeoServerSystemTestSupport {

    /** {@code /geogig/repos/<repoId>} */
    private String BASE_URL;

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    private LogStore logStore;

    private URI repoURL;

    /** Override to avoid creating default geoserver test data */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do nothing
    }

    @Before
    public void before() throws Exception {
        // protected void onSetUp(SystemTestData testData) throws Exception {

        geogigData
                .init() //
                .config("user.name", "gabriel") //
                .config("user.email", "gabriel@test.com") //
                .createTypeTree("lines", "geom:LineString:srid=4326") //
                .createTypeTree("points", "geom:Point:srid=4326") //
                .add() //
                .commit("created type trees") //
                .get();

        RepositoryManager repositoryManager = RepositoryManager.get();

        RepositoryInfo info = new RepositoryInfo();
        repoURL = geogigData.repoDirectory().getAbsoluteFile().toURI();
        info.setLocation(repoURL);
        info = repositoryManager.save(info);

        BASE_URL = "/geogig/repos/testrepo";

        logStore = GeoServerExtensions.bean(LogStore.class);
        assertNotNull(logStore);

        SecurityLogger logger = GeoServerExtensions.bean(SecurityLogger.class);
        assertNotNull(logger);
    }

    @After
    public void after() {
        RepositoryManager.close();
    }

    private void login() throws Exception {
        super.login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRemoteAdd() throws Exception {
        String remoteURL = "http://example.com/geogig/upstream";
        final String url = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        Document dom = getAsDOM(url);
        // <response><success>true</success><name>upstream</name></response>
        assertXpathEvaluatesTo("true", "/response/success", dom);

        List<LogEvent> entries = new ArrayList<>(logStore.getLogEntries(0, 10));
        assertEquals(entries.toString(), 2, entries.size());
        LogEvent first = entries.get(1);

        assertEquals(Severity.DEBUG, first.getSeverity());
        assertEquals("anonymous", first.getUser());
        assertEquals(repoURL.toString(), first.getRepositoryURL());
        assertTrue(first.getMessage(), first.getMessage().contains("Remote add:"));
        assertTrue(first.getMessage(), first.getMessage().contains("name='upstream'"));

        LogEvent second = entries.get(0);
        assertEquals(Severity.INFO, second.getSeverity());
        assertEquals("anonymous", second.getUser());
        assertEquals(repoURL.toString(), second.getRepositoryURL());
        assertTrue(first.getMessage(), second.getMessage().contains("Remote add success"));
        assertTrue(first.getMessage(), second.getMessage().contains("name='upstream'"));
    }

    @Test
    public void testRemoteAddExisting() throws Exception {
        String remoteURL = "http://example.com/geogig/upstream";
        final String url = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        Document dom = getAsDOM(url);
        assertXpathEvaluatesTo("true", "/response/success", dom);

        dom = getAsDOM(url);
        assertXpathEvaluatesTo("false", "/response/success", dom);

        List<LogEvent> entries = new ArrayList<>(logStore.getLogEntries(0, 10));
        assertTrue(entries.toString(), entries.size() > 0);
        LogEvent last = entries.get(0);

        assertEquals(Severity.ERROR, last.getSeverity());
        assertEquals("anonymous", last.getUser());
        assertEquals(repoURL.toString(), last.getRepositoryURL());
        assertTrue(last.getMessage(), last.getMessage().contains("Remote add failed"));
        assertTrue(last.getMessage(), last.getMessage().contains("name='upstream'"));
        assertTrue(last.getMessage(), last.getMessage().contains("REMOTE_ALREADY_EXISTS"));
    }

    @Test
    public void testUserLogged() throws Exception {
        login();
        super.setRequestAuth("admin", "geoserver");

        String remoteURL = "http://example.com/geogig/upstream";
        final String url = BASE_URL + "/remote?remoteName=upstream&remoteURL=" + remoteURL;
        Document dom = getAsDOM(url);
        print(dom);
        assertXpathEvaluatesTo("true", "/response/success", dom);

        List<LogEvent> entries = new ArrayList<>(logStore.getLogEntries(0, 10));
        assertTrue(entries.size() > 0);
        for (LogEvent e : entries) {
            assertEquals("admin", e.getUser());
        }
    }
}
