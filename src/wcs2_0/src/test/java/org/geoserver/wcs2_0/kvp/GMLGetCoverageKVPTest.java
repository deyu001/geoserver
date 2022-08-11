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

import java.io.ByteArrayInputStream;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.response.GMLCoverageResponseDelegate;
import org.geotools.util.NumberRange;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Testing {@link GMLCoverageResponseDelegate}
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GMLGetCoverageKVPTest extends WCSTestSupport {

    private static final double DELTA = 1E-6;

    @Test
    public void gmlFormat() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&format=application%2Fgml%2Bxml");

        assertEquals("application/gml+xml", response.getContentType());
        // checks it's xml....
        dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
    }

    @Test
    public void gmlFormatCoverageBandDetails() throws Exception {
        Catalog catalog = getCatalog();

        CoverageInfo c = catalog.getCoverageByName("wcs", "BlueMarble");
        List<CoverageDimensionInfo> dimensions = c.getDimensions();
        CoverageDimensionInfo dimension = dimensions.get(0);
        assertEquals("RED_BAND", dimension.getName());
        NumberRange range = dimension.getRange();
        assertEquals(Double.NEGATIVE_INFINITY, range.getMinimum(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, range.getMaximum(), DELTA);
        assertEquals("GridSampleDimension[-Infinity,Infinity]", dimension.getDescription());
        List<Double> nullValues = dimension.getNullValues();
        assertEquals(0, nullValues.size());
        assertEquals("W.m-2.Sr-1", dimension.getUnit());

        int i = 1;
        for (CoverageDimensionInfo dimensionInfo : dimensions) {
            // Updating dimension properties
            dimensionInfo.getNullValues().add(-999d);
            dimensionInfo.setDescription("GridSampleDimension[-100.0,1000.0]");
            dimensionInfo.setUnit("m");
            dimensionInfo.setRange(NumberRange.create(-100, 1000));
            dimensionInfo.setName("Band" + (i++));
        }
        catalog.save(c);

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?request=GetCoverage&service=WCS&version=2.0.1"
                                + "&coverageId=wcs__BlueMarble&format=application%2Fgml%2Bxml");
        Document dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        String name = xpath.evaluate("//swe:field/@name", dom);
        assertEquals("Band1", name);
        String interval = xpath.evaluate("//swe:interval", dom);
        assertEquals("-100 1000", interval);
        String unit = xpath.evaluate("//swe:uom/@code", dom);
        assertEquals("m", unit);
        String noData = xpath.evaluate("//swe:nilValue", dom);
        assertEquals("-999.0", noData);
    }
}
