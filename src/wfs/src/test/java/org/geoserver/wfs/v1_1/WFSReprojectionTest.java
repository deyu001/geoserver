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

package org.geoserver.wfs.v1_1;

import static org.junit.Assert.assertEquals;

import java.util.StringTokenizer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WFSReprojectionTest extends WFSTestSupport {
    private static final String TARGET_CRS_CODE = "EPSG:900913";
    MathTransform tx;
    CoordinateReferenceSystem epsg32615;

    @Before
    public void init() throws Exception {

        CoordinateReferenceSystem epsgTarget = CRS.decode(TARGET_CRS_CODE);
        epsg32615 = CRS.decode("urn:x-ogc:def:crs:EPSG:6.11.2:32615");

        tx = CRS.findMathTransform(epsg32615, epsgTarget);
    }

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);
    }

    @Test
    public void testGetFeatureGet() throws Exception {

        Document dom1 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart());
        print(dom1);
        Document dom2 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart()
                                + "&srsName="
                                + TARGET_CRS_CODE);
        print(dom2);

        runTest(dom1, dom2);
    }

    @Test
    public void testGetFeaturePost() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\""
                        + SystemTestData.POLYGONS.getPrefix()
                        + ":"
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "\"> "
                        + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom1 = postAsDOM("wfs", xml);
        //        print(dom1);

        xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query srsName=\""
                        + TARGET_CRS_CODE
                        + "\" typeName=\""
                        + SystemTestData.POLYGONS.getPrefix()
                        + ":"
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "\"> "
                        + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        Document dom2 = postAsDOM("wfs", xml);
        //        print(dom2);

        runTest(dom1, dom2);
    }

    @Test
    public void testGetFeatureWithAutoBoxGet() throws Exception {
        WFSInfo wfs = getWFS();
        boolean oldFeatureBounding = wfs.isFeatureBounding();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        try {
            String q =
                    "wfs?request=getfeature&service=wfs&version=1.1&typeName="
                            + SystemTestData.POLYGONS.getLocalPart();
            Document dom = getAsDOM(q);
            // print(dom);
            Element envelope = getFirstElementByTagName(dom, "gml:Envelope");
            String lc =
                    getFirstElementByTagName(envelope, "gml:lowerCorner")
                            .getFirstChild()
                            .getNodeValue();
            String uc =
                    getFirstElementByTagName(envelope, "gml:upperCorner")
                            .getFirstChild()
                            .getNodeValue();
            double[] c =
                    new double[] {
                        Double.parseDouble(lc.split(" ")[0]),
                        Double.parseDouble(lc.split(" ")[1]),
                        Double.parseDouble(uc.split(" ")[0]),
                        Double.parseDouble(uc.split(" ")[1])
                    };

            // use an equirectangular projection
            String targetCrsCode = "AUTO:42004,9001,0,33";
            CoordinateReferenceSystem epsgAUTO = CRS.decode(targetCrsCode);
            MathTransform txAuto = CRS.findMathTransform(epsg32615, epsgAUTO);

            // System.out.println(Arrays.toString(c));

            double[] cr = new double[4];
            txAuto.transform(c, 0, cr, 0, 2);

            q += "&bbox=" + cr[0] + "," + cr[1] + "," + cr[2] + "," + cr[3] + "," + targetCrsCode;
            dom = getAsDOM(q);

            assertEquals(
                    1,
                    dom.getElementsByTagName(
                                    SystemTestData.POLYGONS.getPrefix()
                                            + ":"
                                            + SystemTestData.POLYGONS.getLocalPart())
                            .getLength());
        } finally {
            wfs.setFeatureBounding(oldFeatureBounding);
            getGeoServer().save(wfs);
        }
    }

    @Test
    public void testGetFeatureWithProjectedBoxGet() throws Exception {
        WFSInfo wfs = getWFS();
        boolean oldFeatureBounding = wfs.isFeatureBounding();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        try {
            String q =
                    "wfs?request=getfeature&service=wfs&version=1.1&typeName="
                            + SystemTestData.POLYGONS.getLocalPart();
            Document dom = getAsDOM(q);
            //        print(dom);
            Element envelope = getFirstElementByTagName(dom, "gml:Envelope");
            String lc =
                    getFirstElementByTagName(envelope, "gml:lowerCorner")
                            .getFirstChild()
                            .getNodeValue();
            String uc =
                    getFirstElementByTagName(envelope, "gml:upperCorner")
                            .getFirstChild()
                            .getNodeValue();
            double[] c =
                    new double[] {
                        Double.parseDouble(lc.split(" ")[0]), Double.parseDouble(lc.split(" ")[1]),
                        Double.parseDouble(uc.split(" ")[0]), Double.parseDouble(uc.split(" ")[1])
                    };
            double[] cr = new double[4];
            tx.transform(c, 0, cr, 0, 2);

            q += "&bbox=" + cr[0] + "," + cr[1] + "," + cr[2] + "," + cr[3] + "," + TARGET_CRS_CODE;
            dom = getAsDOM(q);

            assertEquals(
                    1,
                    dom.getElementsByTagName(
                                    SystemTestData.POLYGONS.getPrefix()
                                            + ":"
                                            + SystemTestData.POLYGONS.getLocalPart())
                            .getLength());
        } finally {
            wfs.setFeatureBounding(oldFeatureBounding);
            getGeoServer().save(wfs);
        }
    }

    @Test
    public void testGetFeatureWithProjectedBoxPost() throws Exception {
        WFSInfo wfs = getWFS();
        boolean oldFeatureBounding = wfs.isFeatureBounding();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        try {
            String q =
                    "wfs?request=getfeature&service=wfs&version=1.1&typeName="
                            + SystemTestData.POLYGONS.getLocalPart();
            Document dom = getAsDOM(q);
            Element envelope = getFirstElementByTagName(dom, "gml:Envelope");
            String lc =
                    getFirstElementByTagName(envelope, "gml:lowerCorner")
                            .getFirstChild()
                            .getNodeValue();
            String uc =
                    getFirstElementByTagName(envelope, "gml:upperCorner")
                            .getFirstChild()
                            .getNodeValue();
            double[] c =
                    new double[] {
                        Double.parseDouble(lc.split(" ")[0]), Double.parseDouble(lc.split(" ")[1]),
                        Double.parseDouble(uc.split(" ")[0]), Double.parseDouble(uc.split(" ")[1])
                    };
            double[] cr = new double[4];
            tx.transform(c, 0, cr, 0, 2);

            String xml =
                    "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\""
                            + " xmlns:"
                            + SystemTestData.POLYGONS.getPrefix()
                            + "=\""
                            + SystemTestData.POLYGONS.getNamespaceURI()
                            + "\""
                            + " xmlns:ogc=\"http://www.opengis.net/ogc\" "
                            + " xmlns:gml=\"http://www.opengis.net/gml\" "
                            + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                            + "> "
                            + "<wfs:Query typeName=\""
                            + SystemTestData.POLYGONS.getPrefix()
                            + ":"
                            + SystemTestData.POLYGONS.getLocalPart()
                            + "\">"
                            + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                            + "<ogc:Filter>"
                            + "<ogc:BBOX>"
                            + "<ogc:PropertyName>polygonProperty</ogc:PropertyName>"
                            + "<gml:Envelope srsName=\""
                            + TARGET_CRS_CODE
                            + "\">"
                            + "<gml:lowerCorner>"
                            + cr[0]
                            + " "
                            + cr[1]
                            + "</gml:lowerCorner>"
                            + "<gml:upperCorner>"
                            + cr[2]
                            + " "
                            + cr[3]
                            + "</gml:upperCorner>"
                            + "</gml:Envelope>"
                            + "</ogc:BBOX>"
                            + "</ogc:Filter>"
                            + "</wfs:Query> "
                            + "</wfs:GetFeature>";

            dom = postAsDOM("wfs", xml);

            assertEquals(
                    1,
                    dom.getElementsByTagName(
                                    SystemTestData.POLYGONS.getPrefix()
                                            + ":"
                                            + SystemTestData.POLYGONS.getLocalPart())
                            .getLength());
        } finally {
            wfs.setFeatureBounding(oldFeatureBounding);
            getGeoServer().save(wfs);
        }
    }

    private void runTest(Document dom1, Document dom2) throws Exception {
        Element box = getFirstElementByTagName(dom1.getDocumentElement(), "gml:Box");
        Element coordinates = getFirstElementByTagName(box, "gml:coordinates");
        double[] d1 = coordinates(coordinates.getFirstChild().getNodeValue());

        box = getFirstElementByTagName(dom2.getDocumentElement(), "gml:Box");
        coordinates = getFirstElementByTagName(box, "gml:coordinates");
        double[] d2 = coordinates(coordinates.getFirstChild().getNodeValue());

        double[] d3 = new double[d1.length];
        tx.transform(d1, 0, d3, 0, d1.length / 2);

        for (int i = 0; i < d2.length; i++) {
            assertEquals(d2[i], d3[i], 0.001);
        }
    }

    private double[] coordinates(String string) {
        StringTokenizer st = new StringTokenizer(string, " ");
        double[] coordinates = new double[st.countTokens() * 2];
        int i = 0;
        while (st.hasMoreTokens()) {
            String tuple = st.nextToken();
            coordinates[i++] = Double.parseDouble(tuple.split(",")[0]);
            coordinates[i++] = Double.parseDouble(tuple.split(",")[1]);
        }

        return coordinates;
    }

    @Test
    public void testFilterReprojection() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service='WFS' "
                        + "version='1.0.0' "
                        + "xmlns:cdf='http://www.opengis.net/cite/data' "
                        + "xmlns:ogc='http://www.opengis.net/ogc' "
                        + "xmlns:wfs='http://www.opengis.net/wfs' "
                        + "> "
                        + "<wfs:Query typeName='"
                        + SystemTestData.POLYGONS.getPrefix()
                        + ":"
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "' "
                        + " srsName='"
                        + TARGET_CRS_CODE
                        + "'> "
                        + "> "
                        + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                        + "<ogc:Filter>"
                        + "<ogc:Intersects>"
                        + "<ogc:PropertyName>polygonProperty</ogc:PropertyName>"
                        + "<gml:Point xmlns:gml='http://www.opengis.net/gml'>"
                        + "<gml:coordinates decimal='.' cs=',' ts=' '>-1.035246176730227E7,504135.14926478104</gml:coordinates>"
                        + "</gml:Point>"
                        + "</ogc:Intersects>"
                        + "</ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals(
                1,
                dom.getElementsByTagName(
                                SystemTestData.POLYGONS.getPrefix()
                                        + ":"
                                        + SystemTestData.POLYGONS.getLocalPart())
                        .getLength());
    }
}
