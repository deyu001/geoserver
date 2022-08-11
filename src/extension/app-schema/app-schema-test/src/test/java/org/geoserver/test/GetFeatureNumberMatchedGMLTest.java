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

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test the proper encoding of duplicated/repeated features with Ids
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class GetFeatureNumberMatchedGMLTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    /** Tests that a count for All the features works * */
    @Test
    public void testGetMappedFeatureHitsCount() {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits");
        LOGGER.info("WFS GetFeature, typename=gsml:MappedFeature response:\n" + prettyString(doc));

        assertNumberMathcedAndNumberReturned(doc, 4, 0);
    }

    /** Test that count with a filter pointing to a root property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnRootAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");
        LOGGER.info(prettyString(doc));
        assertNumberMathcedAndNumberReturned(doc, 1, 0);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'");
        LOGGER.info(prettyString(doc));
        assertNumberMathcedAndNumberReturned(doc, 1, 0);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttribute2() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");
        LOGGER.info(prettyString(doc));

        assertNumberMathcedAndNumberReturned(doc, 3, 0);
    }

    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttributeWithMaxNumber() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27&count=1");
        LOGGER.info(prettyString(doc));
        assertNumberMathcedAndNumberReturned(doc, 3, 0);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureNumberMatchedWithFilterOnNestedAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'");
        LOGGER.info(prettyString(doc));

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithFilterOnNestedAttribute2() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");
        LOGGER.info(prettyString(doc));

        assertNumberMathcedAndNumberReturned(doc, 3, 3);
    }

    @Test
    public void testGetFeatureNumberMatchedWithAndNestedFilterOnSameTypes() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'"
                                + "AND gsml:specification.gsml:GeologicUnit.gml:name = 'New Group'");
        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimpleProperty() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMathcedAndNumberReturned(doc, 4, 4);
    }

    @Test
    public void testGetFeatureNumberMatchedWithSimplePropertyANDComplexProperty() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'"
                                + " AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimplePropertyWithPagination()
            throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'&startIndex=3&count=2");

        assertNumberMathcedAndNumberReturned(doc, 4, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithMultipleAND() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:name = 'New Group'"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%25%27 AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilter() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");

        assertNumberMathcedAndNumberReturned(doc, 3, 3);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterWithPagination() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27&startIndex=1");

        assertNumberMathcedAndNumberReturned(doc, 3, 2);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterManyAND() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27 AND gsml:MappedFeature.gml:name = 'GUNTHORPE FORMATION'");

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    private void assertNumberMathcedAndNumberReturned(
            Document doc, int numberMatched, int numberReturned) {
        assertXpathEvaluatesTo(
                String.valueOf(numberMatched), "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathEvaluatesTo(
                String.valueOf(numberReturned), "/wfs:FeatureCollection/@numberReturned", doc);
    }
}
