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

package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.view.DefaultView;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.test.TestData;
import org.geotools.util.URLs;
import org.geotools.xml.styling.SLDParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Test suite for HTMLImageMapMapProducer GetMapOutputFormat
 *
 * @author Mauro Bartolomeoli
 */
public class HTMLImageMapTest {

    private static final StyleFactory sFac = CommonFactoryFinder.getStyleFactory(null);

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    HTMLImageMapTest.class.getPackage().getName());

    private HTMLImageMapMapProducer mapProducer;

    private HTMLImageMapResponse response;

    private CoordinateReferenceSystem WGS84;

    private DataStore testDS = null;

    private int mapWidth = 600;

    private int mapHeight = 600;

    @Before
    public void setUp() throws Exception {
        // initializes GeoServer Resource Loading (is needed by some tests to not produce
        // exceptions)

        System.setProperty("org.geotools.referencing.forceXY", "true");
        File testdata = TestData.file(this, ".");
        System.setProperty("GEOSERVER_DATA_DIR", testdata.getAbsolutePath());
        GeoServerResourceLoader loader = new GeoServerResourceLoader(testdata);
        @SuppressWarnings("PMD.CloseResource")
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        context.getBeanFactory().registerSingleton("resourceLoader", loader);

        // initialized WGS84 CRS (used by many tests)
        WGS84 = CRS.decode("EPSG:4326");

        testDS = getTestDataStore();

        // initializes GetMapOutputFormat factory and actual producer
        // this.mapFactory = getProducerFactory();
        this.mapProducer = new HTMLImageMapMapProducer();
        this.response = new HTMLImageMapResponse();
    }

    @After
    public void tearDown() throws Exception {
        this.mapProducer = null;
        this.response = null;
    }

    public DataStore getTestDataStore() throws IOException {
        File testdata = TestData.file(this, "featureTypes");

        return new PropertyDataStore(testdata);
    }

    protected Style getTestStyle(String styleName) throws Exception {
        SLDParser parser = new SLDParser(sFac);
        File styleRes = TestData.file(this, "styles/" + styleName);

        parser.setInput(styleRes);

        Style s = parser.readXML()[0];

        return s;
    }

    protected void assertTestResult(String testName, EncodeHTMLImageMap imageMap) throws Exception {

        ByteArrayOutputStream out = null;
        StringBuffer testText = new StringBuffer();
        try {

            out = new ByteArrayOutputStream();
            this.response.write(imageMap, out, null);
            out.flush();
            out.close();
            File testFile = TestData.file(this, "results/" + testName + ".txt");
            try (BufferedReader reader = new BufferedReader(new FileReader(testFile))) {
                String s = null;
                while ((s = reader.readLine()) != null) testText.append(s + "\n");
            }

        } finally {
            imageMap.dispose();
        }
        assertNotNull(out);
        assertTrue(out.size() > 0);
        String s = new String(out.toByteArray());

        assertEquals(testText.toString(), s);
    }

    @Test
    public void testStates() throws Exception {
        File shapeFile = TestData.file(this, "featureTypes/states.shp");
        ShapefileDataStore ds = new ShapefileDataStore(URLs.fileToUrl(shapeFile));

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds.getFeatureSource("states");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("Population.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap imageMap = this.mapProducer.produceMap(map);

        assertTestResult("States", imageMap);
    }

    @Test
    public void testMapProduceBasicPolygons() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("BasicPolygons");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("default.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BasicPolygons", result);
    }

    @Test
    public void testMapProducePolygonsWithHoles() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("PolygonWithHoles");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("default.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("PolygonWithHoles", result);
    }

    @Test
    public void testMapProducePolygonsWithSkippedHoles() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("PolygonWithSkippedHoles");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);

        Style basicStyle = getTestStyle("default.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("PolygonWithSkippedHoles", result);
    }

    @Test
    public void testMapProduceReproject() throws Exception {
        final DataStore ds = getTestDataStore();
        final SimpleFeatureSource origional = ds.getFeatureSource("ProjectedPolygon");

        Query query = new Query("ProjectedPolygon");
        query.setCoordinateSystem(CRS.decode("EPSG:3004"));
        final SimpleFeatureSource fs = new DefaultView(origional, query);

        final ReferencedEnvelope env =
                new ReferencedEnvelope(fs.getBounds(), CRS.decode("EPSG:3004"));

        LOGGER.info("about to create map ctx for ProjectedPolygon with bounds " + env);

        final WMSMapContent map = new WMSMapContent();

        CoordinateReferenceSystem sourceCrs = CRS.decode("EPSG:3004");
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:3003");

        MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
        Envelope projEnv = JTS.transform(env, transform);
        ReferencedEnvelope refEnv = new ReferencedEnvelope(projEnv, targetCrs);

        map.getViewport().setBounds(refEnv);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setBgColor(Color.red);
        map.setTransparent(false);

        map.getViewport().setCoordinateReferenceSystem(targetCrs);
        Style basicStyle = getTestStyle("BasicPolygons.sld");

        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("ProjectedPolygon", result);
    }

    @Test
    public void testMapProduceLines() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("RoadSegments");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for RoadSegments with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("RoadSegments.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("RoadSegments", result);
    }

    @Test
    public void testMapRuleWithFilters() throws Exception {
        /*
         * Filter
         * f=filterFactory.equals(filterFactory.property("NAME"),filterFactory.literal("Route 5"));
         * Query q=new Query("RoadSegments",f);
         */
        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("RoadSegments");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info(
                "about to create map ctx for RoadSegments with filter on name and bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("RoadSegmentsFiltered.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("RoadSegmentsFiltered", result);
    }

    @Test
    public void testMapProducePoints() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("BuildingCenters");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BuildingCenters", result);
    }

    @Test
    public void testMapProducePointsWithSize() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("BuildingCenters");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters2.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BuildingCenters2", result);
    }

    @Test
    public void testMapProducePointsWithDifferenSizeInScale1() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("BuildingCenters");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters3.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));
        EncodeHTMLImageMap result = mapProducer.produceMap(map);

        assertTestResult("BuildingCenters3", result);
    }

    @Test
    public void testMapProducePointsWithDifferenSizeInScale2() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("BuildingCenters");
        ReferencedEnvelope tmp = fs.getBounds();
        tmp.expandBy(5, 5);
        final ReferencedEnvelope env = new ReferencedEnvelope(tmp, WGS84);

        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters3.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));
        EncodeHTMLImageMap result = mapProducer.produceMap(map);

        assertTestResult("BuildingCenters4", result);
    }

    @Test
    public void testMapProduceMultiPoints() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("BuildingCentersMultiPoint");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for BuildingCentersMultiPoint with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("BuildingCenters.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("BuildingCentersMultiPoint", result);
    }

    @Test
    public void testMapProduceCollection() throws Exception {

        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("CollectionSample");
        final ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);

        LOGGER.info("about to create map ctx for RoadSegments with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("CollectionSample.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("CollectionSample", result);
    }

    @Test
    public void testMapProduceNoCoords() throws Exception {
        final FeatureSource<SimpleFeatureType, SimpleFeature> fs =
                testDS.getFeatureSource("NoCoords");
        final ReferencedEnvelope env = new ReferencedEnvelope(2.0, 6.0, 2.0, 6.0, WGS84);

        LOGGER.info("about to create map ctx for NamedPlaces with bounds " + env);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);

        map.setTransparent(false);

        Style basicStyle = getTestStyle("NamedPlaces.sld");
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult("NoCoords", result);
    }

    @Test
    public void testPointsAreAlwaysRenderedAsCircles() throws Exception {
        final String s = "VariousPoints";
        FeatureSource<SimpleFeatureType, SimpleFeature> fs = testDS.getFeatureSource(s);
        ReferencedEnvelope env = new ReferencedEnvelope(fs.getBounds(), WGS84);
        LOGGER.info("about to create map ctx for BuildingCenters with bounds " + env);

        WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(mapWidth);
        map.setMapHeight(mapHeight);
        map.setTransparent(false);
        Style basicStyle = getTestStyle(String.format("%s.sld", s));
        map.addLayer(new FeatureLayer(fs, basicStyle));

        EncodeHTMLImageMap result = mapProducer.produceMap(map);
        assertTestResult(s, result);
    }
}
