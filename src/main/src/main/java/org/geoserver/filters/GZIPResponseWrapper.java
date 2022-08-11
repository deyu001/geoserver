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

/* This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Modified by David Winslow <dwinslow@openplans.org> on 2007-12-13.
 */
package org.geoserver.filters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class GZIPResponseWrapper extends HttpServletResponseWrapper {
    protected HttpServletResponse origResponse = null;
    protected AlternativesResponseStream stream = null;
    protected PrintWriter writer = null;
    protected Set formatsToCompress;
    protected String requestedURL;
    protected Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.filters");
    private int contentLength = -1;

    public GZIPResponseWrapper(HttpServletResponse response, Set toCompress, String url) {
        super(response);
        requestedURL = url;
        origResponse = response;
        // TODO: allow user-configured format list here
        formatsToCompress = toCompress;
    }

    protected AlternativesResponseStream createOutputStream() throws IOException {
        return new AlternativesResponseStream(origResponse, formatsToCompress, contentLength);
    }

    /**
     * The default behavior of this method is to return setHeader(String name, String value) on the
     * wrapped response object.
     */
    public void setHeader(String name, String value) {
        if (name.equalsIgnoreCase("Content-Length")) {
            try {
                contentLength = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                super.setHeader(name, value);
            }
        } else {
            super.setHeader(name, value);
        }
    }

    /**
     * The default behavior of this method is to return addHeader(String name, String value) on the
     * wrapped response object.
     */
    public void addHeader(String name, String value) {
        if (name.equalsIgnoreCase("Content-Length")) {
            try {
                contentLength = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                super.addHeader(name, value);
            }
        } else {
            super.addHeader(name, value);
        }
    }

    /**
     * The default behavior of this method is to call setIntHeader(String name, int value) on the
     * wrapped response object.
     */
    public void setIntHeader(String name, int value) {
        if (name.equalsIgnoreCase("Content-Length")) {
            contentLength = value;
        } else {
            super.setIntHeader(name, value);
        }
    }

    /**
     * The default behavior of this method is to call addIntHeader(String name, int value) on the
     * wrapped response object.
     */
    public void addIntHeader(String name, int value) {
        if (name.equalsIgnoreCase("Content-Length")) {
            contentLength = value;
        } else {
            super.addIntHeader(name, value);
        }
    }

    public void setContentType(String type) {
        //        if (stream != null && stream.isDirty()){
        //            logger.warning("Setting mimetype after acquiring stream! was:" +
        //                    getContentType() + "; set to: " + type + "; url was: " +
        // requestedURL);
        //        }
        origResponse.setContentType(type);
    }

    public void finishResponse() {
        try {
            if (writer != null) {
                writer.close();
            } else {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {
        }
    }

    public void flushBuffer() throws IOException {
        if (stream != null) {
            // Try to make sure Content-Encoding header gets set.
            stream.getStream();
        }
        getResponse().flushBuffer();
        if (writer != null) {
            writer.flush();
        } else if (stream != null) {
            stream.flush();
        }
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called!");
        }

        if (stream == null) stream = createOutputStream();
        return (stream);
    }

    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return (writer);
        }

        if (stream != null) {
            throw new IllegalStateException("getOutputStream() has already been called!");
        }

        stream = createOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
        return (writer);
    }

    public void setContentLength(int length) {
        this.contentLength = length;
    }
}
