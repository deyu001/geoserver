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
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.UpdateElementType;
import org.geoserver.data.test.CiteTestData;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.w3c.dom.Document;

/**
 * This test must be run with the server configured with the wfs 1.0 cite configuration, with data
 * initialized.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class TransactionListenerTest extends WFSTestSupport {

    TransactionListenerTester listener;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/wfs/TransactionListenerTestContext.xml");
    }

    @Before
    public void clearState() throws Exception {
        listener =
                (TransactionListenerTester) applicationContext.getBean("transactionListenerTester");
        listener.clear();
    }

    @Test
    public void testDelete() throws Exception {
        // perform a delete
        String delete =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\"> "
                        + "<wfs:Delete typeName=\"cgf:Points\"> "
                        + "<ogc:Filter> "
                        + "<ogc:PropertyIsEqualTo> "
                        + "<ogc:PropertyName>cgf:id</ogc:PropertyName> "
                        + "<ogc:Literal>t0000</ogc:Literal> "
                        + "</ogc:PropertyIsEqualTo> "
                        + "</ogc:Filter> "
                        + "</wfs:Delete> "
                        + "</wfs:Transaction>";

        postAsDOM("wfs", delete);
        assertEquals(1, listener.events.size());
        TransactionEvent event = listener.events.get(0);
        assertTrue(event.getSource() instanceof DeleteElementType);
        assertEquals(TransactionEventType.PRE_DELETE, event.getType());
        assertEquals(CiteTestData.POINTS, event.getLayerName());
        assertEquals(1, listener.features.size());
        Feature deleted = listener.features.get(0);
        assertEquals("t0000", deleted.getProperty("id").getValue());
    }

    @Test
    public void testInsert() throws Exception {
        // perform an insert
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert > "
                        + "<cgf:Lines>"
                        + "<cgf:lineStringProperty>"
                        + "<gml:LineString>"
                        + "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">"
                        + "494475.71056415,5433016.8189323 494982.70115662,5435041.95096618"
                        + "</gml:coordinates>"
                        + "</gml:LineString>"
                        + "</cgf:lineStringProperty>"
                        + "<cgf:id>t0002</cgf:id>"
                        + "</cgf:Lines>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        postAsDOM("wfs", insert);
        assertEquals(2, listener.events.size());

        TransactionEvent firstEvent = listener.events.get(0);
        assertTrue(firstEvent.getSource() instanceof InsertElementType);
        assertEquals(TransactionEventType.PRE_INSERT, firstEvent.getType());
        assertEquals(CiteTestData.LINES, firstEvent.getLayerName());
        // one feature from the pre-insert hook, one from the post-insert hook
        assertEquals(2, listener.features.size());

        // what was the fid of the inserted feature?
        String getFeature =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cgf:Lines\"> "
                        + "<ogc:PropertyName>id</ogc:PropertyName> "
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo>"
                        + "<ogc:PropertyName>id</ogc:PropertyName>"
                        + "<ogc:Literal>t0002</ogc:Literal>"
                        + "</ogc:PropertyIsEqualTo>"
                        + "</ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", getFeature);
        String fid =
                dom.getElementsByTagName("cgf:Lines")
                        .item(0)
                        .getAttributes()
                        .item(0)
                        .getNodeValue();

        TransactionEvent secondEvent = listener.events.get(1);
        assertTrue(secondEvent.getSource() instanceof InsertElementType);
        assertEquals(TransactionEventType.POST_INSERT, secondEvent.getType());
        Feature inserted = listener.features.get(1);
        assertEquals(fid, inserted.getIdentifier().getID());
    }

    @Test
    public void testUpdate() throws Exception {
        // perform an update
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cgf=\"http://www.opengis.net/cite/geometry\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Update typeName=\"cgf:Polygons\" > "
                        + "<wfs:Property>"
                        + "<wfs:Name>id</wfs:Name>"
                        + "<wfs:Value>t0003</wfs:Value>"
                        + "</wfs:Property>"
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo>"
                        + "<ogc:PropertyName>id</ogc:PropertyName>"
                        + "<ogc:Literal>t0002</ogc:Literal>"
                        + "</ogc:PropertyIsEqualTo>"
                        + "</ogc:Filter>"
                        + "</wfs:Update>"
                        + "</wfs:Transaction>";

        postAsDOM("wfs", insert);
        assertEquals(2, listener.events.size());
        TransactionEvent firstEvent = listener.events.get(0);
        assertTrue(firstEvent.getSource() instanceof UpdateElementType);
        assertEquals(TransactionEventType.PRE_UPDATE, firstEvent.getType());
        assertEquals(CiteTestData.POLYGONS, firstEvent.getLayerName());
        Feature updatedBefore = listener.features.get(0);
        assertEquals("t0002", updatedBefore.getProperty("id").getValue());

        TransactionEvent secondEvent = listener.events.get(1);
        assertTrue(secondEvent.getSource() instanceof UpdateElementType);
        assertEquals(TransactionEventType.POST_UPDATE, secondEvent.getType());
        assertEquals(CiteTestData.POLYGONS, secondEvent.getLayerName());
        Feature updatedAfter = listener.features.get(1);
        assertEquals("t0003", updatedAfter.getProperty("id").getValue());

        assertEquals(2, listener.features.size());
    }
}
