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

package org.geoserver.monitor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MonitorServletRequest extends HttpServletRequestWrapper {

    /** Don't restrict the maximum length of a request body. */
    public static final long BODY_SIZE_UNBOUNDED = -1;

    MonitorInputStream input;

    long maxSize;

    public MonitorServletRequest(HttpServletRequest request, long maxSize) {
        super(request);
        this.maxSize = maxSize;
    }

    public byte[] getBodyContent() throws IOException {
        @SuppressWarnings("PMD.CloseResource") // wraps the servlet one
        MonitorInputStream stream = getInputStream();
        return stream.getData();
    }

    public long getBytesRead() {
        try {
            @SuppressWarnings("PMD.CloseResource") // wraps the servlet one
            MonitorInputStream stream = getInputStream();
            return stream.getBytesRead();
        } catch (IOException ex) {
            return 0;
        }
    }

    @Override
    public MonitorInputStream getInputStream() throws IOException {
        if (input == null) {
            @SuppressWarnings("PMD.CloseResource") // managed by servlet container
            ServletInputStream delegateTo = super.getInputStream();
            input = new MonitorInputStream(delegateTo, maxSize);
        }
        return input;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        String encoding = getCharacterEncoding();
        if (encoding == null) {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        } else {
            return new BufferedReader(new InputStreamReader(getInputStream(), encoding));
        }
    }

    static class MonitorInputStream extends ServletInputStream {

        ByteArrayOutputStream buffer;

        ServletInputStream delegate;

        long nbytes = 0;

        long maxSize;

        public MonitorInputStream(ServletInputStream delegate, long maxSize) {
            this.delegate = delegate;
            this.maxSize = maxSize;
            if (maxSize > 0 || maxSize == BODY_SIZE_UNBOUNDED) {
                buffer = new ByteArrayOutputStream();
            }
        }

        public int available() throws IOException {
            return delegate.available();
        }

        public void close() throws IOException {
            delegate.close();
        }

        public void mark(int readlimit) {
            delegate.mark(readlimit);
        }

        public boolean markSupported() {
            return delegate.markSupported();
        }

        public void reset() throws IOException {
            delegate.reset();
        }

        public long skip(long n) throws IOException {
            nbytes += n;
            return delegate.skip(n);
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (!bufferIsFull()) {
                buffer.write((byte) b);
            }

            if (b >= 0) nbytes += 1; // Increment byte count unless EoF marker
            return b;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int n = delegate.read(b);
            fill(b, 0, n);

            nbytes += n;
            return n;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            fill(b, off, n);

            nbytes += n;
            return n;
        }

        @Override
        public int readLine(byte[] b, int off, int len) throws IOException {
            int n = delegate.readLine(b, off, len);
            fill(b, off, n);

            nbytes += n;
            return n;
        }

        void fill(byte[] b, int off, int len) {
            if (len < 0) return;
            if (!bufferIsFull()) {
                if (maxSize > 0) {
                    long residual = maxSize - buffer.size();
                    len = len < residual ? len : (int) residual;
                }
                buffer.write(b, off, len);
            }
        }

        boolean bufferIsFull() {
            return maxSize == 0
                    || (buffer.size() >= maxSize && maxSize > 0 && maxSize != BODY_SIZE_UNBOUNDED);
        }

        public byte[] getData() {
            return buffer == null ? new byte[0] : buffer.toByteArray();
        }

        public long getBytesRead() {
            return nbytes;
        }

        public void dispose() {
            buffer = null;
            delegate = null;
        }
    }
}
