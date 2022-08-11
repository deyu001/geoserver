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

package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.IOException;
import org.geoserver.backuprestore.tasklet.CatalogBackupRestoreTasklet;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.backuprestore.writer.ResourceInfoAdditionalResourceWriter;
import org.geoserver.backuprestore.writer.StyleInfoAdditionalResourceWriter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.junit.Before;
import org.junit.Test;

/** @author Alessio Fabiani, GeoSolutions */
public class ResourceWriterTest extends BackupRestoreTestSupport {

    protected static Backup backupFacade;

    @Before
    public void beforeTest() throws InterruptedException {
        backupFacade = (Backup) applicationContext.getBean("backupFacade");
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testResourceInfoAdditionalResourceWriter() throws IOException {
        Catalog cat = getCatalog();

        GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();
        GeoServerDataDirectory td = new GeoServerDataDirectory(root);

        Resource srcTemplatesDir = BackupUtils.dir(dd.get(Paths.BASE), "templates");
        File srcTitleFtl =
                Resources.createNewFile(
                        Files.asResource(new File(srcTemplatesDir.dir(), "title.ftl")));
        File srcHeaderFtl =
                Resources.createNewFile(
                        Files.asResource(
                                new File(
                                        Paths.toFile(
                                                dd.get(Paths.BASE).dir(),
                                                Paths.path("workspaces", "gs", "foo", "t1")),
                                        "header.ftl")));
        File srcFakeFtl =
                Resources.createNewFile(
                        Files.asResource(
                                new File(
                                        Paths.toFile(
                                                dd.get(Paths.BASE).dir(),
                                                Paths.path("workspaces", "gs", "foo", "t1")),
                                        "fake.ftl")));

        assertTrue(Resources.exists(Files.asResource(srcTitleFtl)));
        assertTrue(Resources.exists(Files.asResource(srcHeaderFtl)));
        assertTrue(Resources.exists(Files.asResource(srcFakeFtl)));

        FeatureTypeInfo ft = cat.getFeatureTypeByName("t1");

        assertNotNull(ft);

        ResourceInfoAdditionalResourceWriter riarw = new ResourceInfoAdditionalResourceWriter();
        riarw.writeAdditionalResources(backupFacade, td.get(Paths.BASE), ft);

        Resource trgTemplatesDir = BackupUtils.dir(td.get(Paths.BASE), "templates");

        assertTrue(Resources.exists(trgTemplatesDir));

        Resource trgTitleFtl = Files.asResource(new File(trgTemplatesDir.dir(), "title.ftl"));
        Resource trgHeaderFtl =
                Files.asResource(
                        new File(
                                Paths.toFile(
                                        td.get(Paths.BASE).dir(),
                                        Paths.path("workspaces", "gs", "foo", "t1")),
                                "header.ftl"));
        Resource trgFakeFtl =
                Files.asResource(
                        new File(
                                Paths.toFile(
                                        td.get(Paths.BASE).dir(),
                                        Paths.path("workspaces", "gs", "foo", "t1")),
                                "fake.ftl"));

        assertTrue(Resources.exists(trgTitleFtl));
        assertTrue(Resources.exists(trgHeaderFtl));
        assertTrue(!Resources.exists(trgFakeFtl));
    }

    @Test
    public void testStyleInfoAdditionalResourceWriter() throws IOException {
        GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();
        GeoServerDataDirectory td = new GeoServerDataDirectory(root);

        StyleInfo style = catalog.getStyleByName(StyleInfo.DEFAULT_POINT);

        StyleInfoAdditionalResourceWriter siarw = new StyleInfoAdditionalResourceWriter();
        siarw.writeAdditionalResources(backupFacade, td.get(Paths.BASE), style);

        Resource srcStylesDir = BackupUtils.dir(dd.get(Paths.BASE), "styles");
        Resource trgStylesDir = BackupUtils.dir(td.get(Paths.BASE), "styles");

        assertTrue(Resources.exists(srcStylesDir));
        assertTrue(Resources.exists(trgStylesDir));

        assertTrue(
                Resources.exists(
                        Files.asResource(new File(trgStylesDir.dir(), style.getFilename()))));
    }

    @Test
    public void testSidecarFilesWriter() throws Exception {
        CatalogBackupRestoreTasklet catalogTsklet = new CatalogBackupRestoreTasklet(backupFacade);

        File tmpDd = File.createTempFile("template", "tmp", new File("target"));
        tmpDd.delete();
        tmpDd.mkdir();

        GeoServerDataDirectory dd = new GeoServerDataDirectory(tmpDd);

        File tmpTd = File.createTempFile("template", "tmp", new File("target"));
        tmpTd.delete();
        tmpTd.mkdir();

        GeoServerDataDirectory td = new GeoServerDataDirectory(tmpTd);

        BackupUtils.extractTo(file("data.zip"), dd.get(Paths.BASE));

        // Backup other configuration bits, like images, palettes, user projections and so on...
        catalogTsklet.backupRestoreAdditionalResources(dd.getResourceStore(), td.get(Paths.BASE));

        assertTrue(Resources.exists(Files.asResource(new File(td.get(Paths.BASE).dir(), "demo"))));
        assertTrue(
                Resources.exists(Files.asResource(new File(td.get(Paths.BASE).dir(), "images"))));
        assertTrue(Resources.exists(Files.asResource(new File(td.get(Paths.BASE).dir(), "logs"))));
        assertTrue(
                Resources.exists(Files.asResource(new File(td.get(Paths.BASE).dir(), "palettes"))));
        assertTrue(
                Resources.exists(
                        Files.asResource(new File(td.get(Paths.BASE).dir(), "user_projections"))));
        assertTrue(
                Resources.exists(
                        Files.asResource(new File(td.get(Paths.BASE).dir(), "validation"))));
        assertTrue(Resources.exists(Files.asResource(new File(td.get(Paths.BASE).dir(), "www"))));
    }

    @Test
    public void testGeoServerGlobalSettingsStorage() throws Exception {
        Catalog cat = getCatalog();
        GeoServer geoserver = getGeoServer();

        CatalogBackupRestoreTasklet catalogTsklet = new CatalogBackupRestoreTasklet(backupFacade);

        GeoServerDataDirectory td = new GeoServerDataDirectory(root);

        catalogTsklet.doWrite(geoserver.getGlobal(), td.get(Paths.BASE), "global.xml");
        catalogTsklet.doWrite(geoserver.getSettings(), td.get(Paths.BASE), "settings.xml");
        catalogTsklet.doWrite(geoserver.getLogging(), td.get(Paths.BASE), "logging.xml");

        assertTrue(
                Resources.exists(
                        Files.asResource(new File(td.get(Paths.BASE).dir(), "global.xml"))));
        assertTrue(
                Resources.exists(
                        Files.asResource(new File(td.get(Paths.BASE).dir(), "settings.xml"))));
        assertTrue(
                Resources.exists(
                        Files.asResource(new File(td.get(Paths.BASE).dir(), "logging.xml"))));

        XStreamPersister xstream = catalogTsklet.getxStreamPersisterFactory().createXMLPersister();
        xstream.setCatalog(cat);
        xstream.setReferenceByName(true);
        xstream.setExcludeIds();
        XStream xp = xstream.getXStream();

        GeoServerInfo gsGlobal =
                (GeoServerInfo) xp.fromXML(new File(td.get(Paths.BASE).dir(), "global.xml"));

        assertNotNull(gsGlobal);

        SettingsInfo gsSettins =
                (SettingsInfo) xp.fromXML(new File(td.get(Paths.BASE).dir(), "settings.xml"));

        assertNotNull(gsSettins);

        LoggingInfo gsLogging =
                (LoggingInfo) xp.fromXML(new File(td.get(Paths.BASE).dir(), "logging.xml"));

        assertNotNull(gsLogging);

        assertEquals(geoserver.getGlobal(), gsGlobal);
        assertEquals(geoserver.getSettings(), gsSettins);
        assertEquals(geoserver.getLogging(), gsLogging);

        catalogTsklet.doWrite(
                cat.getDefaultWorkspace(),
                BackupUtils.dir(td.get(Paths.BASE), "workspaces"),
                "default.xml");

        assertTrue(
                Resources.exists(
                        Files.asResource(
                                new File(
                                        BackupUtils.dir(td.get(Paths.BASE), "workspaces").dir(),
                                        "default.xml"))));

        WorkspaceInfo defaultWorkspace =
                (WorkspaceInfo)
                        xp.fromXML(
                                new File(
                                        BackupUtils.dir(td.get(Paths.BASE), "workspaces").dir(),
                                        "default.xml"));

        assertEquals(cat.getDefaultWorkspace().getName(), defaultWorkspace.getName());
    }
}
