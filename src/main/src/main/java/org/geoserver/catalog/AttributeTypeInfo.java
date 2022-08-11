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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * An attribute exposed by a {@link FeatureTypeInfo}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface AttributeTypeInfo extends Serializable {

    /** Name of the attribute. */
    String getName();

    /** Sets name of the attribute. */
    void setName(String name);

    /** Minimum number of occurrences of the attribute. */
    int getMinOccurs();

    /** Sets minimum number of occurrences of the attribute. */
    void setMinOccurs(int minOccurs);

    /** Maximum number of occurrences of the attribute. */
    int getMaxOccurs();

    /** Sets maximum number of occurrences of the attribute. */
    void setMaxOccurs(int maxOccurs);

    /** Flag indicating if null is an acceptable value for the attribute. */
    boolean isNillable();

    /** Sets flag indicating if null is an acceptable value for the attribute. */
    void setNillable(boolean nillable);

    /** The feature type this attribute is part of. */
    FeatureTypeInfo getFeatureType();

    /** Sets the feature type this attribute is part of. */
    void setFeatureType(FeatureTypeInfo featureType);

    /**
     * A persistent map of metadata.
     *
     * <p>Data in this map is intended to be persisted. Common case of use is to have services
     * associate various bits of data with a particular attribute. An example might be its
     * associated xml or gml type.
     */
    Map<String, Serializable> getMetadata();

    /**
     * The underlying attribute descriptor.
     *
     * <p>Note that this value is not persisted with other attributes, and could be <code>null
     * </code>.
     */
    AttributeDescriptor getAttribute() throws IOException;

    /** Sets the underlying attribute descriptor. */
    void setAttribute(AttributeDescriptor attribute);

    /** The java class that values of this attribute are bound to. */
    Class getBinding();

    /** Sets the binding for this attribute */
    void setBinding(Class type);

    /**
     * Returns the length of this attribute. It's usually non null only for string and numeric types
     */
    Integer getLength();

    /** Sets the attribute length */
    void setLength(Integer length);

    /**
     * The same as {@link #equals(Object)}, except doesn't compare {@link FeatureTypeInfo}s, to
     * avoid recursion.
     */
    boolean equalsIngnoreFeatureType(Object obj);
}
