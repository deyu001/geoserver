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

package org.geoserver.wps.gs.download;

import it.geosolutions.imageio.stream.output.FilterImageOutputStream;
import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;

/**
 * An image output stream, which limits its data size. This stream is used if the content length is
 * unknown.
 *
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
abstract class LimitedImageOutputStream extends FilterImageOutputStream {
    /** The maximum size of an item, in bytes. */
    private long sizeMax;

    /** The current number of bytes. */
    private long count;

    /** Whether this stream is already closed. */
    private boolean closed;

    /**
     * Creates a new instance.
     *
     * @param pOut The input stream, which shall be limited.
     * @param pSizeMax The limit; no more than this number of bytes shall be returned by the source
     *     stream.
     */
    public LimitedImageOutputStream(ImageOutputStream pOut, long pSizeMax) {
        super(pOut);
        sizeMax = pSizeMax;
    }

    /**
     * Called to indicate, that the input streams limit has been exceeded.
     *
     * @param pSizeMax The input streams limit, in bytes.
     * @param pCount The actual number of bytes.
     * @throws IOException The called method is expected to raise an IOException.
     */
    protected abstract void raiseError(long pSizeMax, long pCount) throws IOException;

    /**
     * Called to check, whether the input streams limit is reached.
     *
     * @throws IOException The given limit is exceeded.
     */
    private void checkLimit() throws IOException {
        if (count > sizeMax) {
            raiseError(sizeMax, count);
        }
    }

    /**
     * Returns, whether this stream is already closed.
     *
     * @return True, if the stream is closed, otherwise false.
     * @throws IOException An I/O error occurred.
     */
    public boolean isClosed() throws IOException {
        return closed;
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream. This
     * method simply performs <code>in.close()</code>.
     *
     * @throws IOException if an I/O error occurs.
     * @see java.io.FilterInputStream#in
     */
    public void close() throws IOException {
        if (!isClosed()) {
            super.close();
            closed = true;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        count += len;
        checkLimit();
        super.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        count++;
        checkLimit();
        super.write(b);
    }
}
