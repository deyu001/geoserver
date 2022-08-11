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
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.namespace.QName;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReprojectionWriteTest extends WFSTestSupport {
    private static final String TARGET_CRS_CODE = "EPSG:900913";
    public static QName NULL_GEOMETRIES =
            new QName(SystemTestData.CITE_URI, "NullGeometries", SystemTestData.CITE_PREFIX);
    public static QName GOOGLE =
            new QName(SystemTestData.CITE_URI, "GoogleFeatures", SystemTestData.CITE_PREFIX);
    MathTransform tx;

    @Before
    public void setUpTX() throws Exception {
        CoordinateReferenceSystem epsg4326 = CRS.decode(TARGET_CRS_CODE);
        CoordinateReferenceSystem epsg32615 = CRS.decode("EPSG:32615");

        tx = CRS.findMathTransform(epsg32615, epsg4326);

        this.revertLayer(SystemTestData.POLYGONS);
    }

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {
        dataDirectory.addVectorLayer(
                NULL_GEOMETRIES, Collections.emptyMap(), getClass(), getCatalog());
        Map<LayerProperty, Object> extra = new HashMap<>();
        extra.put(LayerProperty.PROJECTION_POLICY, ProjectionPolicy.REPROJECT_TO_DECLARED);
        extra.put(LayerProperty.SRS, 900913);
        dataDirectory.addVectorLayer(GOOGLE, extra, getClass(), getCatalog());
    }

    @Test
    public void testInsertSrsName() throws Exception {
        String q =
                "wfs?request=getfeature&service=wfs&version=1.0.0&typeName="
                        + SystemTestData.POLYGONS.getLocalPart();
        Document dom = getAsDOM(q);

        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName(polygonProperty, "gml:coordinates");

        double[] c = coordinates(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length / 2);

        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:cgf=\""
                        + SystemTestData.CGF_URI
                        + "\">"
                        + "<wfs:Insert handle=\"insert-1\" srsName=\""
                        + TARGET_CRS_CODE
                        + "\">"
                        + " <cgf:Polygons>"
                        + "<cgf:polygonProperty>"
                        + "<gml:Polygon >"
                        + "<gml:outerBoundaryIs>"
                        + "<gml:LinearRing>"
                        + "<gml:coordinates>";
        for (int i = 0; i < cr.length; ) {
            xml += cr[i++] + "," + cr[i++];
            if (i < cr.length - 1) {
                xml += " ";
            }
        }
        xml +=
                "</gml:coordinates>"
                        + "</gml:LinearRing>"
                        + "</gml:outerBoundaryIs>"
                        + "</gml:Polygon>"
                        + "</cgf:polygonProperty>"
                        + " </cgf:Polygons>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";
        postAsDOM("wfs", xml);

        dom = getAsDOM(q);

        assertEquals(
                2,
                dom.getElementsByTagName(
                                SystemTestData.POLYGONS.getPrefix()
                                        + ":"
                                        + SystemTestData.POLYGONS.getLocalPart())
                        .getLength());
    }

    @Test
    public void testInsertGeomSrsName() throws Exception {
        String q =
                "wfs?request=getfeature&service=wfs&version=1.0&typeName="
                        + SystemTestData.POLYGONS.getLocalPart();
        Document dom = getAsDOM(q);

        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName(polygonProperty, "gml:coordinates");

        double[] c = coordinates(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length / 2);

        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:cgf=\""
                        + SystemTestData.CGF_URI
                        + "\">"
                        + "<wfs:Insert handle=\"insert-1\">"
                        + " <cgf:Polygons>"
                        + "<cgf:polygonProperty>"
                        + "<gml:Polygon srsName=\""
                        + TARGET_CRS_CODE
                        + "\">"
                        + "<gml:outerBoundaryIs>"
                        + "<gml:LinearRing>"
                        + "<gml:coordinates>";
        for (int i = 0; i < cr.length; ) {
            xml += cr[i++] + "," + cr[i++];
            if (i < cr.length - 1) {
                xml += " ";
            }
        }
        xml +=
                "</gml:coordinates>"
                        + "</gml:LinearRing>"
                        + "</gml:outerBoundaryIs>"
                        + "</gml:Polygon>"
                        + "</cgf:polygonProperty>"
                        + " </cgf:Polygons>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";
        postAsDOM("wfs", xml);

        dom = getAsDOM(q);

        assertEquals(
                2,
                dom.getElementsByTagName(
                                SystemTestData.POLYGONS.getPrefix()
                                        + ":"
                                        + SystemTestData.POLYGONS.getLocalPart())
                        .getLength());
    }

    @Test
    public void testUpdate() throws Exception {
        String q =
                "wfs?request=getfeature&service=wfs&version=1.0&typeName="
                        + SystemTestData.POLYGONS.getLocalPart();

        Document dom = getAsDOM(q);

        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName(polygonProperty, "gml:coordinates");

        double[] c = coordinates(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length / 2);

        // perform an update
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Update typeName=\"cgf:Polygons\" > "
                        + "<wfs:Property>"
                        + "<wfs:Name>polygonProperty</wfs:Name>"
                        + "<wfs:Value>"
                        + "<gml:Polygon srsName=\""
                        + TARGET_CRS_CODE
                        + "\">"
                        + "<gml:outerBoundaryIs>"
                        + "<gml:LinearRing>"
                        + "<gml:coordinates>";
        for (int i = 0; i < cr.length; ) {
            xml += cr[i++] + "," + cr[i++];
            if (i < cr.length - 1) {
                xml += " ";
            }
        }
        xml +=
                "</gml:coordinates>"
                        + "</gml:LinearRing>"
                        + "</gml:outerBoundaryIs>"
                        + "</gml:Polygon>"
                        + "</wfs:Value>"
                        + "</wfs:Property>"
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo>"
                        + "<ogc:PropertyName>id</ogc:PropertyName>"
                        + "<ogc:Literal>t0002</ogc:Literal>"
                        + "</ogc:PropertyIsEqualTo>"
                        + "</ogc:Filter>"
                        + "</wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        Element success = getFirstElementByTagName(dom, "wfs:SUCCESS");
        assertNotNull(success);

        dom = getAsDOM(q);

        polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        posList = getFirstElementByTagName(polygonProperty, "gml:coordinates");
        double[] c1 = coordinates(posList.getFirstChild().getNodeValue());

        assertEquals(c.length, c1.length);
        for (int i = 0; i < c.length; i++) {
            int x = (int) (c[i] + 0.5);
            int y = (int) (c1[i] + 0.5);

            assertEquals(x, y);
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
}
