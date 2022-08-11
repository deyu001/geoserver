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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class FlushSafeFilterTest {

    @Test
    public void testRetrieveSameOutputStream() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        // make sure repeated calls to get output stream give us back the same
                        // output stream,
                        // e.g., that we're not creating a new wrapper each time
                        try (ServletOutputStream os1 = response.getOutputStream();
                                ServletOutputStream os2 = response.getOutputStream()) {
                            assertSame(os1, os2);
                            assertTrue(
                                    os1 instanceof FlushSafeResponse.FlushSafeServletOutputStream);
                        }
                    }
                };

        // run the filter
        FlushSafeFilter filter = new FlushSafeFilter();
        filter.init(new MockFilterConfig());
        filter.doFilter(request, response, chain);
    }

    @Test
    public void testFlushAfterClose() throws ServletException, IOException {
        // prepare request, response, and chain
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response =
                new MockHttpServletResponse() {

                    ServletOutputStream os;

                    @Override
                    @SuppressWarnings("PMD.CloseResource")
                    public ServletOutputStream getOutputStream() {
                        if (os == null) {
                            final ServletOutputStream wrapped = super.getOutputStream();
                            os =
                                    new ServletOutputStream() {
                                        boolean closed;

                                        @Override
                                        public void write(int b) throws IOException {
                                            wrapped.write(b);
                                        }

                                        @Override
                                        public void close() throws IOException {
                                            closed = true;
                                            wrapped.close();
                                        }

                                        @Override
                                        public void flush() throws IOException {
                                            if (closed) {
                                                // we should never reach this code
                                                throw new RuntimeException(
                                                        "Aaarg, I'm already closed, your JVM shall die now!");
                                            }
                                            wrapped.flush();
                                        }
                                    };
                        }

                        return os;
                    }
                };
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    @SuppressWarnings("PMD.CloseResource")
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        ServletOutputStream os = response.getOutputStream();
                        os.print("Some random text");
                        os.close();
                        // ka-blam! (or not?)
                        os.flush();
                    }
                };

        // run the filter
        FlushSafeFilter filter = new FlushSafeFilter();
        filter.init(new MockFilterConfig());
        filter.doFilter(request, response, chain);

        // if we got here without exception, it's already a good sign. Let's check the output
        assertEquals("Some random text", response.getContentAsString());
    }
}
