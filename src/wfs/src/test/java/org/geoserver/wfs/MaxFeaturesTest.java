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

package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class MaxFeaturesTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        // set global max to 5
        GeoServer gs = getGeoServer();

        WFSInfo wfs = getWFS();
        wfs.setMaxFeatures(5);
        gs.save(wfs);
    }

    @Before
    public void resetLocalMaxes() {
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(0);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(0);
        getCatalog().save(info);
    }

    @Test
    public void testGlobalMax() throws Exception {
        // fifteen has 15 elements, but global max is 5
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(5, featureMembers.getLength());
    }

    @Test
    public void testLocalMax() throws Exception {
        // setup different max on local
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(3);
        getCatalog().save(info);

        // fifteen has 15 elements, but global max is 5 and local is 3
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(3, featureMembers.getLength());
    }

    @Test
    public void testLocalMaxBigger() throws Exception {
        // setup different max on local
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(10);
        getCatalog().save(info);

        // fifteen has 15 elements, but global max is 5 and local is 10
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(5, featureMembers.getLength());
    }

    @Test
    public void testCombinedLocalMaxes() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&srsName=EPSG:4326&typename=cdf:Fifteen,cite:BasicPolygons"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(4, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(2, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(2, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }

    @Test
    public void testCombinedLocalMaxesBigger() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(4);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&srsName=EPSG:4326&typename=cdf:Fifteen,cite:BasicPolygons"
                                + "&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(5, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(4, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(1, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }

    @Test
    public void testCombinedLocalMaxesBiggerRequestOverride() throws Exception {
        // fifteen has 15 features, basic polygons 3
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(3);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        getCatalog().save(info);

        info.setMaxFeatures(2);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&srsName=EPSG:4326&typename=cdf:Fifteen,cite:BasicPolygon"
                                + "s&version=1.0.0&service=wfs&maxFeatures=4");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(4, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(3, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(1, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }

    @Test
    public void testMaxFeaturesBreak() throws Exception {
        // See https://osgeo-org.atlassian.net/browse/GEOS-1489
        FeatureTypeInfo info = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        info.setMaxFeatures(3);
        getCatalog().save(info);

        info = getFeatureTypeInfo(SystemTestData.BASIC_POLYGONS);
        info.setMaxFeatures(2);
        getCatalog().save(info);

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen,cite:BasicPolygon"
                                + "s&version=1.0.0&service=wfs&maxFeatures=3");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(3, doc.getElementsByTagName("gml:featureMember").getLength());
        assertEquals(3, doc.getElementsByTagName("cdf:Fifteen").getLength());
        assertEquals(0, doc.getElementsByTagName("cite:BasicPolygons").getLength());
    }
}
