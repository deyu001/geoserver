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

package org.geoserver.wms.map;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

/** Allows encoding in JPEG or PNG depending on whether the image has transparency, or not */
public class JpegPngMapResponse extends RenderedImageMapResponse {

    public static final String MIME = "image/vnd.jpeg-png";
    public static final String MIME8 = "image/vnd.jpeg-png8";

    private static final String[] OUTPUT_FORMATS = {MIME, MIME8};

    private static MapProducerCapabilities CAPABILITIES =
            new MapProducerCapabilities(true, false, false, true, null);

    private PNGMapResponse pngResponse;

    private JPEGMapResponse jpegResponse;

    public JpegPngMapResponse(WMS wms, JPEGMapResponse jpegResponse, PNGMapResponse pngResponse) {
        super(OUTPUT_FORMATS, wms);
        this.jpegResponse = jpegResponse;
        this.pngResponse = pngResponse;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        RenderedImageMap map = ((RenderedImageMap) value);
        return JpegOrPngChooser.getFromMap(map).getMime();
    }

    /**
     * Transforms the rendered image into the appropriate format, streaming to the output stream.
     *
     * @see RasterMapOutputFormat#formatImageOutputStream(RenderedImage, OutputStream)
     */
    @Override
    public void formatImageOutputStream(
            RenderedImage image, OutputStream outStream, WMSMapContent mapContent)
            throws ServiceException, IOException {
        JpegOrPngChooser chooser = JpegOrPngChooser.getFromMapContent(image, mapContent);
        if (chooser.isJpegPreferred()) {
            jpegResponse.formatImageOutputStream(image, outStream, mapContent);
        } else {
            pngResponse.formatImageOutputStream(image, outStream, mapContent);
        }
    }

    @Override
    public MapProducerCapabilities getCapabilities(String outputFormat) {
        return CAPABILITIES;
    }

    @Override
    public String getExtension(RenderedImage image, WMSMapContent mapContent) {
        JpegOrPngChooser chooser = JpegOrPngChooser.getFromMapContent(image, mapContent);
        if (chooser.isJpegPreferred()) {
            return "jpg";
        } else {
            return "png";
        }
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        String fileName = ((WebMap) value).getAttachmentFileName();
        int idx = fileName.lastIndexOf(".");
        if (idx > 0) {
            return fileName.substring(0, idx)
                    + "."
                    + getExtension(
                            ((RenderedImageMap) value).getImage(),
                            ((RenderedImageMap) value).getMapContext());
        } else {
            return fileName;
        }
    }
}
