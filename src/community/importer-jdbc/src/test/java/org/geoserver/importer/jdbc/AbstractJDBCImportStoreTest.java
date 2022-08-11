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

package org.geoserver.importer.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.geoserver.importer.CountingVisitor;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImporterTestSupport;
import org.geoserver.importer.RemoteData;
import org.geoserver.importer.SearchingVisitor;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.test.FixtureUtilities;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/** Abstract tests for the import store, based on a fixture */
public abstract class AbstractJDBCImportStoreTest extends ImporterTestSupport {

    private JDBCDataStore datastore;

    JDBCImportStore store;

    abstract String getFixtureId();

    @Before
    public void setupDataStore() throws IOException {
        // skip the test if the fixture is not found
        Properties props = getFixture();
        Assume.assumeNotNull(props);

        this.datastore =
                (JDBCDataStore)
                        DataStoreFinder.getDataStore(DataUtilities.toConnectionParameters(props));
        if (datastore == null) {
            fail("Could not locate datastore with properties: " + props);
        }
        this.store = new JDBCImportStore(datastore, importer);
        this.store.init();
        // clean up before rather than after, to leave some evidence in the db, to help debugging
        store.removeAll();
    }

    protected Properties getFixture() {
        File fixtureFile = FixtureUtilities.getFixtureFile(getFixtureDirectory(), getFixtureId());
        if (fixtureFile.exists()) {
            return FixtureUtilities.loadProperties(fixtureFile);
        } else {
            Properties exampleFixture = createExampleFixture();
            if (exampleFixture != null) {
                File exFixtureFile = new File(fixtureFile.getAbsolutePath() + ".example");
                if (!exFixtureFile.exists()) {
                    createExampleFixture(exFixtureFile, exampleFixture);
                }
            }
            FixtureUtilities.printSkipNotice(getFixtureId(), fixtureFile);
            return null;
        }
    }

    void createExampleFixture(File exFixtureFile, Properties exampleFixture) {
        try {
            exFixtureFile.getParentFile().mkdirs();
            exFixtureFile.createNewFile();

            FileOutputStream fout = new FileOutputStream(exFixtureFile);

            exampleFixture.store(
                    fout,
                    "This is an example fixture. Update the "
                            + "values and remove the .example suffix to enable the test");
            fout.flush();
            fout.close();
            // System.out.println("Wrote example fixture file to " + exFixtureFile);
        } catch (IOException ioe) {
            // System.out.println("Unable to write out example fixture " + exFixtureFile);
            java.util.logging.Logger.getGlobal().log(java.util.logging.Level.INFO, "", ioe);
        }
    }

    protected abstract Properties createExampleFixture();

    File getFixtureDirectory() {
        return new File(System.getProperty("user.home") + File.separator + ".geoserver");
    }

    @After
    public void shutdown() {
        // clean up the DB
        if (store != null) {
            store.destroy();
        }
        if (datastore != null) datastore.dispose();
    }

    @Test
    public void testAdd() throws Exception {
        runAddTest();
    }

    public Long runAddTest() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(new Directory(dir));

        assertEquals(1, context.getTasks().size());
        for (int i = 0; i < context.getTasks().size(); i++) {
            assertNotNull(context.getTasks().get(i).getStore());
            assertNotNull(context.getTasks().get(i).getStore().getCatalog());
        }

        CountingVisitor cv = new CountingVisitor();

        store.add(context);
        assertNotNull(context.getId());
        assertNotNull(context.getTasks().get(0).getLayer());

        ImportContext context2 = store.get(context.getId());
        assertNotNull(context2);
        assertEquals(context.getId(), context2.getId());

        store.query(cv);
        assertEquals(1, cv.getCount());

        SearchingVisitor sv = new SearchingVisitor(context.getId());
        store.query(sv);
        assertTrue(sv.isFound());

        importer.reattach(context2);

        // ensure various transient bits are set correctly on deserialization
        assertEquals(1, context2.getTasks().size());
        for (int i = 0; i < context2.getTasks().size(); i++) {
            assertNotNull(context2.getTasks().get(i).getStore());
            assertNotNull(context2.getTasks().get(i).getStore().getCatalog());
        }
        assertNotNull(context2.getTasks().get(0).getLayer());

        return context.getId();
    }

    @Test
    public void testSave() throws Exception {
        Long id = runAddTest();

        ImportContext context = store.get(id);
        assertNotNull(context);

        assertEquals(ImportContext.State.PENDING, context.getState());
        context.setState(ImportContext.State.COMPLETE);

        ImportContext context2 = store.get(id);
        assertNotNull(context2);
        assertEquals(ImportContext.State.PENDING, context2.getState());

        store.save(context);
        context2 = store.get(id);
        assertNotNull(context2);
        assertEquals(ImportContext.State.COMPLETE, context2.getState());
    }

    @Test
    public void testSaveRemoteData() throws Exception {
        ImportContext context = importer.registerContext(null);
        RemoteData data = new RemoteData("ftp://geoserver.org");
        data.setUsername("geoserver");
        data.setPassword("gisIsCool");
        context.setData(data);

        store.add(context);
        assertNotNull(context.getId());

        ImportContext context2 = store.get(context.getId());
        assertEquals(data, context2.getData());
    }
}
