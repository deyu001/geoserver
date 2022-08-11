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

package org.geoserver.wfs.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.mock.web.MockHttpServletResponse;

public class ExcelOutputFormatTest extends WFSTestSupport {
    @Test
    public void testExcel97OutputFormat() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.0.0&typeName=sf:PrimitiveGeoFeature&outputFormat=excel");
        try (InputStream in = getBinaryInputStream(resp)) {

            // check the mime type
            assertEquals("application/msexcel", resp.getContentType());

            // check the content disposition
            assertEquals(
                    "attachment; filename=PrimitiveGeoFeature.xls",
                    resp.getHeader("Content-Disposition"));

            try (HSSFWorkbook wb = new HSSFWorkbook(in)) {
                testExcelOutputFormat(wb);
            }
        }
    }

    @Test
    public void testExcel2007OutputFormat() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.0.0&typeName=sf:PrimitiveGeoFeature&outputFormat=excel2007");
        try (InputStream in = getBinaryInputStream(resp)) {

            // check the mime type
            assertEquals(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    resp.getContentType());

            // check the content disposition
            assertEquals(
                    "attachment; filename=PrimitiveGeoFeature.xlsx",
                    resp.getHeader("Content-Disposition"));

            try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
                testExcelOutputFormat(wb);
            }
        }
    }

    private void testExcelOutputFormat(Workbook wb) throws IOException {
        Sheet sheet = wb.getSheet("PrimitiveGeoFeature");
        assertNotNull(sheet);

        SimpleFeatureSource fs = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);

        // check the number of rows in the output
        final int feautureRows = fs.getCount(Query.ALL);
        assertEquals(feautureRows + 1, sheet.getPhysicalNumberOfRows());

        // check the header is what we expect
        final SimpleFeatureType schema = (SimpleFeatureType) fs.getSchema();
        final Row header = sheet.getRow(0);
        assertEquals("FID", header.getCell(0).getRichStringCellValue().toString());
        for (int i = 0; i < schema.getAttributeCount(); i++) {
            assertEquals(
                    schema.getDescriptor(i).getLocalName(),
                    header.getCell(i + 1).getRichStringCellValue().toString());
        }

        // check some selected values to see if the content and data type is the one
        // we expect
        SimpleFeature sf = DataUtilities.first(fs.getFeatures());

        // ... a string cell
        Cell cell = sheet.getRow(1).getCell(1);
        assertEquals(CellType.STRING, cell.getCellType());
        assertEquals(sf.getAttribute(0), cell.getRichStringCellValue().toString());
        // ... a geom cell
        cell = sheet.getRow(1).getCell(4);
        assertEquals(CellType.STRING, cell.getCellType());
        assertEquals(sf.getAttribute(3).toString(), cell.getRichStringCellValue().toString());
        // ... a number cell
        cell = sheet.getRow(1).getCell(6);
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(((Number) sf.getAttribute(5)).doubleValue(), cell.getNumericCellValue(), 0d);
        // ... a date cell (they are mapped as numeric in xms?)
        cell = sheet.getRow(1).getCell(10);
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(sf.getAttribute(9), cell.getDateCellValue());
        // ... a boolean cell (they are mapped as numeric in xms?)
        cell = sheet.getRow(1).getCell(12);
        assertEquals(CellType.BOOLEAN, cell.getCellType());
        assertEquals(sf.getAttribute(11), cell.getBooleanCellValue());
        // ... an empty cell (original value is null -> no cell)
        cell = sheet.getRow(1).getCell(3);
        assertNull(cell);
    }

    @Test
    public void testExcel97MultipleFeatureTypes() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&typeName=sf:PrimitiveGeoFeature,sf:GenericEntity&outputFormat=excel");
        try (InputStream in = getBinaryInputStream(resp);
                Workbook wb = new HSSFWorkbook(in)) {
            testMultipleFeatureTypes(wb);
        }
    }

    @Test
    public void testExcel2007MultipleFeatureTypes() throws Exception {
        // grab the real binary stream, avoiding mangling to due char conversion
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&typeName=sf:PrimitiveGeoFeature,sf:GenericEntity&outputFormat=excel2007");
        try (InputStream in = getBinaryInputStream(resp);
                Workbook wb = new XSSFWorkbook(in); ) {
            testMultipleFeatureTypes(wb);
        }
    }

    private void testMultipleFeatureTypes(Workbook wb) throws IOException {
        // check we have the expected sheets
        Sheet sheet = wb.getSheet("PrimitiveGeoFeature");
        assertNotNull(sheet);

        // check the number of rows in the output
        FeatureSource fs = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        assertEquals(fs.getCount(Query.ALL) + 1, sheet.getPhysicalNumberOfRows());

        sheet = wb.getSheet("GenericEntity");
        assertNotNull(sheet);

        // check the number of rows in the output
        fs = getFeatureSource(MockData.GENERICENTITY);
        assertEquals(fs.getCount(Query.ALL) + 1, sheet.getPhysicalNumberOfRows());
    }
}
