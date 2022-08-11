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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests whether Property Selection is properly applied on complex features
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class PropertySelectionTest extends AbstractAppSchemaTestSupport {

    @Override
    protected PropertySelectionMockData createTestData() {
        return new PropertySelectionMockData();
    }

    /** Test GetFeature with Property Selection. */
    @Test
    public void testGetFeature() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:description");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:description Response:\n"
                        + prettyString(doc));

        // using custom IDs - this is being tested too

        // check if requested property is present
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:description", doc);

        // check if required property is present
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit",
                doc);

        // check if non-requested property is not present
        assertXpathCount(
                0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:description",
                doc);
    }

    /** Test Property Selection with Feature Chaining. */
    @Test
    public void testGetFeatureFeatureChaining() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typename=gsml:MappedFeature&propertyname=gsml:specification/gsml:GeologicUnit/gml:description");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typename=gsml:MappedFeature&propertyname=gsml:specification/gsml:GeologicUnit/gml:description response:\n"
                        + prettyString(doc));

        // check if requested property is present
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:description",
                doc);

        // check if required property is present
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit",
                doc);

        // check if non-requested property is not present
        assertXpathCount(
                0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        assertXpathCount(
                0,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathCount(
                0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:description", doc);
    }

    /** Test GetFeature with Property Selection, using client properties. */
    @Test
    public void testGetFeatureClientProperty() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature&propertyname=gsml:metadata");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature&propertyname=gsml:metadata response:\n"
                        + prettyString(doc));

        // test client property works
        assertXpathEvaluatesTo(
                "zzzgu.25699",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata/@xlink:href",
                doc);
    }

    /** Test GetFeature with Property Selection, with an invalid column name. */
    @Test
    public void testGetFeatureInvalidName() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:name");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature&propertyname=gml:name response:\n"
                        + prettyString(doc));

        // test exception refering to missing column
        assertTrue(
                evaluate("//ows:ExceptionText", doc)
                        .contains(
                                "Could not find working property accessor for attribute (DOESNT_EXIST)"));
    }

    /** Test Posting GetFeature */
    @Test
    public void testPostGetFeature() {
        String xml =
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:gsml=\""
                        + AbstractAppSchemaMockData.GSML_URI
                        + "\" " //
                        + ">" //
                        + "<wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "<ogc:PropertyName>gml:description</ogc:PropertyName> "
                        + "<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName> "
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);

        LOGGER.info("WFS GetFeature POST response:\n" + prettyString(doc));

        // check if requested property is present
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:description", doc);

        // check if required property is present
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue",
                doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:specification/gsml:GeologicUnit",
                doc);

        // check if non-requested property is not present
        assertXpathCount(
                0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
    }

    /**
     * Test GetFeature with Property Selection, with properties names with same name but different
     * namespace.
     */
    @Test
    public void testSameNameDiffNamespace1() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=ex:MyTestFeature&propertyname=ex:name");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=ex:MyTestFeature&propertyname=ex:name response:\n"
                        + prettyString(doc));

        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/ex:name", doc);
        assertXpathCount(0, "//ex:MyTestFeature[@gml:id='f1']/gml:name", doc);
    }

    /**
     * Test GetFeature with Property Selection, with properties names with same name but different
     * namespace.
     */
    @Test
    public void testSameNameDiffNamespace2() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=ex:MyTestFeature&propertyname=gml:name");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=ex:MyTestFeature&propertyname=gml:name response:\n"
                        + prettyString(doc));

        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/gml:name", doc);
        assertXpathCount(0, "//ex:MyTestFeature[@gml:id='f1']/ex:name", doc);
    }

    /** Test GetFeature with Property Selection, with an invalid column name. */
    @Test
    public void testSameNameDiffNamespace3() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=ex:MyTestFeature");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=ex:MyTestFeature response:\n"
                        + prettyString(doc));
    }
}
