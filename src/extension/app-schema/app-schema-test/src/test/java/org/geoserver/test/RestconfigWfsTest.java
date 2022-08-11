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
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geoserver.test.onlineTest.setup.AppSchemaTestOracleSetup;
import org.geoserver.test.onlineTest.setup.AppSchemaTestPostgisSetup;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.WFSInfo;
import org.geotools.appschema.resolver.xml.AppSchemaXSDRegistry;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
import org.geotools.data.complex.DataAccessRegistry;
import org.geotools.xml.resolver.SchemaCache;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Test REST configuration of app-schema. Note that the mapping and properties file are still copied
 * locally.
 *
 * @author Ben Caradoc-Davies (CSIRO Earth Science and Resource Engineering)
 */
public class RestconfigWfsTest extends CatalogRESTTestSupport {

    @Override
    protected void onSetUp(org.geoserver.data.test.SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setCanonicalSchemaLocation(true);
        wfs.setEncodeFeatureMember(true);
        getGeoServer().save(wfs);
        // disable schema caching in tests, as schemas are expected to provided on the classpath
        SchemaCache.disableAutomaticConfiguration();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        DataAccessRegistry.getInstance().disposeAndUnregisterAll();
        AppSchemaDataAccessRegistry.clearAppSchemaProperties();
        AppSchemaXSDRegistry.getInstance().dispose();
    }

    private static final String WORKSPACE =
            "<workspace>" //
                    + "<name>gsml</name>" //
                    + "</workspace>";

    private static final String NAMESPACE =
            "<namespace>" //
                    + "<uri>urn:cgi:xmlns:CGI:GeoSciML:2.0</uri>" //
                    + "</namespace>";

    private static final String DATASTORE =
            "<dataStore>" //
                    + "<name>MappedFeature</name>" //
                    + "<enabled>true</enabled>" //
                    + "<workspace>" //
                    + "<name>gsml</name>" //
                    + "</workspace>" //
                    + "<connectionParameters>" //
                    + "<entry key='dbtype'>app-schema</entry>" //
                    + "<entry key='url'>file:workspaces/gsml/MappedFeature/MappedFeature.xml</entry>" //
                    + "<entry key='namespace'>urn:cgi:xmlns:CGI:GeoSciML:2.0</entry>" //
                    + "</connectionParameters>" //
                    + "</dataStore>";

    private static final String FEATURETYPE =
            "<featureType>" //
                    + "<name>MappedFeature</name>" //
                    + "<nativeName>MappedFeature</nativeName>" //
                    + "<namespace>" //
                    + "<prefix>gsml</prefix>" //
                    + "</namespace>" //
                    + "<title>... TITLE ...</title>" //
                    + "<abstract>... ABSTRACT ...</abstract>" //
                    + "<srs>EPSG:4326</srs>" //
                    + "<latLonBoundingBox>" //
                    + "<minx>-180</minx>" //
                    + "<maxx>180</maxx>" //
                    + "<miny>-90</miny>" //
                    + "<maxy>90</maxy>" //
                    + "<crs>EPSG:4326</crs>" //
                    + "</latLonBoundingBox>" //
                    + "<projectionPolicy>REPROJECT_TO_DECLARED</projectionPolicy>" //
                    + "<enabled>true</enabled>" //
                    + "<metadata>" //
                    + "<entry key='kml.regionateFeatureLimit'>10</entry>" //
                    + "<entry key='indexingEnabled'>false</entry>" //
                    + "<entry key='cachingEnabled'>false</entry>" //
                    + "</metadata>" //
                    + "<store class='dataStore'>" //
                    + "<name>MappedFeature</name>" //
                    + "</store>" //
                    + "<maxFeatures>0</maxFeatures>" //
                    + "<numDecimals>0</numDecimals>" //
                    + "</featureType>";

    public static final String DS_PARAMETERS =
            "<parameters>" //
                    + "<Parameter>" //
                    + "<name>directory</name>" //
                    + "<value>file:./</value>" //
                    + "</Parameter>" //
                    + "</parameters>"; //

    public static final String MAPPING =
            "<as:AppSchemaDataAccess xmlns:as='http://www.geotools.org/app-schema'>" //
                    + "<namespaces>" //
                    + "<Namespace>" //
                    + "<prefix>gsml</prefix>" //
                    + "<uri>urn:cgi:xmlns:CGI:GeoSciML:2.0</uri>" //
                    + "</Namespace>" //
                    + "</namespaces>" //
                    + "<sourceDataStores>" //
                    + "<DataStore>" //
                    + "<id>datastore</id>" //
                    + DS_PARAMETERS
                    + "</DataStore>" //
                    + "</sourceDataStores>" //
                    + "<targetTypes>" //
                    + "<FeatureType>" //
                    + "<schemaUri>http://www.geosciml.org/geosciml/2.0/xsd/geosciml.xsd</schemaUri>" //
                    + "</FeatureType>" //
                    + "</targetTypes>" //
                    + "<typeMappings>" //
                    + "<FeatureTypeMapping>" //
                    + "<sourceDataStore>datastore</sourceDataStore>" //
                    + "<sourceType>MAPPEDFEATURE</sourceType>" //
                    + "<targetElement>gsml:MappedFeature</targetElement>" //
                    + "<attributeMappings>" //
                    + "<AttributeMapping>" //
                    + "<targetAttribute>gsml:shape</targetAttribute>" //
                    + "<sourceExpression>" //
                    + "<OCQL>SHAPE</OCQL>" //
                    + "</sourceExpression>" //
                    + "</AttributeMapping>" //
                    + "</attributeMappings>" //
                    + "</FeatureTypeMapping>" //
                    + "</typeMappings>" //
                    + "</as:AppSchemaDataAccess>";

    public static final String PROPERTIES =
            "_=SHAPE:Geometry:srid=4326\n" //
                    + "mf.1=POINT(0 1)\n" //
                    + "mf.2=POINT(2 3)\n";

    /**
     * Test that REST can be used to configure an app-schema datastore and that this datastore can
     * be used to service a WFS request.
     */
    @Test
    public void testRestconfig() throws Exception {
        MockHttpServletResponse response;
        // create workspace
        response = postAsServletResponse("/rest/workspaces", WORKSPACE, "text/xml");
        assertEquals(201, response.getStatus());
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("gsml");
        assertNotNull(ws);
        // set namespace uri (response is 200 as update not create)
        // (default http://gsml was created when workspace was created)
        response = putAsServletResponse("/rest/namespaces/gsml", NAMESPACE, "text/xml");
        assertEquals(200, response.getStatus());
        NamespaceInfo ns = getCatalog().getNamespaceByPrefix("gsml");
        assertNotNull(ns);
        assertEquals("urn:cgi:xmlns:CGI:GeoSciML:2.0", ns.getURI());
        // create datastore
        response = postAsServletResponse("/rest/workspaces/gsml/datastores", DATASTORE, "text/xml");
        assertEquals(201, response.getStatus());
        DataStoreInfo ds = getCatalog().getDataStoreByName("gsml", "MappedFeature");
        assertNotNull(ds);
        // copy the mapping and properties files
        copyFiles();
        // create featuretype
        response =
                postAsServletResponse(
                        "/rest/workspaces/gsml/datastores/MappedFeature/featuretypes",
                        FEATURETYPE,
                        "text/xml");
        assertEquals(201, response.getStatus());
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        assertNotNull(ft);
        // test that features can be obtained via WFS
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathCount(2, "//gsml:MappedFeature", doc);
    }

    /** Copy the mapping and properties files to the data directory. */
    private void copyFiles() throws Exception {
        File dir =
                new File(
                        new File(
                                new File(getTestData().getDataDirectoryRoot(), "workspaces"),
                                "gsml"),
                        "MappedFeature");
        dir.mkdirs();
        File propertiesFile = new File(dir, "MAPPEDFEATURE.properties");
        IOUtils.copy(new ByteArrayInputStream(PROPERTIES.getBytes("UTF-8")), propertiesFile);

        String mapping = MAPPING;
        String onlineTestId = System.getProperty("testDatabase");
        if (onlineTestId != null) {
            // if test if running in online mode, need to use db params
            onlineTestId = onlineTestId.trim().toLowerCase();
            Map<String, File> propertyFiles = new HashMap<>();
            propertyFiles.put(propertiesFile.getName(), dir);
            AbstractReferenceDataSetup setup;
            if (onlineTestId.equals("oracle")) {
                // oracle
                mapping =
                        mapping.replaceAll(
                                DS_PARAMETERS,
                                Matcher.quoteReplacement(AppSchemaTestOracleSetup.DB_PARAMS));
                setup = AppSchemaTestOracleSetup.getInstance(propertyFiles);
            } else {
                // postgis
                mapping =
                        mapping.replaceAll(
                                DS_PARAMETERS,
                                Matcher.quoteReplacement(AppSchemaTestPostgisSetup.DB_PARAMS));
                setup = AppSchemaTestPostgisSetup.getInstance(propertyFiles);
            }
            // Run the sql script to create the tables from properties files
            setup.setUp();
            setup.tearDown();
        }
        IOUtils.copy(
                new ByteArrayInputStream(mapping.getBytes("UTF-8")),
                new File(dir, "MappedFeature.xml"));
    }

    /**
     * Return {@link Document} as a pretty-printed string.
     *
     * @param document document to be prettified
     * @return the prettified string
     */
    private String prettyString(Document document) {
        OutputStream output = new ByteArrayOutputStream();
        prettyPrint(document, output);
        return output.toString();
    }

    /**
     * Pretty-print a {@link Document} to an {@link OutputStream}.
     *
     * @param document document to be prettified
     * @param output stream to which output is written
     */
    private void prettyPrint(Document document, OutputStream output) {
        try {
            Transformer tx = TransformerFactory.newInstance().newTransformer();
            tx.setOutputProperty(OutputKeys.INDENT, "yes");
            tx.transform(new DOMSource(document), new StreamResult(output));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Assert that there are count occurrences of xpath. */
    private void assertXpathCount(int count, String xpath, Document document) throws Exception {
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("gsml", "urn:cgi:xmlns:CGI:GeoSciML:2.0");
        xpathEngine.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        assertEquals(count, xpathEngine.getMatchingNodes(xpath, document).getLength());
    }
}
