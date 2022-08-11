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

package org.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.image.WorldImageWriter;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Encodes coverages in "world image" formats, png, jpeg and gif.
 *
 * <p>Notice that depending on the underlying coverage structure this is not always possible.
 *
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class IMGCoverageResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate {

    @SuppressWarnings("serial")
    public IMGCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList("png", "jpeg", "JPEG", "PNG"), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put("png", "png");
                        put("jpeg", "jpeg");
                        put("JPEG", "jpeg");
                        put("PNG", "png");
                        put("image/png", "png");
                        put("image/jpeg", "jpeg");
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put("png", "image/png");
                        put("jpeg", "image/jpeg");
                        put("PNG", "image/png");
                        put("JPEG", "image/jpeg");
                    }
                });
    }

    @SuppressWarnings("PMD.UseTryWithResources")
    public void encode(
            GridCoverage2D sourceCoverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException(
                    "It seems prepare() has not been called or has not succeed");
        }

        final WorldImageWriter writer = new WorldImageWriter(output);

        // writing parameters for Image
        final Format writerParams = writer.getFormat();
        final ParameterValueGroup writeParameters = writerParams.getWriteParameters();
        final ParameterValue<?> format = writeParameters.parameter("Format");
        format.setValue(getFileExtension(outputFormat));

        try {
            // writing
            writer.write(sourceCoverage, new GeneralParameterValue[] {format});
            output.flush();
        } finally {

            // freeing everything
            IOUtils.closeQuietly(output);

            try {
                writer.dispose();
            } catch (Throwable e) {
                // eat me
            }
            sourceCoverage.dispose(true);
        }
    }
}
