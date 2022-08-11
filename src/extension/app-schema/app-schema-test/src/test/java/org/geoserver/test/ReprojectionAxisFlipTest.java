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

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import org.geoserver.catalog.FeatureTypeInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Validates that reprojection and axis flipping are correctly handled. */
public final class ReprojectionAxisFlipTest extends AbstractAppSchemaTestSupport {

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
            // add stations feature type
            addAppSchemaFeatureType(
                    STATIONS_PREFIX,
                    null,
                    "Station",
                    "/test-data/stations/noDefaultGeometry/stations.xml",
                    Collections.emptyMap(),
                    "/test-data/stations/noDefaultGeometry/stations.xsd",
                    "/test-data/stations/noDefaultGeometry/stations.properties");
        }
    }

    @Before
    public void beforeTest() {
        // set the declared SRS on the feature type info
        setDeclaredCrs("st:Station", "EPSG:4052");
    }

    @Test
    public void testWfsGetFeatureWithBbox() throws Exception {
        genericWfsGetFeatureWithBboxTest(
                () ->
                        getAsServletResponse(
                                "wfs?service=WFS"
                                        + "&version=2.0&request=GetFeature&typeName=st:Station&maxFeatures=1"
                                        + "&outputFormat=gml32&srsName=urn:ogc:def:crs:EPSG::4052&bbox=3,-3,6,0"));
    }

    @Test
    public void testWfsGetFeatureWithBboxPost() throws Exception {
        // execute the WFS 2.0 request
        genericWfsGetFeatureWithBboxTest(
                () ->
                        postAsServletResponse(
                                "wfs",
                                readResource(
                                        "/test-data/stations/noDefaultGeometry/requests/wfs20_get_feature_1.xml")));
    }

    /**
     * Helper method holding the common code used to test a WFS GetFeature operation with a BBOX
     * requiring axis flipping,
     */
    private void genericWfsGetFeatureWithBboxTest(Request request) throws Exception {
        // execute the WFS 2.0 request
        MockHttpServletResponse response = request.execute();
        // check that both stations were returned
        String content = response.getContentAsString();
        assertThat(content, containsString("gml:id=\"st.1\""));
        assertThat(content, containsString("gml:id=\"st.2\""));
        assertThat(countMatches(content, "<wfs:member>"), is(2));
        // check that with no declared SRS no features are returned
        setDeclaredCrs("st:Station", null);
        response = request.execute();
        content = response.getContentAsString();
        assertThat(countMatches(content, "Exception"), is(0));
        assertThat(countMatches(content, "<wfs:member>"), is(0));
    }

    /**
     * Helper method that sets the provided SRS as the declared SRS on feature type info
     * corresponding to provided feature type name.
     */
    private void setDeclaredCrs(String featureTypeName, String srs) {
        // get the feature type info
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(featureTypeName);
        assertThat(info, notNullValue());
        // set the declared SRS
        info.setSRS(srs);
        getCatalog().save(info);
    }

    @FunctionalInterface
    private interface Request {

        // executes a request allowing an exception to be throw
        MockHttpServletResponse execute() throws Exception;
    }
}
