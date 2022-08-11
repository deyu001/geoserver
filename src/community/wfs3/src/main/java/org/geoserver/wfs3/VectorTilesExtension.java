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

package org.geoserver.wfs3;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import java.io.InputStream;
import java.util.Collections;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs3.response.CollectionDocument;
import org.geoserver.wfs3.response.Link;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;

/** WFS3 extension adding support for the vector tiling OpenAPI paths */
public class VectorTilesExtension extends AbstractWFS3Extension {

    private static final String TILING_SPECIFICATION;
    static final String TILING_SCHEMES_PATH = "/tilingSchemes";
    static final String TILING_SCHEME_PATH = "/tilingSchemes/{tilingSchemeId}";
    static final String TILES_PATH =
            "/collections/{collectionId}/tiles/{tilingSchemeId}/{zoomLevel}/{row}/{column}";
    static final String COLLECTION_TILES_SCHEMES_PATH = "/collections/{collectionId}/tiles";

    static {
        try (InputStream is = VectorTilesExtension.class.getResourceAsStream("tiling.yml")) {
            TILING_SPECIFICATION = IOUtils.toString(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the tiling.yaml template", e);
        }
    }

    @Override
    public void extendAPI(OpenAPI api) {
        // load the pre-cooked building blocks
        OpenAPI tileAPITemplate = readTemplate(TILING_SPECIFICATION);

        // customize paths
        Paths paths = api.getPaths();
        paths.addPathItem(TILING_SCHEMES_PATH, tileAPITemplate.getPaths().get(TILING_SCHEMES_PATH));
        paths.addPathItem(
                TILING_SCHEME_PATH,
                tileAPITemplate.getPaths().get("/tilingSchemes/{tilingSchemeId}"));
        paths.addPathItem(
                TILING_SCHEME_PATH,
                tileAPITemplate.getPaths().get("/tilingSchemes/{tilingSchemeId}"));
        paths.addPathItem(TILES_PATH, tileAPITemplate.getPaths().get(TILES_PATH));
        paths.addPathItem(
                COLLECTION_TILES_SCHEMES_PATH,
                tileAPITemplate.getPaths().get(COLLECTION_TILES_SCHEMES_PATH));

        addSchemasAndParameters(api, tileAPITemplate);
    }

    @Override
    public void extendCollection(CollectionDocument collection, BaseRequest request) {
        String collectionId = collection.getName();

        // links
        String baseUrl = request.getBaseUrl();
        String tilingSchemeURL =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs3/collections/" + collectionId + "/tiles/{tilingSchemeId}",
                        Collections.emptyMap(),
                        URLMangler.URLType.SERVICE);
        collection
                .getLinks()
                .add(
                        new Link(
                                tilingSchemeURL,
                                "tilingScheme",
                                MapBoxTileBuilderFactory.MIME_TYPE,
                                collectionId
                                        + " associated tiling schemes. The link is a URI template \"\n"
                                        + "                                        + \"where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' resource",
                                "items"));

        String tilesURL =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs3/collections/"
                                + collectionId
                                + "/tiles/{tilingSchemeId}/{level}/{row}/{col}",
                        Collections.emptyMap(),
                        URLMangler.URLType.SERVICE);
        collection
                .getLinks()
                .add(
                        new Link(
                                tilesURL,
                                "tiles",
                                MapBoxTileBuilderFactory.MIME_TYPE,
                                collectionId
                                        + " as Mapbox vector tiles. The link is a URI template "
                                        + "where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' resource, and {level}/{row}/{col} the tile based on the tiling scheme.",
                                "items"));

        String tilingSchemesURL =
                ResponseUtils.buildURL(
                        baseUrl,
                        "wfs3/collections/" + collectionId + "/tiles",
                        Collections.emptyMap(),
                        URLMangler.URLType.SERVICE);
        collection
                .getLinks()
                .add(
                        new Link(
                                tilingSchemesURL,
                                "tilingSchemes",
                                "application/json",
                                collectionId
                                        + " associated tiling schemes. The link is a URI template \"\n"
                                        + "                                        + \"where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' resource",
                                "items"));
    }
}
