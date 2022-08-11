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

package org.geoserver.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Validates the encoding of filters on nested properties with some advanced mappings and xpaths.
 */
public final class NestedElementsFilteringTest extends AbstractAppSchemaTestSupport {

    private static final String STATIONS_PREFIX = "st";
    private static final String STATIONS_URI = "http://www.stations.org/1.0";

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        return new MockData();
    }

    /** Helper class that will setup custom complex feature types using the stations data set. */
    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add stations namespaces
            putNamespace(STATIONS_PREFIX, STATIONS_URI);
            // add stations feature types
            addAppSchemaFeatureType(
                    STATIONS_PREFIX,
                    null,
                    "Station",
                    "/test-data/stations/nestedElements/stations.xml",
                    Collections.emptyMap(),
                    "/test-data/stations/nestedElements/stations.xsd",
                    "/test-data/stations/nestedElements/institutes.xml",
                    "/test-data/stations/nestedElements/persons.xml",
                    "/test-data/stations/nestedElements/stations.properties",
                    "/test-data/stations/nestedElements/institutes.properties",
                    "/test-data/stations/nestedElements/persons.properties");
        }
    }

    @Test
    public void testWfsGetFeatureWithAdvancedNestedFilter() throws Exception {
        // execute the WFS 2.0 request
        MockHttpServletResponse response =
                postAsServletResponse(
                        "wfs",
                        readResource(
                                "/test-data/stations/nestedElements/requests/wfs_get_feature_1.xml"));
        // check that station 1 was returned
        String content = response.getContentAsString();
        assertThat(content, containsString("gml:id=\"ins.1\""));
        assertThat(StringUtils.countMatches(content, "<wfs:member>"), is(1));
    }

    /** Checks for included types namespaces on output GML. */
    @Test
    public void testWfsIncludedNamespacesDeclaration() throws Exception {
        // execute the WFS 2.0 request
        Document doc =
                postAsDOM(
                        "wfs",
                        readResource(
                                "/test-data/stations/nestedElements/requests/wfs_get_feature_1.xml"));
        Node perAttributeNode = doc.getFirstChild().getAttributes().getNamedItem("xmlns:per");
        assertTrue(perAttributeNode instanceof Attr);
        Attr perAttr = (Attr) perAttributeNode;
        assertEquals("http://www.person.org/1.0", perAttr.getValue());
    }
}
