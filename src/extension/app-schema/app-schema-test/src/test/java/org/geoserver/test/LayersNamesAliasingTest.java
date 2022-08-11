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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests that validate that layer names aliasing works correctly for App-Schema defined feature
 * types, i.e. that is possible to create a layer with a name different from the App-Schema feature
 * type.
 */
public final class LayersNamesAliasingTest extends AbstractAppSchemaTestSupport {

    // xpath engines used to check WFS responses
    private XpathEngine WFS11_XPATH_ENGINE;
    private XpathEngine WFS20_XPATH_ENGINE;

    private static Path testFolderPath;

    @BeforeClass
    public static void prepare() throws IOException {
        testFolderPath = Files.createTempDirectory(Paths.get("target/test-classes"), "layernames");
        File srcDir = new File("target/test-classes/test-data/stations/layerNamesTest");
        File destDir = Paths.get(testFolderPath.toString(), "layerNamesTest").toFile();
        destDir.deleteOnExit();
        FileUtils.copyDirectory(srcDir, destDir);
    }

    @Before
    public void beforeTest() {
        // instantiate WFS 1.1 xpath engine
        WFS11_XPATH_ENGINE =
                StationsMockData.buildXpathEngine(
                        getBaseNamespaces(),
                        "wfs",
                        "http://www.opengis.net/wfs",
                        "gml",
                        "http://www.opengis.net/gml");
        // instantiate WFS 2.0 xpath engine
        WFS20_XPATH_ENGINE =
                StationsMockData.buildXpathEngine(
                        getBaseNamespaces(),
                        "wfs",
                        "http://www.opengis.net/wfs/2.0",
                        "gml",
                        "http://www.opengis.net/gml/3.2");
    }

    @Override
    protected AliasStationsMockData createTestData() {
        // instantiate our custom complex types
        return new AliasStationsMockData() {
            @Override
            protected Optional<String> extraStationFeatures() {
                String features =
                        "\nst.2=st.2|station2|32154895|station2@stations.org|POINT(-1.0E-7 1.0E-7)";
                return Optional.of(features);
            }

            @Override
            protected Optional<String> extraMeasurementFeatures() {
                String features = "\nms.3=ms.3|wind|km/h|st.2";
                return Optional.of(features);
            }
        };
    }

    Map<String, String> getBaseNamespaces() {
        Map<String, String> nss = new HashMap<>();
        nss.put("st_gml31", "http://www.stations_gml31.org/1.0");
        nss.put("ms_gml31", "http://www.measurements_gml31.org/1.0");
        nss.put("st_gml32", "http://www.stations_gml32.org/1.0");
        nss.put("ms_gml32", "http://www.measurements_gml32.org/1.0");
        return nss;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        FeatureTypeInfo info = catalog.getFeatureTypeByName("st_gml31", "lyr_Station_gml31");
        info.setEnabled(true);
        info.setNativeName("Station_gml31");
        catalog.save(info);

        info = catalog.getFeatureTypeByName("st_gml32", "lyr_Station_gml32");
        info.setEnabled(true);
        info.setNativeName("Station_gml32");
        catalog.save(info);
    }

    @Test
    public void testAliasedNameWfsGetFeature11() throws Exception {
        Document document =
                getAsDOM(
                        "ows?service=wfs&request=GetFeature&version=1.1.0"
                                + "&typenames=st_gml31:lyr_Station_gml31");
        // requested with lyr_Station_gml31, must returns Station:
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                2,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");
    }

    @Test
    public void testAliasedNameWfsGetFeature20() throws Exception {
        Document document =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0.0"
                                + "&typeNames=st_gml32:lyr_Station_gml32");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                2,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32");
    }

    @Test
    public void testAliasedNameWfsGetFeaturePost11() throws Exception {
        String xmlQuery = resourceToString("wfs110query.xml");
        Document document = postAsDOM("wfs", xmlQuery);
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");
    }

    @Test
    public void testAliasedNameWfsGetFeaturePost20() throws Exception {
        // load wfs 2.0.0 query body at wfs200query.xml
        String xmlQuery = resourceToString("wfs200query.xml");
        Document document = postAsDOM("wfs", xmlQuery);
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32");
    }

    @Test
    public void testLocalWorkspaceNoPrefixWfsGetFeature11() throws Exception {
        Document document =
                getAsDOM(
                        "st_gml31/wfs?request=GetFeature&version=1.1.0&typename=lyr_Station_gml31");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                2,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");
    }

    @Test
    public void testLocalWorkspaceNoPrefixWfsGetFeature20() throws Exception {
        Document document =
                getAsDOM(
                        "st_gml32/wfs?request=GetFeature&version=2.0.0&typeNames=lyr_Station_gml32");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                2,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32");
    }

    @Test
    public void testLocalWorkspaceNoPrefixWfsGetFeaturePost11() throws Exception {
        String xmlQuery = resourceToString("wfs110queryNoPrefix.xml");
        Document document = postAsDOM("st_gml31/wfs", xmlQuery);
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");
    }

    @Test
    public void testLocalWorkspaceNoPrefixWfsGetFeaturePost20() throws Exception {
        String xmlQuery = resourceToString("wfs200queryNoPrefix.xml");
        Document document = postAsDOM("st_gml32/wfs", xmlQuery);
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32");
    }

    /**
     * This test checks default layer CQL filter is executed on a WFS GetFeature request without an
     * explicit query on the URL.
     */
    @Test
    public void testDefaultCqlNameWfsGetFeature11() throws Exception {
        try {
            setCqlFilter(
                    "st_gml31",
                    "lyr_Station_gml31",
                    "st_gml31:Station_gml31.st_gml31:name='station2'");
            Document document =
                    getAsDOM(
                            "ows?service=wfs&request=GetFeature&version=1.1.0"
                                    + "&typenames=st_gml31:lyr_Station_gml31");
            // requested with lyr_Station_gml31, must returns Station:
            checkCount(
                    WFS11_XPATH_ENGINE,
                    document,
                    1,
                    "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");
            checkCount(
                    WFS11_XPATH_ENGINE,
                    document,
                    1,
                    "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id='st.2']");
        } finally {
            cleanCqlFilter("st_gml31", "lyr_Station_gml31");
        }
    }

    /**
     * This test checks default layer CQL filter is executed on a WFS GetFeature request without an
     * explicit query on the URL.
     */
    @Test
    public void testDefaultCqlWfsGetFeature20() throws Exception {
        try {
            setCqlFilter(
                    "st_gml32",
                    "lyr_Station_gml32",
                    "st_gml32:Station_gml32.st_gml32:name='station2'");
            Document document =
                    getAsDOM(
                            "wfs?request=GetFeature&version=2.0.0"
                                    + "&typeNames=st_gml32:lyr_Station_gml32");
            checkCount(
                    WFS20_XPATH_ENGINE,
                    document,
                    1,
                    "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32");
            checkCount(
                    WFS20_XPATH_ENGINE,
                    document,
                    1,
                    "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id='st.2']");
        } finally {
            cleanCqlFilter("st_gml32", "lyr_Station_gml32");
        }
    }

    private String resourceToString(String filename) throws IOException {
        return IOUtils.toString(
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("test-data/stations/layerNamesTest/" + filename),
                StandardCharsets.UTF_8);
    }

    /**
     * Helper method that checks if the provided XPath expression evaluated against the provided XML
     * document yields the expected number of matches.
     */
    private void checkCount(
            XpathEngine xpathEngine, Document document, int expectedCount, String xpath) {
        try {
            // evaluate the xpath and compare the number of nodes found
            assertEquals(expectedCount, xpathEngine.getMatchingNodes(xpath, document).getLength());
        } catch (Exception exception) {
            throw new RuntimeException("Error evaluating xpath.", exception);
        }
    }

    private void setCqlFilter(String namespace, String layerName, String cql) {
        Catalog catalog = getCatalog();
        FeatureTypeInfo info = catalog.getFeatureTypeByName(namespace, layerName);
        info.setCqlFilter(cql);
        catalog.save(info);
    }

    private void cleanCqlFilter(String namespace, String layerName) {
        Catalog catalog = getCatalog();
        FeatureTypeInfo info = catalog.getFeatureTypeByName(namespace, layerName);
        info.setCqlFilter(null);
        catalog.save(info);
    }
}
