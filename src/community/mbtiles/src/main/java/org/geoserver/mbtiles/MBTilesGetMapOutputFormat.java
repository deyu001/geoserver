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

package org.geoserver.mbtiles;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.ServiceException;
import org.geoserver.tiles.AbstractTilesGetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.mbtiles.MBTilesTile;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.GridSubset;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * WMS GetMap Output Format for mbtiles
 *
 * @author Justin Deoliveira, Boundless
 * @author Niels Charlier
 */
public class MBTilesGetMapOutputFormat extends AbstractTilesGetMapOutputFormat {

    private static final CoordinateReferenceSystem SPHERICAL_MERCATOR;

    private static final CoordinateReferenceSystem WGS_84;

    static {
        try {
            SPHERICAL_MERCATOR = CRS.decode("EPSG:3857", true);
            WGS_84 = CRS.decode("EPSG:4326", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class MbTilesFileWrapper implements TilesFile {

        MBTilesFile mbTiles;

        public MbTilesFileWrapper() throws IOException {
            mbTiles = new MBTilesFile();
            mbTiles.init();
        }

        public MbTilesFileWrapper(MBTilesFile file) throws IOException {
            mbTiles = file;
        }

        @Override
        public void setMetadata(
                String name,
                ReferencedEnvelope box,
                String imageFormat,
                int srid,
                List<MapLayerInfo> mapLayers,
                int[] minmax,
                GridSubset gridSubset)
                throws IOException, ServiceException {
            MBTilesMetadata metadata = new MBTilesMetadata();

            metadata.setName(name);
            metadata.setVersion("0");

            if (mapLayers.size() == 1) {
                ResourceInfo r = mapLayers.get(0).getResource();
                metadata.setDescription(r.getDescription());
                metadata.setType(MBTilesMetadata.t_type.BASE_LAYER);
            } else {
                String descr = "";
                for (MapLayerInfo l : mapLayers) {
                    descr += l.getResource().getDescription() + ", ";
                }
                descr = descr.substring(0, descr.length() - 2);
                metadata.setDescription(descr);
                metadata.setType(MBTilesMetadata.t_type.OVERLAY);
            }

            try {
                metadata.setBounds(box.transform(WGS_84, true));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to transform bounding box", e);
            }

            // save metadata
            metadata.setFormat(
                    JPEG_MIME_TYPE.equals(imageFormat)
                            ? MBTilesMetadata.t_format.JPEG
                            : MBTilesMetadata.t_format.PNG);
            LOGGER.fine("Creating tile entry" + metadata.getName());
            mbTiles.saveMetaData(metadata);
        }

        @Override
        public void addTile(int zoom, int x, int y, byte[] data) throws IOException {
            MBTilesTile tile = new MBTilesTile(zoom, x, y);
            tile.setData(data);
            mbTiles.saveTile(tile);
        }

        @Override
        public File getFile() {
            return mbTiles.getFile();
        }

        @Override
        public void close() {
            mbTiles.close();
        }
    }

    static final String MIME_TYPE = "application/x-sqlite3";

    static final String EXTENSION = ".mbtiles";

    static final Set<String> NAMES = Sets.newHashSet("mbtiles");

    // lazy loading converted bounds
    protected ReferencedEnvelope convertedBounds = null;

    public MBTilesGetMapOutputFormat(WebMapService webMapService, WMS wms, GWC gwc) {
        super(MIME_TYPE, EXTENSION, NAMES, webMapService, wms, gwc);
    }

    @Override
    protected TilesFile createTilesFile() throws IOException {
        return new MbTilesFileWrapper();
    }

    @Override
    protected ReferencedEnvelope bounds(GetMapRequest req) {
        ReferencedEnvelope convertedBounds = null;
        try {
            convertedBounds =
                    new ReferencedEnvelope(req.getBbox(), req.getCrs())
                            .transform(SPHERICAL_MERCATOR, true);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        return convertedBounds;
    }

    @Override
    protected CoordinateReferenceSystem getCoordinateReferenceSystem(GetMapRequest req) {
        return SPHERICAL_MERCATOR;
    }

    @Override
    protected String getSRS(GetMapRequest req) {
        return "EPSG:900913";
    }

    /** Add tiles to an existing MBtile file */
    public void addTiles(MBTilesFile mbtiles, GetMapRequest req, String name) throws IOException {
        addTiles(new MbTilesFileWrapper(mbtiles), req, name);
    }
}
