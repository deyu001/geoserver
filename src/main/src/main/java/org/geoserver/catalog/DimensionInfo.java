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

package org.geoserver.catalog;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents a dimension, such as the standard TIME and ELEVATION ones, but could be a custom one
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface DimensionInfo extends Serializable {

    /** Default value for elevation dimension 'units'. * */
    public static final String ELEVATION_UNITS = "EPSG:5030";
    /** Default value for elevation dimension 'unitSymbol'. * */
    public static final String ELEVATION_UNIT_SYMBOL = "m";
    /** Default value for time dimension 'unitSymbol'. * */
    public static final String TIME_UNITS = "ISO8601";

    /** The maximum number of dimension values GeoServer accepts if not otherwise configured */
    public static int DEFAULT_MAX_REQUESTED_DIMENSION_VALUES = 100;

    /** Whether this dimension is enabled or not */
    public boolean isEnabled();

    /** Sets the dimension as enabled, or not */
    public void setEnabled(boolean enabled);

    /** The attribute on which the dimension is based. Used only for vector data */
    public String getAttribute();

    public void setAttribute(String attribute);

    /**
     * The attribute on which the end of the dimension is based. Used only for vector data. This
     * attribute is optional.
     */
    public String getEndAttribute();

    public void setEndAttribute(String attribute);

    /** The way the dimension is going to be presented in the capabilities documents */
    public DimensionPresentation getPresentation();

    public void setPresentation(DimensionPresentation presentation);

    /**
     * The interval resolution in case {@link DimensionPresentation#DISCRETE_INTERVAL} presentation
     * has been chosen (it can be a representation of a elevation resolution or a time interval in
     * milliseconds)
     */
    public BigDecimal getResolution();

    public void setResolution(BigDecimal resolution);

    /**
     * The units attribute for the elevation dimension. This method has no affect on the time
     * dimension.
     *
     * @return the value for units
     */
    public String getUnits();

    public void setUnits(String units);

    /**
     * The unitSymbol attribute for the elevation dimension. This method has no affect on the time
     * dimension.
     *
     * @return the value for unitSymbol
     */
    public String getUnitSymbol();

    public void setUnitSymbol(String unitSymbol);

    /**
     * The setting for selecting the default value for this dimension.
     *
     * @return the current default value setting
     */
    public DimensionDefaultValueSetting getDefaultValue();

    public void setDefaultValue(DimensionDefaultValueSetting defaultValue);

    /**
     * Returns true if the nearest match behavior is implemented. Right now it's only available for
     * the TIME dimension, support for other dimensions might come later
     */
    public boolean isNearestMatchEnabled();

    /** Enables/disables nearest match. */
    public void setNearestMatchEnabled(boolean nearestMatch);

    /**
     * Returns true if the nearest match behavior is implemented for raw data requests. Right now
     * it's only available for the TIME dimension, support for other dimensions might come later.
     * Raw Nearest Match means nearest match on WCS when dealing with a coverage layer or WFS for
     * feature layer. Right now it's only available for WCS, support for other services might come
     * later.
     */
    public boolean isRawNearestMatchEnabled();

    /** Enables/disables raw nearest match. */
    public void setRawNearestMatchEnabled(boolean rawNearestMatch);

    /**
     * Returns a string specifying the search range. Can be empty, a single value (to be parsed in
     * the data type of the dimension, in particular, it will be a ISO period for times) or a
     * {code}before/after{code} range specifying how far to search from the requested value (e.g.,
     * {code}PT12H/PT1H{code} to allow searching 12 hours in the past but only 1 hour in the
     * future).
     */
    public String getAcceptableInterval();

    /**
     * Allows setting the search range for nearest matches, see also {@link
     * #getAcceptableInterval()}.
     */
    public void setAcceptableInterval(String acceptableInterval);
}
