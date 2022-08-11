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

package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.factory.Hints;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TextFeatureInfoOutputFormatTest extends WMSTestSupport {

    public static TimeZone defaultTimeZone;

    private TextFeatureInfoOutputFormat outputFormat;

    private FeatureCollectionType fcType;

    Map<String, Object> parameters;

    GetFeatureInfoRequest getFeatureInfoRequest;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("org.geotools.localDateTimeHandling", "true");
        System.getProperties().remove("org.geotools.dateTimeFormatHandling");
        Hints.scanSystemProperties();
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));
    }

    @AfterClass
    public static void afterClass() {
        System.getProperties().remove("org.geotools.dateTimeFormatHandling");
        Hints.scanSystemProperties();
        TimeZone.setDefault(defaultTimeZone);
    }

    @Before
    public void setUp() throws URISyntaxException, IOException {
        outputFormat = new TextFeatureInfoOutputFormat(getWMS());

        Request request = new Request();
        parameters = new HashMap<>();
        parameters.put("LAYER", "testLayer");
        Map<String, String> env = new HashMap<>();
        env.put("TEST1", "VALUE1");
        env.put("TEST2", "VALUE2");
        parameters.put("ENV", env);
        request.setKvp(parameters);

        Dispatcher.REQUEST.set(request);

        final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.NULLS);

        fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
        @SuppressWarnings("unchecked")
        EList<FeatureCollection> feature = fcType.getFeature();
        feature.add(featureType.getFeatureSource(null, null).getFeatures());

        // fake layer list
        List<MapLayerInfo> queryLayers = new ArrayList<>();
        LayerInfo layerInfo = new LayerInfoImpl();
        layerInfo.setType(PublishedType.VECTOR);
        ResourceInfo resourceInfo = new FeatureTypeInfoImpl(null);
        NamespaceInfo nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix("topp");
        nameSpace.setURI("http://www.topp.org");
        resourceInfo.setNamespace(nameSpace);
        layerInfo.setResource(resourceInfo);
        MapLayerInfo mapLayerInfo = new MapLayerInfo(layerInfo);
        queryLayers.add(mapLayerInfo);
        getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
    }

    /** Test null geometry is correctly handled (GEOS-6829). */
    @Test
    public void testNullGeometry() throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());

        assertFalse(result.contains("java.lang.NullPointerException"));
        assertTrue(result.contains("pointProperty = null"));
    }

    @Test
    public void testDateTimeFormattingEnabled() throws Exception {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));
        try {
            System.setProperty("org.geotools.dateTimeFormatHandling", "true");
            Hints.scanSystemProperties();
            final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
            fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
            @SuppressWarnings("unchecked")
            EList<FeatureCollection> feature = fcType.getFeature();
            feature.add(featureType.getFeatureSource(null, null).getFeatures());

            getFeatureInfoRequest.setFeatureCount(10);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
            String result = new String(outStream.toByteArray());
            assertTrue(result.contains("dateTimeProperty = 2006-06-26T19:00:00-05:00"));

        } finally {
            getFeatureInfoRequest.setFeatureCount(1);
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    @Test
    public void testDateTimeFormattingDisabled() throws Exception {
        System.setProperty("org.geotools.dateTimeFormatHandling", "false");
        Hints.scanSystemProperties();
        try {
            final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
            fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
            @SuppressWarnings("unchecked")
            EList<FeatureCollection> feature = fcType.getFeature();
            feature.add(featureType.getFeatureSource(null, null).getFeatures());

            getFeatureInfoRequest.setFeatureCount(10);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
            String result = new String(outStream.toByteArray());
            assertTrue(result.contains("dateTimeProperty = 2006-06-26 19:00:00.0"));
        } finally {
            getFeatureInfoRequest.setFeatureCount(1);
            System.getProperties().remove("org.geotools.dateTimeFormatHandling");
            Hints.scanSystemProperties();
        }
    }
}
