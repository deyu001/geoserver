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

package org.geoserver.monitor.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.feature.type.DateUtil;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Convert MonitorResutls to a csv file. */
@Component
public class CSVMonitorConverter extends BaseMonitorConverter {

    static Pattern ESCAPE_REQUIRED = Pattern.compile("[\\,\\s\"]");

    private static final class CSVRequestDataVisitor implements RequestDataVisitor {
        private BufferedWriter writer;
        private String[] fields;

        CSVRequestDataVisitor(BufferedWriter writer, String fields[]) {
            this.writer = writer;
            this.fields = fields;
        }

        @Override
        public void visit(RequestData data, Object... aggregates) {
            try {
                StringBuffer sb = new StringBuffer();

                for (String fld : fields) {
                    Object val = OwsUtils.get(data, fld);
                    if (val instanceof Date) {
                        val = DateUtil.serializeDateTime((Date) val);
                    }
                    if (val != null) {
                        String string = val.toString();
                        Matcher match = ESCAPE_REQUIRED.matcher(string);
                        if (match.find()) { // may need escaping, so escape
                            string =
                                    string.replaceAll(
                                            "\"", "\"\""); // Double all double quotes to escape
                            sb.append("\"");
                            sb.append(string);
                            sb.append("\"");
                        } else { // No need for escaping
                            sb.append(string);
                        }
                    }
                    sb.append(",");
                }
                sb.setLength(sb.length() - 1); // Remove trailing comma
                sb.append("\n");
                writer.write(sb.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public CSVMonitorConverter() {
        super(MonitorRequestController.CSV_MEDIATYPE);
    }

    @Override
    protected void writeInternal(MonitorQueryResults results, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Object result = results.getResult();
        String[] fields = results.getFields();
        Monitor monitor = results.getMonitor();

        @SuppressWarnings("PMD.CloseResource") // managed by servlet container
        OutputStream os = outputMessage.getBody();
        writeCSVfile(result, fields, monitor, os);
    }

    /**
     * Write CSV file (also called by {@link ZIPMonitorConverter}
     *
     * @param result Query, List or individual RequestData)
     * @param monitor used to cancel output process
     * @param os Output stream (not closed by this method allowing use of zipfile)
     */
    void writeCSVfile(Object result, String[] fields, Monitor monitor, OutputStream os)
            throws IOException {
        final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));

        // write out the header
        StringBuffer sb = new StringBuffer();
        for (String fld : fields) {
            sb.append(fld).append(",");
        }
        sb.setLength(sb.length() - 1);
        w.write(sb.append("\n").toString());

        handleRequests(result, new CSVRequestDataVisitor(w, fields), monitor);

        w.flush();
    }
}
