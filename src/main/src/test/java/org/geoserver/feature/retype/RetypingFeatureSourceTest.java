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

package org.geoserver.feature.retype;

import static org.geoserver.data.test.MockData.BRIDGES;
import static org.geoserver.data.test.MockData.BUILDINGS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.util.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class RetypingFeatureSourceTest {

    static final String RENAMED = "houses";

    RetypingDataStore rts;

    private File data;

    private PropertyDataStore store;

    @Before
    public void setUp() throws Exception {
        data = File.createTempFile("retype", "data", new File("./target"));
        data.delete();
        data.mkdir();

        copyTestData(BUILDINGS, data);
        copyTestData(BRIDGES, data);

        store = new PropertyDataStore(data);
    }

    void copyTestData(QName testData, File data) throws IOException {
        String fileName = testData.getLocalPart() + ".properties";
        URL properties = MockData.class.getResource(fileName);
        IOUtils.copy(properties.openStream(), new File(data, fileName));
    }

    @Test
    public void testSimpleRename() throws IOException {
        SimpleFeatureSource fs = store.getFeatureSource(BUILDINGS.getLocalPart());
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(fs.getSchema());
        tb.setName("houses");
        SimpleFeatureType target = tb.buildFeatureType();

        SimpleFeatureSource retyped = RetypingFeatureSource.getRetypingSource(fs, target);
        assertTrue(retyped instanceof SimpleFeatureLocking);
        assertEquals(target, retyped.getSchema());
        assertEquals(target, ((DataStore) retyped.getDataStore()).getSchema("houses"));
        assertEquals(target, retyped.getFeatures().getSchema());

        SimpleFeature f = DataUtilities.first(retyped.getFeatures());
        assertEquals(target, f.getType());
    }

    @Test
    public void testConflictingRename() throws IOException {
        // we rename buildings to a feature type that's already available in the data store
        SimpleFeatureSource fs = store.getFeatureSource(BUILDINGS.getLocalPart());
        assertEquals(2, store.getTypeNames().length);
        assertNotNull(store.getSchema(BRIDGES.getLocalPart()));

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(fs.getSchema());
        tb.setName(BRIDGES.getLocalPart());
        SimpleFeatureType target = tb.buildFeatureType();

        SimpleFeatureSource retyped = RetypingFeatureSource.getRetypingSource(fs, target);
        assertEquals(target, retyped.getSchema());
        DataStore rs = (DataStore) retyped.getDataStore();
        assertEquals(1, rs.getTypeNames().length);
        assertEquals(BRIDGES.getLocalPart(), rs.getTypeNames()[0]);

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        String fid = BRIDGES.getLocalPart() + ".1107531701011";
        Filter fidFilter = ff.id(Collections.singleton(ff.featureId(fid)));

        try (SimpleFeatureIterator it = retyped.getFeatures(fidFilter).features()) {
            assertTrue(it.hasNext());
            SimpleFeature f = it.next();
            assertFalse(it.hasNext());

            // _=the_geom:MultiPolygon,FID:String,ADDRESS:String
            // Buildings.1107531701010=MULTIPOLYGON (((0.0008 0.0005, 0.0008 0.0007, 0.0012 0.0007,
            // 0.0012 0.0005, 0.0008 0.0005)))|113|123 Main Street
            // Buildings.1107531701011=MULTIPOLYGON (((0.002 0.0008, 0.002 0.001, 0.0024 0.001,
            // 0.0024
            // 0.0008, 0.002 0.0008)))|114|215 Main Street
            assertEquals("114", f.getAttribute("FID"));
            assertEquals("215 Main Street", f.getAttribute("ADDRESS"));
        }
    }
}
