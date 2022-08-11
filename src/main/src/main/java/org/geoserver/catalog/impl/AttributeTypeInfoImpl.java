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

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.opengis.feature.type.AttributeDescriptor;

public class AttributeTypeInfoImpl implements AttributeTypeInfo {

    protected String id;
    protected String name;
    protected int minOccurs;
    protected int maxOccurs;
    protected boolean nillable;
    protected transient AttributeDescriptor attribute;
    protected MetadataMap metadata = new MetadataMap();
    protected FeatureTypeInfo featureType;
    protected Class binding;
    protected Integer length;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    public FeatureTypeInfo getFeatureType() {
        return featureType;
    }

    public void setFeatureType(FeatureTypeInfo featureType) {
        this.featureType = featureType;
    }

    public AttributeDescriptor getAttribute() {
        return attribute;
    }

    public void setAttribute(AttributeDescriptor attribute) {
        this.attribute = attribute;
    }

    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return name;
    }

    public Class getBinding() {
        return binding;
    }

    public void setBinding(Class binding) {
        this.binding = binding;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((binding == null) ? 0 : binding.hashCode());
        result = prime * result + ((featureType == null) ? 0 : featureType.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((length == null) ? 0 : length.hashCode());
        result = prime * result + maxOccurs;
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + minOccurs;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (nillable ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AttributeTypeInfoImpl other = (AttributeTypeInfoImpl) obj;
        if (attribute == null) {
            if (other.attribute != null) return false;
        } else if (!attribute.equals(other.attribute)) return false;
        if (binding == null) {
            if (other.binding != null) return false;
        } else if (!binding.equals(other.binding)) return false;
        if (featureType == null) {
            if (other.featureType != null) return false;
        } else if (!featureType.equals(other.featureType)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (length == null) {
            if (other.length != null) return false;
        } else if (!length.equals(other.length)) return false;
        if (maxOccurs != other.maxOccurs) return false;
        if (metadata == null) {
            if (other.metadata != null) return false;
        } else if (!metadata.equals(other.metadata)) return false;
        if (minOccurs != other.minOccurs) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (nillable != other.nillable) return false;
        return true;
    }

    @Override
    public boolean equalsIngnoreFeatureType(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        AttributeTypeInfoImpl other = (AttributeTypeInfoImpl) obj;
        if (attribute == null) {
            if (other.attribute != null) return false;
        } else if (!attribute.equals(other.attribute)) return false;
        if (binding == null) {
            if (other.binding != null) return false;
        } else if (!binding.equals(other.binding)) return false;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        if (length == null) {
            if (other.length != null) return false;
        } else if (!length.equals(other.length)) return false;
        if (maxOccurs != other.maxOccurs) return false;
        if (metadata == null) {
            if (other.metadata != null) return false;
        } else if (!metadata.equals(other.metadata)) return false;
        if (minOccurs != other.minOccurs) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (nillable != other.nillable) return false;
        return true;
    }
}
