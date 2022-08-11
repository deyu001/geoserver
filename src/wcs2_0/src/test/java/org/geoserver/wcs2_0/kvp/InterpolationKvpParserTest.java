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

package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.opengis.wcs20.InterpolationAxisType;
import net.opengis.wcs20.InterpolationType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InterpolationKvpParserTest extends GeoServerSystemTestSupport {

    InterpolationKvpParser parser = new InterpolationKvpParser();

    private String axisPrefix;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{""}, {"http://www.opengis.net/def/axis/OGC/1/"}});
    }

    public InterpolationKvpParserTest(String axisPrefix) {
        this.axisPrefix = axisPrefix;
    }

    @Test
    public void testInvalidValues() throws Exception {
        try {
            parser.parse(":interpolation");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("a:linear,,b:nearest");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("a::linear");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }
    }

    private void checkInvalidSyntaxException(OWS20Exception e) {
        assertNotNull(e.getHttpCode());
        assertEquals(400, e.getHttpCode().intValue());
        assertEquals("InvalidEncodingSyntax", e.getCode());
        assertEquals("interpolation", e.getLocator());
    }

    @Test
    public void testUniformValue() throws Exception {
        InterpolationType it =
                (InterpolationType)
                        parser.parse("http://www.opengis.net/def/interpolation/OGC/1/linear");
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                it.getInterpolationMethod().getInterpolationMethod());
    }

    @Test
    public void testSingleAxis() throws Exception {
        InterpolationType it =
                (InterpolationType)
                        parser.parse(
                                axisPrefix
                                        + "latitude:http://www.opengis.net/def/interpolation/OGC/1/linear");
        EList<InterpolationAxisType> axes = it.getInterpolationAxes().getInterpolationAxis();
        assertEquals(1, axes.size());
        assertEquals(axisPrefix + "latitude", axes.get(0).getAxis());
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                axes.get(0).getInterpolationMethod());
    }

    @Test
    public void testMultiAxis() throws Exception {
        InterpolationType it =
                (InterpolationType)
                        parser.parse(
                                axisPrefix
                                        + "latitude:"
                                        + "http://www.opengis.net/def/interpolation/OGC/1/linear,"
                                        + axisPrefix
                                        + "longitude:"
                                        + "http://www.opengis.net/def/interpolation/OGC/1/nearest");
        EList<InterpolationAxisType> axes = it.getInterpolationAxes().getInterpolationAxis();
        assertEquals(2, axes.size());
        assertEquals(axisPrefix + "latitude", axes.get(0).getAxis());
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/linear",
                axes.get(0).getInterpolationMethod());
        assertEquals(axisPrefix + "longitude", axes.get(1).getAxis());
        assertEquals(
                "http://www.opengis.net/def/interpolation/OGC/1/nearest",
                axes.get(1).getInterpolationMethod());
    }

    @Test
    public void testParserForVersion() throws Exception {
        // look up parser objects
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);
        KvpParser parser = KvpUtils.findParser("interpolation", "WCS", null, "2.0.0", parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), InterpolationKvpParser.class);
        // Version 2.0.1
        parser = KvpUtils.findParser("interpolation", "WCS", null, "2.0.1", parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), InterpolationKvpParser.class);
    }
}
