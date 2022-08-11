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

import org.geoserver.platform.ServiceException;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;

/**
 * Checks if spatial filters are used against non spatial properties, if so, throws a
 * ServiceException (mandated for CSW cite compliance). Works fine for simple data models, but you
 * might need to use a custom one for more complex models (e.g., SpatialFilterChecker is known not
 * to work with ebRIM)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SpatialFilterChecker extends DefaultFilterVisitor {

    FeatureType schema;

    public SpatialFilterChecker(FeatureType schema) {
        this.schema = schema;
    }

    private void checkBinarySpatialOperator(BinarySpatialOperator filter) {
        verifyGeometryProperty(filter.getExpression1());
        verifyGeometryProperty(filter.getExpression2());
    }

    private void verifyGeometryProperty(Expression expression) {
        if (expression instanceof PropertyName) {
            PropertyName pn = ((PropertyName) expression);

            if (!(pn.evaluate(schema) instanceof GeometryDescriptor)) {
                throw new ServiceException(
                        "Invalid spatial filter, property "
                                + pn.getPropertyName()
                                + " is not a geometry");
            }
        }
    }

    public Object visit(final BBOX filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Beyond filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Contains filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Crosses filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Disjoint filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(DWithin filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Equals filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Intersects filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Overlaps filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Touches filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }

    public Object visit(Within filter, Object data) {
        checkBinarySpatialOperator(filter);
        return data;
    }
}
