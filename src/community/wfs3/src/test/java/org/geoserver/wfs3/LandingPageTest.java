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

package org.geoserver.wfs3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;

public class LandingPageTest extends WFS3TestSupport {

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("wfs3", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("wfs3/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("wfs3?f=json", 200);
        checkJSONLandingPage(json);
    }

    private void checkJSONLandingPage(DocumentContext json) {
        assertEquals(16, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*wfs3\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*wfs3\\/\\?.*/)].rel",
                "alternate",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    private void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*wfs3\\/api.*/)].rel",
                "service",
                "service",
                "service",
                "service",
                "service");
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*wfs3\\/conformance.*/)].rel",
                "conformance",
                "conformance",
                "conformance");
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*wfs3\\/collections.*/)].rel",
                "data",
                "data",
                "data",
                "data");
    }

    private <T> void assertJSONList(DocumentContext json, String path, T... expected) {
        List<T> selfRels = json.read(path);
        assertThat(selfRels, Matchers.containsInAnyOrder(expected));
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("cdf/wfs3/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageXML() throws Exception {
        Document dom = getAsDOM("wfs3/?f=application/xml");
        print(dom);
        // TODO: add actual tests in here
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("wfs3/?f=application/x-yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/x-yaml' && @.href =~ /.*wfs3\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/x-yaml' && @.href =~ /.*wfs3\\/\\?.*/)].rel",
                "alternate",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("wfs3?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/wfs3/api?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    @Test
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/wfs3?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/wfs3/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/wfs3/api?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }
}
