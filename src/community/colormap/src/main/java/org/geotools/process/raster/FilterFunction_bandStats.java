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

package org.geotools.process.raster;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.List;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Utilities;
import org.opengis.filter.capability.FunctionName;

/**
 * Filter function to retrieve a grid coverage band min/max value
 *
 * @author Andrea Aime, GeoSolutions SAS
 */
public class FilterFunction_bandStats extends FunctionExpressionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "bandStats",
                    parameter("value", Number.class),
                    parameter("bandIndex", Number.class),
                    parameter("property", String.class));

    public FilterFunction_bandStats() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        try {
            Integer bandIndex = (getExpression(0).evaluate(feature, Integer.class));
            String propertyName = (getExpression(1).evaluate(feature, String.class));
            Object val = null;
            if (feature instanceof GridCoverage2D) {
                GridCoverage2D coverage = (GridCoverage2D) feature;
                val = evaluate(coverage, bandIndex, propertyName);
            }
            if (val != null) {
                return val;
            }
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageStats: Unable to find the stat "
                            + propertyName
                            + " from the input object of type "
                            + feature.getClass());
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function gridCoverageStats", e);
        }
    }

    Object evaluate(final GridCoverage2D coverage, final int bandIndex, final String statName) {
        Utilities.ensureNonNull("coverage", coverage);
        GridSampleDimension sd = coverage.getSampleDimension(bandIndex);
        if ("minimum".equalsIgnoreCase(statName)) {
            return ensureNotNull(sd, bandIndex, statName, getMinimum(sd));
        } else if ("maximum".equalsIgnoreCase(statName)) {
            return ensureNotNull(sd, bandIndex, statName, getMaximum(sd));
        } else {
            throw new IllegalArgumentException(
                    "Invalid property "
                            + statName
                            + ", supported values are 'minimum' and 'maximum'");
        }
    }

    private double ensureNotNull(
            GridSampleDimension sd, int bandIndex, String statName, Double value) {
        if (value != null) {
            return value;
        } else {
            throw new RuntimeException(
                    "Could not find the " + statName + " from " + sd + " of band " + bandIndex);
        }
    }

    private Double getMaximum(GridSampleDimension sd) {
        for (Category cat : sd.getCategories()) {
            final double result = cat.getRange().getMaximum();
            if (!Category.NODATA.getName().equals(cat.getName()) && !Double.isNaN(result)) {
                return result;
            }
        }

        return null;
    }

    private Double getMinimum(GridSampleDimension sd) {
        final List<Category> categories = sd.getCategories();
        for (int i = categories.size() - 1; i >= 0; i--) {
            Category cat = categories.get(i);
            final double result = cat.getRange().getMinimum();
            if (!Category.NODATA.getName().equals(cat.getName()) && !Double.isNaN(result)) {
                return result;
            }
        }

        return null;
    }
}
