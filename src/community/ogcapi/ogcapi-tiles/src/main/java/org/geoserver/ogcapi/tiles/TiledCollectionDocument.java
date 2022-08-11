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

package org.geoserver.ogcapi.tiles;

import static org.geoserver.ogcapi.tiles.TilesService.isStyleGroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.QueryablesDocument;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.wms.WMS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links", "styles"})
public class TiledCollectionDocument extends AbstractCollectionDocument<TileLayer> {
    static final Logger LOGGER = Logging.getLogger(TiledCollectionDocument.class);
    WMS wms;
    TileLayer layer;
    List<StyleDocument> styles = new ArrayList<>();
    boolean dataTiles;
    boolean mapTiles;
    boolean queryable;

    /**
     * Builds a description of a tiled collection
     *
     * @param tileLayer The tile layer being described
     * @param summary If true, the info provided is minimal and assumed to be part of a {@link
     *     TiledCollectionsDocument}, otherwise it's full and assumed to be the main response
     */
    public TiledCollectionDocument(WMS wms, TileLayer tileLayer, boolean summary)
            throws FactoryException, TransformException {
        super(tileLayer);
        // basic info
        this.layer = tileLayer;
        this.id =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();
        if (tileLayer instanceof GeoServerTileLayer) {
            PublishedInfo published =
                    (PublishedInfo) ((GeoServerTileLayer) tileLayer).getPublishedInfo();
            setTitle(published.getTitle());
            setDescription(published.getAbstract());
            this.extent = getExtentsFromPublished(published);
        } else {
            this.extent = getExtentFromGridsets(tileLayer);
        }

        // backlinks in same and other formats
        addSelfLinks("ogc/tiles/collections/" + id);

        if (!summary) {
            // raw tiles links, if any (if the vector tiles plugin is missing or formats not
            // configured, will be empty)
            List<MimeType> tileTypes = tileLayer.getMimeTypes();

            dataTiles = tileTypes.stream().anyMatch(mt -> mt.isVector());
            if (dataTiles) {
                // tiles
                addLinksFor(
                        "ogc/tiles/collections/" + id + "/tiles",
                        TilesDocument.class,
                        "Tiles metadata as ",
                        "dataTiles",
                        null,
                        "tiles");
            }

            // map tiles links (a layer might not have image tiles configured, need to check)
            mapTiles = tileTypes.stream().anyMatch(mt -> !mt.isVector());
            if (mapTiles) {
                addLinksFor(
                        "ogc/tiles/collections/" + id + "/map/tiles",
                        TilesDocument.class,
                        "Map tiles metadata as ",
                        "mapTiles",
                        null,
                        "tiles");
            }

            // style links
            if (tileLayer instanceof GeoServerTileLayer) {
                PublishedInfo published = ((GeoServerTileLayer) tileLayer).getPublishedInfo();
                if (published instanceof LayerInfo) {
                    LayerInfo layerInfo = (LayerInfo) published;
                    LinkedHashSet<StyleInfo> stylesInfo =
                            new LinkedHashSet<>(Arrays.asList(layerInfo.getDefaultStyle()));
                    stylesInfo.addAll(layerInfo.getStyles());
                    stylesInfo.forEach(
                            style -> {
                                this.styles.add(new StyleDocument(style));
                            });
                } else {
                    LayerGroupInfo group = (LayerGroupInfo) published;
                    if (group != null && isStyleGroup(group)) {
                        StyleDocument styleDocument = new StyleDocument(group.getStyles().get(0));
                        this.styles.add(styleDocument);
                    } else {
                        // layer group? no named styles for the moment
                        this.styles.add(
                                new StyleDocument(
                                        StyleDocument.DEFAULT_STYLE_NAME,
                                        "The layer default style"));
                    }
                }
            } else {
                String style = tileLayer.getStyles();
                if (style != null) {
                    this.styles.add(new StyleDocument(style, "The layer default style"));
                } else {
                    this.styles.add(
                            new StyleDocument(
                                    StyleDocument.DEFAULT_STYLE_NAME, "The layer default style"));
                }
            }

            // filtering support
            if (TilesService.supportsFiltering(tileLayer)) {
                this.queryable = true;
                addLinksFor(
                        "ogc/tiles/collections/" + id + "/queryables",
                        QueryablesDocument.class,
                        "Collection queryables as ",
                        "queryables",
                        null,
                        "queryables");
            }
        }
    }

    private CollectionExtents getExtentFromGridsets(TileLayer tileLayer)
            throws FactoryException, TransformException {
        Set<String> srsSet =
                layer.getGridSubsets()
                        .stream()
                        .map(gs -> tileLayer.getGridSubset(gs).getSRS().toString())
                        .collect(Collectors.toSet());
        if (srsSet.isEmpty()) {
            throw new APIException(
                    "IllegalState",
                    "Could not compute the extent for layer "
                            + tileLayer.getName()
                            + ", no gridsets are configured",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (srsSet.contains("EPSG:4326")) {
            GridSubset subset = layer.getGridSubsetForSRS(SRS.getEPSG4326());
            return getExtentsFromGridSubset(subset);
        } else {
            // take the first and reproject...
            String srs = srsSet.iterator().next();

            try {
                GridSubset subset = layer.getGridSubsetForSRS(SRS.getSRS(srs));
                return getExtentsFromGridSubset(subset);
            } catch (GeoWebCacheException ex) {
                throw new APIException(
                        "IllegalState",
                        "Could not convert " + srs + " value: " + ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private CollectionExtents getExtentsFromGridSubset(GridSubset subset)
            throws FactoryException, TransformException {
        BoundingBox bbox = subset.getOriginalExtent();
        ReferencedEnvelope re =
                new ReferencedEnvelope(
                        bbox.getMinX(),
                        bbox.getMaxX(),
                        bbox.getMinY(),
                        bbox.getMaxY(),
                        CRS.decode(subset.getSRS().toString(), true));
        if (!CRS.equalsIgnoreMetadata(
                re.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84)) {
            re = re.transform(DefaultGeographicCRS.WGS84, true);
        }
        return new CollectionExtents(re);
    }

    private CollectionExtents getExtentsFromPublished(PublishedInfo published) {
        try {
            ReferencedEnvelope bbox = null;
            if (published instanceof LayerInfo) {
                bbox = ((LayerInfo) published).getResource().getLatLonBoundingBox();
            } else if (published instanceof LayerGroupInfo) {
                bbox = ((LayerGroupInfo) published).getBounds();
                if (!CRS.equalsIgnoreMetadata(bbox, DefaultGeographicCRS.WGS84)) {
                    bbox = bbox.transform(DefaultGeographicCRS.WGS84, true);
                }
            }
            if (bbox != null) {
                return new CollectionExtents(bbox);
            }
        } catch (TransformException | FactoryException e) {
            throw new APIException(
                    "InternalError",
                    "Failed to reproject native bounds to WGS84",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }

        return null;
    }

    @Override
    public List<StyleDocument> getStyles() {
        return styles;
    }

    public boolean isDataTiles() {
        return dataTiles;
    }

    public boolean isMapTiles() {
        return mapTiles;
    }

    @JsonIgnore
    public boolean isQueryable() {
        return queryable;
    }
}
