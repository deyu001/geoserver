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

package org.geoserver.wps.gs;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Utilities;

public abstract class BaseRasterToVectorTest extends WPSTestSupport {

    static final double EPS = 1e-6;
    public static QName RESTRICTED = new QName(MockData.SF_URI, "restricted", MockData.SF_PREFIX);
    public static QName DEM = new QName(MockData.SF_URI, "sfdem", MockData.SF_PREFIX);
    public static QName TASMANIA_BM_ZONES =
            new QName(MockData.SF_URI, "BmZones", MockData.SF_PREFIX);

    public BaseRasterToVectorTest() {
        super();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);
        testData.addRasterLayer(DEM, "sfdem.tiff", TIFF, null, getClass(), getCatalog());

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope(
                        181985.7630,
                        818014.2370,
                        1973809.4640,
                        8894102.4298,
                        CRS.decode("EPSG:26713", true)));

        testData.addVectorLayer(
                RESTRICTED, props, "restricted.properties", getClass(), getCatalog());
        testData.addVectorLayer(
                TASMANIA_BM_ZONES, props, "tazdem_zones.properties", getClass(), getCatalog());
    }

    /**
     * This method takes the input {@link SimpleFeatureCollection} and transforms it into a
     * shapefile using the provided file.
     *
     * <p>Make sure the provided files ends with .shp.
     *
     * @param fc the {@link SimpleFeatureCollection} to be encoded as a shapefile.
     * @param destination the {@link File} where we want to write the shapefile.
     * @throws IOException in case an {@link IOException} is thrown by the underlying code.
     */
    protected static void featureCollectionToShapeFile(
            final SimpleFeatureCollection fc, final File destination) throws IOException {

        //
        // checks
        //
        org.geotools.util.Utilities.ensureNonNull("fc", fc);
        Utilities.ensureNonNull("destination", destination);
        // checks on the file
        if (destination.exists()) {

            if (destination.isDirectory())
                throw new IOException(
                        "The provided destination maps to a directory:" + destination);

            if (!destination.canWrite())
                throw new IOException(
                        "The provided destination maps to an existing file that cannot be deleted:"
                                + destination);

            if (!destination.delete())
                throw new IOException(
                        "The provided destination maps to an existing file that cannot be deleted:"
                                + destination);
        }

        // real work
        final DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", destination.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore store = null;
        Transaction transaction = null;
        try {
            store = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            store.createSchema(fc.getSchema());

            final SimpleFeatureStore featureStore =
                    (SimpleFeatureStore) store.getFeatureSource(fc.getSchema().getName());
            transaction = featureStore.getTransaction();

            featureStore.addFeatures(fc);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (transaction != null) {

                transaction.commit();
                transaction.close();
            }

            if (store != null) {
                store.dispose();
            }
        }
    }
}
