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
 * WFS test based on GeoSciML 3.2 Borehole type, a GML 3.2 application schema.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class Gsml32BoreholeIntervalWfsTest extends AbstractAppSchemaTestSupport {

    /** @see org.geoserver.test.AbstractAppSchemaTestSupport#buildTestData() */
    @Override
    protected Gsml32BoreholeIntervalMockData createTestData() {
        return new Gsml32BoreholeIntervalMockData();
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() throws Exception {
        String path = "wfs?request=GetFeature&typename=gsmlbh:Borehole&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));

        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(2, "//gsmlbh:Borehole", doc);

        // #First linestring
        // 1. First borehole
        assertXpathCount(1, "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']", doc);
        // Ensure fabricated LineString works as expected
        // Also custom srsName and 1D posList works
        String lineStringPath =
                "gsmlbh:downholeDrillingDetails/gsmlbh:DrillingDetails/gsmlbh:interval/gml:LineString";
        assertXpathEvaluatesTo(
                "borehole.drillingDetails.interval.17322",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "#borehole.shape.GA.17322",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/"
                        + lineStringPath
                        + "/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "0 153.92",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/gml:posList",
                doc);
        // 2. Second borehole
        assertXpathEvaluatesTo(
                "borehole.drillingDetails.interval.17338",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "#borehole.shape.GA.17338",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/"
                        + lineStringPath
                        + "/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "0 91.55",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/gml:posList",
                doc);

        // #Second LineString
        // 1. First borehole
        lineStringPath = "gsmlbh:logElement/gsmlbh:MappedInterval/gsml:shape/gml:LineString";
        assertXpathEvaluatesTo(
                "borehole.mappedInterval.shape.100",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "#borehole.shape.GA.17322",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/"
                        + lineStringPath
                        + "/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "57.9 66.4",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/gml:posList",
                doc);
        // 2. Second borehole
        lineStringPath = "gsmlbh:logElement/gsmlbh:MappedInterval/gsml:shape/gml:LineString";
        assertXpathEvaluatesTo(
                "borehole.mappedInterval.shape.102",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "#borehole.shape.GA.17338",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/"
                        + lineStringPath
                        + "/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "85.3 89.6",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/gml:posList",
                doc);

        // test empty Curve
        assertXpathEvaluatesTo(
                "borehole.shape.GA.17322",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/sams:shape/gml:Curve/@gml:id",
                doc);
        assertXpathCount(
                0,
                "/gsmlbh:Borehole[@gml:id='borehole.GA.17322']/sams:shape/gml:Curve/@srsName",
                doc);
        assertXpathCount(
                0,
                "/gsmlbh:Borehole[@gml:id='borehole.GA.17322']/sams:shape/gml:Curve/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "borehole.shape.GA.17338",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/sams:shape/gml:Curve/@gml:id",
                doc);
        assertXpathCount(
                0,
                "/gsmlbh:Borehole[@gml:id='borehole.GA.17338']/sams:shape/gml:Curve/@srsDimension",
                doc);
    }

    // TODO: Reenable after GEOT-4519 is fixed.
    //    /**
    //     * Test filtering fabricated LineString.
    //     */
    //    @Test
    //    public void testFilter() throws Exception {
    //        String xml = "<wfs:GetFeature service=\"WFS\" " //
    //                + "version=\"1.1.0\" " //
    //                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
    //                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
    //                + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
    //                + "xmlns:gsmlbh=\"http://xmlns.geosciml.org/Borehole/3.2\" " //
    //                + "xmlns:sa=\"http://www.opengis.net/sampling/2.0\" " //
    //                + "xmlns:spec=\"http://www.opengis.net/samplingSpecimen/2.0\" " //
    //                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
    //                + "xsi:schemaLocation=\"" //
    //                + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd "
    // //
    //                + "http://xmlns.geosciml.org/Borehole/3.2
    // http://schemas.geosciml.org/borehole/3.2/borehole.xsd " //
    //                + "http://www.opengis.net/samplingSpecimen/2.0
    // http://schemas.opengis.net/samplingSpecimen/2.0/specimen.xsd" //
    //                + "\">"
    //                + "<wfs:Query typeName=\"gsmlbh:Borehole\">"
    //                + "    <ogc:Filter>"
    //                + "         <ogc:PropertyIsEqualTo>"
    //                + "            <ogc:Literal>85.3 89.6</ogc:Literal>"
    //                + "
    // <ogc:PropertyName>gsmlbh:logElement/gsmlbh:MappedInterval/gsml:shape/gml:LineString/gml:posList</ogc:PropertyName>"
    //                + "         </ogc:PropertyIsEqualTo>"
    //                + "    </ogc:Filter>"
    //                + "</wfs:Query> "
    //                + "</wfs:GetFeature>";
    //        validate(xml);
    //        Document doc = postAsDOM("wfs", xml);
    //        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
    //        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    //        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
    //        assertXpathCount(1, "//gsmlbh:Borehole", doc);
    //        assertXpathEvaluatesTo("borehole.GA.17338", "//gsmlbh:Borehole/@gml:id", doc);
    //    }
}
