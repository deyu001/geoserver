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

package org.geoserver.importer.bdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.sleepycat.je.DatabaseEntry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.ImporterTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class XStreamInfoSerialBindingTest extends ImporterTestSupport {

    @Override
    protected void setupImporterFieldInternal() {
        importer = (Importer) applicationContext.getBean("importer");
    }

    @Test
    public void testSerializeWithNewStore() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        ImportContext context = importer.createContext(new Directory(dir));

        XStreamPersister xp = importer.createXStreamPersisterXML();
        XStreamInfoSerialBinding<ImportContext> binding =
                new XStreamInfoSerialBinding<ImportContext>(xp, ImportContext.class);
        binding.setCompress(false);

        DatabaseEntry e = new DatabaseEntry();
        binding.objectToEntry(context, e);

        Document dom = dom(new ByteArrayInputStream(e.getData(), 0, e.getSize()));
        print(dom);
        XMLAssert.assertXpathExists("/import", dom);

        print(dom);

        // workspace referenced by id
        XMLAssert.assertXpathExists("/import/targetWorkspace/id", dom);

        // store inline
        XMLAssert.assertXpathExists("/import/tasks/task[position()=1]/store/name", dom);
        XMLAssert.assertXpathNotExists("/import/tasks/task[position()=1]/store/id", dom);

        ImportContext context2 = binding.entryToObject(e);
        assertNotNull(context2.getTargetWorkspace());
        assertNotNull(context2.getTargetWorkspace().getId());
        assertNotNull(context2.getTargetWorkspace().getName());

        ImportTask task = context2.getTasks().get(0);
        assertNotNull(task.getStore());
        assertNull(task.getStore().getId());
        assertNotNull(task.getStore().getName());

        assertNotNull(task.getLayer());
        // assertNotNull(item.getLayer().getResource());
    }

    Document dom(DatabaseEntry e) throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>" + new String(e.getData());
        System.out.println(xml);
        return dom(new ByteArrayInputStream(xml.getBytes()));
    }

    @Test
    public void testSerialize2() throws Exception {
        Catalog cat = getCatalog();

        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setName("spearfish");
        ds.setType("H2");

        Map params = new HashMap();
        params.put("database", getTestData().getDataDirectoryRoot().getPath() + "/spearfish");
        params.put("dbtype", "h2");
        ds.getConnectionParameters().putAll(params);
        ds.setEnabled(true);
        cat.add(ds);

        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ds = cat.getDataStore(ds.getId());
        ImportContext context = importer.createContext(new Directory(dir), ds);
        assertEquals(2, context.getTasks().size());

        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        xp.getXStream().omitField(ImportTask.class, "context");

        XStreamInfoSerialBinding<ImportContext> binding =
                new XStreamInfoSerialBinding<ImportContext>(xp, ImportContext.class);
        binding.setCompress(false);

        DatabaseEntry e = new DatabaseEntry();
        binding.objectToEntry(context, e);
    }
}
