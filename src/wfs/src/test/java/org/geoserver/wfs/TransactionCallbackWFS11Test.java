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

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.data.test.MockData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class TransactionCallbackWFS11Test extends WFSTestSupport {

    public static final String DELETE_ROAD_102 =
            "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                    + " xmlns:cite=\"http://www.opengis.net/cite\""
                    + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                    + " xmlns:gml=\"http://www.opengis.net/gml\""
                    + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                    + " <wfs:Delete typeName=\"cite:RoadSegments\">"
                    + "   <ogc:Filter>"
                    + "     <ogc:PropertyIsEqualTo>"
                    + "       <ogc:PropertyName>FID</ogc:PropertyName>"
                    + "       <ogc:Literal>102</ogc:Literal>"
                    + "     </ogc:PropertyIsEqualTo>"
                    + "   </ogc:Filter>"
                    + " </wfs:Delete>"
                    + "</wfs:Transaction>";
    private TransactionCallbackTester plugin;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/wfs/TransactionCallbackTestContext.xml");
    }

    @Before
    public void clearState() throws Exception {
        revertLayer(MockData.ROAD_SEGMENTS);
        plugin =
                (TransactionCallbackTester) applicationContext.getBean("transactionCallbackTester");
        plugin.clear();
    }

    @Test
    public void testInsert() throws Exception {
        // perform an insert
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert srsName=\"EPSG:32615\"> "
                        + "<cgf:Points>"
                        + "<cgf:pointProperty>"
                        + "<gml:Point>"
                        + "<gml:pos>1 1</gml:pos>"
                        + "</gml:Point>"
                        + "</cgf:pointProperty>"
                        + "<cgf:id>t0003</cgf:id>"
                        + "</cgf:Points>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insert);
        // print(dom);
        assertXpathEvaluatesTo("1", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(1, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the id has been modified
        Document pointFeatures =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cgf:Points"
                                + "&CQL_FILTER=id='t0003-modified'");
        // print(pointFeatures);
        assertXpathEvaluatesTo("1", "count(//cgf:Points)", pointFeatures);
    }

    @Test
    public void testUpdate() throws Exception {
        String xml =
                "<wfs:Transaction service=\"WFS\" version=\"1.1.0\""
                        + " xmlns:cite=\"http://www.opengis.net/cite\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + " xmlns:gml=\"http://www.opengis.net/gml\""
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:Update typeName=\"cite:RoadSegments\">"
                        + "   <wfs:Property>"
                        + "     <wfs:Name>cite:the_geom</wfs:Name>"
                        + "     <wfs:Value>"
                        + "      <gml:MultiLineString xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "       <gml:lineStringMember>"
                        + "         <gml:LineString>"
                        + "            <gml:posList>4.2582 52.0643 4.2584 52.0648</gml:posList>"
                        + "         </gml:LineString>"
                        + "       </gml:lineStringMember>"
                        + "      </gml:MultiLineString>"
                        + "     </wfs:Value>"
                        + "   </wfs:Property>"
                        + "   <ogc:Filter>"
                        + "     <ogc:PropertyIsEqualTo>"
                        + "       <ogc:PropertyName>FID</ogc:PropertyName>"
                        + "       <ogc:Literal>102</ogc:Literal>"
                        + "     </ogc:PropertyIsEqualTo>"
                        + "   </ogc:Filter>"
                        + " </wfs:Update>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("1", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(1, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the road name has been modified too
        Document roadSegments =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments"
                                + "&CQL_FILTER=FID=102");
        // print(roadSegments);
        assertXpathEvaluatesTo(
                TransactionCallbackTester.FOLSOM_STREET,
                "//cite:RoadSegments/cite:NAME",
                roadSegments);
    }

    @Test
    public void testDelete() throws Exception {
        // the plugin swaps the filter with id > 102
        String xml = DELETE_ROAD_102;

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("4", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(4, plugin.result.getTotalDeleted().intValue());

        // check the one surviving road segment has id 102
        Document roadSegments =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("1", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo("102", "//cite:RoadSegments/cite:FID", roadSegments);
    }

    @Test
    public void testReplaceWithInsert() throws Exception {
        // the plugin will remove all elements and replace it with an insert
        plugin.beforeTransaction = TransactionCallbackTester::replaceWithFixedRoadsInsert;
        String xml = DELETE_ROAD_102;

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("1", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(1, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the new feature is there
        Document roadSegments =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("6", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo(
                "New Road", "//cite:RoadSegments[cite:FID = 107]/cite:NAME", roadSegments);
    }

    @Test
    public void testReplaceWithUpdate() throws Exception {
        // the plugin will remove all elements and replace it with a fixed delete on road 106
        plugin.beforeTransaction = TransactionCallbackTester::replaceWithFixedRoadsUpdate;
        String xml = DELETE_ROAD_102;

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("1", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(1, plugin.result.getTotalUpdated().intValue());
        assertEquals(0, plugin.result.getTotalDeleted().intValue());

        // check the new feature is there
        Document roadSegments =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("5", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo(
                "Clean Road", "//cite:RoadSegments[cite:FID = 106]/cite:NAME", roadSegments);
    }

    @Test
    public void testReplaceWithDelete() throws Exception {
        // the plugin will remove all elements and replace it with a fixed delete on road 106
        plugin.beforeTransaction = TransactionCallbackTester::replaceWithFixedRoadsDelete;
        String xml = DELETE_ROAD_102;

        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertXpathEvaluatesTo("0", "//wfs:totalInserted", dom);
        assertXpathEvaluatesTo("0", "//wfs:totalUpdated", dom);
        assertXpathEvaluatesTo("1", "//wfs:totalDeleted", dom);

        // check the plugin reports
        assertTrue(plugin.beforeCommitCalled);
        assertTrue(plugin.committed);
        assertTrue(plugin.dataStoreChanged);
        assertEquals(0, plugin.result.getTotalInserted().intValue());
        assertEquals(0, plugin.result.getTotalUpdated().intValue());
        assertEquals(1, plugin.result.getTotalDeleted().intValue());

        // check the new feature is there
        Document roadSegments =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=cite:RoadSegments");
        // print(roadSegments);
        assertXpathEvaluatesTo("4", "count(//cite:RoadSegments)", roadSegments);
        assertXpathEvaluatesTo("0", "count(//cite:RoadSegments[cite:FID = 106])", roadSegments);
    }
}
