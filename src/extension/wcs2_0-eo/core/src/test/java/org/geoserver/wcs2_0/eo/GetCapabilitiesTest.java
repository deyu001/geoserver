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

package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.assertEquals;

import org.geoserver.wcs.WCSInfo;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetCapabilitiesTest extends WCSEOTestSupport {

    @Test
    public void testEOExtensions() throws Exception {
        Document dom = getAsDOM("wcs?request=GetCapabilities&version=2.0.1&service=WCS");
        // print(dom);

        // operations metadata checks
        assertEquals(
                "1", xpath.evaluate("count(//ows:Operation[@name='DescribeEOCoverageSet'])", dom));
        assertEquals("1", xpath.evaluate("count(//ows:Constraint[@name='CountDefault'])", dom));
        assertEquals(
                "20",
                xpath.evaluate("//ows:Constraint[@name='CountDefault']/ows:DefaultValue", dom));

        // dataset series checks
        assertEquals("4", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary)", dom));
        // check time ranges
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss'])",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/ows:WGS84BoundingBox)",
                        dom));
        assertEquals(
                "2008-10-31T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/gml:TimePeriod/gml:beginPosition",
                        dom));
        assertEquals(
                "2008-11-07T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__timeranges_dss']/gml:TimePeriod/gml:endPosition",
                        dom));
        // check water temp
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss'])",
                        dom));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/ows:WGS84BoundingBox)",
                        dom));
        assertEquals(
                "2008-10-31T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/gml:TimePeriod/gml:beginPosition",
                        dom));
        assertEquals(
                "2008-11-01T00:00:00.000Z",
                xpath.evaluate(
                        "//wcs:Extension/wcseo:DatasetSeriesSummary[wcseo:DatasetSeriesId='sf__watertemp_dss']/gml:TimePeriod/gml:endPosition",
                        dom));
    }

    @Test
    public void testDisableEOExtensions() throws Exception {
        // disable EO extensions
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(WCSEOMetadata.ENABLED.key, false);
        getGeoServer().save(wcs);

        Document dom = getAsDOM("wcs?request=GetCapabilities&version=2.0.1&service=WCS");

        assertEquals(
                "0", xpath.evaluate("count(//ows:Operation[@name='DescribeEOCoverageSet'])", dom));
        assertEquals("0", xpath.evaluate("count(//ows:Constraint[@name='CountDefault'])", dom));
        assertEquals("0", xpath.evaluate("count(//wcs:Extension/wcseo:DatasetSeriesSummary)", dom));
    }
}
