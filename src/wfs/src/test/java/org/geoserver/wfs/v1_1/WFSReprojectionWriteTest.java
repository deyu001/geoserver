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
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WFSReprojectionWriteTest extends WFSTestSupport {
    private static final String TARGET_CRS_CODE = "EPSG:900913";
    MathTransform tx;

    @Override
    protected void setUpInternal(SystemTestData systemTestData) throws Exception {
        getServiceDescriptor11().getOperations().add("ReleaseLock");
    }

    @Before
    public void init() throws Exception {
        CoordinateReferenceSystem epsgTarget = CRS.decode(TARGET_CRS_CODE);
        CoordinateReferenceSystem epsg32615 = CRS.decode("urn:x-ogc:def:crs:EPSG:6.11.2:32615");

        tx = CRS.findMathTransform(epsg32615, epsgTarget);
    }

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.POLYGONS);
        revertLayer(CiteTestData.GENERICENTITY);
    }

    @Test
    public void testInsertSrsName() throws Exception {
        String q =
                "wfs?request=getfeature&service=wfs&version=1.1&typeName="
                        + SystemTestData.POLYGONS.getLocalPart();
        Document dom = getAsDOM(q);
        assertEquals(
                1,
                dom.getElementsByTagName(
                                SystemTestData.POLYGONS.getPrefix()
                                        + ":"
                                        + SystemTestData.POLYGONS.getLocalPart())
                        .getLength());

        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName(polygonProperty, "gml:posList");

        double[] c = posList(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length / 2);

        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
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
                        + "<gml:exterior>"
                        + "<gml:LinearRing>"
                        + "<gml:posList>";
        for (int i = 0; i < cr.length; i++) {
            xml += cr[i];
            if (i < cr.length - 1) {
                xml += " ";
            }
        }
        xml +=
                "</gml:posList>"
                        + "</gml:LinearRing>"
                        + "</gml:exterior>"
                        + "</gml:Polygon>"
                        + "</cgf:polygonProperty>"
                        + " </cgf:Polygons>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";
        postAsDOM("wfs", xml);

        dom = getAsDOM(q);
        //        print(dom);
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
                "wfs?request=getfeature&service=wfs&version=1.1&typeName="
                        + SystemTestData.POLYGONS.getLocalPart();
        Document dom = getAsDOM(q);

        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName(polygonProperty, "gml:posList");

        double[] c = posList(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length / 2);

        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
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
                        + "<gml:exterior>"
                        + "<gml:LinearRing>"
                        + "<gml:posList>";
        for (int i = 0; i < cr.length; i++) {
            xml += cr[i];
            if (i < cr.length - 1) {
                xml += " ";
            }
        }
        xml +=
                "</gml:posList>"
                        + "</gml:LinearRing>"
                        + "</gml:exterior>"
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
                "wfs?request=getfeature&service=wfs&version=1.1&typeName="
                        + SystemTestData.POLYGONS.getLocalPart();

        Document dom = getAsDOM(q);
        //        print(dom);

        Element polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        Element posList = getFirstElementByTagName(polygonProperty, "gml:posList");

        double[] c = posList(posList.getFirstChild().getNodeValue());
        double[] cr = new double[c.length];
        tx.transform(c, 0, cr, 0, cr.length / 2);

        // perform an update
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
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
                        + "<gml:exterior>"
                        + "<gml:LinearRing>"
                        + "<gml:posList>";
        for (int i = 0; i < cr.length; i++) {
            xml += cr[i];
            if (i < cr.length - 1) {
                xml += " ";
            }
        }
        xml +=
                "</gml:posList>"
                        + "</gml:LinearRing>"
                        + "</gml:exterior>"
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
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        Element totalUpdated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals("1", totalUpdated.getFirstChild().getNodeValue());

        dom = getAsDOM(q);
        polygonProperty = getFirstElementByTagName(dom, "cgf:polygonProperty");
        posList = getFirstElementByTagName(polygonProperty, "gml:posList");
        double[] c1 = posList(posList.getFirstChild().getNodeValue());

        assertEquals(c.length, c1.length);
        for (int i = 0; i < c.length; i++) {
            int x = (int) (c[i] + 0.5);
            int y = (int) (c1[i] + 0.5);

            assertEquals(x, y);
        }
    }

    @Test
    public void testUpdateReprojectFilter() throws Exception {
        testUpdateReprojectFilter("srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\"");
    }

    @Test
    public void testUpdateReprojectFilterDefaultCRS() throws Exception {
        testUpdateReprojectFilter("");
    }

    private void testUpdateReprojectFilter(String envelopeSRS) throws Exception {
        // slightly adapted from CITE WFS 1.1, "Test wfs:wfs-1.1.0-LockFeature-tc3.1"

        // perform an update
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Update handle=\"upd-1\" typeName=\"sf:GenericEntity\">"
                        + "<wfs:Property>"
                        + "  <wfs:Name>sf:boolProperty</wfs:Name>"
                        + "  <wfs:Value>true</wfs:Value>"
                        + "</wfs:Property>"
                        + "  <ogc:Filter>"
                        + "    <ogc:BBOX>"
                        + "      <ogc:PropertyName>sf:attribut.geom</ogc:PropertyName>"
                        + "        <gml:Envelope "
                        + envelopeSRS
                        + ">"
                        + "          <gml:lowerCorner>34.5 -10.0</gml:lowerCorner>"
                        + "          <gml:upperCorner>72.0 32.0</gml:upperCorner>"
                        + "        </gml:Envelope>"
                        + "    </ogc:BBOX>"
                        + "  </ogc:Filter>"
                        + "</wfs:Update> </wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        Element totalUpdated = getFirstElementByTagName(dom, "wfs:totalUpdated");
        assertEquals("3", totalUpdated.getFirstChild().getNodeValue());
    }

    @Test
    public void testDeleteReprojectFilter() throws Exception {
        testDeleteReprojectFilter("srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\"");
    }

    @Test
    public void testDeleteReprojectFilterDefaultCRS() throws Exception {
        testDeleteReprojectFilter("");
    }

    private void testDeleteReprojectFilter(String envelopeSRS) throws Exception {
        // slightly adapted from CITE WFS 1.1, "Test wfs:wfs-1.1.0-LockFeature-tc3.1"

        // perform an update
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Delete typeName=\"sf:GenericEntity\">"
                        + "  <ogc:Filter>"
                        + "    <ogc:BBOX>"
                        + "      <ogc:PropertyName>sf:attribut.geom</ogc:PropertyName>"
                        + "        <gml:Envelope "
                        + envelopeSRS
                        + ">"
                        + "          <gml:lowerCorner>34.5 -10.0</gml:lowerCorner>"
                        + "          <gml:upperCorner>72.0 32.0</gml:upperCorner>"
                        + "        </gml:Envelope>"
                        + "    </ogc:BBOX>"
                        + "  </ogc:Filter>"
                        + "</wfs:Delete> </wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:TransactionResponse", dom.getDocumentElement().getNodeName());
        Element totalUpdated = getFirstElementByTagName(dom, "wfs:totalDeleted");
        assertEquals("3", totalUpdated.getFirstChild().getNodeValue());
    }

    @Test
    public void testLockReprojectFilter() throws Exception {
        testLockReprojectFilter("srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\"");
    }

    @Test
    public void testLockReprojectFilterDefaultCRS() throws Exception {
        testLockReprojectFilter("");
    }

    private void testLockReprojectFilter(String envelopeSRS) throws Exception {
        // slightly adapted from CITE WFS 1.1, "Test wfs:wfs-1.1.0-LockFeature-tc3.1"

        // perform a lock
        String xml =
                "<wfs:LockFeature xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:myparsers=\"http://teamengine.sourceforge.net/parsers\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" "
                        + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                        + "expiry=\"5\" "
                        + "handle=\"LockFeature-tc3\" "
                        + "lockAction=\"ALL\" "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\"> "
                        + "<wfs:Lock handle=\"lock-1\" typeName=\"sf:GenericEntity\"> "
                        + "<ogc:Filter>"
                        + "<ogc:BBOX>"
                        + " <ogc:PropertyName>sf:attribut.geom</ogc:PropertyName>"
                        + " <gml:Envelope "
                        + envelopeSRS
                        + ">"
                        + "    <gml:lowerCorner>34.5 -10.0</gml:lowerCorner>"
                        + "    <gml:upperCorner>72.0 32.0</gml:upperCorner>"
                        + " </gml:Envelope>"
                        + "</ogc:BBOX>"
                        + "</ogc:Filter>"
                        + "</wfs:Lock>"
                        + "</wfs:LockFeature>";
        //        System.out.println(xml);

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(3, dom.getElementsByTagName("ogc:FeatureId").getLength());

        // release the lock
        String lockId = dom.getElementsByTagName("wfs:LockId").item(0).getTextContent();
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);
    }

    private double[] posList(String string) {
        StringTokenizer st = new StringTokenizer(string, " ");
        double[] coordinates = new double[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            coordinates[i++] = Double.parseDouble(st.nextToken());
        }

        return coordinates;
    }
}
