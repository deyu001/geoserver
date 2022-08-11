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

package org.vfny.geoserver.servlets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.DispatcherOutputStream;
import org.geoserver.ows.ServiceStrategy;

/**
 * A safe ServiceConfig strategy that uses a temporary file until writeTo completes.
 *
 * @author $author$
 * @version $Revision: 1.23 $
 */
public class FileStrategy implements ServiceStrategy {
    public String getId() {
        return "FILE";
    }

    /** Buffer size used to copy safe to response.getOutputStream() */
    private static int BUFF_SIZE = 4096;

    /** Temporary file number */
    static int sequence = 0;

    /** Class logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    /** OutputStream provided to writeTo method */
    private OutputStream safe;

    /** Temporary file used by safe */
    private File temp;

    /**
     * Provides a outputs stream on a temporary file.
     *
     * <p>I have changed this to use a BufferedWriter to agree with SpeedStrategy.
     *
     * @param response Response being handled
     * @return Outputstream for a temporary file
     * @throws IOException If temporary file could not be created.
     */
    public DispatcherOutputStream getDestination(HttpServletResponse response) throws IOException {
        // REVISIT: Should do more than sequence here
        // (In case we are running two GeoServers at once)
        // - Could we use response.getHandle() in the filename?
        // - ProcessID is traditional, I don't know how to find that in Java
        sequence++;

        // lets check for file permissions first so we can throw a clear error
        try {
            temp = File.createTempFile("wfs" + sequence, "tmp");

            if (!temp.canRead() || !temp.canWrite()) {
                String errorMsg =
                        "Temporary-file permission problem for location: " + temp.getPath();
                throw new IOException(errorMsg);
            }
        } catch (IOException e) {
            String errorMsg = "Possible file permission problem. Root cause: \n" + e.toString();
            IOException newE = new IOException(errorMsg);
            throw newE;
        }

        safe = new BufferedOutputStream(new FileOutputStream(temp));

        return new DispatcherOutputStream(safe);
    }

    /**
     * Closes safe output stream, copies resulting file to response.
     *
     * @throws IOException If temporay file or response is unavailable
     * @throws IllegalStateException if flush is called before getDestination
     */
    public void flush(HttpServletResponse response) throws IOException {
        if ((temp == null) || (response == null) || (safe == null) || !temp.exists()) {
            LOGGER.fine(
                    "temp is "
                            + temp
                            + ", response is "
                            + response
                            + " safe is "
                            + safe
                            + ", temp exists "
                            + (temp == null ? "false" : temp.exists()));
            throw new IllegalStateException("flush should only be called after getDestination");
        }

        InputStream copy = null;

        try {
            safe.flush();
            safe.close();
            safe = null;

            // service succeeded in producing a response!
            // copy result to the real output stream
            copy = new BufferedInputStream(new FileInputStream(temp));

            @SuppressWarnings("PMD.CloseResource") // managed by servlet container
            OutputStream out = response.getOutputStream();
            out = new BufferedOutputStream(out, 1024 * 1024);

            byte[] buffer = new byte[BUFF_SIZE];
            int b;

            while ((b = copy.read(buffer, 0, BUFF_SIZE)) > 0) {
                out.write(buffer, 0, b);
            }

            // Speed Writer closes output Stream
            // I would prefer to leave that up to doService...
            out.flush();

            // out.close();
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (copy != null) {
                try {
                    copy.close();
                } catch (Exception ex) {
                }
            }

            copy = null;

            if ((temp != null) && temp.exists()) {
                temp.delete();
            }

            temp = null;
            response = null;
            safe = null;
        }
    }

    /**
     * Clean up after writeTo fails.
     *
     * @see org.geoserver.ows.ServiceStrategy#abort()
     */
    public void abort() {
        if (safe != null) {
            try {
                safe.close();
            } catch (IOException ioException) {
            }

            safe = null;
        }

        if ((temp != null) && temp.exists()) {
            temp.delete();
        }

        temp = null;
    }

    public Object clone() throws CloneNotSupportedException {
        return new FileStrategy();
    }
}
