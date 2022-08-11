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

package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.TASMANIA_DEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.GetCapabilities;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Test for {@link GetCapabilities}
 *
 * @author Simone Giannecchini, GeoSolutions
 */
public class GetCapabilitiesTest extends WCSTestSupport {

    @Before
    public void cleanupLimitedSRS() {
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        service.getSRS().clear();
        getGeoServer().save(service);
    }

    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);

        checkFullCapabilitiesDocument(dom);
    }

    @Test
    public void testCase() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=wCS");
        // print(dom);

        // check that we have the crs extension
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])",
                dom);
        assertXpathEvaluatesTo(
                "1", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void testLimitedSRS() throws Exception {
        // check we support a lot of SRS by default
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);
        NodeList list =
                xpath.getMatchingNodes(
                        "//wcs:ServiceMetadata/wcs:Extension/crs:CrsMetadata/crs:crsSupported",
                        dom);
        assertTrue(list.getLength() > 1000);

        // setup limited list
        WCSInfo service = getGeoServer().getService(WCSInfo.class);
        service.getSRS().add("4326");
        service.getSRS().add("32632");
        getGeoServer().save(service);

        dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        // print(dom);
        list =
                xpath.getMatchingNodes(
                        "//wcs:ServiceMetadata/wcs:Extension/crs:CrsMetadata/crs:crsSupported",
                        dom);
        assertEquals(2, list.getLength());
    }

    @Test
    public void testSectionsBogus() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&sections=Bogus");
        Element root = dom.getDocumentElement();
        assertEquals("ows:ExceptionReport", root.getNodeName());
        assertEquals("2.0.0", root.getAttribute("version"));
        assertEquals("http://www.opengis.net/ows/2.0", root.getAttribute("xmlns:ows"));
        assertXpathEvaluatesTo(
                WcsExceptionCode.InvalidParameterValue.toString(),
                "/ows:ExceptionReport/ows:Exception/@exceptionCode",
                dom);
    }

    @Test
    public void testSectionsAll() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&sections=All");
        assertXpathEvaluatesTo("1", "count(//ows:ServiceIdentification)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:ServiceProvider)", dom);
        assertXpathEvaluatesTo("1", "count(//ows:OperationsMetadata)", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:Contents)", dom);
    }

    @Test
    public void testAcceptVersions() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS&acceptversions=2.0.1");

        // make sure no exception is thrown
        assertXpathEvaluatesTo("0", "count(//ows:ExceptionReport)", dom);
        assertXpathEvaluatesTo("0", "count(//ows:ExceptionReport//ows:Exception)", dom);
        assertXpathEvaluatesTo(
                "0",
                "count(//ows:ExceptionReport//ows:Exception[@exceptionCode='InvalidParameterValue'])",
                dom);
        assertXpathEvaluatesTo(
                "0", "count(//ows:ExceptionReport//ows:Exception[@locator='wCS'])", dom);
    }

    @Test
    public void testMetadata() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        ci.setTitle("My Title");
        ci.setDescription("My Abstract");
        ci.getKeywords().add(0, new Keyword("my_keyword"));
        MetadataLinkInfo mdl1 = catalog.getFactory().createMetadataLink();
        mdl1.setContent("http://www.geoserver.org/tasmania/dem.xml");
        mdl1.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(mdl1);
        MetadataLinkInfo mdl2 = catalog.getFactory().createMetadataLink();
        mdl2.setContent("/metadata?key=value");
        mdl2.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(mdl2);
        catalog.save(ci);
        Document dom = getAsDOM("wcs?service=WCS&version=2.0.1&request=GetCapabilities");
        // print(dom);

        checkValidationErrors(dom, getWcs20Schema());
        String base = "//wcs:Capabilities/wcs:Contents/wcs:CoverageSummary[wcs:CoverageId = '";
        base += getLayerId(TASMANIA_DEM).replace(":", "__") + "']/";
        assertXpathEvaluatesTo("My Title", base + "ows:Title", dom);
        assertXpathEvaluatesTo("My Abstract", base + "ows:Abstract", dom);
        assertXpathEvaluatesTo("4", "count(" + base + "ows:Keywords/ows:Keyword)", dom);
        assertXpathEvaluatesTo("my_keyword", base + "ows:Keywords/ows:Keyword[1]", dom);
        assertXpathEvaluatesTo("2", "count(" + base + "ows:Metadata)", dom);
        assertXpathEvaluatesTo("http://www.geoserver.org", base + "ows:Metadata[1]/@about", dom);
        assertXpathEvaluatesTo("simple", base + "ows:Metadata[1]/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "http://www.geoserver.org/tasmania/dem.xml",
                base + "ows:Metadata[1]/@xlink:href",
                dom);
        assertXpathEvaluatesTo("http://www.geoserver.org", base + "ows:Metadata[2]/@about", dom);
        assertXpathEvaluatesTo("simple", base + "ows:Metadata[2]/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "src/test/resources/geoserver/metadata?key=value",
                base + "ows:Metadata[2]/@xlink:href",
                dom);
    }
}
