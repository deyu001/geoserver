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

package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;

/** The object representing the list of available tiling schemes */
@JacksonXmlRootElement(localName = "TilingSchemeDescription")
public class TilingSchemeDescriptionDocument {

    private final GridSet gridSet;

    public TilingSchemeDescriptionDocument(GridSet gridSet) {
        this.gridSet = gridSet;
    }

    @JacksonXmlProperty(localName = "identifier")
    public String getIdentifier() {
        return gridSet.getName();
    }

    @JacksonXmlProperty(localName = "supportedCRS")
    public String getSupportedCRS() {
        return gridSet.getSrs().toString();
    }

    @JacksonXmlProperty(localName = "title")
    public String getTitle() {
        return gridSet.getName();
    }

    @JacksonXmlProperty(localName = "type")
    public String getType() {
        return "TileMatrixSet";
    }

    @JacksonXmlProperty(localName = "wellKnownScaleSet")
    public String getWellKnownScaleSet() {
        return "http://www.opengis.net/def/wkss/OGC/1.0/" + gridSet.getName();
    }

    @JacksonXmlProperty(localName = "boundingBox")
    public BoundingBoxDocument getBoundingBox() {
        BoundingBox bbox = gridSet.getBounds();
        BoundingBoxDocument bboxDoc = new BoundingBoxDocument();
        bboxDoc.setLowerCorner(Arrays.asList(bbox.getMinX(), bbox.getMinY()));
        bboxDoc.setUpperCorner(Arrays.asList(bbox.getMaxX(), bbox.getMaxY()));
        bboxDoc.setCrs("http://www.opengis.net/def/crs/EPSG/0/" + gridSet.getSrs().getNumber());
        return bboxDoc;
    }

    @JacksonXmlProperty(localName = "TileMatrix")
    public List<TileMatrixDocument> getTileMatrix() {
        List<TileMatrixDocument> tileMatrix = new ArrayList<>();
        for (Integer i = 0; i < gridSet.getNumLevels(); i++) {
            Grid grid = gridSet.getGrid(i);
            TileMatrixDocument tm = new TileMatrixDocument();
            tm.setIdentifier(i.toString());
            tm.setMatrixHeight(grid.getNumTilesHigh());
            tm.setMatrixWidth(grid.getNumTilesWide());
            tm.setScaleDenominator(grid.getScaleDenominator());
            tm.setTileHeight(256);
            tm.setTileWidth(256);
            tm.setTopLeftCorner(
                    Arrays.asList(
                            gridSet.getOrderedTopLeftCorner(i)[0],
                            gridSet.getOrderedTopLeftCorner(i)[1]));
            tileMatrix.add(tm);
        }
        return tileMatrix;
    }
}
