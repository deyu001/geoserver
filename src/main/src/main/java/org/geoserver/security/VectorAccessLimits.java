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

package org.geoserver.security;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

/**
 * Describes the access limits on a vector layer
 *
 * @author Andrea Aime - GeoSolutions
 */
public class VectorAccessLimits extends DataAccessLimits {
    private static final long serialVersionUID = 1646981660625898503L;
    private static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2(null);

    /** The list of attributes the user is allowed to read (will be band names for raster data) */
    transient List<PropertyName> readAttributes;

    /** The set of attributes the user is allowed to write on */
    transient List<PropertyName> writeAttributes;

    /** Limits the features that can actually be written */
    transient Filter writeFilter;

    Geometry clipVectorFilter;

    Geometry intersectVectorFilter;

    /**
     * Builds a new vector access limits
     *
     * @param readAttributes The list of attributes that can be read
     * @param readFilter Only matching features will be returned to the user
     * @param writeAttributes The list of attributes that can be modified
     * @param writeFilter Only matching features will be allowed to be created/modified/deleted
     */
    public VectorAccessLimits(
            CatalogMode mode,
            List<PropertyName> readAttributes,
            Filter readFilter,
            List<PropertyName> writeAttributes,
            Filter writeFilter,
            MultiPolygon clipVectorFilter) {
        super(mode, readFilter);
        this.readAttributes = readAttributes;
        this.writeAttributes = writeAttributes;
        this.writeFilter = writeFilter;
        this.clipVectorFilter = clipVectorFilter;
    }

    public VectorAccessLimits(
            CatalogMode mode,
            List<PropertyName> readAttributes,
            Filter readFilter,
            List<PropertyName> writeAttributes,
            Filter writeFilter) {
        this(mode, readAttributes, readFilter, writeAttributes, writeFilter, null);
    }

    /** The list of attributes the user is allowed to read */
    public List<PropertyName> getReadAttributes() {
        return readAttributes;
    }

    /** The list of attributes the user is allowed to write */
    public List<PropertyName> getWriteAttributes() {
        return writeAttributes;
    }

    /** Identifies the features the user can write onto */
    public Filter getWriteFilter() {
        return writeFilter;
    }

    /** Returns a GeoTools query wrapping the read attributes and the read filter */
    public Query getReadQuery() {
        return buildQuery(readAttributes, readFilter);
    }

    /** Returns a GeoTools query wrapping the write attributes and the write filter */
    public Query getWriteQuery() {
        return buildQuery(writeAttributes, writeFilter);
    }

    /** Returns a GeoTools query build with the provided attributes and filters */
    private Query buildQuery(List<PropertyName> attributes, Filter filter) {
        if (attributes == null && (filter == null || filter == Filter.INCLUDE)) {
            return Query.ALL;
        } else {
            Query q = new Query();
            q.setFilter(filter);
            // TODO: switch this to property names when possible
            q.setPropertyNames(flattenNames(attributes));
            return q;
        }
    }

    /** Turns a list of {@link PropertyName} into a list of {@link String} */
    List<String> flattenNames(List<PropertyName> names) {
        if (names == null) {
            return null;
        }

        List<String> result = new ArrayList<>(names.size());
        for (PropertyName name : names) {
            result.add(name.getPropertyName());
        }

        return result;
    }

    @Override
    public String toString() {
        return "VectorAccessLimits [readAttributes="
                + readAttributes
                + ", writeAttributes="
                + writeAttributes
                + ", writeFilter="
                + writeFilter
                + ", readFilter="
                + readFilter
                + ", mode="
                + mode
                + "]";
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        readAttributes = readProperties(in);
        readFilter = readFilter(in);
        writeAttributes = readProperties(in);
        writeFilter = readFilter(in);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        writeProperties(readAttributes, out);
        writeFilter(readFilter, out);
        writeProperties(writeAttributes, out);
        writeFilter(writeFilter, out);
    }

    private void writeProperties(List<PropertyName> attributes, ObjectOutputStream oos)
            throws IOException {
        if (attributes == null) {
            oos.writeInt(-1);
        } else {
            oos.writeInt(attributes.size());
            for (PropertyName property : attributes) {
                oos.writeObject(property.getPropertyName());
                // TODO: write out the namespace support as well
            }
        }
    }

    private List<PropertyName> readProperties(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        int size = ois.readInt();
        if (size == -1) {
            return null;
        } else {
            List<PropertyName> properties = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String name = (String) ois.readObject();
                properties.add(FF.property(name));
                // TODO: read out the namespace support as well
            }
            return properties;
        }
    }

    public Geometry getClipVectorFilter() {
        return clipVectorFilter;
    }

    public void setClipVectorFilter(Geometry clipVectorFilter) {
        this.clipVectorFilter = clipVectorFilter;
    }

    public Geometry getIntersectVectorFilter() {
        return intersectVectorFilter;
    }

    public void setIntersectVectorFilter(Geometry intersectVectorFilter) {
        this.intersectVectorFilter = intersectVectorFilter;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((readAttributes == null) ? 0 : readAttributes.hashCode());
        result = prime * result + ((writeAttributes == null) ? 0 : writeAttributes.hashCode());
        result = prime * result + ((writeFilter == null) ? 0 : writeFilter.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        VectorAccessLimits other = (VectorAccessLimits) obj;
        if (readAttributes == null) {
            if (other.readAttributes != null) return false;
        } else if (!readAttributes.equals(other.readAttributes)) return false;
        if (writeAttributes == null) {
            if (other.writeAttributes != null) return false;
        } else if (!writeAttributes.equals(other.writeAttributes)) return false;
        if (writeFilter == null) {
            if (other.writeFilter != null) return false;
        } else if (!writeFilter.equals(other.writeFilter)) return false;
        return true;
    }
}
