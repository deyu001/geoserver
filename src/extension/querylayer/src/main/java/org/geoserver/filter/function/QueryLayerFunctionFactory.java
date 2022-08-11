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

package org.geoserver.filter.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;

/**
 * Factory for the functions that do query the GeoServer catalog as well as the support ones used to
 * mix them into larger filters
 *
 * @author Andrea Aime - GeoSolutions
 */
public class QueryLayerFunctionFactory implements FunctionFactory {
    static final Name COLLECT_GEOMETRIES = new NameImpl("collectGeometries");

    static final Name QUERY_COLLECTION = new NameImpl("queryCollection");

    static final Name QUERY_SINGLE = new NameImpl("querySingle");

    static final Logger LOGGER = Logging.getLogger(QueryLayerFunctionFactory.class);

    List<FunctionName> functionNames;

    Catalog catalog;

    int maxFeatures = 1000;

    long maxCoordinates = 1024 * 1024 / 28; // this results 1MB of Coordinate object max

    public QueryLayerFunctionFactory() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        List<FunctionName> names = new ArrayList<>();
        names.add(ff.functionName(QUERY_SINGLE, -1)); // 2 or 3 args
        names.add(ff.functionName(QUERY_COLLECTION, -1)); // 2 or 3 args
        names.add(ff.functionName(COLLECT_GEOMETRIES, 1));
        functionNames = Collections.unmodifiableList(names);
    }

    public long getMaxFeatures() {
        return maxFeatures;
    }

    /** Sets the max number of features returned by a free query */
    public void setMaxFeatures(int maxFeatures) {
        if (maxFeatures <= 0) {
            throw new IllegalArgumentException(
                    "The max features retrieved by a query layer "
                            + "function must be a positive number");
        }
        this.maxFeatures = maxFeatures;
    }

    /**
     * Sets the maximum number of coordinates to be collected, a non positive value implies no limit
     */
    public void setMaxCoordinates(long maxCoordinates) {
        this.maxCoordinates = maxCoordinates;
    }

    /** Initializes the catalog reference, without it the factory won't generate any function */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public Function function(String name, List<Expression> args, Literal fallback) {
        return function(new NameImpl(name), args, fallback);
    }

    @Override
    public Function function(Name name, List<Expression> args, Literal fallback) {
        if (!isInitialized()) {
            return null;
        }

        if (QUERY_SINGLE.equals(name)) {
            return new QueryFunction(QUERY_SINGLE, catalog, args, fallback, true, 1);
        } else if (QUERY_COLLECTION.equals(name)) {
            return new QueryFunction(QUERY_COLLECTION, catalog, args, fallback, false, maxFeatures);
        } else if (COLLECT_GEOMETRIES.equals(name)) {
            return new CollectGeometriesFunction(
                    COLLECT_GEOMETRIES, args, fallback, maxCoordinates);
        } else {
            return null;
        }
    }

    public List<FunctionName> getFunctionNames() {
        if (isInitialized()) {
            return functionNames;
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isInitialized() {
        if (catalog == null) {
            LOGGER.log(
                    Level.INFO,
                    "Looking for functions but the catalog still "
                            + "has not been set into QueryLayerFunctionFactory");
            return false;
        } else {
            return true;
        }
    }
}
