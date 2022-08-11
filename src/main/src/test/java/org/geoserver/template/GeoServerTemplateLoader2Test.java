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

package org.geoserver.template;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Files;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeoServerTemplateLoader2Test {

    static File root;
    private File fooFile;

    @BeforeClass
    public static void createTmpDir() throws Exception {
        root = File.createTempFile("template", "tmp", new File("target"));
        root.delete();
        root.mkdir();
    }

    @Before
    public void createTestFile() throws IOException {
        this.fooFile = new File("./target/foo");
        fooFile.delete();
        fooFile.createNewFile(); // file needs to actually be there
    }

    GeoServerDataDirectory createDataDirectoryMock() {
        GeoServerDataDirectory dd = createNiceMock(GeoServerDataDirectory.class);
        expect(dd.root()).andReturn(root).anyTimes();
        return dd;
    }

    @Test
    public void testRelativeToFeatureType() throws IOException {

        GeoServerDataDirectory dd = createDataDirectoryMock();
        replay(dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        Object source = tl.findTemplateSource("dummy.ftl");
        assertNull(source);

        reset(dd);

        FeatureTypeInfo ft = createMock(FeatureTypeInfo.class);
        expect(dd.get(ft, "dummy.ftl")).andReturn(Files.asResource(fooFile)).once();
        replay(ft, dd);

        tl.setFeatureType(ft);

        source = tl.findTemplateSource("dummy.ftl");
        assertNotNull(source);

        verify(ft, dd);
    }

    @Test
    public void testRelativeToStore() throws IOException {
        GeoServerDataDirectory dd = createDataDirectoryMock();
        replay(dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        Object source = tl.findTemplateSource("dummy.ftl");
        assertNull(source);

        reset(dd);

        DataStoreInfo s = createNiceMock(DataStoreInfo.class);
        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        expect(ft.getStore()).andReturn(s).anyTimes();
        tl.setFeatureType(ft);

        replay(ft, s, dd);

        assertNull(tl.findTemplateSource("dummy.ftl"));

        reset(dd);
        expect(dd.get(s, "dummy.ftl")).andReturn(Files.asResource(fooFile)).once();
        replay(dd);

        assertNotNull(tl.findTemplateSource("dummy.ftl"));
        verify(dd);
    }

    @Test
    public void testRelativeToWorkspace() throws IOException {
        GeoServerDataDirectory dd = createDataDirectoryMock();

        DataStoreInfo s = createNiceMock(DataStoreInfo.class);
        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        expect(ft.getStore()).andReturn(s).anyTimes();
        expect(s.getWorkspace()).andReturn(ws).anyTimes();

        replay(ft, s, ws, dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        tl.setFeatureType(ft);
        Object source = tl.findTemplateSource("dummy.ftl");
        assertNull(source);

        reset(dd);
        expect(dd.get(ws, "dummy.ftl")).andReturn(Files.asResource(fooFile)).once();
        replay(dd);

        assertNotNull(tl.findTemplateSource("dummy.ftl"));
        verify(dd);
    }

    @Test
    public void testGlobal() throws IOException {
        GeoServerDataDirectory dd = createDataDirectoryMock();

        DataStoreInfo s = createNiceMock(DataStoreInfo.class);
        FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        expect(ft.getStore()).andReturn(s).anyTimes();
        expect(s.getWorkspace()).andReturn(ws).anyTimes();
        replay(ft, s, ws, dd);

        GeoServerTemplateLoader tl = new GeoServerTemplateLoader(getClass(), dd);
        tl.setResource(ft);
        assertNull(tl.findTemplateSource("dummy.ftl"));

        reset(dd);

        expect(dd.getWorkspaces("dummy.ftl")).andReturn(Files.asResource(fooFile)).once();
        replay(dd);

        assertNotNull(tl.findTemplateSource("dummy.ftl"));
        verify(dd);
    }
}
