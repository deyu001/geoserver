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

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.DispatcherOutputStream;
import org.geoserver.ows.ServiceStrategy;
import org.vfny.geoserver.util.PartialBufferedOutputStream2;

/**
 * <b>PartialBufferStrategy</b><br>
 * Oct 19, 2005<br>
 * <b>Purpose:</b><br>
 * This strategy will buffer the response before it starts streaming it to the user. This will allow
 * for errors to be caught early so a proper error message can be sent to the user. Right now it
 * buffers the first 20KB, enough for a full getCapabilities document.
 *
 * @author Brent Owens (The Open Planning Project)
 * @version
 */
public class PartialBufferStrategy2 implements ServiceStrategy {
    /** Class logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    public static final int DEFAULT_BUFFER_SIZE = 50;
    private PartialBufferedOutputStream2 out = null;
    private int bufferSize;

    public String getId() {
        return "PARTIAL-BUFFER2";
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    private int bufferedSize() {
        if (bufferSize > 0) {
            return bufferSize;
        }

        return DEFAULT_BUFFER_SIZE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.vfny.geoserver.servlets.AbstractService.ServiceStrategy#getDestination(javax.servlet.http.HttpServletResponse)
     */
    public DispatcherOutputStream getDestination(HttpServletResponse response) throws IOException {
        out = new PartialBufferedOutputStream2(response, bufferedSize());

        return new DispatcherOutputStream(out);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.vfny.geoserver.servlets.AbstractService.ServiceStrategy#flush()
     */
    public void flush(HttpServletResponse response) throws IOException {
        if (out != null) {
            out.forceFlush();
            out = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.vfny.geoserver.servlets.AbstractService.ServiceStrategy#abort()
     */
    public void abort() {
        if (out != null) {
            try {
                if (out.abort()) {
                    LOGGER.info("OutputStream was successfully aborted.");
                } else {
                    LOGGER.warning(
                            "OutputStream could not be aborted in time. An error has occurred and could not be sent to the user.");
                }
            } catch (IOException e) {
                LOGGER.warning("Error aborting OutputStream");
                e.printStackTrace();
            }
        }
    }

    public Object clone() throws CloneNotSupportedException {
        PartialBufferStrategy2 clone = new PartialBufferStrategy2();
        clone.bufferSize = bufferSize;

        return clone;
    }
}
