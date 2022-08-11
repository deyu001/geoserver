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

package org.geoserver.filter.function;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class WFSFilteringTest extends WFSTestSupport {

    static final String QUERY_SINGLE = //
            "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                    + //
                    "                xmlns:cite=\"http://www.opengis.net/cite\"\n"
                    + //
                    "                xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                    + //
                    "                service=\"WFS\" version=\"${version}\">\n"
                    + //
                    "  <wfs:Query typeName=\"cite:Buildings\">\n"
                    + //
                    "    <ogc:Filter>\n"
                    + //
                    "      <ogc:DWithin>\n"
                    + //
                    "        <ogc:PropertyName>the_geom</ogc:PropertyName>\n"
                    + //
                    "        <ogc:Function name=\"querySingle\">\n"
                    + //
                    "           <ogc:Literal>cite:Streams</ogc:Literal>\n"
                    + //
                    "           <ogc:Literal>the_geom</ogc:Literal>\n"
                    + //
                    "           <ogc:Literal>INCLUDE</ogc:Literal>\n"
                    + //
                    "        </ogc:Function>\n"
                    + //
                    "        <ogc:Distance units=\"meter\">${distance}</ogc:Distance>\n"
                    + //
                    "      </ogc:DWithin>\n"
                    + //
                    "    </ogc:Filter>\n"
                    + //
                    "  </wfs:Query>\n"
                    + //
                    "</wfs:GetFeature>";

    static final String QUERY_MULTI = //
            "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                    + //
                    "  xmlns:cite=\"http://www.opengis.net/cite\" "
                    + //
                    "  xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                    + //
                    "  service=\"WFS\" version=\"${version}\">\n"
                    + //
                    "  <wfs:Query typeName=\"cite:Buildings\">\n"
                    + //
                    "    <ogc:Filter>\n"
                    + //
                    "      <ogc:DWithin>\n"
                    + //
                    "        <ogc:PropertyName>the_geom</ogc:PropertyName>\n"
                    + //
                    "        <ogc:Function name=\"collectGeometries\">\n"
                    + //
                    "          <ogc:Function name=\"queryCollection\">\n"
                    + //
                    "            <ogc:Literal>cite:RoadSegments</ogc:Literal>\n"
                    + //
                    "            <ogc:Literal>the_geom</ogc:Literal>\n"
                    + //
                    "            <ogc:Literal>NAME = 'Route 5'</ogc:Literal>\n"
                    + //
                    "          </ogc:Function>\n"
                    + //
                    "        </ogc:Function>\n"
                    + //
                    "        <ogc:Distance units=\"meter\">0.001</ogc:Distance>\n"
                    + //
                    "      </ogc:DWithin>\n"
                    + //
                    "    </ogc:Filter>\n"
                    + //
                    "  </wfs:Query>\n"
                    + //
                    "</wfs:GetFeature>";

    @Test
    public void testSingleSmallDistance10() throws Exception {
        _testSingleSmallDistance("1.0.0");
    }

    @Test
    public void testSingleSmallDistance11() throws Exception {
        _testSingleSmallDistance("1.1.0");
    }

    void _testSingleSmallDistance(String version) throws Exception {
        String request =
                QUERY_SINGLE.replace("${distance}", "0.00000001").replace("${version}", version);
        Document doc = postAsDOM("wfs", request);
        print(doc);

        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        assertXpathEvaluatesTo("0", "count(cite:Buildings)", doc);
    }

    @Test
    public void testSingleLargeDistance10() throws Exception {
        _testSingleLargeDistance("1.0.0");
    }

    @Test
    public void testSingleLargeDistance11() throws Exception {
        _testSingleLargeDistance("1.1.0");
    }

    void _testSingleLargeDistance(String version) throws Exception {
        String request =
                QUERY_SINGLE.replace("${distance}", "0.001").replace("${version}", version);
        Document doc = postAsDOM("wfs", request);
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        assertXpathEvaluatesTo("1", "count(//cite:Buildings)", doc);
    }

    @Test
    public void testMultiple10() throws Exception {
        _testMultiple("1.0.0");
    }

    @Test
    public void testMultiple11() throws Exception {
        _testMultiple("1.1.0");
    }

    void _testMultiple(String version) throws Exception {
        Document doc = postAsDOM("wfs", QUERY_MULTI.replace("${version}", version));
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        assertXpathEvaluatesTo("2", "count(//cite:Buildings)", doc);
    }
}
