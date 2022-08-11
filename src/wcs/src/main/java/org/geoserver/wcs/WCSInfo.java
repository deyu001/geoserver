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

package org.geoserver.wcs;

import java.util.List;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.config.ServiceInfo;
import org.geotools.coverage.grid.io.OverviewPolicy;

/**
 * Service configuration object for Web Coverage Service.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface WCSInfo extends ServiceInfo {

    /** Flag determining if gml prefixing is used. */
    boolean isGMLPrefixing();

    /** Sets flag determining if gml prefixing is used. */
    void setGMLPrefixing(boolean gmlPrefixing);

    /**
     * Returns the maximum input size, in kilobytes. The input size is computed as the amount of
     * bytes needed to fully store in memory the input data, {code}width x heigth x pixelsize{code}
     * (whether that memory will actually be fully used or not depends on the data source) A
     * negative or null value implies there is no input limit.
     */
    long getMaxInputMemory();

    /** Sets the maximum input size. See {@link #getMaxInputMemory()} */
    void setMaxInputMemory(long size);

    /**
     * Returns the maximum output size, in kilobytes. The output size is computed as the amount of
     * bytes needed to store in memory the resulting image {code}width x heigth x pixelsize{code}.
     * Whether that memory will be used or not depends on the data source as well as the output
     * format. A negative or null value implies there is no output limit.
     */
    long getMaxOutputMemory();

    /** @param size */
    void setMaxOutputMemory(long size);

    /** Returns the overview policy used when returning WCS data */
    OverviewPolicy getOverviewPolicy();

    /** Sets the overview policyt to be used when processing WCS data */
    void setOverviewPolicy(OverviewPolicy policy);

    /** Enables the use of subsampling */
    boolean isSubsamplingEnabled();

    /** Enableds/disables the use of subsampling during the coverage reads */
    public void setSubsamplingEnabled(boolean enabled);

    /**
     * Allows users to request data in lat-lon order.
     *
     * <p>Default to <code>false</code>.
     *
     * @param latLon <code>true</code> for lat-lon order, <code>false</code> otherwise.
     */
    public void setLatLon(boolean latLon);

    /**
     * Tells me whether we should spit out data in lat-lon or lon-lat order.
     *
     * @return <code>true</code> for lat-lon order, <code>false</code> otherwise.
     */
    public boolean isLatLon();

    /** The srs's that the wcs service supports (not all versions of WCS support this) */
    List<String> getSRS();

    /**
     * Returns the maximum number of dimension items that can be requested by a client without
     * getting a service exception. The default is
     * {DimensionInfo#DEFAULT_MAX_REQUESTED_DIMENSION_VALUES} that is, no limit.
     */
    default int getMaxRequestedDimensionValues() {
        return DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES;
    }

    /**
     * Sets the maximum number of dimension items that can be requested by a client without. Zero or
     * negative will disable the limit.
     *
     * @param maxRequestedDimensionValues Any integer number
     */
    default void setMaxRequestedDimensionValues(int maxRequestedDimensionValues) {
        // if not implemented nothing is done
    }
}
