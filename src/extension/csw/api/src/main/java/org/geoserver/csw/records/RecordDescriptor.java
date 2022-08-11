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

package org.geoserver.csw.records;

import java.util.LinkedHashSet;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geotools.data.Query;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes a record, its schema, its possible representations, in a pluggable way
 *
 * @author Andrea Aime - GeoSolutions
 * @author Niels Charlier
 */
public interface RecordDescriptor {

    /** The GeoTools feature type representing this kind of record */
    FeatureType getFeatureType();

    /** The GeoTools descriptor representing this kind of record */
    AttributeDescriptor getFeatureDescriptor();

    /** The outputSchema name for this feature type */
    String getOutputSchema();

    /**
     * The set of feature properties to be returned for the specified elementSetName (only needs to
     * answer for the ElementSetType#BRIEF and ElementSetType#SUMMARY). The chosen Set
     * implementation must respect the order in which the attributes are supposed to be encoded
     * ({@link LinkedHashSet} will do)
     */
    List<Name> getPropertiesForElementSet(ElementSetType elementSet);

    /**
     * Provides the namespace support needed to handle all schemas used/referenced by this record
     */
    NamespaceSupport getNamespaceSupport();

    /**
     * Allow the descriptor to adjust the query to the internal representation of records. For
     * example, in the case of SimpleLiteral we have a complex type with simple content, something
     * that we cannot readily represent in GeoTools
     */
    Query adaptQuery(Query query);

    /**
     * Return the property name (with dots) for the bounding box property
     *
     * @return the bounding box property name
     */
    String getBoundingBoxPropertyName();

    /**
     * Return the queryables for this type of record (for getcapabilities)
     *
     * @return list of queryable property names
     */
    List<Name> getQueryables();

    /**
     * Return a description of the queriables according to iso standard (for getcapabilities)
     *
     * @return the description string
     */
    String getQueryablesDescription();

    /**
     * Translate a property from a queryable name to a propertyname, possibly converting to an
     * x-path
     *
     * @return the property name
     */
    PropertyName translateProperty(Name name);

    /**
     * Checks that the spatial filters are actually referring to a spatial property. The {@link
     * SpatialFilterChecker} utility class can be used against simple records (like CSW), but more
     * complex record types will need a more sophisticated approach
     */
    void verifySpatialFilters(Filter filter);
}
