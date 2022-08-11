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

package org.geoserver.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.junit.Test;

public abstract class ImporterDbTestBase extends ImporterDbTestSupport {

    @Override
    protected void doSetUpInternal() throws Exception {
        try (Connection cx = getConnection();
                Statement st = cx.createStatement()) {

            dropTable("widgets", st);
            dropTable("archsites", st);
            dropTable("bugsites", st);

            createWidgetsTable(st);
        }
    }

    protected String tableName(String name) {
        return name;
    }

    protected abstract void createWidgetsTable(Statement st) throws Exception;

    protected void dropTable(String tableName, Statement st) throws Exception {
        runSafe("DROP TABLE " + tableName(tableName), st);
    }

    @Test
    public void testDirectImport() throws Exception {
        Database db = new Database(getConnectionParams());

        ImportContext context = importer.createContext(db);
        assertEquals(ImportContext.State.PENDING, context.getState());

        assertEquals(1, context.getTasks().size());

        importer.run(context);
        runChecks("gs:" + tableName("widgets"));
    }

    @Test
    public void testIndirectToShapefile() throws Exception {
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ImportContext context = importer.createContext(new Directory(dir));
        importer.run(context);

        runChecks("gs:archsites");
        runChecks("gs:bugsites");

        DataStoreInfo store = (DataStoreInfo) context.getTasks().get(0).getStore();
        assertNotNull(store);
        assertEquals(2, getCatalog().getFeatureTypesByDataStore(store).size());

        context = importer.createContext(new Database(getConnectionParams()), store);
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());

        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());

        assertEquals(3, getCatalog().getFeatureTypesByDataStore(store).size());
        runChecks("gs:" + tableName("widgets"));
    }

    @Test
    public void testIndirectToDb() throws Exception {
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("oracle");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);
        ds.getConnectionParameters().putAll(getConnectionParams());
        cat.add(ds);

        assertEquals(0, cat.getFeatureTypesByDataStore(ds).size());
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ImportContext context = importer.createContext(new Directory(dir), ds);
        assertEquals(2, context.getTasks().size());

        assertEquals(ImportTask.State.READY, context.getTasks().get(0).getState());
        assertEquals(ImportTask.State.READY, context.getTasks().get(1).getState());

        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());

        assertEquals(2, cat.getFeatureTypesByDataStore(ds).size());
        runChecks("gs:" + tableName("archsites"));
        runChecks("gs:" + tableName("bugsites"));
    }
}
