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

import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.filter.v1_1.OGC;
import org.junit.Test;
import org.w3c.dom.Document;

public class LockFeatureTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        Service service = (Service) GeoServerExtensions.bean("wfsService-1.1.0");
        // register fake operation to ease testing
        service.getOperations().add("ReleaseLock");
    }

    @Test
    public void testLock() throws Exception {
        String xml =
                "<wfs:LockFeature xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" xmlns:wfs=\"http://www.opengis.net/wfs\" expiry=\"5\" handle=\"LockFeature-tc1\" "
                        + " lockAction=\"ALL\" "
                        + " service=\"WFS\" "
                        + " version=\"1.1.0\">"
                        + "<wfs:Lock handle=\"lock-1\" typeName=\"sf:PrimitiveGeoFeature\"/>"
                        + "</wfs:LockFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(5, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());

        // release the lock
        releaseLock(dom);
    }

    @Test
    public void testLockGet() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=LockFeature&typename=sf:GenericEntity",
                        200);

        print(dom);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(3, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());

        // release the lock
        releaseLock(dom);
    }

    @Test
    public void testLockWithNamespacesGet() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=LockFeature&typename=ns53:GenericEntity"
                                + "&namespace=xmlns(ns53=http://cite.opengeospatial.org/gmlsf)",
                        200);

        // print(dom);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(3, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());
        releaseLock(dom);
    }

    public void releaseLock(Document dom) throws Exception {
        // release the lock
        String lockId = XMLUnit.newXpathEngine().evaluate("//wfs:LockId", dom);
        get("wfs?request=ReleaseLock&version=1.1.0&lockId=" + lockId);
    }

    @Test
    public void testLockByBBOX() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=LockFeature&typeName=sf:PrimitiveGeoFeature"
                                + "&BBOX=57.0,-4.5,62.0,1.0,EPSG:4326",
                        200);

        // print(dom);
        assertEquals("wfs:LockFeatureResponse", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagNameNS(OGC.NAMESPACE, "FeatureId").getLength());

        // release the lock
        releaseLock(dom);
    }
}
