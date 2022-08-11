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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.opengis.feature.Feature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Queries a GeoServer layer and extracts the value(s) of an attribute TODO: add sorting
 *
 * @author Andrea Aime - GeoSolutions
 */
public class QueryFunction extends FunctionImpl {

    Catalog catalog;

    int maxResults;

    boolean single;

    public QueryFunction(
            Name name,
            Catalog catalog,
            List<Expression> args,
            Literal fallback,
            boolean single,
            int maxResults) {
        if (args.size() < 3 || args.size() > 4) {
            throw new IllegalArgumentException(
                    "QuerySingle function requires 3 or 4 arguments (feature type qualified name, "
                            + "cql filter, extracted attribute name and sort by clause");
        }

        this.catalog = catalog;
        this.maxResults = maxResults;
        this.single = single;

        functionName = new FunctionNameImpl(name, args.size());
        setName(name.getLocalPart());
        setFallbackValue(fallback);
        setParameters(args);
    }

    @Override
    public Object evaluate(Object object) {
        FeatureIterator fi = null;
        try {
            // extract layer
            String layerName = getParameters().get(0).evaluate(object, String.class);
            if (layerName == null) {
                throw new IllegalArgumentException(
                        "The first argument should be a vector layer name");
            }
            FeatureTypeInfo ft = catalog.getFeatureTypeByName(layerName);
            if (ft == null) {
                throw new IllegalArgumentException(
                        "Could not find vector layer " + layerName + " in the GeoServer catalog");
            }

            // extract and check the attribute
            String attribute = getParameters().get(1).evaluate(object, String.class);
            if (attribute == null) {
                throw new IllegalArgumentException(
                        "The second argument of the query "
                                + "function should be the attribute name");
            }
            CoordinateReferenceSystem crs = null;
            PropertyDescriptor ad = ft.getFeatureType().getDescriptor(attribute);
            if (ad == null) {
                throw new IllegalArgumentException(
                        "Attribute " + attribute + " could not be found in layer " + layerName);
            } else if (ad instanceof GeometryDescriptor) {
                crs = ((GeometryDescriptor) ad).getCoordinateReferenceSystem();
                if (crs == null) {
                    crs = ft.getCRS();
                }
            }

            // extract and check the filter
            String cql = getParameters().get(2).evaluate(object, String.class);
            if (cql == null) {
                throw new IllegalArgumentException(
                        "The third argument of the query "
                                + "function should be a valid (E)CQL filter");
            }
            Filter filter;
            try {
                filter = ECQL.toFilter(cql);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "The third argument of the query "
                                + "function should be a valid (E)CQL filter",
                        e);
            }

            // perform the query
            Query query = new Query(null, filter, new String[] {attribute});
            // .. just enough to judge if we went beyond the limit
            query.setMaxFeatures(maxResults + 1);
            FeatureSource fs = ft.getFeatureSource(null, null);
            fi = fs.getFeatures(query).features();
            List<Object> results = new ArrayList<>(maxResults);
            while (fi.hasNext()) {
                Feature f = fi.next();
                Object value = f.getProperty(attribute).getValue();
                if (value instanceof Geometry && crs != null) {
                    // if the crs is not associated with the geometry do so, this
                    // way other code will get to know the crs (e.g. for reprojection purposes)
                    Geometry geom = (Geometry) value;
                    geom.apply(new GeometryCRSFilter(crs));
                }
                results.add(value);
            }

            if (results.isEmpty()) {
                return null;
            }
            if (maxResults > 0 && results.size() > maxResults && !single) {
                throw new IllegalStateException(
                        "The query in "
                                + getName()
                                + " returns too many "
                                + "features, the limit is "
                                + maxResults);
            }
            if (maxResults == 1) {
                return results.get(0);
            } else {
                return results;
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to evaluated the query: " + e.getMessage(), e);
        } finally {
            if (fi != null) {
                fi.close();
            }
        }
    }

    /**
     * Applies the CRS to all geometry components
     *
     * @author aaime
     */
    static final class GeometryCRSFilter implements GeometryComponentFilter {
        CoordinateReferenceSystem crs;

        public GeometryCRSFilter(CoordinateReferenceSystem crs) {
            this.crs = crs;
        }

        @Override
        public void filter(Geometry g) {
            g.setUserData(crs);
        }
    }
}
