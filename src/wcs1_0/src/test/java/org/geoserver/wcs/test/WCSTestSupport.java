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

package org.geoserver.wcs.test;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base support class for wcs tests.
 *
 * @author Andrea Aime, TOPP
 */
public abstract class WCSTestSupport extends CoverageTestSupport {
    protected static XpathEngine xpath;

    protected static final boolean IS_WINDOWS;

    protected static final Schema WCS10_GETCAPABILITIES_SCHEMA;

    protected static final Schema WCS10_GETCOVERAGE_SCHEMA;

    protected static final Schema WCS10_DESCRIBECOVERAGE_SCHEMA;

    static {
        try {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS10_GETCAPABILITIES_SCHEMA =
                    factory.newSchema(new File("./schemas/wcs/1.0.0/wcsCapabilities.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.0.0 schemas", e);
        }
        try {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS10_GETCOVERAGE_SCHEMA =
                    factory.newSchema(new File("./schemas/wcs/1.0.0/getCoverage.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.0.0 schemas", e);
        }
        try {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS10_DESCRIBECOVERAGE_SCHEMA =
                    factory.newSchema(new File("./schemas/wcs/1.0.0/describeCoverage.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.0.0 schemas", e);
        }
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").matches(".*Windows.*");
        } catch (Exception e) {
            // no os.name? oh well, never mind
        }
        IS_WINDOWS = windows;
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        // add a raster mosaic with time and elevation
        testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null, null, TestData.class);
        // a raster layer with time, elevation and custom dimensions as ranges
        testData.setUpRasterLayer(TIMERANGES, "timeranges.zip", null, null, TestData.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wcs", "http://www.opengis.net/wcs");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }

    protected String checkOws11Exception(Document dom) throws Exception {
        assertEquals("ServiceExceptionReport", dom.getFirstChild().getNodeName());

        assertEquals(
                "1.2.0",
                dom.getFirstChild().getAttributes().getNamedItem("version").getNodeValue());
        assertXpathEvaluatesTo("1.2.0", "/ServiceExceptionReport/@version", dom);

        Node root = xpath.getMatchingNodes("/ServiceExceptionReport", dom).item(0);
        assertNotNull(root);

        NodeList nodes = dom.getElementsByTagName("ows:ExceptionText");
        if (nodes.getLength() > 0) {
            return nodes.item(0).getNodeValue();
        }
        return null;
    }

    protected void setupRasterDimension(
            QName layer, String metadata, DimensionPresentation presentation, Double resolution) {
        CoverageInfo info = getCatalog().getCoverageByName(layer.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        if (resolution != null) {
            di.setResolution(BigDecimal.valueOf(resolution));
        }
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }
}
