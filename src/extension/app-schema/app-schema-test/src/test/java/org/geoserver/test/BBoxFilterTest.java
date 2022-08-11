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


package org.geoserver.test;

import org.geotools.geometry.jts.JTS;
import org.geotools.gml.producer.CoordinateFormatter;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;

/**
 * This is to test spatial (bbox) queries for complex features
 *
 * @author Derrick Wong, Curtin University of Technology
 */
public class BBoxFilterTest extends AbstractAppSchemaTestSupport {
    private final String WFS_GET_FEATURE =
            "wfs?request=GetFeature&version=1.1.0&typename=ex:geomContainer";

    private final String WFS_GET_FEATURE_LOG =
            "WFS GetFeature&typename=ex:geomContainerresponse:\n";

    private final String LONGLAT = "&BBOX=130,-29,134,-24";

    private final String LATLONG = "&BBOX=-29,130,-24,134";

    private final String EPSG_4326 = "EPSG:4326";

    private final String EPSG_4283 = "urn:x-ogc:def:crs:EPSG:4283";

    protected BBoxMockData createTestData() {
        return new BBoxMockData();
    }

    /**
     * The following performs a WFS request and obtains all features specified in
     * BBoxTestPropertyfile.properties
     */
    @Test
    public void testQuery() {
        Document doc = getAsDOM(WFS_GET_FEATURE);
        LOGGER.info(WFS_GET_FEATURE_LOG + prettyString(doc));
        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(3, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering longitude
     * latitude.
     */
    @Test
    public void testQueryBboxLongLat() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LONGLAT);
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));
        assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(0, "//ex:geomContainer", doc);
    }

    /**
     * This uses long lat bbox, with srsName specified in long lat format (EPSG code). This should
     * return the results.
     */
    @Test
    public void testQueryBboxLongLatEPSGCode() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LONGLAT + ",EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * This uses long lat bbox, with srsName specified in lat long format (URN). This should not
     * return the results.
     */
    @Test
    public void testQueryBboxLongLatURN() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LONGLAT + ",urn:x-ogc:def:crs:EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));
        assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(0, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude. This test should return features since WFS 1.1.0 defaults to lat long if
     * unspecified.
     */
    @Test
    public void testQueryBboxLatLong() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG);
        LOGGER.info(WFS_GET_FEATURE_LOG + LATLONG + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude and srsName in EPSG code format. This test should not return features if the axis
     * ordering behaves similar to queries to Simple features.
     */
    @Test
    public void testQueryBboxLatLongEPSGCode() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG + ",EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LATLONG + prettyString(doc));
        assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(0, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude and srsName in URN format. This test should return features if the axis ordering
     * behaves similar to queries to Simple features.
     */
    @Test
    public void testQueryBboxLatLongURN() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG + ",urn:x-ogc:def:crs:EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LATLONG + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude and srsName in URN format using POST request (GEOS-6216). This test should return
     * features if the axis ordering behaves similar to queries to Simple features.
     */
    @Test
    public void testQueryBboxLatLongPost() {

        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:ex=\"http://example.com\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd \">" //
                        + "<wfs:Query typeName=\"ex:geomContainer\">" //
                        + "    <ogc:Filter>" //
                        + "        <ogc:BBOX>" //
                        + "            <ogc:PropertyName>ex:geom</ogc:PropertyName>" //
                        + "            <gml:Envelope srsName=\"urn:x-ogc:def:crs:EPSG:4326\">" //
                        + "                  <gml:lowerCorner>-29 130</gml:lowerCorner>" //
                        + "                  <gml:upperCorner>-24 134</gml:upperCorner>" //
                        + "            </gml:Envelope>" //
                        + "        </ogc:BBOX>" //
                        + "    </ogc:Filter>" //
                        + "</wfs:Query>" //
                        + "</wfs:GetFeature>"; //
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(WFS_GET_FEATURE_LOG + " with POST filter " + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering longitude
     * latitude along with srs reprojection.
     */
    @Test
    public void testQueryBboxLatLongSrs4283()
            throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException,
                    TransformException {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG + "&srsName=urn:x-ogc:def:crs:EPSG:4283");
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));

        CoordinateReferenceSystem sourceCRS = CRS.decode(EPSG_4326);
        CoordinateReferenceSystem targetCRS = CRS.decode(EPSG_4283);
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        GeometryFactory factory = new GeometryFactory();
        Point targetPoint =
                (Point)
                        JTS.transform(
                                factory.createPoint(new Coordinate(132.61, -26.98)), transform);
        CoordinateFormatter format = new CoordinateFormatter(8);
        String targetPointCoord1 =
                format.format(targetPoint.getCoordinate().x)
                        + " "
                        + format.format(targetPoint.getCoordinate().y);
        targetPoint =
                (Point)
                        JTS.transform(
                                factory.createPoint(new Coordinate(132.71, -26.46)), transform);
        String targetPointCoord2 =
                format.format(targetPoint.getCoordinate().x)
                        + " "
                        + format.format(targetPoint.getCoordinate().y);

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2", "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/@srsDimension", doc);
        assertXpathEvaluatesTo(
                targetPointCoord1,
                "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                targetPointCoord1,
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/gml:pos",
                doc);

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2", "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/@srsDimension", doc);
        assertXpathEvaluatesTo(
                targetPointCoord2,
                "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                targetPointCoord2,
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/gml:pos",
                doc);
    }
}
