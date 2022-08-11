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

package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RenderingTransformation;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public class GeorectifyCoverageTest extends WPSTestSupport {

    @Test
    public void testIsRenderingProcess() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Function f = ff.function("gs:GeorectifyCoverage");
        assertNotNull(f);
        assertTrue(f instanceof RenderingTransformation);
    }

    @Test
    public void testGeorectify()
            throws IOException, MismatchedDimensionException, NoSuchAuthorityCodeException,
                    FactoryException {
        GeorectifyCoverage process = applicationContext.getBean(GeorectifyCoverage.class);
        if (!process.isAvailable()) {
            LOGGER.warning("GDAL utilities are not in the path, skipping the test");
            return;
        }

        BufferedImage image = ImageIO.read(new File("./src/test/resources/rotated-image.png"));
        GridCoverage2D coverage =
                new GridCoverageFactory()
                        .create(
                                "test",
                                image,
                                new ReferencedEnvelope(
                                        0,
                                        image.getWidth(),
                                        0,
                                        image.getHeight(),
                                        CRS.decode("EPSG:404000")));
        String gcps =
                "["
                        + //
                        "[[183, 33], [-74.01183158, 40.70852996]],"
                        + //
                        "[[103, 114], [-74.01083751, 40.70754684]],"
                        + //
                        "[[459, 298], [-74.00857344, 40.71194565]],"
                        + //
                        "[[252, 139], [-74.01053024, 40.70938712]]"
                        + //
                        "]";
        Map<String, Object> map =
                process.execute(
                        coverage,
                        gcps,
                        null,
                        DefaultGeographicCRS.WGS84,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null);
        GridCoverage2D warped = (GridCoverage2D) map.get("result");
        assertEquals(CRS.decode("EPSG:4326", true), warped.getCoordinateReferenceSystem());
        // check the expected location, the output file also got verified visually
        Envelope2D envelope = warped.getEnvelope2D();
        assertEquals(-74.0122393, envelope.getMinX(), 1e-6);
        assertEquals(-74.0078822, envelope.getMaxX(), 1e-6);
        assertEquals(40.7062701, envelope.getMinY(), 1e-6);
        assertEquals(40.7126021, envelope.getMaxY(), 1e-6);
    }
}
