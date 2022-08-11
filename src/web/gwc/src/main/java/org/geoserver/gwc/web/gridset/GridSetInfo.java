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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Angle;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.Grid;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import si.uom.NonSI;
import si.uom.SI;

class GridSetInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    private String description;

    private CoordinateReferenceSystem crs;

    private ReferencedEnvelope bounds;

    private int tileWidth;

    private int tileHeight;

    private boolean alignTopLeft;

    private List<Grid> levels;

    private double pixelSize;

    private boolean internal;

    /** Same as {@link GridSet#isResolutionsPreserved()} */
    private boolean resolutionsPreserved;

    public GridSetInfo() {
        this.internal = false;
        this.levels = new ArrayList<>();
        this.tileWidth = 256;
        this.tileHeight = 256;
        this.pixelSize = GridSetFactory.DEFAULT_PIXEL_SIZE_METER;
        this.resolutionsPreserved = true;
    }

    /** @param internal whether this gridset is one of the GWC internally defined ones */
    public GridSetInfo(final GridSet gridset, final boolean internal) {
        this.internal = internal;

        this.name = gridset.getName();
        this.description = gridset.getDescription();
        String code = "EPSG:" + gridset.getSrs().getNumber();
        final boolean longitudeFirst = true;
        try {
            this.crs = CRS.decode(code, longitudeFirst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        BoundingBox gridsetBounds = gridset.getOriginalExtent();
        double x1 = gridsetBounds.getMinX();
        double x2 = gridsetBounds.getMaxX();
        double y1 = gridsetBounds.getMinY();
        double y2 = gridsetBounds.getMaxY();
        this.bounds = new ReferencedEnvelope(x1, x2, y1, y2, this.crs);

        this.pixelSize = gridset.getPixelSize();
        this.tileWidth = gridset.getTileWidth();
        this.tileHeight = gridset.getTileHeight();
        this.alignTopLeft = gridset.isTopLeftAligned();
        this.resolutionsPreserved = gridset.isResolutionsPreserved();

        this.levels = new ArrayList<>();

        if (gridset.getNumLevels() > 0) {
            for (int i = 0; i < gridset.getNumLevels(); i++) {
                this.levels.add(gridset.getGrid(i).clone());
            }
        }
    }

    /** @see GridSet#isResolutionsPreserved() */
    public boolean isResolutionsPreserved() {
        return resolutionsPreserved;
    }

    /** @see GridSet#isResolutionsPreserved() */
    public void setResolutionsPreserved(boolean resolutionsPreserved) {
        this.resolutionsPreserved = resolutionsPreserved;
    }

    /** @return the pixelSize */
    public double getPixelSize() {
        return pixelSize;
    }

    /** @param pixelSize the pixelSize to set */
    public void setPixelSize(double pixelSize) {
        this.pixelSize = pixelSize;
    }

    /** @return {@code true} if this is a GWC internally defined GridSet */
    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @param name the name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the crs */
    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    /** @param crs the crs to set */
    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    /** @return the bounds */
    public ReferencedEnvelope getBounds() {
        return bounds;
    }

    /** @param bounds the bounds to set */
    public void setBounds(ReferencedEnvelope bounds) {
        this.bounds = bounds;
    }

    /** @return the tileWidth */
    public int getTileWidth() {
        return tileWidth;
    }

    /** @param tileWidth the tileWidth to set */
    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    /** @return the tileHeight */
    public int getTileHeight() {
        return tileHeight;
    }

    /** @param tileHeight the tileHeight to set */
    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    /** @return the alignTopLeft */
    public boolean isAlignTopLeft() {
        return alignTopLeft;
    }

    /** @param alignTopLeft the alignTopLeft to set */
    public void setAlignTopLeft(boolean alignTopLeft) {
        this.alignTopLeft = alignTopLeft;
    }

    /** @return the levels */
    public List<Grid> getLevels() {
        return levels;
    }

    /** @param levels the levels to set */
    public void setLevels(List<Grid> levels) {
        this.levels = levels;
    }

    public Double getMetersPerUnit() {
        CoordinateReferenceSystem crs = getCrs();
        Double metersPerUnit = getMetersPerUnit(crs);
        return metersPerUnit;
    }

    /** @throws IllegalArgumentException if the equivalence can't be established */
    public Double getMetersPerUnit(CoordinateReferenceSystem crs) {
        if (crs == null) {
            return null;
        }

        final CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
        final Unit<?> unit = axis.getUnit();

        return metersPerUnit(unit);
    }

    /** @throws IllegalArgumentException if the provided unit can't be converted to meters */
    static Double metersPerUnit(final Unit<?> unit) {
        double meters;
        final Unit<Angle> degree = NonSI.DEGREE_ANGLE;

        if (degree.equals(unit)) {
            meters = GridSetFactory.EPSG4326_TO_METERS;
        } else {
            try {
                meters = unit.getConverterToAny(SI.METRE).convert(1);
            } catch (Exception e) {
                UnitConverter converter;
                try {
                    converter = unit.getConverterToAny(degree);
                    double toDegree = converter.convert(1);
                    meters = toDegree * GridSetFactory.EPSG4326_TO_METERS;
                } catch (UnconvertibleException | IncommensurableException e1) {
                    throw new IllegalArgumentException(e1);
                }
            }
        }
        return meters;
    }
}
