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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.IntegerFieldToDateTransform;
import org.geoserver.importer.transform.NumberFormatTransform;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class ImportTransformTest extends ImporterTestSupport {

    DataStoreInfo store;

    @Before
    public void setupStore() {
        Catalog cat = getCatalog();

        store = cat.getFactory().createDataStore();
        store.setWorkspace(cat.getDefaultWorkspace());
        store.setName("spearfish");
        store.setType("H2");

        Map<String, Serializable> params = new HashMap<>();
        params.put("database", getTestData().getDataDirectoryRoot().getPath() + "/spearfish");
        params.put("dbtype", "h2");
        store.getConnectionParameters().putAll(params);
        store.setEnabled(true);
        cat.add(store);
    }

    @After
    public void dropStore() {
        Catalog cat = getCatalog();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(cat);
        store.accept(visitor);
    }

    @Test
    public void testNumberFormatTransform() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/restricted.zip");

        SpatialFile file = new SpatialFile(new File(dir, "restricted.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        assertEquals(1, context.getTasks().size());

        context.setTargetStore(store);

        ImportTask task = context.getTasks().get(0);
        task.addTransform(new NumberFormatTransform("cat", Integer.class));
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(store, "restricted");
        assertNotNull(ft);

        SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
        assertEquals(Integer.class, schema.getDescriptor("cat").getType().getBinding());

        try (FeatureIterator it = ft.getFeatureSource(null, null).getFeatures().features()) {
            assertTrue(it.hasNext());
            while (it.hasNext()) {
                SimpleFeature f = (SimpleFeature) it.next();
                assertTrue(f.getAttribute("cat") instanceof Integer);
            }
        }
    }

    @Test
    public void testIntegerToDateTransform() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        assertEquals(1, context.getTasks().size());

        context.setTargetStore(store);

        ImportTask task = context.getTasks().get(0);
        // this is a silly test - CAT_ID ranges from 1-25 and is not supposed to be a date
        // java date handling doesn't like dates in year 1
        task.addTransform(new IntegerFieldToDateTransform("CAT_ID"));
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(store, "archsites");
        assertNotNull(ft);

        SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
        assertEquals(Timestamp.class, schema.getDescriptor("CAT_ID").getType().getBinding());

        int year = 2;
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        try (FeatureIterator it = ft.getFeatureSource(null, null).getFeatures().features()) {
            // make sure we have something
            assertTrue(it.hasNext());
            // the first date will be bogus due to java date limitation
            it.next();
            while (it.hasNext()) {
                SimpleFeature f = (SimpleFeature) it.next();
                // class will be timestamp
                cal.setTime((Date) f.getAttribute("CAT_ID"));
                assertEquals(year++, cal.get(Calendar.YEAR));
            }
        }
    }

    @Test
    public void testDateFormatTransform() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/ivan.zip");

        SpatialFile file = new SpatialFile(new File(dir, "ivan.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        assertEquals(1, context.getTasks().size());

        context.setTargetStore(store);

        ImportTask task = context.getTasks().get(0);
        task.addTransform(new DateFormatTransform("timestamp", "yyyy-MM-dd HH:mm:ss.S"));

        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(store, "ivan");
        assertNotNull(ft);

        SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
        assertTrue(
                Date.class.isAssignableFrom(
                        schema.getDescriptor("timestamp").getType().getBinding()));

        try (FeatureIterator it = ft.getFeatureSource(null, null).getFeatures().features()) {
            assertTrue(it.hasNext());
            while (it.hasNext()) {
                SimpleFeature f = (SimpleFeature) it.next();
                assertTrue(f.getAttribute("timestamp") instanceof Date);
            }
        }
    }

    @Test
    public void testReprojectTransform() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        LayerInfo l1 = context.getTasks().get(0).getLayer();
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        CRS.decode("EPSG:26713"), l1.getResource().getNativeCRS()));
        assertEquals("EPSG:26713", l1.getResource().getSRS());

        dir = unpack("shape/archsites_epsg_prj.zip");

        file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        context = importer.createContext(file, store);
        ImportTask item = context.getTasks().get(0);
        item.addTransform(new ReprojectTransform(CRS.decode("EPSG:4326")));
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        LayerInfo l2 = context.getTasks().get(0).getLayer();
        assertTrue(
                CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), l2.getResource().getNativeCRS()));
        assertEquals("EPSG:4326", l2.getResource().getSRS());

        assertNotEquals(
                l1.getResource().getNativeBoundingBox(), l2.getResource().getNativeBoundingBox());
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        l2.getResource().getNativeCRS(),
                        l2.getResource().getNativeBoundingBox().getCoordinateReferenceSystem()));

        assertTrue(
                CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), l2.getResource().getNativeCRS()));
        assertEquals("EPSG:4326", l2.getResource().getSRS());
    }
}
