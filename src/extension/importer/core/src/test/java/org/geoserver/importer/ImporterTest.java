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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.FileSystemWatcher;
import org.geoserver.platform.resource.Resource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Before;
import org.junit.Test;

public class ImporterTest extends ImporterTestSupport {

    @Before
    public void addPrimitiveGeoFeature() throws IOException {
        revertLayer(SystemTestData.PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testCreateContextSingleFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file);
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(file, task.getData());
    }

    @Test
    public void testCreateContextDirectoryHomo() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        Directory d = new Directory(dir);
        ImportContext context = importer.createContext(d);
        assertEquals(2, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(d.part("archsites"), task.getData());

        task = context.getTasks().get(1);
        assertEquals(d.part("bugsites"), task.getData());
    }

    @Test
    public void testCreateContextDirectoryHetero() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("geotiff/EmissiveCampania.tif.bz2", dir);

        Directory d = new Directory(dir);

        ImportContext context = importer.createContext(d);
        assertEquals(2, context.getTasks().size());

        // cannot ensure order of tasks due to hashing
        Set<ImportData> files = new HashSet<>();
        files.add(context.getTasks().get(0).getData());
        files.add(context.getTasks().get(1).getData());
        assertTrue(files.containsAll(d.getFiles()));
    }

    @Test
    public void testCreateContextFromArchive() throws Exception {
        File file = file("shape/archsites_epsg_prj.zip");
        Archive arch = new Archive(file);

        ImportContext context = importer.createContext(arch);
        assertEquals(1, context.getTasks().size());
    }

    @Test
    public void testCreateContextIgnoreHidden() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        FileUtils.touch(new File(dir, ".DS_Store"));

        ImportContext context = importer.createContext(new Directory(dir));
        assertEquals(1, context.getTasks().size());
    }

    @Test
    public void testCalculateBounds() throws Exception {

        FeatureTypeInfo resource = getCatalog().getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        ReferencedEnvelope nativeBounds = cb.getNativeBounds(resource);
        resource.setNativeBoundingBox(nativeBounds);
        resource.setLatLonBoundingBox(cb.getLatLonBounds(nativeBounds, resource.getCRS()));
        getCatalog().save(resource);

        assertNotNull(resource.getNativeBoundingBox());
        assertFalse(resource.getNativeBoundingBox().isEmpty());

        ReferencedEnvelope bbox = resource.getNativeBoundingBox();

        // Test null bbox
        resource.setNativeBoundingBox(null);
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertEquals(bbox, resource.getNativeBoundingBox());

        // Test empty bbox
        resource.setNativeBoundingBox(new ReferencedEnvelope());
        assertTrue(resource.getNativeBoundingBox().isEmpty());
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertEquals(bbox, resource.getNativeBoundingBox());

        // Test nonempty bbox - should not be changed
        ReferencedEnvelope customBbox =
                new ReferencedEnvelope(30, 60, -10, 30, bbox.getCoordinateReferenceSystem());
        resource.setNativeBoundingBox(customBbox);
        assertNotEquals(bbox, resource.getNativeBoundingBox());
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertNotEquals(bbox, resource.getNativeBoundingBox());

        // Test with "recalculate-bounds"=false
        resource.setNativeBoundingBox(customBbox);
        resource.getMetadata().put("recalculate-bounds", false);
        assertNotEquals(bbox, resource.getNativeBoundingBox());
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertNotEquals(bbox, resource.getNativeBoundingBox());

        // Test with "recalculate-bounds"=true
        resource.setNativeBoundingBox(customBbox);
        resource.getMetadata().put("recalculate-bounds", true);
        assertNotEquals(bbox, resource.getNativeBoundingBox());
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertEquals(bbox, resource.getNativeBoundingBox());

        // Test with "recalculate-bounds"="true"
        resource.setNativeBoundingBox(customBbox);
        resource.getMetadata().put("recalculate-bounds", "true");
        assertNotEquals(bbox, resource.getNativeBoundingBox());
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertEquals(bbox, resource.getNativeBoundingBox());
    }

    @Test
    public void testImporterConfiguration() throws Exception {
        // schedule for shorter delays
        ((FileSystemWatcher) getResourceLoader().getResourceNotificationDispatcher())
                .schedule(10, TimeUnit.MILLISECONDS);

        // update the configuration
        Resource props = getDataDirectory().get("importer/importer.properties");
        ImporterInfoDAO dao = new ImporterInfoDAO();
        ImporterInfo config = new ImporterInfoImpl();
        config.setMaxAsynchronousImports(5);
        config.setMaxSynchronousImports(7);
        dao.write(config, props);

        // forcing the importer to reload manually, as we don't know how fast the polling thread
        // will be able to catch up
        importer.reloadConfiguration();

        // make sure the importer picked up the change
        assertEquals(5, importer.asynchronousJobs.getMaximumPoolSize());
        assertEquals(7, importer.synchronousJobs.getMaximumPoolSize());
    }
}
