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

package org.geoserver.ows.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.xml.XMLConstants;
import org.geoserver.platform.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class NamespaceKvpParserTest {

    private NamespaceKvpParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new NamespaceKvpParser("namespace");
    }

    @Test
    public void testEmpty() throws Exception {
        NamespaceSupport ctx = parser.parse("");
        assertNotNull(ctx);
        List<String> prefixes = getPrefixes(ctx);
        assertTrue(prefixes.contains("xml")); // this one is always present
        assertEquals(1, prefixes.size());
    }

    @Test
    public void testFormatError() throws Exception {
        try {
            parser.parse("xmlns[bad=format]");
            fail("Expected IAE");
        } catch (ServiceException e) {
            assertProperServiceException(e);
        }

        try {
            parser.parse("xmlns(bad=http://format]");
            fail("Expected IAE");
        } catch (ServiceException e) {
            assertProperServiceException(e);
        }

        try {
            parser.parse("bad=http://format");
            fail("Expected IAE");
        } catch (ServiceException e) {
            assertProperServiceException(e);
        }
    }

    void assertProperServiceException(ServiceException e) {
        assertEquals(ServiceException.INVALID_PARAMETER_VALUE, e.getCode());
        assertEquals(parser.getKey(), e.getLocator());
    }

    @Test
    public void testSingle() throws Exception {
        NamespaceSupport ctx = parser.parse("xmlns(foo=http://bar)");
        assertEquals("http://bar", ctx.getURI("foo"));
    }

    @Test
    public void testMultiple() throws Exception {
        NamespaceSupport ctx =
                parser.parse(
                        "xmlns(foo=http://bar), xmlns(ex=http://example.com),xmlns(gs=http://geoserver.org)");
        assertEquals("http://bar", ctx.getURI("foo"));
        assertEquals("http://example.com", ctx.getURI("ex"));
        assertEquals("http://geoserver.org", ctx.getURI("gs"));
    }

    @Test
    public void testDefaultNamespace() throws Exception {
        NamespaceSupport ctx = parser.parse("xmlns(http://default.namespace.com)");
        assertEquals("http://default.namespace.com", ctx.getURI(XMLConstants.DEFAULT_NS_PREFIX));
    }

    @SuppressWarnings("unchecked")
    private List<String> getPrefixes(NamespaceSupport ctx) {
        Enumeration<String> prefixes = ctx.getPrefixes();
        List<String> l = new ArrayList<>();
        while (prefixes.hasMoreElements()) {
            l.add(prefixes.nextElement());
        }
        return l;
    }

    @Test
    public void testWfs20Syntax() throws Exception {
        NamespaceKvpParser parser = new NamespaceKvpParser("namespaces", true);
        NamespaceSupport ctx =
                parser.parse(
                        "xmlns(http://bar), xmlns(ex,http://example.com),xmlns(gs,http://geoserver.org)");
        assertEquals("http://bar", ctx.getURI(""));
        assertEquals("http://example.com", ctx.getURI("ex"));
        assertEquals("http://geoserver.org", ctx.getURI("gs"));
    }
}
