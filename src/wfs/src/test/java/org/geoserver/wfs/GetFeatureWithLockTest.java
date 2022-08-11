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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetFeatureWithLockTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData systemTestData) throws Exception {
        getServiceDescriptor11().getOperations().add("ReleaseLock");
    }

    @Test
    public void testUpdateLockedFeatureWithLockId() throws Exception {
        // get feature
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        // get a fid
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertNotEquals(0, dom.getElementsByTagName("cdf:Locks").getLength());

        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock a feature
        xml =
                "<wfs:GetFeatureWithLock "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\">"
                        + "<ogc:Filter>"
                        + "<ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml =
                "<wfs:Transaction "
                        + "  service=\"WFS\" "
                        + "  version=\"1.0.0\" "
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\"> "
                        + "    <wfs:Property> "
                        + "      <wfs:Name>cdf:id</wfs:Name> "
                        + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                        + "    </wfs:Property> "
                        + "    <ogc:Filter> "
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/> "
                        + "    </ogc:Filter> "
                        + "  </wfs:Update> "
                        + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testUpdateLockedFeatureWithoutLockId() throws Exception {

        // get feature
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);

        // get a fid
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertNotEquals(0, dom.getElementsByTagName("cdf:Locks").getLength());

        String fid = ((Element) dom.getElementsByTagName("cdf:Locks").item(0)).getAttribute("fid");

        // lock a feature
        xml =
                "<wfs:GetFeatureWithLock "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "expiry=\"10\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + "<wfs:Query typeName=\"cdf:Locks\">"
                        + "<ogc:Filter>"
                        + "<ogc:FeatureId fid=\""
                        + fid
                        + "\"/>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");

        // try to update it
        xml =
                "<wfs:Transaction "
                        + "  service=\"WFS\" "
                        + "  version=\"1.0.0\" "
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "  <wfs:Update typeName=\"cdf:Locks\"> "
                        + "    <wfs:Property> "
                        + "      <wfs:Name>cdf:id</wfs:Name> "
                        + "      <wfs:Value>gfwlbt0001</wfs:Value> "
                        + "    </wfs:Property> "
                        + "    <ogc:Filter> "
                        + "      <ogc:FeatureId fid=\""
                        + fid
                        + "\"/> "
                        + "    </ogc:Filter> "
                        + "  </wfs:Update> "
                        + "</wfs:Transaction> ";

        dom = postAsDOM("wfs", xml);

        // release the lock
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        // assertEquals( "wfs:WFS_TransactionResponse",
        // dom.getDocumentElement().getNodeName() );
        assertTrue(
                1 == dom.getElementsByTagName("wfs:FAILED").getLength()
                        || "ServiceExceptionReport".equals(dom.getDocumentElement().getNodeName()));
    }

    @Test
    public void testGetFeatureWithLockReleaseActionSome() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("fid");
        String fid2 = ((Element) locks.item(1)).getAttribute("fid");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        // release locks
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testGetFeatureWithLockReleaseActionSome2() throws Exception {
        String xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"cdf:Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\"Locks.1\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);

        // relase with "some" but actually releases the only locked feature
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"cdf:Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\"Locks.1\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("fid");
        String fid2 = ((Element) locks.item(1)).getAttribute("fid");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/wfs", xml);

        // release locks
        get("cdf/wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }

    @Test
    public void testLayerQualified() throws Exception {
        String xml =
                "<wfs:GetFeature"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\"/>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("cdf/Locks/wfs", xml);

        // get two fids
        NodeList locks = dom.getElementsByTagName("cdf:Locks");
        String fid1 = ((Element) locks.item(0)).getAttribute("fid");
        String fid2 = ((Element) locks.item(1)).getAttribute("fid");

        xml =
                "<wfs:GetFeatureWithLock"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  expiry=\"10\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:Query typeName=\"Locks\">"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Query>"
                        + "</wfs:GetFeatureWithLock>";

        dom = postAsDOM("cdf/Fifteen/wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", dom);

        dom = postAsDOM("cdf/Locks/wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());

        String lockId = dom.getDocumentElement().getAttribute("lockId");
        // System.out.println(lockId);
        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  releaseAction=\"SOME\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0003</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid1
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/Locks/wfs", xml);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        xml =
                "<wfs:Transaction"
                        + "  service=\"WFS\""
                        + "  version=\"1.0.0\""
                        + "  xmlns:cdf=\"http://www.opengis.net/cite/data\""
                        + "  xmlns:ogc=\"http://www.opengis.net/ogc\""
                        + "  xmlns:wfs=\"http://www.opengis.net/wfs\""
                        + ">"
                        + "  <wfs:LockId>"
                        + lockId
                        + "</wfs:LockId>"
                        + "  <wfs:Update typeName=\"Locks\">"
                        + "    <wfs:Property>"
                        + "      <wfs:Name>cdf:id</wfs:Name>"
                        + "      <wfs:Value>gfwlrs0004</wfs:Value>"
                        + "    </wfs:Property>"
                        + "    <ogc:Filter>"
                        + "      <ogc:FeatureId fid=\""
                        + fid2
                        + "\"/>"
                        + "    </ogc:Filter>"
                        + "  </wfs:Update>"
                        + "</wfs:Transaction>";

        dom = postAsDOM("cdf/Locks/wfs", xml);

        // release locks
        get("cdf/Locks/wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);

        assertEquals("wfs:WFS_TransactionResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:SUCCESS").getLength());
    }
}
