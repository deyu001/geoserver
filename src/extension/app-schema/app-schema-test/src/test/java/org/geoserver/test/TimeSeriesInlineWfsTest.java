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
 * This is to test using isList to group multiple values as a concatenated single value without
 * feature chaining.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class TimeSeriesInlineWfsTest extends TimeSeriesWfsTest {

    protected TimeSeriesInlineMockData createTestData() {
        // only the test data is different since the config is slightly different (not using feature
        // chaining)
        // but the test cases from TimeSeriesWfsTest are the same
        return new TimeSeriesInlineMockData();
    }

    /** Test subsetting timePositionList. */
    @Test
    public void testTimePositionSubset() {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:csml=\""
                        + TimeSeriesMockData.CSML_URI
                        + "\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" //
                        + "\"" //
                        + ">" //
                        + "<wfs:Query typeName=\"csml:PointSeriesFeature\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:PropertyIsBetween>"
                        + "             <ogc:PropertyName>csml:PointSeriesFeature/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList</ogc:PropertyName>"
                        + "             <ogc:LowerBoundary><ogc:Literal>1949-05-01</ogc:Literal></ogc:LowerBoundary>"
                        + "             <ogc:UpperBoundary><ogc:Literal>1949-09-01</ogc:Literal></ogc:UpperBoundary>"
                        + "        </ogc:PropertyIsBetween>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(1, "//csml:PointSeriesFeature", doc);
        checkPointFeatureTwo(doc);

        // HACK HACK HACK
        // The result is a subset of the timePositionList value that matches the filter
        // This is an experimental/temporary solution for Bureau of Meteorology subsetting
        // requirement
        assertXpathEvaluatesTo(
                "1949-05-01 1949-06-01 1949-07-01 1949-08-01 1949-09-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);

        // matching subset for QuantityList without feature chaining
        assertXpathEvaluatesTo(
                "16.2 17.1 22.0 25.1 23.9",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);
        // END OF HACK
    }

    @Override
    /** Test filtering quantity list that is not feature chained. */
    @Test
    public void testQuantityListSubset() {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:csml=\""
                        + TimeSeriesMockData.CSML_URI
                        + "\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" //
                        + "\"" //
                        + ">" //
                        + "<wfs:Query typeName=\"csml:PointSeriesFeature\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"\\\">"
                        + "            <ogc:PropertyName>csml:PointSeriesFeature/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList</ogc:PropertyName>"
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
        assertXpathCount(1, "//csml:PointSeriesFeature", doc);
        checkPointFeatureTwo(doc);
        // subsetting works without feature chaining, therefore a subset of QuantityList expected
        assertXpathEvaluatesTo(
                "16.2",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);

        // matching subset of timePositionList expected
        assertXpathEvaluatesTo(
                "1949-05-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);
    }
}
