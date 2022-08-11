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

package org.geoserver.wps.ppio;

import java.awt.image.RenderedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Process parameter input / output for GridCoverage on a specific mime type. Current implementation
 * only supports PNG/JPEG encoding.
 */
public abstract class CoveragePPIO extends BinaryPPIO {

    private static float DEFAULT_QUALITY = 0.75f;

    private static final Logger LOGGER = Logging.getLogger(CoveragePPIO.class);

    protected CoveragePPIO(final String mimeType) {
        super(GridCoverage2D.class, GridCoverage2D.class, mimeType);
    }

    @Override
    public void encode(Object value, OutputStream outputStream) throws Exception {
        // Call default implementation with no params
        encode(value, null, outputStream);
    }

    private static float extractQuality(Map<String, Object> encodingParameters) {
        float quality = DEFAULT_QUALITY;
        if (encodingParameters != null
                && !encodingParameters.isEmpty()
                && encodingParameters.containsKey(QUALITY_KEY)) {
            String compressionQuality = (String) encodingParameters.get(QUALITY_KEY);
            try {
                quality = Float.parseFloat(compressionQuality);
            } catch (NumberFormatException nfe) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(
                            "Specified quality is not valid (it should be in the range [0,1])."
                                    + " quality = "
                                    + compressionQuality
                                    + "\nUsing default Quality: "
                                    + DEFAULT_QUALITY);
                }
            }
        }
        return quality;
    }

    /**
     * GridCoverage2D to PNG encoding PPIO. Note that we cannot decode a GridCoverage2D out of a
     * pure PNG Image. Report this by overriding the getDirection method and throwing an
     * UnsupportedOperationException on a decode call.
     */
    public static class PNGPPIO extends CoveragePPIO {

        public PNGPPIO() {
            super("image/png");
        }

        @Override
        public void encode(
                Object value, Map<String, Object> encodingParameters, OutputStream outputStream)
                throws Exception {
            GridCoverage2D gridCoverage = (GridCoverage2D) value;
            RenderedImage renderedImage = gridCoverage.getRenderedImage();
            ImageWorker worker = new ImageWorker(renderedImage);
            float quality = extractQuality(encodingParameters);
            worker.writePNG(outputStream, "FILTERED", quality, false, false);
        }

        @Override
        public String getFileExtension() {
            return "png";
        }

        @Override
        public PPIODirection getDirection() {
            return PPIODirection.ENCODING;
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            // ComplexPPIO requires overriding the decode method
            throw new UnsupportedOperationException();
        }
    }

    /**
     * GridCoverage2D to JPEG encoding PPIO. Note that we cannot decode a GridCoverage2D out of a
     * pure JPEG Image. Report this by overriding the getDirection method and throwing an
     * UnsupportedOperationException on a decode call.
     */
    public static class JPEGPPIO extends CoveragePPIO {

        public JPEGPPIO() {
            super("image/jpeg");
        }

        @Override
        public void encode(
                Object value, Map<String, Object> encodingParameters, OutputStream outputStream)
                throws Exception {
            GridCoverage2D gridCoverage = (GridCoverage2D) value;
            RenderedImage renderedImage = gridCoverage.getRenderedImage();
            ImageWorker worker = new ImageWorker(renderedImage);
            float quality = extractQuality(encodingParameters);
            worker.writeJPEG(outputStream, "JPEG", quality, false);
        }

        @Override
        public String getFileExtension() {
            return "jpeg";
        }

        @Override
        public PPIODirection getDirection() {
            return PPIODirection.ENCODING;
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            // ComplexPPIO requires overriding the decode method
            throw new UnsupportedOperationException();
        }
    }
}
