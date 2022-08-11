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

package org.geoserver.gwc.web.gridset;

import java.util.List;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

class GridSetBuilder {

    public static GridSet build(final GridSetInfo info) throws IllegalStateException {

        String name = checkNotNull(info.getName(), "Name is not set");
        CoordinateReferenceSystem crs = checkNotNull(info.getCrs(), "CRS is not set");
        String epsgCode = checkNotNull(CRS.toSRS(crs, false), "EPSG code not found for CRS");
        if (!epsgCode.startsWith("EPSG:")) {
            throw new IllegalStateException(
                    "EPSG code didn't resolve to a EPSG:XXX identifier: " + epsgCode);
        }

        SRS srs;
        try {
            srs = SRS.getSRS(epsgCode);
        } catch (GeoWebCacheException e) {
            throw new IllegalStateException(e.getMessage());
        }

        ReferencedEnvelope bounds = checkNotNull(info.getBounds(), "Bounds not set");
        if (bounds.isNull()) {
            throw new IllegalArgumentException("Bounds can't be null");
        }
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            throw new IllegalArgumentException(
                    "Bounds can't be empty. Witdh: "
                            + bounds.getWidth()
                            + ". Height: "
                            + bounds.getHeight());
        }

        BoundingBox extent =
                new BoundingBox(
                        bounds.getMinimum(0),
                        bounds.getMinimum(1),
                        bounds.getMaximum(0),
                        bounds.getMaximum(1));

        boolean alignTopLeft = info.isAlignTopLeft();

        final List<Grid> levels = checkNotNull(info.getLevels(), "GridSet levels not set");
        double[] resolutions;
        double[] scaleDenoms;
        if (info.isResolutionsPreserved()) {
            resolutions = resolutions(levels);
            scaleDenoms = null;
        } else {
            resolutions = null;
            scaleDenoms = scaleDenominators(levels);
        }
        String[] scaleNames = scaleNames(levels);

        final Double metersPerUnit =
                checkNotNull(info.getMetersPerUnit(), "Meters per unit not set");
        final double pixelSize = GridSetFactory.DEFAULT_PIXEL_SIZE_METER;
        final int tileWidth = info.getTileWidth();
        final int tileHeight = info.getTileHeight();
        // if CRS axis order is NORTH_EAST (y,x) set to true, else false
        boolean yCoordinateFirst = false;
        try {
            CoordinateReferenceSystem crsNoForceOrder = CRS.decode("urn:ogc:def:crs:" + epsgCode);
            yCoordinateFirst = CRS.getAxisOrder(crsNoForceOrder) == CRS.AxisOrder.NORTH_EAST;
        } catch (FactoryException e) {
            throw new IllegalStateException(
                    "EPSG code didn't resolve to a EPSG:XXX identifier: " + epsgCode);
        }
        // create GridSet
        GridSet gridSet =
                GridSetFactory.createGridSet(
                        name,
                        srs,
                        extent,
                        alignTopLeft,
                        resolutions,
                        scaleDenoms,
                        metersPerUnit,
                        pixelSize,
                        scaleNames,
                        tileWidth,
                        tileHeight,
                        yCoordinateFirst);

        gridSet.setDescription(info.getDescription());

        return gridSet;
    }

    private static String[] scaleNames(List<Grid> levels) {
        String[] scaleNames = new String[levels.size()];
        for (int i = 0; i < scaleNames.length; i++) {
            scaleNames[i] = levels.get(i).getName();
        }
        return scaleNames;
    }

    private static double[] resolutions(List<Grid> levels) {
        double[] resolutions = new double[levels.size()];
        for (int i = 0; i < resolutions.length; i++) {
            resolutions[i] = levels.get(i).getResolution();
        }
        return resolutions;
    }

    private static double[] scaleDenominators(List<Grid> levels) {
        double[] scales = new double[levels.size()];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = levels.get(i).getScaleDenominator();
        }
        return scales;
    }

    private static <T extends Object> T checkNotNull(final T val, final String msg)
            throws IllegalStateException {
        if (val == null) {
            throw new IllegalStateException(msg);
        }
        return val;
    }
}
