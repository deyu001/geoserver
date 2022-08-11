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

package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.process.Processors;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.junit.After;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public abstract class WPSTestSupport extends GeoServerSystemTestSupport {

    protected static Catalog catalog;
    protected static XpathEngine xp;

    // WCS 1.1
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_DEM = new QName(WCS_URI, "DEM", WCS_PREFIX);
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName ROTATED_CAD = new QName(WCS_URI, "RotatedCad", WCS_PREFIX);
    public static QName WORLD = new QName(WCS_URI, "World", WCS_PREFIX);
    public static String TIFF = "tiff";

    List<GridCoverage> coverages = new ArrayList<>();

    static {
        Processors.addProcessFactory(MonkeyProcess.getFactory());
        Processors.addProcessFactory(MultiRawProcess.getFactory());
        Processors.addProcessFactory(MultiOutputEchoProcess.getFactory());
    }

    protected void scheduleForDisposal(GridCoverage coverage) {
        this.coverages.add(coverage);
    }

    @After
    public void disposeCoverages() {
        for (GridCoverage coverage : coverages) {
            CoverageCleanerCallback.disposeCoverage(coverage);
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // addUser("admin", "geoxserver", null, Arrays.asList("ROLE_ADMINISTRATOR"));
        // addLayerAccessRule("*", "*", AccessMode.READ, "*");
        // addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("feature", "http://geoserver.sf.net");

        testData.registerNamespaces(namespaces);
        registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
    }

    /** Subclasses can override to register custom namespace mappings for xml unit */
    protected void registerNamespaces(Map<String, String> namespaces) {
        // TODO Auto-generated method stub

    }

    protected final void setUpUsers(Properties props) {}

    protected final void setUpLayerRoles(Properties properties) {}

    //    @Before
    //    public void login() throws Exception {
    //        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    //    }

    protected String root() {
        return "wps?";
    }

    /** Validates a document based on the WPS schema */
    protected void checkValidationErrors(Document dom) throws Exception {
        checkValidationErrors(dom, new WPSConfiguration());
    }

    /** Validates a document against the */
    protected void checkValidationErrors(Document dom, Configuration configuration)
            throws Exception {
        Parser p = new Parser(configuration);
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Exception exception : p.getValidationErrors()) {
                SAXParseException ex = (SAXParseException) exception;
                LOGGER.warning(
                        ex.getLineNumber() + "," + ex.getColumnNumber() + " -" + ex.toString());
            }
            fail("Document did not validate.");
        }
    }

    protected String readFileIntoString(String filename) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(filename);
                BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /** Adds the wcs 1.1 coverages. */
    public void addWcs11Coverages(SystemTestData testData) throws Exception {
        String styleName = "raster";
        testData.addStyle(styleName, "raster.sld", MockData.class, getCatalog());

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(LayerProperty.STYLE, styleName);

        // wcs 1.1
        testData.addRasterLayer(
                TASMANIA_DEM, "tazdem.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(
                TASMANIA_BM, "tazbm.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(
                ROTATED_CAD, "rotated.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(WORLD, "world.tiff", TIFF, props, MockData.class, getCatalog());
    }

    /** Submits an asynch execute request and waits for the final result, which is then returned */
    protected Document submitAsynchronous(String xml, long maxWaitSeconds) throws Exception {
        Document dom = postAsDOM("wps", xml);
        assertXpathExists("//wps:ProcessAccepted", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String fullStatusLocation = xpath.evaluate("//wps:ExecuteResponse/@statusLocation", dom);
        String statusLocation = fullStatusLocation.substring(fullStatusLocation.indexOf('?') - 3);
        return waitForProcessEnd(statusLocation, maxWaitSeconds);
    }

    protected Document waitForProcessEnd(String statusLocation, long maxWaitSeconds)
            throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        Document dom = null;
        long start = System.currentTimeMillis();
        while ((((System.currentTimeMillis() - start) / 1000) < maxWaitSeconds)) {
            MockHttpServletResponse response = getAsServletResponse(statusLocation);
            String contents = response.getContentAsString();
            // super weird... and I believe related to the testing harness... just ignoring it
            // for the moment.
            if ("".equals(contents)) {
                continue;
            }
            dom = dom(new ByteArrayInputStream(contents.getBytes()));
            // print(dom);
            // are we still waiting for termination?
            if (xpath.getMatchingNodes("//wps:Status/wps:ProcessAccepted", dom).getLength() > 0
                    || xpath.getMatchingNodes("//wps:Status/wps:ProcessStarted", dom).getLength()
                            > 0
                    || xpath.getMatchingNodes("//wps:Status/wps:ProcessQueued", dom).getLength()
                            > 0) {
                Thread.sleep(100);
            } else {
                return dom;
            }
        }
        throw new Exception("Waited for the process to complete more than " + maxWaitSeconds);
    }

    protected Document waitForProcessEnd(String statusLocation, int maxWaitSeconds)
            throws Exception {
        return waitForProcessEnd(
                statusLocation,
                maxWaitSeconds,
                new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        Thread.sleep(100);
                        return null;
                    }
                });
    }

    protected Document waitForProcessEnd(
            String statusLocation, int maxWaitSeconds, Callable<Void> waitAction) throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        Document dom = null;
        long start = System.currentTimeMillis();
        while ((((System.currentTimeMillis() - start) / 1000) < maxWaitSeconds)) {
            MockHttpServletResponse response = getAsServletResponse(statusLocation);
            String contents = response.getContentAsString();
            // super weird... and I believe related to the testing harness... just ignoring it
            // for the moment.
            if ("".equals(contents)) {
                continue;
            }
            dom = dom(new ByteArrayInputStream(contents.getBytes()));
            // print(dom);
            // are we still waiting for termination?
            if (xpath.getMatchingNodes("//wps:Status/wps:ProcessAccepted", dom).getLength() > 0
                    || xpath.getMatchingNodes("//wps:Status/wps:ProcessStarted", dom).getLength()
                            > 0
                    || xpath.getMatchingNodes("//wps:Status/wps:ProcessQueued", dom).getLength()
                            > 0) {
                waitAction.call();
            } else {
                return dom;
            }
        }
        throw new Exception("Waited for the process to complete more than " + maxWaitSeconds);
    }

    protected Document waitForProcessStart(String statusLocation, long maxWaitSeconds)
            throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        Document dom = null;
        long start = System.currentTimeMillis();
        while ((((System.currentTimeMillis() - start) / 1000) < maxWaitSeconds)) {
            MockHttpServletResponse response = getAsServletResponse(statusLocation);
            String contents = response.getContentAsString();
            // super weird... and I believe related to the testing harness... just ignoring it
            // for the moment.
            if ("".equals(contents)) {
                continue;
            }
            dom = dom(new ByteArrayInputStream(contents.getBytes()));
            // print(dom);
            // are we still waiting for termination?
            if (xpath.getMatchingNodes("//wps:Status/wps:ProcessAccepted", dom).getLength() > 0
                    || xpath.getMatchingNodes("//wps:Status/wps:ProcessQueued", dom).getLength()
                            > 0) {
                Thread.sleep(100);
            } else {
                return dom;
            }
        }
        throw new Exception("Waited for the process to complete more than " + maxWaitSeconds);
    }
}
