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

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * This is to test using isList to group multiple values as a concatenated single value with
 * proposed waterml timeseries schema. It involves QuantityList as a simple type.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class WaterMLTimeSeriesWfsTest extends AbstractAppSchemaTestSupport {

    protected WaterMLTimeSeriesMockData createTestData() {
        return new WaterMLTimeSeriesMockData();
    }

    /** Test get feature is fine with QuantityList as a list of simple type. */
    @Test
    public void testGetFeature() {
        String path =
                "wfs?request=GetFeature&outputFormat=gml32&typename=wml2dr:MeasurementTimeseriesDomainRange";
        Document doc = getAsDOM(path);
        LOGGER.info(
                "WFS GetFeature, typename=wml2dr:MeasurementTimeseriesDomainRange response:\n"
                        + prettyString(doc));
        validateGet(path);
        assertXpathCount(2, "//wml2dr:MeasurementTimeseriesDomainRange", doc);

        String id = "ID1";
        assertXpathEvaluatesTo(id, "(//wml2dr:MeasurementTimeseriesDomainRange)[1]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "tpl." + id,
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:domainSet/wml2dr:TimePositionList/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "1948-01-01T00:00:00Z 1948-02-01T00:00:00Z 1948-03-01T00:00:00Z 1948-04-01T00:00:00Z 1948-05-01T00:00:00Z "
                        + "1948-06-01T00:00:00Z 1948-07-01T00:00:00Z 1948-08-01T00:00:00Z 1948-09-01T00:00:00Z 1948-10-01T00:00:00Z "
                        + "1948-11-01T00:00:00Z 1948-12-01T00:00:00Z 1949-01-01T00:00:00Z 1949-02-01T00:00:00Z 1949-03-01T00:00:00Z "
                        + "1949-04-01T00:00:00Z",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList",
                doc);
        assertXpathEvaluatesTo(
                "missing missing 8.9 7.9 14.2 15.4 18.1 19.1 21.7 20.8 19.6 14.9 10.8 8.8 8.5 10.4",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:rangeSet/gml:QuantityList",
                doc);
        assertXpathEvaluatesTo(
                "degC",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:rangeSet/gml:QuantityList/@uom",
                doc);
        assertXpathEvaluatesTo(
                "string",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:coverageFunction/gml:MappingRule",
                doc);
        assertXpathEvaluatesTo(
                "http://ns.bgs.ac.uk/thesaurus/lithostratigraphy",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gmlcov:rangeType/@xlink:href",
                doc);

        id = "ID2";
        assertXpathEvaluatesTo(id, "(//wml2dr:MeasurementTimeseriesDomainRange)[2]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "tpl." + id,
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:domainSet/wml2dr:TimePositionList/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "1949-05-01T00:00:00Z 1949-06-01T00:00:00Z 1949-07-01T00:00:00Z 1949-08-01T00:00:00Z 1949-09-01T00:00:00Z "
                        + "1949-10-01T00:00:00Z 1949-11-01T00:00:00Z 1949-12-01T00:00:00Z 1950-01-01T00:00:00Z 1950-02-01T00:00:00Z "
                        + "1950-03-01T00:00:00Z 1950-04-01T00:00:00Z 1950-05-01T00:00:00Z 1950-06-01T00:00:00Z 1950-07-01T00:00:00Z "
                        + "1950-08-01T00:00:00Z 1950-09-01T00:00:00Z 1950-10-01T00:00:00Z 1950-11-01T00:00:00Z 1950-12-01T00:00:00Z",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList",
                doc);
        assertXpathEvaluatesTo(
                "16.2 17.1 22.0 25.1 23.9 22.8 17.0 10.2 9.2 7.1 12.3 12.9 17.2 23.6 21.6 21.9 17.6 14.0 9.3 3.8",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:rangeSet/gml:QuantityList",
                doc);
        assertXpathEvaluatesTo(
                "degC",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:rangeSet/gml:QuantityList/@uom",
                doc);
        assertXpathEvaluatesTo(
                "string",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gml:coverageFunction/gml:MappingRule",
                doc);
        assertXpathEvaluatesTo(
                "http://ns.bgs.ac.uk/thesaurus/lithostratigraphy",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + id
                        + "']/gmlcov:rangeType/@xlink:href",
                doc);
    }

    /** Test filtering QuantityList as a list of simple type. */
    @Test
    public void testFilter() {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:wml2dr=\"http://www.opengis.net/waterml/DR/2.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" //
                        + "\"" //
                        + ">" //
                        + "<wfs:Query typeName=\"wml2dr:MeasurementTimeseriesDomainRange\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"\\\">"
                        + "            <ogc:PropertyName>wml2dr:MeasurementTimeseriesDomainRange/gml:rangeSet/gml:QuantityList</ogc:PropertyName>"
                        + "            <ogc:Literal>*16.2*</ogc:Literal>"
                        + "        </ogc:PropertyIsLike>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(1, "//wml2dr:MeasurementTimeseriesDomainRange", doc);
        assertXpathEvaluatesTo("ID2", "//wml2dr:MeasurementTimeseriesDomainRange/@gml:id", doc);
        assertXpathEvaluatesTo(
                "tpl." + "ID2",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:domainSet/wml2dr:TimePositionList/@gml:id",
                doc);
        // truncated value as a result of subset filtering changes
        assertXpathEvaluatesTo(
                "16.2",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:rangeSet/gml:QuantityList",
                doc);
        assertXpathEvaluatesTo(
                "degC",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:rangeSet/gml:QuantityList/@uom",
                doc);
        assertXpathEvaluatesTo(
                "string",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:coverageFunction/gml:MappingRule",
                doc);
        assertXpathEvaluatesTo(
                "http://ns.bgs.ac.uk/thesaurus/lithostratigraphy",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gmlcov:rangeType/@xlink:href",
                doc);
        // timePositition is truncated too as they're value pairs with QuantityList
        assertXpathEvaluatesTo(
                "1949-05-01T00:00:00Z",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList",
                doc);
    }

    /** Test filtering timePositionList expecting a subset. */
    @Test
    public void testTimePositionSubset() {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:wml2dr=\"http://www.opengis.net/waterml/DR/2.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" //
                        + "\"" //
                        + ">" //
                        + "<wfs:Query typeName=\"wml2dr:MeasurementTimeseriesDomainRange\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:PropertyIsBetween>"
                        + "             <ogc:PropertyName>"
                        + " wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList"
                        + "</ogc:PropertyName>"
                        + "             <ogc:LowerBoundary><ogc:Literal>1950-03-01T00:00:00Z</ogc:Literal></ogc:LowerBoundary>"
                        + "             <ogc:UpperBoundary><ogc:Literal>1950-06-01T00:00:00Z</ogc:Literal></ogc:UpperBoundary>"
                        + "        </ogc:PropertyIsBetween>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathEvaluatesTo("ID2", "(//wml2dr:MeasurementTimeseriesDomainRange)/@gml:id", doc);
        assertXpathCount(1, "//wml2dr:MeasurementTimeseriesDomainRange", doc);
        assertXpathEvaluatesTo(
                "tpl." + "ID2",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:domainSet/wml2dr:TimePositionList/@gml:id",
                doc);
        // timePositionList subset
        assertXpathEvaluatesTo(
                "1950-03-01T00:00:00Z 1950-04-01T00:00:00Z 1950-05-01T00:00:00Z 1950-06-01T00:00:00Z",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList",
                doc);
        // matching subset of QuantityList
        assertXpathEvaluatesTo(
                "12.3 12.9 17.2 23.6",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:rangeSet/gml:QuantityList",
                doc);
        assertXpathEvaluatesTo(
                "degC",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:rangeSet/gml:QuantityList/@uom",
                doc);
        assertXpathEvaluatesTo(
                "string",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gml:coverageFunction/gml:MappingRule",
                doc);
        assertXpathEvaluatesTo(
                "http://ns.bgs.ac.uk/thesaurus/lithostratigraphy",
                "//wml2dr:MeasurementTimeseriesDomainRange[@gml:id='"
                        + "ID2"
                        + "']/gmlcov:rangeType/@xlink:href",
                doc);
    }
}
