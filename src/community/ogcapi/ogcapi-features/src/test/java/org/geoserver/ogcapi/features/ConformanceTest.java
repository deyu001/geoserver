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

package org.geoserver.ogcapi.features;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

public class ConformanceTest extends FeaturesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(1, (int) json.read("$.length()", Integer.class));
        assertEquals(6, (int) json.read("$.conformsTo.length()", Integer.class));
        assertEquals(FeatureService.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(FeatureService.OAS30, json.read("$.conformsTo[1]", String.class));
        assertEquals(FeatureService.HTML, json.read("$.conformsTo[2]", String.class));
        assertEquals(FeatureService.GEOJSON, json.read("$.conformsTo[3]", String.class));
        assertEquals(FeatureService.GMLSF0, json.read("$.conformsTo[4]", String.class));
        assertEquals(FeatureService.CQL_TEXT, json.read("$.conformsTo[5]", String.class));
    }

    @Test
    @Ignore
    public void testConformanceXML() throws Exception {
        Document dom = getAsDOM("ogc/features?f=application/xml");
        print(dom);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/features/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features/conformance?f=text/html");
        assertEquals("GeoServer OGC API Features Conformance", document.select("#title").text());
        assertEquals(FeatureService.CORE, document.select("#content li:eq(0)").text());
        assertEquals(FeatureService.OAS30, document.select("#content li:eq(1)").text());
        assertEquals(FeatureService.HTML, document.select("#content li:eq(2)").text());
        assertEquals(FeatureService.GEOJSON, document.select("#content li:eq(3)").text());
        assertEquals(FeatureService.GMLSF0, document.select("#content li:eq(4)").text());
        assertEquals(FeatureService.CQL_TEXT, document.select("#content li:eq(5)").text());
    }
}
