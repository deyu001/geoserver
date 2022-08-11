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

package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetFeatureCurvesTest extends WFS20TestSupport {

    QName CURVELINES = new QName(MockData.CITE_URI, "curvelines", MockData.CITE_PREFIX);

    QName CURVEMULTILINES = new QName(MockData.CITE_URI, "curvemultilines", MockData.CITE_PREFIX);

    QName CURVEPOLYGONS = new QName(MockData.CITE_URI, "curvepolygons", MockData.CITE_PREFIX);

    XpathEngine xpath;

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        // TODO Auto-generated method stub
        super.setUpInternal(testData);

        testData.addWorkspace(MockData.CITE_PREFIX, MockData.CITE_URI, getCatalog());
        testData.addVectorLayer(
                CURVELINES,
                Collections.emptyMap(),
                "curvelines.properties",
                MockData.class,
                getCatalog());
        testData.addVectorLayer(
                CURVEMULTILINES,
                Collections.emptyMap(),
                "curvemultilines.properties",
                MockData.class,
                getCatalog());
        testData.addVectorLayer(
                CURVEPOLYGONS,
                Collections.emptyMap(),
                "curvepolygons.properties",
                MockData.class,
                getCatalog());

        FeatureTypeInfo curveLines = getCatalog().getFeatureTypeByName(getLayerId(CURVELINES));
        curveLines.setCircularArcPresent(true);
        curveLines.setLinearizationTolerance(null);
        getCatalog().save(curveLines);

        FeatureTypeInfo curveMultiLines =
                getCatalog().getFeatureTypeByName(getLayerId(CURVEMULTILINES));
        curveMultiLines.setCircularArcPresent(true);
        curveMultiLines.setLinearizationTolerance(null);
        getCatalog().save(curveMultiLines);
    }

    @Before
    public void setXPath() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do not call super, we only need the curved data sets
        testData.setUpSecurity();
    }

    private int countCoordinates(Document dom, XpathEngine xpath, String path)
            throws XpathException {
        String coords = xpath.evaluate(path, dom);
        int coordCount = coords.split("\\s+").length;
        return coordCount;
    }

    @Test
    public void testCurveLine() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=wfs&version=2.0&request=GetFeature&typeName="
                                + getLayerId(CURVELINES));
        // print(dom);

        // check the compound curve
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                10,
                countCoordinates(
                        dom,
                        xpath,
                        "//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:LineStringSegment)",
                        dom));
        assertEquals(
                8,
                countCoordinates(
                        dom,
                        xpath,
                        "//cite:curvelines[@gml:id='cp.1']/cite:geom/gml:Curve/gml:segments/gml:LineStringSegment/gml:posList"));

        // check the circle
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.2']/cite:geom/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                10,
                countCoordinates(
                        dom,
                        xpath,
                        "//cite:curvelines[@gml:id='cp.2']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));

        // check the wave
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvelines[@gml:id='cp.3']/cite:geom/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                10,
                countCoordinates(
                        dom,
                        xpath,
                        "//cite:curvelines[@gml:id='cp.3']/cite:geom/gml:Curve/gml:segments/gml:ArcString/gml:posList"));
    }

    @Test
    public void testCurveMultiLine() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=wfs&version=2.0&request=GetFeature&typeName="
                                + getLayerId(CURVEMULTILINES)
                                + "&featureid=cp.1");
        // print(dom);

        // check the compound curve
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:LineString)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:Curve)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//cite:curvemultilines[@gml:id='cp.1']/cite:geom/gml:MultiCurve/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                        dom));
    }

    @Test
    public void testCurvePolygons() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=wfs&version=2.0&request=GetFeature&typeName="
                                + getLayerId(CURVEPOLYGONS)
                                + "&featureid=cp.1");
        // print(dom);

        // check the compound curve
        xpath.evaluate(
                "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:exterior/gml:Ring/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                dom);
        xpath.evaluate(
                "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:exterior/gml:Ring/gml:curveMember/gml:LineString)",
                dom);
        xpath.evaluate(
                "count(//cite:curvepolygons[@gml:id='cp.1']/cite:geom/gml:Polygon/gml:interior/gml:Ring/gml:curveMember/gml:Curve/gml:segments/gml:ArcString)",
                dom);
    }
}
