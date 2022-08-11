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

package org.geoserver.filters;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * A response stream that figures out whether or not to compress the output just before the first
 * write. The decision is based on the mimetype set for the output request.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class AlternativesResponseStream extends ServletOutputStream {
    HttpServletResponse myResponse;
    ServletOutputStream myStream;
    Set myCompressibleTypes;
    Logger logger = org.geotools.util.logging.Logging.getLogger("org.geoserver.filters");
    int contentLength;

    public AlternativesResponseStream(
            HttpServletResponse response, Set compressible, int contentLength) throws IOException {
        super();
        myResponse = response;
        myCompressibleTypes = compressible;
        this.contentLength = contentLength;
    }

    public void close() throws IOException {
        if (isDirty()) getStream().close();
    }

    public void flush() throws IOException {
        if (isDirty()) getStream().flush();
    }

    public void write(int b) throws IOException {
        getStream().write(b);
    }

    public void write(byte b[]) throws IOException {
        getStream().write(b);
    }

    public void write(byte b[], int off, int len) throws IOException {
        getStream().write(b, off, len);
    }

    protected ServletOutputStream getStream() throws IOException {
        if (myStream != null) return myStream;
        String type = myResponse.getContentType();

        //        if (type == null){
        //            logger.warning("Mime type was not set before first write!");
        //        }

        if (type != null && isCompressible(type)) {
            logger.log(Level.FINE, "Compressing output for mimetype: {0}", type);
            myResponse.addHeader("Content-Encoding", "gzip");
            myStream = new GZIPResponseStream(myResponse);
        } else {
            logger.log(Level.FINE, "Not compressing output for mimetype: {0}", type);
            if (contentLength >= 0) {
                myResponse.setContentLength(contentLength);
            }
            myStream = myResponse.getOutputStream();
        }

        return myStream;
    }

    protected boolean isDirty() {
        return myStream != null;
    }

    protected boolean isCompressible(String mimetype) {
        String stripped = stripParams(mimetype);

        Iterator it = myCompressibleTypes.iterator();

        while (it.hasNext()) {
            Pattern pat = (Pattern) it.next();
            Matcher matcher = pat.matcher(stripped);
            if (matcher.matches()) return true;
        }

        return false;
    }

    protected String stripParams(String mimetype) {
        int firstSemicolon = mimetype.indexOf(";");

        if (firstSemicolon != -1) {
            return mimetype.substring(0, firstSemicolon);
        }

        return mimetype;
    }
}
