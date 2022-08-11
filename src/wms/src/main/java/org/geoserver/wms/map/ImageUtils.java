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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.image.ImageWorker;
import org.geotools.image.palette.CustomPaletteBuilder;
import org.geotools.image.palette.InverseColorMapOp;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;

/**
 * Provides utility methods for the shared handling of images by the raster map and legend
 * producers.
 *
 * @author Gabriel Roldan
 * @author Simone Giannecchini, GeoSolutions S.A.S.
 * @version $Id$
 */
public class ImageUtils {
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map");
    /**
     * This variable is use for testing purposes in order to force this {@link GridCoverageRenderer}
     * to dump images at various steps on the disk.
     */
    private static boolean DEBUG =
            Boolean.valueOf(
                    GeoServerExtensions.getProperty("org.geoserver.wms.map.ImageUtils.debug"));

    private static String DEBUG_DIR;

    static {
        if (DEBUG) {
            final File tempDir =
                    new File(GeoServerExtensions.getProperty("user.home"), ".geoserver");
            if (!tempDir.exists()) {
                if (!tempDir.mkdir())
                    LOGGER.severe("Unable to create debug dir, exiting application!!!");
                DEBUG = false;
                DEBUG_DIR = null;
            } else {
                DEBUG_DIR = tempDir.getAbsolutePath();
                LOGGER.fine("MetatileMapOutputFormat debug dir " + DEBUG_DIR);
            }
        }
    }

    /**
     * Forces the use of the class as a pure utility methods one by declaring a private default
     * constructor.
     */
    private ImageUtils() {
        // do nothing
    }

    /**
     * Sets up a {@link BufferedImage#TYPE_4BYTE_ABGR} if the paletteInverter is not provided, or a
     * indexed image otherwise. Subclasses may override this method should they need a special kind
     * of image
     *
     * @param width the width of the image to create.
     * @param height the height of the image to create.
     * @param palette A {@link IndexColorModel} if the image is to be indexed, or <code>
     *     null</code> otherwise.
     * @return an image of size <code>width x height</code> appropriate for the given color model,
     *     if any, and to be used as a transparent image or not depending on the <code>transparent
     *     </code> parameter.
     */
    public static BufferedImage createImage(
            final int width, int height, final IndexColorModel palette, final boolean transparent) {
        // WARNING: whenever this method is changed, change getDrawingSurfaceMemoryUse
        // accordingly
        if (palette != null) {
            // unfortunately we can't use packed rasters because line rendering
            // gets completely
            // broken, see GEOS-1312 (https://osgeo-org.atlassian.net/browse/GEOS-1312)
            // final WritableRaster raster =
            // palette.createCompatibleWritableRaster(width, height);
            final WritableRaster raster =
                    Raster.createInterleavedRaster(
                            palette.getTransferType(), width, height, 1, null);
            return new BufferedImage(palette, raster, false, null);
        }

        if (transparent) {
            return new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        }

        // in case there was no active rule, the height is going to be zero, push it up
        // so that we build a transparent image
        if (height == 0) {
            height = 1;
        }

        // don't use alpha channel if the image is not transparent (load testing shows this
        // image setup is the fastest to draw and encode on
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    /** Computes the memory usage of the buffered image used as the drawing surface. */
    public static long getDrawingSurfaceMemoryUse(
            final int width,
            final int height,
            final IndexColorModel palette,
            final boolean transparent) {
        long memory = width * height;
        if (palette != null) {
            return memory;
        }
        if (transparent) {
            return memory * 4;
        }
        return memory * 3;
    }

    /**
     * Sets up and returns a {@link Graphics2D} for the given <code>preparedImage</code>, which is
     * already prepared with a transparent background or the given background color.
     *
     * @param transparent whether the graphics is transparent or not.
     * @param bgColor the background color to fill the graphics with if its not transparent.
     * @param preparedImage the image for which to create the graphics.
     * @param extraHints an optional map of extra rendering hints to apply to the {@link
     *     Graphics2D}, other than {@link RenderingHints#KEY_ANTIALIASING}.
     * @return a {@link Graphics2D} for <code>preparedImage</code> with transparent background if
     *     <code>transparent == true</code> or with the background painted with <code>bgColor</code>
     *     otherwise.
     */
    public static Graphics2D prepareTransparency(
            final boolean transparent,
            final Color bgColor,
            final RenderedImage preparedImage,
            final Map<RenderingHints.Key, Object> extraHints) {
        final Graphics2D graphic;

        if (preparedImage instanceof BufferedImage) {
            graphic = ((BufferedImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof TiledImage) {
            graphic = ((TiledImage) preparedImage).createGraphics();
        } else if (preparedImage instanceof VolatileImage) {
            graphic = ((VolatileImage) preparedImage).createGraphics();
        } else {
            throw new ServiceException("Unrecognized back-end image type");
        }

        // fill the background with no antialiasing
        Map<RenderingHints.Key, Object> hintsMap;
        if (extraHints == null) {
            hintsMap = new HashMap<>();
        } else {
            hintsMap = new HashMap<>(extraHints);
        }
        hintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphic.setRenderingHints(hintsMap);
        if (transparent) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("setting to transparent");
            }

            int type = AlphaComposite.SRC;
            graphic.setComposite(AlphaComposite.getInstance(type));

            Color c = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
            graphic.setBackground(bgColor);
            graphic.setColor(c);
            graphic.fillRect(0, 0, preparedImage.getWidth(), preparedImage.getHeight());
            type = AlphaComposite.SRC_OVER;
            graphic.setComposite(AlphaComposite.getInstance(type));
        } else {
            graphic.setColor(bgColor);
            graphic.fillRect(0, 0, preparedImage.getWidth(), preparedImage.getHeight());
        }
        return graphic;
    }

    /** @param invColorMap may be {@code null} */
    public static RenderedImage forceIndexed8Bitmask(
            RenderedImage originalImage, final InverseColorMapOp invColorMap) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Method forceIndexed8Bitmask called ");
            LOGGER.finer("invColorMap is null? " + (invColorMap == null));
            // check image type
            String type = "RI";
            if (originalImage instanceof PlanarImage) {
                type = "PI";
            } else if (originalImage instanceof BufferedImage) {
                type = "BI";
            }
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("OriginalImage type " + type);
                LOGGER.finer("OriginalImage info: " + originalImage.toString());
            }
        }
        // /////////////////////////////////////////////////////////////////
        //
        // Check what we need to do depending on the color model of the image we
        // are working on.
        //
        // /////////////////////////////////////////////////////////////////
        final ColorModel cm = originalImage.getColorModel();
        final boolean dataTypeByte =
                originalImage.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;
        RenderedImage image;

        // /////////////////////////////////////////////////////////////////
        //
        // IndexColorModel and DataBuffer.TYPE_BYTE
        //
        // ///
        //
        // If we got an image whose color model is already indexed on 8 bits
        // we have to check if it is bitmask or not.
        //
        // /////////////////////////////////////////////////////////////////
        if ((cm instanceof IndexColorModel) && dataTypeByte) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Image has IndexColorModel and type byte!");
            }
            final IndexColorModel icm = (IndexColorModel) cm;

            if (icm.getTransparency() != Transparency.TRANSLUCENT) {
                // //
                //
                // The image is indexed on 8 bits and the color model is either
                // opaque or bitmask. WE do not have to do anything.
                //
                // //
                image = originalImage;
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Image has Transparency  != TRANSLUCENT, do nothing");
                }
            } else {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Image has Transparency TRANSLUCENT, forceBitmaskIndexColorModel");
                }
                // //
                //
                // The image is indexed on 8 bits and the color model is
                // Translucent, we have to perform some color operations in
                // order to convert it to bitmask.
                //
                // //
                image =
                        new ImageWorker(originalImage)
                                .forceBitmaskIndexColorModel()
                                .getRenderedImage();
                if (DEBUG) {
                    writeRenderedImage(image, "indexed8translucent");
                }
            }
        } else {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Image has generic color model and/or type");
            }
            // /////////////////////////////////////////////////////////////////
            //
            // NOT IndexColorModel and DataBuffer.TYPE_BYTE
            //
            // ///
            //
            // We got an image that needs to be converted.
            //
            // /////////////////////////////////////////////////////////////////
            image = new ImageWorker(originalImage).rescaleToBytes().getRenderedImage();
            if (invColorMap != null) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("We have an invColorMap");
                }
                // make me parametric which means make me work with other image
                // types
                image = invColorMap.filterRenderedImage(image);
                if (DEBUG) {
                    writeRenderedImage(image, "invColorMap");
                }
            } else {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("We do not have an invColorMap");
                }
                // //
                //
                // We do not have a paletteInverter, let's create a palette that
                // is as good as possible.
                //
                // //
                // make sure we start from a componentcolormodel.
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Making sure we start from a componentcolormodel");
                }
                image = new ImageWorker(image).forceComponentColorModel().getRenderedImage();
                if (DEBUG) {
                    writeRenderedImage(image, "forceComponentColorModel");
                }

                // //
                //
                // Build the CustomPaletteBuilder doing some good subsampling.
                //
                // //
                int subsx = 1 + (int) (Math.log(image.getWidth()) / Math.log(32));
                int subsy = 1 + (int) (Math.log(image.getHeight()) / Math.log(32));
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("CustomPaletteBuilder[subsx=" + subsx + ",subsy=" + subsy + "]");
                    LOGGER.finer("InputImage is:" + image.toString());
                }
                CustomPaletteBuilder cpb =
                        new CustomPaletteBuilder(image, 256, subsx, subsy, 1).buildPalette();
                image = cpb.getIndexedImage();
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer(
                            "Computed Palette:" + paletteRepresentation(cpb.getIndexColorModel()));
                }
                if (DEBUG) {
                    writeRenderedImage(image, "buildPalette");
                }
            }
        }

        return image;
    }

    private static String paletteRepresentation(IndexColorModel indexColorModel) {
        final StringBuilder builder = new StringBuilder();
        final int mapSize = indexColorModel.getMapSize();
        builder.append("PaletteSize:").append(mapSize).append("\n");
        builder.append("Transparency:").append(indexColorModel.getTransparency()).append("\n");
        builder.append("TransparentPixel:")
                .append(indexColorModel.getTransparentPixel())
                .append("\n");
        for (int i = 0; i < mapSize; i++) {
            builder.append("[r=").append(indexColorModel.getRed(i)).append(",");
            builder.append("[g=").append(indexColorModel.getGreen(i)).append(",");
            builder.append("[b=").append(indexColorModel.getBlue(i)).append("]\n");
        }
        return builder.toString();
    }

    /**
     * Write the provided {@link RenderedImage} in the debug directory with the provided file name.
     *
     * @param raster the {@link RenderedImage} that we have to write.
     * @param fileName a {@link String} indicating where we should write it.
     */
    static void writeRenderedImage(final RenderedImage raster, final String fileName) {
        if (DEBUG_DIR == null)
            throw new NullPointerException(
                    "Unable to write the provided coverage in the debug directory");
        if (DEBUG == false)
            throw new IllegalStateException(
                    "Unable to write the provided coverage since we are not in debug mode");
        try {
            ImageIO.write(raster, "tiff", new File(DEBUG_DIR, fileName + ".tiff"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }
}
