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

import org.geotools.filter.v2_0.FES;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test the proper encoding of duplicated/repeated features with Ids
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class FeatureGML32Test extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    @Test
    public void testGetMappedFeature() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&outputFormat=gml32&typename=gsml:MappedFeature");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:MappedFeature response:\n"
                        + prettyString(doc));
        assertXpathEvaluatesTo(
                "#gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href",
                doc);
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href", doc);
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureWithFilter() throws Exception {

        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"2.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + ">" //
                        + "    <wfs:Query typeNames=\"gsml:MappedFeature\">" //
                        + "        <fes:Filter>" //
                        + "            <fes:PropertyIsEqualTo>" //
                        + "                <fes:ValueReference>gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description</fes:ValueReference>" //
                        + "                <fes:Literal>Olivine basalt</fes:Literal>" //
                        + "            </fes:PropertyIsEqualTo>" //
                        + "        </fes:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));

        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
    }

    @Test
    public void testStoredQuery() throws Exception {
        String xml =
                "<wfs:CreateStoredQuery service='WFS' version='2.0.0' "
                        + "   xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "   xmlns:fes='http://www.opengis.net/fes/2.0' "
                        + "   xmlns:gml='http://www.opengis.net/gml/3.2' "
                        + "   xmlns:gsml='urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0'>"
                        + "   <wfs:StoredQueryDefinition id='myStoredQuery'> "
                        + "      <wfs:Parameter name='descr' type='xs:string'/> "
                        + "      <wfs:QueryExpressionText "
                        + "           returnFeatureTypes='gsml:MappedFeature' "
                        + "           language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression' "
                        + "           isPrivate='false'> "
                        + "         <wfs:Query typeNames=\"gsml:MappedFeature\"> "
                        + "            <fes:Filter> "
                        + "               <fes:PropertyIsEqualTo> "
                        + "                  <fes:ValueReference>gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/gml:description</fes:ValueReference> "
                        + "                  ${descr}"
                        + "               </fes:PropertyIsEqualTo> "
                        + "            </fes:Filter> "
                        + "         </wfs:Query> "
                        + "      </wfs:QueryExpressionText> "
                        + "   </wfs:StoredQueryDefinition> "
                        + "</wfs:CreateStoredQuery>";
        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:CreateStoredQueryResponse", doc.getDocumentElement().getNodeName());

        xml =
                "<wfs:GetFeature service='WFS' version='2.0.0' "
                        + "       xmlns:wfs='"
                        + WFS.NAMESPACE
                        + "' xmlns:fes='"
                        + FES.NAMESPACE
                        + "'>"
                        + "   <wfs:StoredQuery id='myStoredQuery'> "
                        + "      <wfs:Parameter name='descr'>"
                        + "        <fes:Literal>Olivine basalt</fes:Literal>"
                        + "      </wfs:Parameter> "
                        + "   </wfs:StoredQuery> "
                        + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));

        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
    }

    /** Test encoding of a multivalued mapping with an xlink:href ClientProperty. */
    @Test
    public void testMultivaluedXlinkHref() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetFeature&typenames=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature, typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        // expect gsml:occurrence to appear twice for this feature with only @xlink:href
        assertXpathCount(
                2, "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/@xlink:href", doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf2",
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf3",
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence[2]/@xlink:href",
                doc);
        // expect no nested features
        assertXpathCount(
                0,
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature",
                doc);
    }
}
