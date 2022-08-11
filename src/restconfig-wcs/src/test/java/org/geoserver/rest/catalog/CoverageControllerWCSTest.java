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

package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geotools.util.URLs;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class CoverageControllerWCSTest extends CatalogRESTTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
    }

    @Before
    public void addBlueMarbleCoverage() throws Exception {
        getTestData().addDefaultRasterLayer(SystemTestData.TASMANIA_BM, getCatalog());
    }

    void addCoverageStore(boolean autoConfigureCoverage) throws Exception {
        URL zip = getClass().getResource("test-data/usa.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/file.worldimage"
                                + (!autoConfigureCoverage ? "?configure=none" : ""),
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
    }

    @Test
    public void testGetAllByCoverageStore() throws Exception {
        removeStore("gs", "usaWorldImage");
        String req =
                "wcs?service=wcs&request=getcoverage&version=1.1.1&identifier=gs:usa"
                        + "&boundingbox=-100,30,-80,44,EPSG:4326&format=image/tiff"
                        + "&gridbasecrs=EPSG:4326&store=true";

        Document dom = getAsDOM(req);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addCoverageStore(true);
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals(1, dom.getElementsByTagName("coverage").getLength());
        assertXpathEvaluatesTo("1", "count(//coverage/name[text()='usa'])", dom);
    }

    @Test
    public void testPostAsXML() throws Exception {
        removeStore("gs", "usaWorldImage");
        String req =
                "wcs?service=wcs&request=getcoverage&version=1.1.1&identifier=gs:usa"
                        + "&boundingbox=-100,30,-80,44,EPSG:4326&format=image/tiff"
                        + "&gridbasecrs=EPSG:4326&store=true";

        Document dom = getAsDOM(req);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addCoverageStore(false);
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals(0, dom.getElementsByTagName("coverage").getLength());

        String xml =
                "<coverage>"
                        + "<name>usa</name>"
                        + "<title>usa is a A raster file accompanied by a spatial data file</title>"
                        + "<description>Generated from WorldImage</description>"
                        + "<srs>EPSG:4326</srs>"
                        +
                        /*"<latLonBoundingBox>"+
                          "<minx>-130.85168</minx>"+
                          "<maxx>-62.0054</maxx>"+
                          "<miny>20.7052</miny>"+
                          "<maxy>54.1141</maxy>"+
                        "</latLonBoundingBox>"+
                        "<nativeBoundingBox>"+
                          "<minx>-130.85168</minx>"+
                          "<maxx>-62.0054</maxx>"+
                          "<miny>20.7052</miny>"+
                          "<maxy>54.1141</maxy>"+
                          "<crs>EPSG:4326</crs>"+
                        "</nativeBoundingBox>"+
                        "<grid dimension=\"2\">"+
                            "<range>"+
                              "<low>0 0</low>"+
                              "<high>983 598</high>"+
                            "</range>"+
                            "<transform>"+
                              "<scaleX>0.07003690742624616</scaleX>"+
                              "<scaleY>-0.05586772575250837</scaleY>"+
                              "<shearX>0.0</shearX>"+
                              "<shearX>0.0</shearX>"+
                              "<translateX>-130.81666154628687</translateX>"+
                              "<translateY>54.08616613712375</translateY>"+
                            "</transform>"+
                            "<crs>EPSG:4326</crs>"+
                        "</grid>"+*/
                        "<supportedFormats>"
                        + "<string>PNG</string>"
                        + "<string>GEOTIFF</string>"
                        + "</supportedFormats>"
                        + "<requestSRS>"
                        + "<string>EPSG:4326</string>"
                        + "</requestSRS>"
                        + "<responseSRS>"
                        + "<string>EPSG:4326</string>"
                        + "</responseSRS>"
                        + "<store>usaWorldImage</store>"
                        + "<namespace>gs</namespace>"
                        + "</coverage>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/",
                        xml,
                        "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/gs/coveragestores/usaWorldImage/coverages/usa"));

        dom = getAsDOM(req);
        assertEquals("wcs:Coverages", dom.getDocumentElement().getNodeName());

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/usa.xml");
        assertXpathEvaluatesTo("-130.85168", "/coverage/latLonBoundingBox/minx", dom);
        assertXpathEvaluatesTo("983 598", "/coverage/grid/range/high", dom);

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals(1, dom.getElementsByTagName("coverage").getLength());
    }

    @Test
    public void testPostAsJSON() throws Exception {
        // remove the test store and test that the layer is not available
        removeStore("gs", "usaWorldImage");
        String request =
                "wcs?service=wcs&request=getcoverage&version=1.1.1&identifier=gs:usa"
                        + "&boundingbox=-100,30,-80,44,EPSG:4326&format=image/tiff&gridbasecrs=EPSG:4326&store=true";
        Document document = getAsDOM(request);
        assertEquals("ows:ExceptionReport", document.getDocumentElement().getNodeName());
        // add the test store, no coverages should be available
        addCoverageStore(false);
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/gs/coveragestores/usaWorldImage/coverages.json");
        assertThat(json.getString("coverages").isEmpty(), is(true));
        // content for the POST request
        String content =
                "{"
                        + "    \"coverage\": {"
                        + "        \"description\": \"Generated from WorldImage\","
                        + "        \"name\": \"usa\","
                        + "        \"namespace\": \"gs\","
                        + "        \"requestSRS\": {"
                        + "            \"string\": ["
                        + "                \"EPSG:4326\""
                        + "            ]"
                        + "        },"
                        + "        \"responseSRS\": {"
                        + "            \"string\": ["
                        + "                \"EPSG:4326\""
                        + "            ]"
                        + "        },"
                        + "        \"srs\": \"EPSG:4326\","
                        + "        \"store\": \"usaWorldImage\","
                        + "        \"supportedFormats\": {"
                        + "            \"string\": ["
                        + "                \"PNG\","
                        + "                \"GEOTIFF\""
                        + "            ]"
                        + "        },"
                        + "        \"title\": \"usa is a A raster file accompanied by a spatial data file\""
                        + "    }"
                        + "}";
        // perform the POST request that will create the USA coverage
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/",
                        content,
                        "application/json");
        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith("/workspaces/gs/coveragestores/usaWorldImage/coverages/usa"));
        // check that the coverage exists using the WCS service
        document = getAsDOM(request);
        assertEquals("wcs:Coverages", document.getDocumentElement().getNodeName());
        // check create coverage attributes
        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/gs/coveragestores/usaWorldImage/coverages/usa.json");
        assertThat(json.getJSONObject("coverage").getString("name"), is("usa"));
        assertThat(
                json.getJSONObject("coverage").getJSONObject("latLonBoundingBox").getString("minx"),
                is("-130.85168"));
        assertThat(
                json.getJSONObject("coverage")
                        .getJSONObject("grid")
                        .getJSONObject("range")
                        .getString("high"),
                is("983 598"));
        // check that the coverage is listed
        json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/gs/coveragestores/usaWorldImage/coverages.json");
        JSONArray coverages = json.getJSONObject("coverages").getJSONArray("coverage");
        assertThat(coverages.size(), is(1));
        assertThat(coverages.getJSONObject(0).getString("name"), is("usa"));
    }

    @Test
    public void testPostAsXMLWithNativeName() throws Exception {
        removeStore("gs", "usaWorldImage");
        String req =
                "wcs?service=wcs&request=getcoverage&version=1.1.1&identifier=gs:differentName"
                        + "&boundingbox=-100,30,-80,44,EPSG:4326&format=image/tiff"
                        + "&gridbasecrs=EPSG:4326&store=true";

        Document dom = getAsDOM(req);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addCoverageStore(false);
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals(0, dom.getElementsByTagName("coverage").getLength());

        String xml =
                "<coverage>"
                        + "<name>differentName</name>"
                        + "<title>usa is a A raster file accompanied by a spatial data file</title>"
                        + "<description>Generated from WorldImage</description>"
                        + "<srs>EPSG:4326</srs>"
                        + "<supportedFormats>"
                        + "<string>PNG</string>"
                        + "<string>GEOTIFF</string>"
                        + "</supportedFormats>"
                        + "<requestSRS>"
                        + "<string>EPSG:4326</string>"
                        + "</requestSRS>"
                        + "<responseSRS>"
                        + "<string>EPSG:4326</string>"
                        + "</responseSRS>"
                        + "<store>usaWorldImage</store>"
                        + "<namespace>gs</namespace>"
                        + "<nativeCoverageName>usa</nativeCoverageName>"
                        + "</coverage>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/",
                        xml,
                        "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                "/workspaces/gs/coveragestores/usaWorldImage/coverages/differentName"));

        dom = getAsDOM(req);
        assertEquals("wcs:Coverages", dom.getDocumentElement().getNodeName());

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/differentName.xml");
        assertXpathEvaluatesTo("-130.85168", "/coverage/latLonBoundingBox/minx", dom);
        assertXpathEvaluatesTo("983 598", "/coverage/grid/range/high", dom);
    }

    @Test
    public void testPostNewAsXMLWithNativeCoverageName() throws Exception {
        removeStore("gs", "usaWorldImage");
        String req =
                "wcs?service=wcs&request=getcoverage&version=1.1.1&identifier=gs:differentName"
                        + "&boundingbox=-100,30,-80,44,EPSG:4326&format=image/tiff"
                        + "&gridbasecrs=EPSG:4326&store=true";

        Document dom = getAsDOM(req);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addCoverageStore(false);
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals(0, dom.getElementsByTagName("coverage").getLength());

        String xml =
                "<coverage>"
                        + "<name>differentName</name>"
                        + "<nativeCoverageName>usa</nativeCoverageName>"
                        + "</coverage>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/",
                        xml,
                        "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                "/workspaces/gs/coveragestores/usaWorldImage/coverages/differentName"));

        dom = getAsDOM(req);
        assertEquals("wcs:Coverages", dom.getDocumentElement().getNodeName());

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/differentName.xml");
        assertXpathEvaluatesTo("differentName", "/coverage/name", dom);
        assertXpathEvaluatesTo("differentName", "/coverage/title", dom);
        assertXpathEvaluatesTo("usa", "/coverage/nativeCoverageName", dom);
    }

    @Test
    public void testPostNewAsXMLWithNativeNameFallback() throws Exception {
        removeStore("gs", "usaWorldImage");
        String req =
                "wcs?service=wcs&request=getcoverage&version=1.1.1&identifier=gs:differentName"
                        + "&boundingbox=-100,30,-80,44,EPSG:4326&format=image/tiff"
                        + "&gridbasecrs=EPSG:4326&store=true";

        Document dom = getAsDOM(req);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        addCoverageStore(false);
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages.xml");
        assertEquals(0, dom.getElementsByTagName("coverage").getLength());

        String xml =
                "<coverage>"
                        + "<name>differentName</name>"
                        + "<nativeName>usa</nativeName>"
                        + "</coverage>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/",
                        xml,
                        "text/xml");

        assertEquals(201, response.getStatus());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location")
                        .endsWith(
                                "/workspaces/gs/coveragestores/usaWorldImage/coverages/differentName"));

        dom = getAsDOM(req);
        assertEquals("wcs:Coverages", dom.getDocumentElement().getNodeName());

        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/gs/coveragestores/usaWorldImage/coverages/differentName.xml");
        assertXpathEvaluatesTo("differentName", "/coverage/name", dom);
        assertXpathEvaluatesTo("differentName", "/coverage/title", dom);
        assertXpathEvaluatesTo("usa", "/coverage/nativeCoverageName", dom);
    }
}
