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

package org.geoserver.wms;

/**
 * Class to record capabilities for a {@link RasterMapProducer}.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class MapProducerCapabilities {

    private final boolean tiledRequestsSupported;

    private final boolean multivalueRequestsSupported;

    private final boolean paletteSupported;

    private final boolean transparencySupported;

    private final String framesMimeType;

    public MapProducerCapabilities(
            boolean tiledRequestsSupported,
            boolean multivalueRequestsSupported,
            boolean paletteSupported,
            boolean transparencySupported,
            String framesMimeType) {
        super();
        this.tiledRequestsSupported = tiledRequestsSupported;
        this.multivalueRequestsSupported = multivalueRequestsSupported;
        this.paletteSupported = paletteSupported;
        this.transparencySupported = transparencySupported;
        this.framesMimeType = framesMimeType;
    }

    /** If the map producer can be used in a meta-tiling context */
    public boolean isTiledRequestsSupported() {
        return tiledRequestsSupported;
    }

    /**
     * Returns true if the map producer can handle list or results (one per time/elevation/dimension
     * value) instead of a single one
     */
    public boolean isMultivalueRequestsSupported() {
        return multivalueRequestsSupported;
    }

    /** Returns true if paletted images are supported */
    public boolean isPaletteSupported() {
        return paletteSupported;
    }

    /** Returns true if background transparency is supported */
    public boolean isTransparencySupported() {
        return transparencySupported;
    }

    /** Returns the MIME TYPE of frames in case of animation is supported by this format */
    public String getFramesMimeType() {
        return framesMimeType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((framesMimeType == null) ? 0 : framesMimeType.hashCode());
        result = prime * result + (multivalueRequestsSupported ? 1231 : 1237);
        result = prime * result + (paletteSupported ? 1231 : 1237);
        result = prime * result + (tiledRequestsSupported ? 1231 : 1237);
        result = prime * result + (transparencySupported ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MapProducerCapabilities)) {
            return false;
        }
        MapProducerCapabilities other = (MapProducerCapabilities) obj;
        if (framesMimeType == null) {
            if (other.framesMimeType != null) {
                return false;
            }
        } else if (!framesMimeType.equals(other.framesMimeType)) {
            return false;
        }
        if (multivalueRequestsSupported != other.multivalueRequestsSupported) {
            return false;
        }
        if (paletteSupported != other.paletteSupported) {
            return false;
        }
        if (tiledRequestsSupported != other.tiledRequestsSupported) {
            return false;
        }
        if (transparencySupported != other.transparencySupported) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MapProducerCapabilities [framesMimeType="
                + framesMimeType
                + ", multivalueRequestsSupported="
                + multivalueRequestsSupported
                + ", paletteSupported="
                + paletteSupported
                + ", tiledRequestsSupported="
                + tiledRequestsSupported
                + ", transparencySupported="
                + transparencySupported
                + "]";
    }
}
