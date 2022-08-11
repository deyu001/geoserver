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
import static org.junit.Assert.assertTrue;

import au.com.bytecode.opencsv.CSVReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test for SF0 CSV outputFormat in App-schema {@link BoreholeViewMockData}
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class CSVOutputFormatTest extends AbstractAppSchemaTestSupport {

    @Override
    protected BoreholeViewMockData createTestData() {
        return new BoreholeViewMockData();
    }

    /** Tests full request with CSV outputFormat. */
    @Test
    public void testFullRequest() throws Exception {

        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typename=gsmlp:BoreholeView&outputFormat=csv");

        // check the mime type
        assertEquals("text/csv", resp.getContentType());

        // check the content disposition
        assertEquals(
                "attachment; filename=BoreholeView.csv", resp.getHeader("Content-Disposition"));

        // read the response back with a parser that can handle escaping, newlines and what not
        List<String[]> lines = readLines(resp.getContentAsString());

        // we should have one header line and then all the features in that feature type
        assertEquals(3, lines.size());

        // check the header
        String[] header =
                new String[] {
                    "gml:id",
                    "gsmlp:identifier",
                    "gsmlp:name",
                    "gsmlp:drillingMethod",
                    "gsmlp:driller",
                    "gsmlp:drillStartDate",
                    "gsmlp:startPoint",
                    "gsmlp:inclinationType",
                    "gsmlp:boreholeMaterialCustodian",
                    "gsmlp:boreholeLength_m",
                    "gsmlp:elevation_m",
                    "gsmlp:elevation_srs",
                    "gsmlp:specification_uri",
                    "gsmlp:metadata_uri",
                    "gsmlp:shape"
                };

        assertTrue(Arrays.asList(lines.get(0)).containsAll(Arrays.asList(header)));

        // check each line has the expected number of elements (num of att + 1 for the id)
        int headerCount = lines.get(0).length;
        assertEquals(headerCount, lines.get(1).length);
        assertEquals(headerCount, lines.get(2).length);
    }

    /** Tests CSV outputFormat with filters. */
    @Test
    public void testFilter() throws Exception {
        String IDENTIFIER = "borehole.GA.17338";

        String xml =
                "<wfs:GetFeature service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gsmlp=\"http://xmlns.geosciml.org/geosciml-portrayal/2.0\" >" //
                        + "    <wfs:Query typeName=\"gsmlp:BoreholeView\" outputFormat=\"csv\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:PropertyIsEqualTo>" //
                        + "                <ogc:Literal>"
                        + IDENTIFIER
                        + "</ogc:Literal>" //
                        + "                <ogc:PropertyName>gsmlp:identifier</ogc:PropertyName>" //
                        + "            </ogc:PropertyIsEqualTo>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        MockHttpServletResponse resp =
                postAsServletResponse(
                        "wfs?service=WFS&request=GetFeature&version=1.1.0&typeName=gsmlp:BoreholeView&outputFormat=csv",
                        xml,
                        "text/csv");
        // check the mime type
        assertEquals("text/csv", resp.getContentType());
        // check the content disposition
        assertEquals(
                "attachment; filename=BoreholeView.csv", resp.getHeader("Content-Disposition"));

        // read the response back with a parser that can handle escaping, newlines and what not
        List<String[]> lines = readLines(resp.getContentAsString());

        // we should have one header line and then all the features in that feature type
        assertEquals(2, lines.size());

        int identifierIndex = Arrays.asList(lines.get(0)).indexOf("gsmlp:identifier");
        assertEquals(IDENTIFIER, lines.get(1)[identifierIndex]);

        // check the header
        String[] header =
                new String[] {
                    "gml:id",
                    "gsmlp:identifier",
                    "gsmlp:name",
                    "gsmlp:drillingMethod",
                    "gsmlp:driller",
                    "gsmlp:drillStartDate",
                    "gsmlp:startPoint",
                    "gsmlp:inclinationType",
                    "gsmlp:boreholeMaterialCustodian",
                    "gsmlp:boreholeLength_m",
                    "gsmlp:elevation_m",
                    "gsmlp:elevation_srs",
                    "gsmlp:specification_uri",
                    "gsmlp:metadata_uri",
                    "gsmlp:shape"
                };

        assertTrue(Arrays.asList(lines.get(0)).containsAll(Arrays.asList(header)));

        // check each line has the expected number of elements (num of att + 1 for the id)
        int headerCount = lines.get(0).length;
        assertEquals(headerCount, lines.get(1).length);
    }

    // TODO: requires a patch in WFS GetFeature.class
    //    /**
    //     * Tests CSV outputFormat with property selections.
    //     *
    //     */
    //    @Test
    //    public void testPropertyName() throws Exception {
    //        MockHttpServletResponse resp =
    // getAsServletResponse("wfs?service=WFS&version=1.1.0&request=GetFeature&typename=gsmlp:BoreholeView&outputFormat=csv&propertyName=gsmlp:identifier,gsmlp:name");
    //        // check the mime type
    //        assertEquals("text/csv", resp.getContentType());
    //
    //        // check the content disposition
    //        assertEquals("attachment; filename=BoreholeView.csv",
    // resp.getHeader("Content-Disposition"));
    //
    //        // read the response back with a parser that can handle escaping, newlines and what
    // not
    //        List<String[]> lines = readLines(resp.getOutputStreamContent());
    //
    //        // we should have one header line and then all the features in that feature type
    //        assertEquals(3, lines.size());
    //
    //        for (String[] line : lines) {
    //            // check each line has the expected number of elements (num of att + 1 for the id)
    //            assertEquals(3, line.length);
    //        }
    //
    //        // check the header
    //        String[] header = new String[] { "gml:id", "gsmlp:identifier", "gsmlp:name" };
    //        assertEquals(Arrays.toString(header), Arrays.toString(lines.get(0)));
    //
    //    }

    /**
     * Convenience to read the csv content . Copied from {@link
     * org.geoserver.wfs.response.CSVOutputFormatTest}
     */
    static List<String[]> readLines(String csvContent) throws IOException {
        CSVReader reader = new CSVReader(new StringReader(csvContent));

        List<String[]> result = new ArrayList<>();
        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            result.add(nextLine);
        }
        return result;
    }
}
