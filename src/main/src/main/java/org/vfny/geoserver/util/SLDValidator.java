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

/*
 * Created on April 20, 2005
 *
 */
package org.vfny.geoserver.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class SLDValidator {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver");

    EntityResolver entityResolver;

    public SLDValidator() {}

    /** Validates against the SLD schema in the classpath */
    public List<SAXException> validateSLD(InputStream xml) {
        return validateSLD(new InputSource(xml));
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public static String getErrorMessage(InputStream xml, List<? extends Exception> errors) {
        return getErrorMessage(new InputStreamReader(xml), errors);
    }

    /**
     * returns a better formated error message - suitable for framing. There's a more complex
     * version in StylesEditorAction. This will kick out a VERY LARGE errorMessage.
     */
    public static String getErrorMessage(Reader xml, List<? extends Exception> errors) {
        BufferedReader reader = null;
        StringBuffer result = new StringBuffer();
        result.append("Your SLD is not valid.\n");
        result.append(
                "Most common problems are: \n(1) no namespaces - use <ows:GetMap>, <sld:Rule>, <ogc:Filter>, <gml:Point>  - the part before the ':' is important\n");
        result.append("(2) capitialization - use '<And>' not '<and>' \n");
        result.append("(3) Order - The order of elements is important \n");
        result.append(
                "(4) Make sure your first tag imports the correct namespaces.  ie. xmlns:sld=\"http://www.opengis.net/sld\" for EVERY NAMESPACE \n");
        result.append("\n");

        try {
            reader = new BufferedReader(xml);

            String line = reader.readLine();
            int linenumber = 1;
            int exceptionNum = 0;

            // check for lineNumber -1 errors  --> invalid XML
            if (!errors.isEmpty() && errors.get(0) instanceof SAXParseException) {
                SAXParseException sax = (SAXParseException) errors.get(0);

                if (sax.getLineNumber() < 0) {
                    result.append("   INVALID XML: " + sax.getLocalizedMessage() + "\n");
                    result.append(" \n");
                    exceptionNum = 1; // skip ahead (you only ever get one error in this case)
                }
            }

            while (line != null) {
                line = line.replace('\n', ' ');
                line = line.replace('\r', ' ');

                String header = linenumber + ": ";
                result.append(header + line + "\n"); // record the current line

                boolean keep_going = true;

                while (keep_going) {
                    if ((exceptionNum < errors.size())
                            && errors.get(exceptionNum) instanceof SAXParseException) {
                        SAXParseException sax = (SAXParseException) errors.get(exceptionNum);

                        if (sax.getLineNumber() <= linenumber) {
                            String head = "---------------------".substring(0, header.length() - 1);
                            String body =
                                    "--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------";

                            int colNum = sax.getColumnNumber(); // protect against col 0 problems

                            if (colNum < 1) {
                                colNum = 1;
                            }

                            if (colNum > body.length()) {
                                body =
                                        body + body + body + body + body
                                                + body; // make it longer (not usually required, but
                                // might be for SLD_BODY=... which is all
                                // one line)

                                if (colNum > body.length()) {
                                    colNum = body.length();
                                }
                            }

                            result.append(head + body.substring(0, colNum - 1) + "^\n");
                            result.append(
                                    "       (line "
                                            + sax.getLineNumber()
                                            + ", column "
                                            + sax.getColumnNumber()
                                            + ")"
                                            + sax.getLocalizedMessage()
                                            + "\n");
                            exceptionNum++;
                        } else {
                            keep_going = false; // report later (sax.getLineNumber() > linenumber)
                        }
                    } else {
                        keep_going = false; // no more errors to report
                    }
                }

                line = reader.readLine(); // will be null at eof
                linenumber++;
            }

            for (int t = exceptionNum; t < errors.size(); t++) {
                SAXParseException sax = (SAXParseException) errors.get(t);
                result.append(
                        "       (line "
                                + sax.getLineNumber()
                                + ", column "
                                + sax.getColumnNumber()
                                + ")"
                                + sax.getLocalizedMessage()
                                + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }

    /**
     * validate a .sld against the schema
     *
     * @param xml input stream representing the .sld file
     * @return list of SAXExceptions (0 if the file's okay)
     */
    public List<SAXException> validateSLD(InputSource xml) {
        URL schemaURL = SLDValidator.class.getResource("/schemas/sld/StyledLayerDescriptor.xsd");
        return ResponseUtils.validate(xml, schemaURL, false, entityResolver);
    }
}
