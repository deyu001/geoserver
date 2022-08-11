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

package org.geoserver.wps.gs.download;

/**
 * Bean that includes the configurations parameters for the download service
 *
 * @author Simone Giannecchini, GeoSolutions
 */
public class DownloadServiceConfiguration {

    /** Value used to indicate no limits */
    public static final int NO_LIMIT = 0;

    public static final String COMPRESSION_LEVEL_NAME = "compressionLevel";

    public static final String HARD_OUTPUT_LIMITS_NAME = "hardOutputLimit";

    public static final String RASTER_SIZE_LIMITS_NAME = "rasterSizeLimits";

    public static final String WRITE_LIMITS_NAME = "writeLimits";

    public static final String MAX_FEATURES_NAME = "maxFeatures";

    public static final String MAX_ANIMATION_FRAMES_NAME = "maxAnimationFrames";

    public static final int DEFAULT_COMPRESSION_LEVEL = 4;

    public static final long DEFAULT_HARD_OUTPUT_LIMITS = NO_LIMIT;

    public static final long DEFAULT_RASTER_SIZE_LIMITS = NO_LIMIT;

    public static final long DEFAULT_WRITE_LIMITS = NO_LIMIT;

    public static final long DEFAULT_MAX_FEATURES = NO_LIMIT;

    public static final int DEFAULT_MAX_ANIMATION_FRAMES = NO_LIMIT;

    /** Max #of features */
    private long maxFeatures = DEFAULT_MAX_FEATURES;

    /** 8000 px X 8000 px */
    private long rasterSizeLimits = DEFAULT_RASTER_SIZE_LIMITS;

    /** Max size in bytes of raw raster output */
    private long writeLimits = DEFAULT_WRITE_LIMITS;

    /** 50 MB */
    private long hardOutputLimit = DEFAULT_HARD_OUTPUT_LIMITS;

    /** STORE =0, BEST =8 */
    private int compressionLevel = DEFAULT_COMPRESSION_LEVEL;

    private int maxAnimationFrames = DEFAULT_MAX_ANIMATION_FRAMES;

    /** Constructor: */
    public DownloadServiceConfiguration(
            long maxFeatures,
            long rasterSizeLimits,
            long writeLimits,
            long hardOutputLimit,
            int compressionLevel,
            int maxAnimationFrames) {
        this.maxFeatures = maxFeatures;
        this.rasterSizeLimits = rasterSizeLimits;
        this.writeLimits = writeLimits;
        this.hardOutputLimit = hardOutputLimit;
        this.compressionLevel = compressionLevel;
        this.maxAnimationFrames = maxAnimationFrames;
    }

    /** Default constructor */
    public DownloadServiceConfiguration() {
        this(
                DEFAULT_MAX_FEATURES,
                DEFAULT_RASTER_SIZE_LIMITS,
                DEFAULT_WRITE_LIMITS,
                DEFAULT_HARD_OUTPUT_LIMITS,
                DEFAULT_COMPRESSION_LEVEL,
                DEFAULT_MAX_ANIMATION_FRAMES);
    }

    public long getMaxFeatures() {
        return maxFeatures;
    }

    public long getRasterSizeLimits() {
        return rasterSizeLimits;
    }

    public long getWriteLimits() {
        return writeLimits;
    }

    public long getHardOutputLimit() {
        return hardOutputLimit;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public int getMaxAnimationFrames() {
        return maxAnimationFrames;
    }

    @Override
    public String toString() {
        return "DownloadServiceConfiguration [maxFeatures="
                + maxFeatures
                + ", rasterSizeLimits="
                + rasterSizeLimits
                + ", writeLimits="
                + writeLimits
                + ", hardOutputLimit="
                + hardOutputLimit
                + ", compressionLevel="
                + compressionLevel
                + "]";
    }
}
