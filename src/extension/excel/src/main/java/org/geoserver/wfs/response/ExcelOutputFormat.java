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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Abstract base class for Excel WFS output format
 *
 * @author Sebastian Benthall, OpenGeo, seb@opengeo.org and Shane StClair, Axiom Consulting,
 *     shane@axiomalaska.com
 */
public abstract class ExcelOutputFormat extends WFSGetFeatureOutputFormat {

    protected static int CELL_CHAR_LIMIT = (int) Math.pow(2, 15) - 1; // 32,767

    protected static String TRUNCATE_WARNING = "DATA TRUNCATED";

    protected int rowLimit;

    protected int colLimit;

    protected String fileExtension;

    protected String mimeType;

    public ExcelOutputFormat(GeoServer gs, String formatName) {
        super(gs, formatName);
    }

    protected abstract Workbook getNewWorkbook();

    /** @return mime type; */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return mimeType;
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return fileExtension;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    /** @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation) */
    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {

        // Create the workbook
        try (Workbook wb = getNewWorkbook()) {
            CreationHelper helper = wb.getCreationHelper();
            ExcelCellStyles styles = new ExcelCellStyles(wb);

            for (org.geotools.feature.FeatureCollection collection :
                    featureCollection.getFeature()) {
                SimpleFeatureCollection fc = (SimpleFeatureCollection) collection;

                // create the sheet for this feature collection
                Sheet sheet = wb.createSheet(fc.getSchema().getTypeName());

                // write out the header
                Row header = sheet.createRow(0);

                SimpleFeatureType ft = fc.getSchema();
                Cell cell;

                cell = header.createCell(0);
                cell.setCellValue(helper.createRichTextString("FID"));
                for (int i = 0; i < ft.getAttributeCount() && i < colLimit; i++) {
                    AttributeDescriptor ad = ft.getDescriptor(i);
                    cell = header.createCell(i + 1);
                    cell.setCellValue(helper.createRichTextString(ad.getLocalName()));
                    cell.setCellStyle(styles.getHeaderStyle());
                }

                // write out the features
                try (SimpleFeatureIterator i = fc.features()) {
                    int r = 0; // row index
                    Row row;
                    while (i.hasNext()) {
                        r++; // start at 1, since header is at 0

                        row = sheet.createRow(r);
                        cell = row.createCell(0);

                        if (r == (rowLimit - 1) && i.hasNext()) {
                            // there are more features than rows available in this
                            // Excel format. write out a warning line and break
                            RichTextString rowWarning =
                                    helper.createRichTextString(
                                            TRUNCATE_WARNING
                                                    + ": ROWS "
                                                    + r
                                                    + " - "
                                                    + fc.size()
                                                    + " NOT SHOWN");
                            cell.setCellValue(rowWarning);
                            cell.setCellStyle(styles.getWarningStyle());
                            break;
                        }

                        SimpleFeature f = i.next();
                        cell.setCellValue(helper.createRichTextString(f.getID()));
                        for (int j = 0; j < f.getAttributeCount() && j < colLimit; j++) {
                            Object att = f.getAttribute(j);
                            if (att != null) {
                                cell = row.createCell(j + 1);
                                if (att instanceof Number) {
                                    cell.setCellValue(((Number) att).doubleValue());
                                } else if (att instanceof Date) {
                                    cell.setCellValue((Date) att);
                                    cell.setCellStyle(styles.getDateStyle());
                                } else if (att instanceof Calendar) {
                                    cell.setCellValue((Calendar) att);
                                    cell.setCellStyle(styles.getDateStyle());
                                } else if (att instanceof Boolean) {
                                    cell.setCellValue((Boolean) att);
                                } else {
                                    // ok, it seems we have no better way than dump it as a string
                                    String stringVal = att.toString();

                                    // if string length > excel cell limit, truncate it and warn the
                                    // user, otherwise excel workbook will be corrupted
                                    if (stringVal.length() > CELL_CHAR_LIMIT) {
                                        stringVal =
                                                TRUNCATE_WARNING
                                                        + " "
                                                        + stringVal.substring(
                                                                0,
                                                                CELL_CHAR_LIMIT
                                                                        - TRUNCATE_WARNING.length()
                                                                        - 1);
                                        cell.setCellStyle(styles.getWarningStyle());
                                    }
                                    cell.setCellValue(helper.createRichTextString(stringVal));
                                }
                            }
                        }
                    }
                }
            }

            // write to output
            wb.write(output);
        }
    }
}
