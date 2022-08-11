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

import org.geoserver.data.test.CiteTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class GeometrylessWriteTest extends WFSTestSupport {

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.GEOMETRYLESS);
    }

    @Test
    public void testUpdate() throws Exception {
        // perform an update
        String update =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cite=\"http://www.opengis.net/cite\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Update typeName=\"cite:Geometryless\" > "
                        + "<wfs:Property>"
                        + "<wfs:Name>name</wfs:Name>"
                        + "<wfs:Value>AnotherName</wfs:Value>"
                        + "</wfs:Property>"
                        + "<ogc:Filter>"
                        + "<ogc:FeatureId fid=\"Geometryless.2\"/>"
                        + "</ogc:Filter>"
                        + "</wfs:Update>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", update);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs&featureId=Geometryless.2");
        assertEquals(
                "AnotherName",
                dom.getElementsByTagName("cite:name").item(0).getFirstChild().getNodeValue());
    }

    @Test
    public void testDelete() throws Exception {
        // perform an update
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cite=\"http://www.opengis.net/cite\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Delete typeName=\"cite:Geometryless\" > "
                        + "<ogc:Filter>"
                        + "<ogc:FeatureId fid=\"Geometryless.2\"/>"
                        + "</ogc:Filter>"
                        + "</wfs:Delete>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insert);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());

        // do another get feature
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs&featureId=Geometryless.2");
        assertEquals(0, dom.getElementsByTagName("cite:Geometryless").getLength());
    }

    @Test
    public void testInsert() throws Exception {
        // perform an insert
        String insert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" "
                        + "xmlns:cite=\"http://www.opengis.net/cite\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\"> "
                        + "<wfs:Insert > "
                        + "<cite:Geometryless fid=\"Geometryless.4\">"
                        + "<cite:name>Gimbo</cite:name>"
                        + "<cite:number>1000</cite:number>"
                        + "</cite:Geometryless>"
                        + "</wfs:Insert>"
                        + "</wfs:Transaction>";

        Document dom = postAsDOM("wfs", insert);
        print(dom);
        assertNotEquals(0, dom.getElementsByTagName("wfs:SUCCESS").getLength());
        assertNotEquals(0, dom.getElementsByTagName("wfs:InsertResult").getLength());

        // do another get feature
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cite:Geometryless&version=1.0.0&service=wfs");
        assertEquals(4, dom.getElementsByTagName("cite:Geometryless").getLength());
    }
}
