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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletInputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.monitor.MonitorServletRequest.MonitorInputStream;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;

public class MonitorServletRequestTest {
    static final String THE_REQUEST = "TheRequest";

    static final class SingleInputCallRequest extends MockHttpServletRequest {
        static final byte[] BUFFER = THE_REQUEST.getBytes();

        AtomicBoolean called = new AtomicBoolean(false);

        public javax.servlet.ServletInputStream getInputStream() {
            checkCalled();
            final ByteArrayInputStream bis = new ByteArrayInputStream(BUFFER);
            return new ServletInputStream() {

                @Override
                public int read() throws IOException {
                    return bis.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            checkCalled();
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(BUFFER)));
        }

        private void checkCalled() {
            if (called.get()) {
                fail("Input got retrieved twice");
            }
            called.set(true);
        }
    }

    @Test
    public void testInputStreamMaxSizeZero() throws Exception {
        byte[] data = data();
        DelegatingServletInputStream mock =
                new DelegatingServletInputStream(new ByteArrayInputStream(data));

        try (MonitorInputStream in = new MonitorInputStream(mock, 0)) {
            byte[] read = read(in);

            assertEquals(data.length, read.length);

            byte[] buffer = in.getData();
            assertEquals(0, buffer.length);

            // ? why does this report 1 off ?
            assertEquals(data.length - 1, in.getBytesRead());
        }
    }

    @Test
    public void testInputStream() throws Exception {
        byte[] data = data();
        DelegatingServletInputStream mock =
                new DelegatingServletInputStream(new ByteArrayInputStream(data));

        try (MonitorInputStream in = new MonitorInputStream(mock, 1024)) {
            byte[] read = read(in);

            assertEquals(data.length, read.length);

            byte[] buffer = in.getData();
            assertEquals(1024, buffer.length);

            for (int i = 0; i < buffer.length; i++) {
                assertEquals(data[i], buffer[i]);
            }

            // ? why does this report 1 off ?
            assertEquals(data.length - 1, in.getBytesRead());
        }
    }

    static byte[] data() throws IOException {
        try (InputStream in = MonitorServletRequest.class.getResourceAsStream("wms.xml")) {
            return read(in);
        }
    }

    static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;
        while ((n = in.read(buf)) > 0) {
            bytes.write(buf, 0, n);
        }

        in.close();
        return bytes.toByteArray();
    }

    @Test
    public void testGetReader() throws IOException {
        MockHttpServletRequest mock = new SingleInputCallRequest();

        MonitorServletRequest request = new MonitorServletRequest(mock, 1024);
        try (BufferedReader reader = request.getReader()) {
            assertEquals(THE_REQUEST, reader.readLine());
        }
        assertArrayEquals(THE_REQUEST.getBytes(), request.getBodyContent());
    }

    @Test
    public void testGetInputStream() throws IOException {
        MockHttpServletRequest mock = new SingleInputCallRequest();

        MonitorServletRequest request = new MonitorServletRequest(mock, 1024);
        try (InputStream is = request.getInputStream()) {
            assertEquals(THE_REQUEST, IOUtils.toString(is, "UTF-8"));
        }
        assertArrayEquals(THE_REQUEST.getBytes(), request.getBodyContent());
    }

    @Test
    public void testNPEIsNotThrownWithBufferSizeUnbounded() throws Exception {
        byte[] data = data();
        DelegatingServletInputStream mock =
                new DelegatingServletInputStream(new ByteArrayInputStream(data));

        try (MonitorInputStream in = new MonitorInputStream(mock, -1)) {
            byte[] read = read(in);

            assertEquals(data.length, read.length);

            byte[] buffer = in.getData();

            for (int i = 0; i < buffer.length; i++) {
                assertEquals(data[i], buffer[i]);
            }

            assertEquals(data.length - 1, in.getBytesRead());
        }
    }
}
