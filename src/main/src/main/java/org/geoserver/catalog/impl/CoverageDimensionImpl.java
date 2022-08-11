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

package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geotools.util.NumberRange;
import org.opengis.coverage.SampleDimensionType;

public class CoverageDimensionImpl implements CoverageDimensionInfo {

    /** */
    private static final long serialVersionUID = 2993765933856195894L;

    String id;

    String name;

    String description;

    NumberRange<? extends Number> range;

    List<Double> nullValues = new ArrayList<>();

    String unit;

    SampleDimensionType dimensionType;

    public CoverageDimensionImpl() {}

    public CoverageDimensionImpl(String id) {
        this.id = id;
    }

    public CoverageDimensionImpl(CoverageDimensionInfo other) {
        this.id = other.getId();
        this.name = other.getName();
        this.description = other.getDescription();
        this.range = other.getRange();
        this.nullValues = other.getNullValues();
        this.unit = other.getUnit();
        this.dimensionType = other.getDimensionType();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NumberRange<? extends Number> getRange() {
        return range;
    }

    public void setRange(NumberRange<? extends Number> range) {
        this.range = range;
    }

    public List<Double> getNullValues() {
        return nullValues;
    }

    public void setNullValues(List<Double> nullValues) {
        this.nullValues = nullValues;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public SampleDimensionType getDimensionType() {
        return dimensionType;
    }

    @Override
    public void setDimensionType(SampleDimensionType dimensionType) {
        this.dimensionType = dimensionType;
    }

    @Override
    public String toString() {
        return "CoverageDimensionImpl [id="
                + id
                + ", name="
                + name
                + ", description="
                + description
                + ", range="
                + range
                + ", nullValues="
                + nullValues
                + ", unit="
                + unit
                + ", dimensionType="
                + dimensionType
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((dimensionType == null) ? 0 : dimensionType.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nullValues == null) ? 0 : nullValues.hashCode());
        result = prime * result + ((range == null) ? 0 : range.hashCode());
        result = prime * result + ((unit == null) ? 0 : unit.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CoverageDimensionImpl other = (CoverageDimensionImpl) obj;
        if (description == null) {
            if (other.description != null) return false;
        } else if (!description.equals(other.description)) return false;
        if (dimensionType == null) {
            if (other.dimensionType != null) return false;
        } else if (!dimensionType.equals(other.dimensionType)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (nullValues == null) {
            if (other.nullValues != null) return false;
        } else if (!nullValues.equals(other.nullValues)) return false;
        if (range == null) {
            if (other.range != null) return false;
        } else if (!range.equals(other.range)) return false;
        if (unit == null) {
            if (other.unit != null) return false;
        } else if (!unit.equals(other.unit)) return false;
        return true;
    }
}
