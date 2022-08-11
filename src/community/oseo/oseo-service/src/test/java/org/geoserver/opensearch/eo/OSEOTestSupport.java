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

package org.geoserver.opensearch.eo;

import static org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport.setupBasicOpenSearch;
import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest.GS_PRODUCT;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Base class for OpenSeach tests
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEOTestSupport extends GeoServerSystemTestSupport {

    private static Schema OS_SCHEMA;

    private static Schema ATOM_SCHEMA;

    protected SimpleNamespaceContext namespaceContext;

    static {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            OS_SCHEMA =
                    factory.newSchema(OSEOTestSupport.class.getResource("/schemas/OpenSearch.xsd"));
            ATOM_SCHEMA =
                    factory.newSchema(
                            OSEOTestSupport.class.getResource("/schemas/searchResults.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the OpenSearch schemas", e);
        }
    }

    private static Schema getOsSchema() {
        if (OS_SCHEMA == null) {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                OS_SCHEMA =
                        factory.newSchema(
                                OSEOTestSupport.class.getResource("/schemas/OpenSearch.xsd"));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse the OpenSearch schemas", e);
            }
        }

        return OS_SCHEMA;
    }

    private static Schema getAtomSchema() {
        if (ATOM_SCHEMA == null) {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                ATOM_SCHEMA =
                        factory.newSchema(
                                OSEOTestSupport.class.getResource("/schemas/searchResults.xsd"));
            } catch (Exception e) {
                throw new RuntimeException("Could not parse the OpenSearch schemas", e);
            }
        }
        return ATOM_SCHEMA;
    }

    protected List<Filter> getFilters() {
        return Collections.singletonList(new OSEOFilter());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data to setup
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServer geoServer = getGeoServer();
        setupBasicOpenSearch(testData, getCatalog(), geoServer, populateGranulesTable());

        // add the custom product class
        OSEOInfo oseo = geoServer.getService(OSEOInfo.class);
        oseo.getProductClasses().add(JDBCOpenSearchAccessTest.GS_PRODUCT);
        geoServer.save(oseo);
    }

    /** Allows subclasses to decide if to populate the granules table, or not */
    protected boolean populateGranulesTable() {
        return false;
    }

    @BeforeClass
    public static void checkOnLine() {
        GeoServerOpenSearchTestSupport.checkOnLine();
    }

    @Before
    public void setupNamespaces() {
        this.namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("atom", "http://www.w3.org/2005/Atom");
        namespaceContext.bindNamespaceUri("os", "http://a9.com/-/spec/opensearch/1.1/");
        namespaceContext.bindNamespaceUri(
                "param", "http://a9.com/-/spec/opensearch/extensions/parameters/1.0/");
        namespaceContext.bindNamespaceUri("at", "http://www.w3.org/2005/Atom");
        namespaceContext.bindNamespaceUri("gml", "http://www.opengis.net/gml");
        namespaceContext.bindNamespaceUri("georss", "http://www.georss.org/georss");
        namespaceContext.bindNamespaceUri("eo", OpenSearchAccess.EO_NAMESPACE);
        namespaceContext.bindNamespaceUri("geo", OpenSearchAccess.GEO_NAMESPACE);
        namespaceContext.bindNamespaceUri("gmi", "http://www.isotc211.org/2005/gmi");
        namespaceContext.bindNamespaceUri("gmd", "http://www.isotc211.org/2005/gmd");
        namespaceContext.bindNamespaceUri("gco", "http://www.isotc211.org/2005/gco");
        namespaceContext.bindNamespaceUri("time", "http://a9.com/-/opensearch/extensions/time/1.0");
        namespaceContext.bindNamespaceUri("owc", "http://www.opengis.net/owc/1.0");
        namespaceContext.bindNamespaceUri("dc", "http://purl.org/dc/elements/1.1/");
        namespaceContext.bindNamespaceUri("media", "http://search.yahoo.com/mrss/");
        for (ProductClass pc : ProductClass.DEFAULT_PRODUCT_CLASSES) {
            namespaceContext.bindNamespaceUri(pc.getPrefix(), pc.getNamespace());
        }
        namespaceContext.bindNamespaceUri(GS_PRODUCT.getPrefix(), GS_PRODUCT.getNamespace());
    }

    protected Matcher<Node> hasXPath(String xPath) {
        return Matchers.hasXPath(xPath, namespaceContext);
    }

    protected Matcher<Node> hasXPath(String xPath, Matcher<String> valueMatcher) {
        return Matchers.hasXPath(xPath, namespaceContext, valueMatcher);
    }

    protected void checkValidOSDD(Document d) throws SAXException, IOException {
        checkValidationErrors(d, getOsSchema());
    }

    protected void checkValidAtomFeed(Document d) throws SAXException, IOException {
        // TODO: we probably need to enrich this with EO specific elements check
        checkValidationErrors(d, getAtomSchema());
    }

    /** Checks the response is a RSS and */
    protected Document getAsOpenSearchException(String path, int expectedStatus) throws Exception {
        return getAsDOM(path, expectedStatus, "application/xml"); // OSEOExceptionHandler.RSS_MIME);
    }

    /**
     * Returns the DOM after checking the status code is 200 and the returned mime type is the
     * expected one
     */
    protected Document getAsDOM(String path, int expectedStatusCode, String expectedMimeType)
            throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(expectedMimeType, response.getContentType());
        assertEquals(expectedStatusCode, response.getStatus());

        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        return dom;
    }
}
