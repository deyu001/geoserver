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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.WebUtils;

public class BufferedRequestWrapperTest extends RequestWrapperTestSupport {

    @Test
    public void testGetInputStream() throws Exception {
        for (String testString : testStrings) {
            doInputStreamTest(testString);
        }
    }

    @Test
    public void testGetReader() throws Exception {
        for (String testString : testStrings) {
            doGetReaderTest(testString);
        }
    }

    @SuppressWarnings("PMD.EmptyWhileStmt")
    public void doInputStreamTest(String testString) throws Exception {
        HttpServletRequest req = makeRequest(testString, null);

        BufferedRequestWrapper wrapper =
                new BufferedRequestWrapper(
                        req, WebUtils.DEFAULT_CHARACTER_ENCODING, testString.getBytes());
        byte b[] = new byte[32];
        try (ServletInputStream sis = req.getInputStream()) {
            /* clear out the request body */
            while ((sis.readLine(b, 0, 32)) > 0) ;
        }

        try (ServletInputStream sis = wrapper.getInputStream()) {
            StringBuffer buff = new StringBuffer();
            int amountRead;
            while ((amountRead = sis.readLine(b, 0, 32)) != 0) {
                buff.append(new String(b, 0, amountRead));
            }

            assertEquals(buff.toString(), testString);
        }
    }

    public void doGetReaderTest(String testString) throws Exception {
        HttpServletRequest req = makeRequest(testString, null);

        clearOutBody(req);

        BufferedRequestWrapper wrapper =
                new BufferedRequestWrapper(
                        req, WebUtils.DEFAULT_CHARACTER_ENCODING, testString.getBytes());
        StringBuffer buff = new StringBuffer();
        int c;
        try (BufferedReader br = wrapper.getReader()) {
            while ((c = br.read()) != -1) {
                buff.append((char) c);
            }

            assertEquals(buff.toString(), testString);
        }
    }

    @Test
    public void testMixedRequest() throws Exception {
        String body = "a=1&b=2";
        String queryString = "c=3&d=4";
        HttpServletRequest req = makeRequest(body, queryString);

        clearOutBody(req);

        BufferedRequestWrapper wrapper = new BufferedRequestWrapper(req, "UTF-8", body.getBytes());
        Map params = wrapper.getParameterMap();
        assertEquals(4, params.size());
        assertEquals("1", ((String[]) params.get("a"))[0]);
        assertEquals("2", ((String[]) params.get("b"))[0]);
        assertEquals("3", ((String[]) params.get("c"))[0]);
        assertEquals("4", ((String[]) params.get("d"))[0]);
    }

    @SuppressWarnings("PMD.EmptyWhileStmt")
    private void clearOutBody(HttpServletRequest req) throws IOException {
        try (BufferedReader br = req.getReader()) {
            /* clear out the body */
            while ((br.readLine()) != null) ;
        }
    }

    @Test
    public void testNoContentType() throws Exception {
        String body = "a=1&b=2";
        String queryString = "c=3&d=4";
        MockHttpServletRequest req = makeRequest(body, queryString);
        // reset the content type
        req.setContentType(null);
        clearOutBody(req);

        // should not NPE like it did
        BufferedRequestWrapper wrapper = new BufferedRequestWrapper(req, "UTF-8", body.getBytes());
        Map params = wrapper.getParameterMap();
        assertEquals(0, params.size());
    }

    @Test
    public void testEmptyPost() throws Exception {
        MockHttpServletRequest req = makeRequest("", "");
        // reset the content type
        req.setContentType(null);

        clearOutBody(req);

        // should not NPE like it did
        BufferedRequestWrapper wrapper = new BufferedRequestWrapper(req, "UTF-8", "".getBytes());
        Map params = wrapper.getParameterMap();
        assertEquals(0, params.size());
    }
}
