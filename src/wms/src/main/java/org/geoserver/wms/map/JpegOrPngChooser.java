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

import java.awt.RenderingHints;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ExtremaDescriptor;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.util.ImageUtilities;

/**
 * A support object attaching itself to the WebMapContent and deciding which format should be used
 * between jpeg and png when using the image/vnd.jpeg-png image format. This is not done in the
 * renderer because when meta-tiling we want to defer the decision about the format to the point in
 * which the single tiles are encoded
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JpegOrPngChooser {

    /** The key used to store the chooser in the map content metadata map */
    static final String JPEG_PNG_CHOOSER = "jpegOrPngChooser";

    public static JpegOrPngChooser getFromMap(RenderedImageMap map) {
        WMSMapContent ctx = map.getMapContext();
        return getFromMapContent(map.getImage(), ctx);
    }

    /** Returns the chooser from the map content, eventually creating it if missing */
    public static JpegOrPngChooser getFromMapContent(RenderedImage image, WMSMapContent ctx) {
        JpegOrPngChooser chooser = (JpegOrPngChooser) ctx.getMetadata().get(JPEG_PNG_CHOOSER);
        if (chooser == null) {
            chooser = new JpegOrPngChooser(image);
            ctx.getMetadata().put(JPEG_PNG_CHOOSER, chooser);
        }
        return chooser;
    }

    boolean jpegPreferred;

    public JpegOrPngChooser(RenderedImage image) {
        this.jpegPreferred = isBestFormatJpeg(image);
    }

    /**
     * Returns the full mime type of the chosen format (<code>image/jpeg</code> or <code>image/png
     * </code>)
     */
    public String getMime() {
        final String mime = jpegPreferred ? "image/jpeg" : "image/png";
        return mime;
    }

    /** Returns the extension of the chosen format (<code>jpeg</code> or <code>png</code>) */
    public String getExtension() {
        String extension = jpegPreferred ? "jpeg" : "png";
        return extension;
    }

    /**
     * Returns true if the best format to encode the image is jpeg (the image is rgb, or rgba
     * without any actual transparency use)
     */
    private boolean isBestFormatJpeg(RenderedImage renderedImage) {
        int numBands = renderedImage.getSampleModel().getNumBands();
        if (numBands == 4 || numBands == 2) {
            RenderingHints renderingHints = ImageUtilities.getRenderingHints(renderedImage);
            RenderedOp extremaOp =
                    ExtremaDescriptor.create(renderedImage, null, 1, 1, false, 1, renderingHints);
            double[][] extrema = (double[][]) extremaOp.getProperty("Extrema");
            double[] mins = extrema[0];

            return mins[mins.length - 1] == 255; // fully opaque
        } else if (renderedImage.getColorModel() instanceof IndexColorModel) {
            // JPEG would still compress a bit better, but in order to figure out
            // if the image has transparency we'd have to expand to RGB or roll
            // a new JAI image op that looks for the transparent pixels. Out of scope for the moment
            return false;
        } else {
            // otherwise support RGB or gray
            return (numBands == 3) || (numBands == 1);
        }
    }

    /** Returns true if the JPEG format was the preferred one */
    public boolean isJpegPreferred() {
        return jpegPreferred;
    }
}
