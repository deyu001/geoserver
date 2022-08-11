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

package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkNotNull;

import it.geosolutions.jaiext.BufferedImageAdapter;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import javax.media.jai.PlanarImage;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.Response;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.map.RenderedImageTimeDecorator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.metadata.i18n.ErrorKeys;
import org.geotools.metadata.i18n.Errors;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeType;

public class GeoServerMetaTile extends MetaTile {

    private WebMap metaTileMap;

    public GeoServerMetaTile(
            GridSubset gridSubset,
            MimeType responseFormat,
            FormatModifier formatModifier,
            long[] tileGridPosition,
            int metaX,
            int metaY,
            Integer gutter) {

        super(gridSubset, responseFormat, formatModifier, tileGridPosition, metaX, metaY, gutter);
    }

    public void setWebMap(WebMap webMap) {
        this.metaTileMap = webMap;
        if (webMap instanceof RenderedImageMap) {
            setImage(((RenderedImageMap) webMap).getImage());
        }
    }

    /**
     * Creates the {@link RenderedImage} corresponding to the tile at index {@code tileIdx} and uses
     * a {@link RenderedImageMapResponse} to encode it into the {@link #getResponseFormat() response
     * format}.
     *
     * @see org.geowebcache.layer.MetaTile#writeTileToStream(int, org.geowebcache.io.Resource)
     * @see RenderedImageMapResponse#write
     */
    @Override
    public boolean writeTileToStream(final int tileIdx, Resource target) throws IOException {

        checkNotNull(metaTileMap, "webMap is not set");

        if (metaTileMap instanceof RawMap) {
            try (OutputStream outStream = target.getOutputStream()) {
                ((RawMap) metaTileMap).writeTo(outStream);
            }
            return true;
        }
        if (!(metaTileMap instanceof RenderedImageMap)) {
            throw new IllegalArgumentException(
                    "Only RenderedImageMaps are supported so far: "
                            + metaTileMap.getClass().getName());
        }

        final RenderedImageMap metaTileMap = (RenderedImageMap) this.metaTileMap;
        final RenderedImageMapResponse mapEncoder;
        {
            final GWC mediator = GWC.get();
            final Response responseEncoder =
                    mediator.getResponseEncoder(responseFormat, metaTileMap);
            mapEncoder = (RenderedImageMapResponse) responseEncoder;
        }

        RenderedImage tile = metaTileMap.getImage();
        WMSMapContent tileContext = metaTileMap.getMapContext();

        if (this.tiles.length > 1 || (this.tiles.length == 1 && metaHasGutter())) {
            final Rectangle tileDim = this.tiles[tileIdx];
            tile = createTile(tileDim.x, tileDim.y, tileDim.width, tileDim.height);
            disposeLater(tile);
            {
                final WMSMapContent metaTileContext = metaTileMap.getMapContext();
                // do not create tileContext with metaTileContext.getLayers() as the layer list.
                // It is not needed at this stage and the constructor would force a
                // MapLayer.getBounds() that might fail
                tileContext = new WMSMapContent();
                tileContext.setRequest(metaTileContext.getRequest());
                tileContext.setBgColor(metaTileContext.getBgColor());
                tileContext.setMapWidth(tileDim.width);
                tileContext.setMapHeight(tileDim.height);
                tileContext.setPalette(metaTileContext.getPalette());
                tileContext.setTransparent(tileContext.isTransparent());
                long[][] tileIndexes = getTilesGridPositions();
                BoundingBox tileBounds = gridSubset.boundsFromIndex(tileIndexes[tileIdx]);
                ReferencedEnvelope tilebbox =
                        new ReferencedEnvelope(metaTileContext.getCoordinateReferenceSystem());
                tilebbox.init(
                        tileBounds.getMinX(),
                        tileBounds.getMaxX(),
                        tileBounds.getMinY(),
                        tileBounds.getMaxY());
                tileContext.getViewport().setBounds(tilebbox);
            }
        }

        try (OutputStream outStream = target.getOutputStream()) {
            // call formatImageOuputStream instead of write to avoid disposition of rendered images
            // when processing a tile from a metatile and instead defer it to this class' dispose()
            // method
            mapEncoder.formatImageOutputStream(tile, outStream, tileContext);
            return true;
        }
    }

    /** Checks if this meta tile has a gutter, or not */
    private boolean metaHasGutter() {
        if (this.gutter == null) {
            return false;
        }

        for (int element : gutter) {
            if (element > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Overrides to use the same method to slice the tiles than {@code MetatileMapOutputFormat} so
     * the GeoServer settings such as use native accel are leveraged in the same way when calling
     * {@link RenderedImageMapResponse#formatImageOutputStream},
     *
     * @see org.geowebcache.layer.MetaTile#createTile(int, int, int, int)
     */
    @Override
    public RenderedImage createTile(
            final int x, final int y, final int tileWidth, final int tileHeight) {
        // check image type
        final int type;
        if (metaTileImage instanceof PlanarImage) {
            type = 1;
        } else if (metaTileImage instanceof BufferedImage) {
            type = 2;
        } else {
            type = 0;
        }

        // now do the splitting
        RenderedImage tile;
        switch (type) {
            case 0:
                // do a crop, and then turn it into a buffered image so that we can release
                // the image chain
                ImageWorker w = new ImageWorker(metaTileImage);
                w.crop(
                        Float.valueOf(x),
                        Float.valueOf(y),
                        Float.valueOf(tileWidth),
                        Float.valueOf(tileHeight));
                tile = w.getBufferedImage();
                disposeLater(w.getRenderedImage());
                break;
            case 1:
                final PlanarImage pImage = (PlanarImage) metaTileImage;
                final WritableRaster wTile =
                        WritableRaster.createWritableRaster(
                                pImage.getSampleModel()
                                        .createCompatibleSampleModel(tileWidth, tileHeight),
                                new Point(x, y));
                Rectangle sourceArea = new Rectangle(x, y, tileWidth, tileHeight);
                sourceArea = sourceArea.intersection(pImage.getBounds());

                // copying the data to ensure we don't have side effects when we clean the cache
                pImage.copyData(wTile);
                if (wTile.getMinX() != 0 || wTile.getMinY() != 0) {
                    tile =
                            new BufferedImage(
                                    pImage.getColorModel(),
                                    (WritableRaster) wTile.createTranslatedChild(0, 0),
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
                final BufferedImage image = (BufferedImage) metaTileImage;
                final BufferedImage subimage = image.getSubimage(x, y, tileWidth, tileHeight);
                tile = new BufferedImageAdapter(subimage);
                break;
            default:
                throw new IllegalStateException(
                        Errors.format(
                                ErrorKeys.ILLEGAL_ARGUMENT_$2,
                                "metaTile class",
                                metaTileImage.getClass().toString()));
        }

        return tile;
    }

    @Override
    public void dispose() {
        if (metaTileMap != null) {
            metaTileMap.dispose();
            metaTileMap = null;
        }

        if (metaTileImage instanceof RenderedImageTimeDecorator) {
            metaTileImage = ((RenderedImageTimeDecorator) metaTileImage).getDelegate();
        }

        super.dispose();
    }
}
