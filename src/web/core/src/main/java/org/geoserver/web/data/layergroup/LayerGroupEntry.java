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

package org.geoserver.web.data.layergroup;

import java.io.Serializable;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;

/** Represents one layer in the layer group */
public class LayerGroupEntry implements Serializable {

    private static final long serialVersionUID = -2212620293553872451L;

    enum Type {
        LAYER("Layer"),
        LAYER_GROUP("Layer Group"),
        STYLE_GROUP("Style Group");

        String type;

        Type(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    String styleId;
    String layerId;
    String layerGroupId;

    public LayerGroupEntry(PublishedInfo layer, StyleInfo style) {
        setLayer(layer);
        setStyle(style);
    }

    public Type getType() {
        if (layerId == null && layerGroupId == null) {
            return Type.STYLE_GROUP;
        } else if (layerGroupId != null) {
            return Type.LAYER_GROUP;
        } else {
            return Type.LAYER;
        }
    }

    public StyleInfo getStyle() {
        if (styleId == null) return null;
        else return GeoServerApplication.get().getCatalog().getStyle(styleId);
    }

    public boolean isDefaultStyle() {
        return styleId == null;
    }

    public void setDefaultStyle(boolean defaultStyle) {
        if (getLayer() == null) {
            setStyle(getStyle());
        } else if (defaultStyle || (getLayer() instanceof LayerGroupInfo)) {
            setStyle(null);
        } else {
            setStyle(((LayerInfo) getLayer()).getDefaultStyle());
        }
    }

    public void setStyle(StyleInfo style) {
        if (style == null) styleId = null;
        else styleId = style.getId();
    }

    public PublishedInfo getLayer() {
        if (layerGroupId != null) {
            return GeoServerApplication.get().getCatalog().getLayerGroup(layerGroupId);
        } else if (layerId != null) {
            return GeoServerApplication.get().getCatalog().getLayer(layerId);
        } else {
            return null;
        }
    }

    public void setLayer(PublishedInfo publishedInfo) {
        if (publishedInfo != null) {
            if (publishedInfo instanceof LayerGroupInfo) {
                layerGroupId = publishedInfo.getId();
            } else {
                layerId = publishedInfo.getId();
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((layerGroupId == null) ? 0 : layerGroupId.hashCode());
        result = prime * result + ((layerId == null) ? 0 : layerId.hashCode());
        result = prime * result + ((styleId == null) ? 0 : styleId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        LayerGroupEntry other = (LayerGroupEntry) obj;
        if (layerGroupId == null) {
            if (other.layerGroupId != null) return false;
        } else if (!layerGroupId.equals(other.layerGroupId)) return false;
        if (layerId == null) {
            if (other.layerId != null) return false;
        } else if (!layerId.equals(other.layerId)) return false;
        if (styleId == null) {
            if (other.styleId != null) return false;
        } else if (!styleId.equals(other.styleId)) return false;
        return true;
    }
}
