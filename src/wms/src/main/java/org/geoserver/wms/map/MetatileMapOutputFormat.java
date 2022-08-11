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

import it.geosolutions.jaiext.BufferedImageAdapter;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.QuickTileCache.MetaTileKey;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.filter.function.EnvFunction;
import org.geotools.metadata.i18n.ErrorKeys;
import org.geotools.metadata.i18n.Errors;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.util.logging.Logging;

/**
 * Wrapping map producer that performs on the fly meta tiling wrapping another map producer. It will
 * first peek inside a tile cache to see if the requested tile has already been computed, if so,
 * it'll encode and return that one, otherwise it'll build a meta tile, split it, and finally encode
 * just the requested tile, putting the others in the tile cache.
 *
 * @author Andrea Aime - TOPP
 * @author Simone Giannecchini - GeoSolutions
 */
public final class MetatileMapOutputFormat implements GetMapOutputFormat {

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(MetatileMapOutputFormat.class);

    /** Small number for double equality comparison */
    public static final double EPS = 1E-6;

    /**
     * This variable is use for testing purposes in order to force this {@link GridCoverageRenderer}
     * to dump images at various steps on the disk.
     */
    private static boolean DEBUG =
            Boolean.valueOf(
                    GeoServerExtensions.getProperty(
                            "org.geoserver.wms.map.MetatileMapOutputFormat.debug"));

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

    private QuickTileCache tileCache;

    private GetMapRequest request;

    private RenderedImageMapOutputFormat delegate;

    public MetatileMapOutputFormat(GetMapRequest request, RenderedImageMapOutputFormat delegate) {
        if (tileCache == null) {
            // the meta tile cache is a singleton, so no need to keep it as a static member
            tileCache = (QuickTileCache) GeoServerExtensions.bean("metaTileCache");
        }
        this.request = request;
        this.delegate = delegate;
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent) */
    public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        // get the key that identifies the meta tile. The cache will make sure
        // two threads asking
        // for the same tile will get the same key, and thus will synchronize
        // with each other
        // (the first eventually builds the meta-tile, the second finds it ready
        // to be used)
        QuickTileCache.MetaTileKey key = tileCache.getMetaTileKey(request);

        synchronized (key) {
            RenderedImage tile = tileCache.getTile(key, request);
            List<GridCoverage2D> renderedCoverages = null;

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer(
                        "Looked for meta tile "
                                + key.metaTileCoords.x
                                + ", "
                                + key.metaTileCoords.y
                                + "in cache: "
                                + ((tile != null) ? "hit!" : "miss"));
            }

            if (tile == null) {
                // compute the meta-tile
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer(
                            "Building meta tile "
                                    + key.metaTileCoords.x
                                    + ", "
                                    + key.metaTileCoords.y
                                    + " of size w="
                                    + key.getTileSize() * key.getMetaFactor()
                                    + ", h="
                                    + key.getTileSize() * key.getMetaFactor()
                                    + " with metatilign factor "
                                    + key.getMetaFactor());
                }

                // alter the map definition so that we build a meta-tile instead
                // of just the tile
                mapContent.getViewport().setBounds(key.getMetaTileEnvelope());
                mapContent.setMapWidth(key.getTileSize() * key.getMetaFactor());
                mapContent.setMapHeight(key.getTileSize() * key.getMetaFactor());
                mapContent.setTileSize(key.getTileSize());

                // adjust the bbox/width/height env vars that GetMap setup, since we
                // are changing them under its feet
                EnvFunction.setLocalValue("wms_bbox", mapContent.getViewport().getBounds());
                EnvFunction.setLocalValue("wms_width", mapContent.getMapWidth());
                EnvFunction.setLocalValue("wms_height", mapContent.getMapHeight());

                RenderedImageMap metaTileMap = delegate.produceMap(mapContent);

                RenderedImage metaTile = metaTileMap.getImage();
                RenderedImage[] tiles = split(key, metaTile);
                tileCache.storeTiles(key, tiles);
                tile = tileCache.getTile(key, request, tiles);
                renderedCoverages = metaTileMap.getRenderedCoverages();
            }
            RenderedImageMap tileMap = new RenderedImageMap(mapContent, tile, getMimeType());
            tileMap.setRenderedCoverages(renderedCoverages);
            return tileMap;
        }
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames() */
    public Set<String> getOutputFormatNames() {
        return delegate.getOutputFormatNames();
    }

    /** @see org.geoserver.wms.GetMapOutputFormat#getMimeType() */
    public String getMimeType() {
        return delegate.getMimeType();
    }

    /**
     * True if the request has the tiled hint, is 256x256 image, and the raw delegate is a raster
     * one
     */
    public static boolean isRequestTiled(GetMapRequest request, GetMapOutputFormat delegate) {
        boolean tiled = request.isTiled();
        Point2D tilesOrigin = request.getTilesOrigin();
        int width = request.getWidth();
        int height = request.getHeight();
        if (tiled
                && tilesOrigin != null
                && width == 256
                && height == 256
                && delegate instanceof RenderedImageMapOutputFormat) {
            return true;
        }

        return false;
    }

    /**
     * Splits the tile into a set of tiles, numbered from lower right and going up so that first row
     * is 0,1,2,...,metaTileFactor, and so on. In the case of a 3x3 meta-tile, the layout is as
     * follows:
     *
     * <pre>
     *    6 7 8
     *    3 4 5
     *    0 1 2
     * </pre>
     */
    static RenderedImage[] split(MetaTileKey key, RenderedImage metaTile) {
        final int metaFactor = key.getMetaFactor();
        final RenderedImage[] tiles = new RenderedImage[key.getMetaFactor() * key.getMetaFactor()];
        final int tileSize = key.getTileSize();

        // check image type
        int type = 0;
        if (metaTile instanceof PlanarImage) {
            type = 1;
        } else if (metaTile instanceof BufferedImage) {
            type = 2;
        }
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Metatile type " + type);
        }

        // now do the splitting
        try {
            if (DEBUG) {
                writeRenderedImage(metaTile, "metaTile");
            }
            for (int i = 0; i < metaFactor; i++) {
                for (int j = 0; j < metaFactor; j++) {
                    int x = j * tileSize;
                    int y = (tileSize * (metaFactor - 1)) - (i * tileSize);

                    RenderedImage tile;
                    switch (type) {
                        case 0:
                            // RENDERED IMAGE
                            if (LOGGER.isLoggable(Level.FINER)) {
                                LOGGER.finer("Metatile split on RenderedImage");
                            }
                            metaTile = PlanarImage.wrapRenderedImage(metaTile);

                        case 1:
                            // PLANAR IMAGE
                            if (LOGGER.isLoggable(Level.FINER)) {
                                LOGGER.finer("Metatile split on PlanarImage");
                            }
                            final PlanarImage pImage = (PlanarImage) metaTile;
                            final WritableRaster wTile =
                                    WritableRaster.createWritableRaster(
                                            pImage.getSampleModel()
                                                    .createCompatibleSampleModel(
                                                            tileSize, tileSize),
                                            new Point(x, y));
                            Rectangle sourceArea = new Rectangle(x, y, tileSize, tileSize);
                            sourceArea = sourceArea.intersection(pImage.getBounds());

                            // copying the data to ensure we don't have side effects when we clean
                            // the cache
                            pImage.copyData(wTile);
                            if (wTile.getMinX() != 0 || wTile.getMinY() != 0) {
                                tile =
                                        new BufferedImage(
                                                pImage.getColorModel(),
                                                wTile.createWritableTranslatedChild(0, 0),
                                                pImage.getColorModel().isAlphaPremultiplied(),
                                                null);
                            } else {
                                tile =
                                        new BufferedImage(
                                                pImage.getColorModel(),
                                                wTile,
                                                pImage.getColorModel().isAlphaPremultiplied(),
                                                null);
                            }
                            break;
                        case 2:
                            // BUFFERED IMAGE
                            if (LOGGER.isLoggable(Level.FINER)) {
                                LOGGER.finer("Metatile split on BufferedImage");
                            }
                            final BufferedImage image = (BufferedImage) metaTile;
                            final BufferedImage subimage =
                                    image.getSubimage(x, y, tileSize, tileSize);
                            tile = new BufferedImageAdapter(subimage);
                            break;
                        default:
                            throw new IllegalStateException(
                                    Errors.format(
                                            ErrorKeys.ILLEGAL_ARGUMENT_$2,
                                            "metaTile class",
                                            metaTile.getClass().toString()));
                    }

                    tiles[(i * key.getMetaFactor()) + j] = tile;
                    if (DEBUG) {
                        writeRenderedImage(tile, "tile" + i + "-" + j);
                    }
                }
            }
        } finally {
            // dispose input image if necessary/possible
            RasterCleaner.addImage(metaTile);
        }
        return tiles;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        throw new RuntimeException("The meta-tile output format should never be invoked directly!");
    }
}
