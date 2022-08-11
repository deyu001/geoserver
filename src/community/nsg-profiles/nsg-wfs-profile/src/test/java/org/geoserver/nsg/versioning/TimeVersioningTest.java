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

package org.geoserver.nsg.versioning;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.nsg.TestsUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.Converters;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;

public final class TimeVersioningTest extends GeoServerSystemTestSupport {

    private XpathEngine WFS20_XPATH_ENGINE;

    @Before
    public void beforeTest() throws IOException {
        // instantiate xpath engine
        WFS20_XPATH_ENGINE =
                buildXpathEngine(
                        "wfs", "http://www.opengis.net/wfs/2.0",
                        "gml", "http://www.opengis.net/gml/3.2");

        // create versioned layer (each time so that its contents are always clean)
        ReferencedEnvelope envelope =
                new ReferencedEnvelope(-5, -5, 5, 5, DefaultGeographicCRS.WGS84);
        Map<SystemTestData.LayerProperty, Object> properties = new HashMap<>();
        properties.put(SystemTestData.LayerProperty.LATLON_ENVELOPE, envelope);
        properties.put(SystemTestData.LayerProperty.ENVELOPE, envelope);
        properties.put(SystemTestData.LayerProperty.SRS, 4326);

        QName versionedLayerName =
                new QName(MockData.DEFAULT_URI, "versioned", MockData.DEFAULT_PREFIX);
        getTestData()
                .addVectorLayer(
                        versionedLayerName,
                        properties,
                        "versioned.properties",
                        getClass(),
                        getCatalog());

        // activate versioning for versioned layer
        TestsUtils.updateFeatureTypeTimeVersioning(
                getCatalog(), "gs:versioned", true, "NAME", "TIME");
    }

    @Test
    public void testInsertVersionedFeature() throws Exception {
        Document doc = postAsDOM("wfs", TestsUtils.readResource("/requests/insert_request_1.xml"));
        assertTransactionResponse(doc);

        List<SimpleFeature> features = TestsUtils.searchFeatures(getCatalog(), "gs:versioned");
        List<SimpleFeature> foundFeatures =
                TestsUtils.searchFeatures(features, "NAME", "TIME", "Feature_3", new Date(), 300);
        assertThat(foundFeatures.size(), is(1));
        SimpleFeature foundFeature = foundFeatures.get(0);
        String description = (String) foundFeature.getAttribute("DESCRIPTION");
        assertThat(description, is("INSERT_1"));
    }

    @Test
    public void testGetFeatureVersionedEarlyDate() throws Exception {
        String earlyDateRequest =
                TestsUtils.readResource("/requests/get_request_1.xml")
                        .replace("${startDate}", "2017-01-01T12:00:00");
        Document doc = postAsDOM("wfs", earlyDateRequest);
        // print(doc);
        // all three states of the feature
        assertEquals("3", WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned)", doc));
        // sorted by time, descending
        assertEquals("v.2", WFS20_XPATH_ENGINE.evaluate("(//gs:versioned)[1]/@gml:id", doc));
        assertEquals("v.3", WFS20_XPATH_ENGINE.evaluate("(//gs:versioned)[2]/@gml:id", doc));
        assertEquals("v.1", WFS20_XPATH_ENGINE.evaluate("(//gs:versioned)[3]/@gml:id", doc));
    }

    @Test
    public void testGetFeatureVersionedLateDate() throws Exception {
        String earlyDateRequest =
                TestsUtils.readResource("/requests/get_request_1.xml")
                        .replace("${startDate}", "2017-07-24T00:00:00Z");
        Document doc = postAsDOM("wfs", earlyDateRequest);
        // print(doc);
        // only the last states of the feature
        assertEquals("1", WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned)", doc));
        // sorted by time, descending
        assertEquals("v.2", WFS20_XPATH_ENGINE.evaluate("//gs:versioned/@gml:id", doc));
    }

    @Test
    public void testGetFeatureVersionedExtraFilter() throws Exception {
        String request =
                TestsUtils.readResource("/requests/get_request_2.xml")
                        .replace("${startDate}", "2017-01-01T00:00:00Z");
        Document doc = postAsDOM("wfs", request);
        // print(doc);
        // only one matches
        assertEquals("1", WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned)", doc));
        assertEquals("v.3", WFS20_XPATH_ENGINE.evaluate("//gs:versioned/@gml:id", doc));
    }

    @Test
    public void testUpdateSingleVersionedFeature() throws Exception {
        Document updateDoc =
                postAsDOM("wfs", TestsUtils.readResource("/requests/update_request_1.xml"));
        // print(insertDoc);
        // TODO: check it returns an update statement, not a insert like it now does
        assertTransactionResponse(updateDoc);
        String getRequest =
                TestsUtils.readResource("/requests/get_request_3.xml")
                        .replace("${startDate}", "2017-01-01T00:00:00");
        Document getDoc = postAsDOM("wfs", getRequest);
        // print(getDoc);
        // one more state for the feature, there was only two
        assertEquals("3", WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned)", getDoc));
        // sorted by time, the update created this new instance and it should have the old value but
        // the new time
        // and the new
        assertEquals(
                "Feature_2", WFS20_XPATH_ENGINE.evaluate("(//gs:versioned)[1]/gs:NAME", getDoc));
        assertEquals(
                "-2 2",
                WFS20_XPATH_ENGINE.evaluate(
                        "(//gs:versioned)[1]/gs:GEOMETRY/gml:Point/gml:pos", getDoc));
        assertEquals(
                "UPDATE_NOW",
                WFS20_XPATH_ENGINE.evaluate("(//gs:versioned)[1]/gs:DESCRIPTION", getDoc));
        String time = WFS20_XPATH_ENGINE.evaluate("(//gs:versioned)[1]/gs:TIME", getDoc);
        Date updateTime = Converters.convert(time, Date.class);
        assertThat(System.currentTimeMillis() - updateTime.getTime(), lessThan(300 * 1000l));
    }

    @Test
    public void testUpdateMultipleVersionedFeature() throws Exception {
        Document updateDoc =
                postAsDOM("wfs", TestsUtils.readResource("/requests/update_request_2.xml"));
        // print(insertDoc);
        assertTransactionResponse(updateDoc);
        // TODO: check it returns an update statement, not a insert like it now does
        String getRequest = TestsUtils.readResource("/requests/get_request_4.xml");
        Document getDoc = postAsDOM("wfs", getRequest);
        // print(getDoc);
        // one more state for each feature, the property file has 5
        assertEquals("7", WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned)", getDoc));
        // sorted by time, the update created this new instance and it should have the old value but
        // the new time
        // and the new
        assertEquals(
                "UPDATE_NOW",
                WFS20_XPATH_ENGINE.evaluate(
                        "(//gs:versioned[gs:NAME='Feature_1'])[1]/gs:DESCRIPTION", getDoc));
        assertEquals(
                "-1 -1",
                WFS20_XPATH_ENGINE.evaluate(
                        "(//gs:versioned[gs:NAME='Feature_1'])[1]/gs:GEOMETRY/gml:Point/gml:pos",
                        getDoc));
        assertEquals(
                "UPDATE_NOW",
                WFS20_XPATH_ENGINE.evaluate(
                        "(//gs:versioned[gs:NAME='Feature_2'])[1]/gs:DESCRIPTION", getDoc));
        assertEquals(
                "-2 2",
                WFS20_XPATH_ENGINE.evaluate(
                        "(//gs:versioned[gs:NAME='Feature_2'])[1]/gs:GEOMETRY/gml:Point/gml:pos",
                        getDoc));
        String time =
                WFS20_XPATH_ENGINE.evaluate(
                        "(//gs:versioned[gs:NAME='Feature_1'])[1]/gs:TIME", getDoc);
        Date updateTime = Converters.convert(time, Date.class);
        assertThat(System.currentTimeMillis() - updateTime.getTime(), lessThan(300 * 1000l));
        assertEquals(
                "UPDATE_NOW",
                WFS20_XPATH_ENGINE.evaluate(
                        "(//gs:versioned[gs:NAME='Feature_2'])[1]/gs:DESCRIPTION", getDoc));
    }

    @Test
    public void testDeleteFeature2() throws Exception {
        // wipe out entire feature
        String deleteRequest = TestsUtils.readResource("/requests/delete_request_1.xml");
        Document deleteResponse = postAsDOM("wfs", deleteRequest);
        // print(deleteResponse);
        assertTransactionResponse(deleteResponse);

        // read back and make sure there is none left
        String getRequest = TestsUtils.readResource("/requests/get_request_4.xml");
        Document getDoc = postAsDOM("wfs", getRequest);
        // only Feature_1 left
        assertEquals("3", WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned)", getDoc));
        assertEquals(
                "3",
                WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned[gs:NAME='Feature_1'])", getDoc));
    }

    @Test
    public void testDeleteBetweenDates() throws Exception {
        // wipe out entire feature
        String deleteRequest = TestsUtils.readResource("/requests/delete_request_2.xml");
        Document deleteResponse = postAsDOM("wfs", deleteRequest);
        // print(deleteResponse);
        assertTransactionResponse(deleteResponse);

        // read back and make sure there is none left
        String getRequest = TestsUtils.readResource("/requests/get_request_4.xml");
        Document getDoc = postAsDOM("wfs", getRequest);
        // Feature_1 fully left, Feature_2 only has one
        assertEquals(
                "3",
                WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned[gs:NAME='Feature_1'])", getDoc));
        assertEquals(
                "1",
                WFS20_XPATH_ENGINE.evaluate("count(//gs:versioned[gs:NAME='Feature_2'])", getDoc));
        assertEquals(
                "UPDATE_2",
                WFS20_XPATH_ENGINE.evaluate(
                        "//gs:versioned[gs:NAME='Feature_2']/gs:DESCRIPTION", getDoc));
    }

    private void assertTransactionResponse(Document doc) throws XpathException {
        assertEquals("1", WFS20_XPATH_ENGINE.evaluate("count(/wfs:TransactionResponse)", doc));
    }

    /**
     * Helper method that builds a xpath engine using some predefined namespaces and all the catalog
     * namespaces. The provided namespaces will be added overriding any existing namespace.
     */
    private XpathEngine buildXpathEngine(String... namespaces) {
        // build xpath engine
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        Map<String, String> finalNamespaces = new HashMap<>();
        // add common namespaces
        finalNamespaces.put("ows", "http://www.opengis.net/ows");
        finalNamespaces.put("ogc", "http://www.opengis.net/ogc");
        finalNamespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        finalNamespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        finalNamespaces.put("xlink", "http://www.w3.org/1999/xlink");
        finalNamespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        // add al catalog namespaces
        getCatalog()
                .getNamespaces()
                .forEach(
                        namespace ->
                                finalNamespaces.put(namespace.getPrefix(), namespace.getURI()));
        // add provided namespaces
        if (namespaces.length % 2 != 0) {
            throw new RuntimeException("Invalid number of namespaces provided.");
        }
        for (int i = 0; i < namespaces.length; i += 2) {
            finalNamespaces.put(namespaces[i], namespaces[i + 1]);
        }
        // add namespaces to the xpath engine
        xpathEngine.setNamespaceContext(new SimpleNamespaceContext(finalNamespaces));
        return xpathEngine;
    }
}
