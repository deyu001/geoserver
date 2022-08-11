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

package org.geoserver.function;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Predicates;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.util.Converters;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.VolatileFunction;

/**
 * This class implements the {@link Function} interface and can be used for checking if an object is
 * an instance of the provided input class.
 *
 * <p>Users can call this function using the {@link Predicates} class:
 *
 * <p>Predicates.isInstanceOf(Class clazz);
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class IsInstanceOf implements VolatileFunction, Function {

    /** Function name and related parameters */
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "isInstanceOf",
                    Boolean.class,
                    FunctionNameImpl.parameter("class", Class.class));

    /** Function parameters */
    private List<Expression> parameters;

    /** Fallback value used as default */
    private Literal fallback;

    public IsInstanceOf() {
        this.parameters = new ArrayList<>();
        this.fallback = null;
    }

    protected IsInstanceOf(List<Expression> parameters, Literal fallback) {
        this.parameters = parameters;
        this.fallback = fallback;
        // Check on the parameters
        if (parameters == null) {
            throw new NullPointerException("parameter required");
        }
        if (parameters.size() != 1) {
            throw new IllegalArgumentException("isInstanceOf(class) requires one parameter only");
        }
    }

    @Override
    public Object evaluate(Object object) {
        return evaluate(object, Boolean.class);
    }

    @Override
    public <T> T evaluate(Object object, Class<T> context) {
        // Selection of the first expression
        Expression clazzExpression = parameters.get(0);

        // Getting the defined class
        Class<?> clazz = clazzExpression.evaluate(object, Class.class);

        // Checking the result
        boolean result = false;

        // If the input class is Object, the function always returns true
        if (clazz != null) {
            if (clazz == Object.class) {
                result = true;
            } else {
                // Otherwise the function checks if the class is an instance of the
                // input class
                result = clazz.isAssignableFrom(object.getClass());
            }
        }

        // Finally the result is converted to the defined context class
        return Converters.convert(result, context);
    }

    @Override
    public Object accept(ExpressionVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

    @Override
    public String getName() {
        return NAME.getName();
    }

    @Override
    public FunctionName getFunctionName() {
        return NAME;
    }

    @Override
    public List<Expression> getParameters() {
        return parameters;
    }

    @Override
    public Literal getFallbackValue() {
        return fallback;
    }

    @Override
    public String toString() {
        List<Expression> params = getParameters();
        if (params == null || params.isEmpty()) {
            return "IsInstanceOf([INVALID])";
        } else {
            return "IsInstanceOf(" + params.get(0) + ")";
        }
    }
}
