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

package org.geoserver.wms.wms_1_1_1;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.geoserver.wms.WMSFilterMosaicTestSupport;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Class to test ImageMosaic cql filter
 *
 * @see {@link WMSFilterMosaicTestSupport}
 * @author carlo cancellieri
 */
@Ignore // not a single test passes, they were not annotated with @Test and were not running
public class FilterMosaicGetMapTest extends WMSFilterMosaicTestSupport {

    static final String layer = WATTEMP.getLocalPart();

    static final String BASE_URL =
            "wms?service=WMS&version=1.1.0"
                    + "&request=GetMap&layers="
                    + layer
                    + "&styles="
                    + "&bbox=0.237,40.562,14.593,44.558&width=200&height=80"
                    + "&srs=EPSG:4326&format=image/png";

    static final String MIME = "image/png";

    // specifying default filter
    static final String cql_filter = "elevation=100 AND ingestion=\'2008-10-31T00:00:00.000Z\'";

    @Test
    public void testAsCQL() throws Exception {
        // CASE 'MOSAIC WITH DEFAULT FILTERS'

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // get mosaic using the default filter
        BufferedImage image = getAsImage(BASE_URL, "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    @Test
    public void testCaseDefault() throws Exception {
        // CASE 'MOSAIC WITHOUT FILTERS'

        // disable the default filter
        super.setupMosaicFilter("", layer);

        // get mosaic without the default filter
        BufferedImage image = getAsImage(BASE_URL, "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
    }

    @Test
    public void testCaseElev100andIngestion31Oct() throws Exception {
        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=100 AND ingestion=\'2008-10-31T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    @Test
    public void testCaseElev100andIngestion01Nov() throws Exception {

        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=100 AND ingestion=\'2008-11-01T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
        assertPixel(image, 68, 72, new Color(246, 246, 255));
    }

    @Test
    public void testCaseElev0andIngestion31Oct() throws Exception {
        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=0 AND ingestion=\'2008-10-31T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
    }

    @Test
    public void testCaseElev0andIngestion01Nov() throws Exception {
        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=0 AND ingestion=\'2008-11-01T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        assertPixel(image, 36, 31, new Color(246, 246, 255));
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(255, 185, 185));
    }
}
