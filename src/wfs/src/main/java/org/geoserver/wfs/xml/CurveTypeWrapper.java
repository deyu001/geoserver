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

package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geometry.jts.CurvedGeometry;
import org.geotools.geometry.jts.MultiCurvedGeometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

public class CurveTypeWrapper implements FeatureType {

    FeatureType delegate;

    public CurveTypeWrapper(FeatureType delegate) {
        this.delegate = delegate;
    }

    public boolean isIdentified() {
        return delegate.isIdentified();
    }

    public GeometryDescriptor getGeometryDescriptor() {
        GeometryDescriptor gd = delegate.getGeometryDescriptor();
        return wrapGeometryDescriptor(gd);
    }

    private GeometryDescriptor wrapGeometryDescriptor(GeometryDescriptor gd) {
        GeometryType type = gd.getType();
        Class<?> binding = type.getBinding();
        if (MultiLineString.class.isAssignableFrom(binding)) {
            GeometryType curvedType =
                    new GeometryTypeImpl(
                            type.getName(),
                            MultiCurvedGeometry.class,
                            type.getCoordinateReferenceSystem(),
                            type.isIdentified(),
                            type.isAbstract(),
                            type.getRestrictions(),
                            type.getSuper(),
                            type.getDescription());
            return new GeometryDescriptorImpl(
                    curvedType,
                    gd.getName(),
                    gd.getMinOccurs(),
                    gd.getMaxOccurs(),
                    gd.isNillable(),
                    gd.getDefaultValue());
        } else if (LineString.class.isAssignableFrom(binding)) {
            GeometryType curvedType =
                    new GeometryTypeImpl(
                            type.getName(),
                            CurvedGeometry.class,
                            type.getCoordinateReferenceSystem(),
                            type.isIdentified(),
                            type.isAbstract(),
                            type.getRestrictions(),
                            type.getSuper(),
                            type.getDescription());
            return new GeometryDescriptorImpl(
                    curvedType,
                    gd.getName(),
                    gd.getMinOccurs(),
                    gd.getMaxOccurs(),
                    gd.isNillable(),
                    gd.getDefaultValue());
        } else {
            return gd;
        }
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem();
    }

    public AttributeType getSuper() {
        return delegate.getSuper();
    }

    public Class<Collection<Property>> getBinding() {
        return delegate.getBinding();
    }

    public Collection<PropertyDescriptor> getDescriptors() {
        List<PropertyDescriptor> result = new ArrayList<>();
        Collection<PropertyDescriptor> descriptors = delegate.getDescriptors();
        for (PropertyDescriptor pd : descriptors) {
            if (pd instanceof GeometryDescriptor) {
                pd = wrapGeometryDescriptor((GeometryDescriptor) pd);
            }
            result.add(pd);
        }

        return result;
    }

    public PropertyDescriptor getDescriptor(Name name) {
        return delegate.getDescriptor(name);
    }

    public PropertyDescriptor getDescriptor(String name) {
        return delegate.getDescriptor(name);
    }

    public Name getName() {
        return delegate.getName();
    }

    public boolean isInline() {
        return delegate.isInline();
    }

    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    public List<Filter> getRestrictions() {
        return delegate.getRestrictions();
    }

    public InternationalString getDescription() {
        return delegate.getDescription();
    }

    public Map<Object, Object> getUserData() {
        return delegate.getUserData();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CurveTypeWrapper other = (CurveTypeWrapper) obj;
        if (delegate == null) {
            if (other.delegate != null) return false;
        } else if (!delegate.equals(other.delegate)) return false;
        return true;
    }
}
