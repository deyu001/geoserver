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

package org.geoserver.jdbcconfig.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.Info;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;
import org.geotools.filter.visitor.ClientTransactionAccessor;
import org.geotools.filter.visitor.LiteralDemultiplyingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

class QueryBuilder<T extends Info> {

    @SuppressWarnings("unused")
    private static final SortBy DEFAULT_ORDER =
            CommonFactoryFinder.getFilterFactory().sort("id", SortOrder.ASCENDING);

    private Integer offset;

    private Integer limit;

    private SortBy[] sortOrder;

    private final boolean isCountQuery;

    // yuck
    private final Dialect dialect;

    private Class<T> queryType;

    private FilterToCatalogSQL predicateBuilder;

    private DbMappings dbMappings;

    private Filter originalFilter;

    private Filter supportedFilter;

    private Filter unsupportedFilter;

    private boolean offsetLimitApplied = false;

    /** */
    private QueryBuilder(
            Dialect dialect,
            final Class<T> clazz,
            DbMappings dbMappings,
            final boolean isCountQuery) {
        this.dialect = dialect;
        this.queryType = clazz;
        this.dbMappings = dbMappings;
        this.isCountQuery = isCountQuery;
        this.originalFilter = this.supportedFilter = this.unsupportedFilter = Filter.INCLUDE;
    }

    public static <T extends Info> QueryBuilder<T> forCount(
            Dialect dialect, final Class<T> clazz, DbMappings dbMappings) {
        return new QueryBuilder<T>(dialect, clazz, dbMappings, true);
    }

    public static <T extends Info> QueryBuilder<T> forIds(
            Dialect dialect, final Class<T> clazz, DbMappings dbMappings) {
        return new QueryBuilder<T>(dialect, clazz, dbMappings, false);
    }

    public Filter getUnsupportedFilter() {
        return unsupportedFilter;
    }

    public Filter getSupportedFilter() {
        return supportedFilter;
    }

    public Map<String, Object> getNamedParameters() {
        Map<String, Object> params = Collections.emptyMap();
        if (predicateBuilder != null) {
            params = predicateBuilder.getNamedParameters();
        }
        return params;
    }

    public QueryBuilder<T> offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public QueryBuilder<T> limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder<T> sortOrder(SortBy order) {
        if (order == null) {
            this.sortOrder();
        } else {
            this.sortOrder(new SortBy[] {order});
        }
        return this;
    }

    public QueryBuilder<T> sortOrder(SortBy... order) {
        if (order == null || order.length == 0) {
            this.sortOrder = null;
        } else {
            this.sortOrder = order;
        }
        return this;
    }

    public QueryBuilder<T> filter(Filter filter) {
        this.originalFilter = filter;
        return this;
    }

    private void querySortBy(StringBuilder query, StringBuilder whereClause, SortBy[] orders) {

        /*
         * Start with the oid and id from the object table selecting for type and the filter.
         *
         * Then left join on oid for each property to sort by to turn it into an attribute.
         *
         * The sort each of the created attribute.
         */

        // Need to put together the ORDER BY clause as we go and then add it at the end
        StringBuilder orderBy = new StringBuilder();
        orderBy.append("ORDER BY ");

        int i = 0;

        query.append("SELECT id FROM ");

        query.append("\n    (SELECT oid, id FROM object WHERE ");
        if (queryType != null) {
            query.append("type_id in (:types) /* ")
                    .append(queryType.getCanonicalName())
                    .append(" */\n      AND ");
        }
        query.append(whereClause).append(") object");

        for (SortBy order : orders) {
            final String sortProperty = order.getPropertyName().getPropertyName();
            final String subSelectName = "subSelect" + i;
            final String attributeName = "prop" + i;
            final String propertyParamName = "sortProperty" + i;

            final Set<Integer> sortPropertyTypeIds;
            sortPropertyTypeIds = dbMappings.getPropertyTypeIds(queryType, sortProperty);

            // Store the property type ID as a named parameter
            Map<String, Object> namedParameters = getNamedParameters();
            namedParameters.put(propertyParamName, sortPropertyTypeIds);

            query.append("\n  LEFT JOIN");
            query.append("\n    (SELECT oid, value ")
                    .append(attributeName)
                    .append(" FROM \n      object_property WHERE property_type IN (:")
                    .append(propertyParamName)
                    .append(")) ")
                    .append(subSelectName);

            query.append("  /* ")
                    .append(order.getPropertyName().getPropertyName())
                    .append(" ")
                    .append(ascDesc(order))
                    .append(" */");

            query.append("\n  ON object.oid = ").append(subSelectName).append(".oid");
            // Update the ORDER BY clause to be added later
            if (i > 0) orderBy.append(", ");
            orderBy.append(attributeName).append(" ").append(ascDesc(order));

            i++;
        }

        query.append("\n  ").append(orderBy);
    }

    private StringBuilder buildWhereClause() {
        final SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();

        this.predicateBuilder = new FilterToCatalogSQL(this.queryType, this.dbMappings);
        Capabilities fcs = new Capabilities(FilterToCatalogSQL.CAPABILITIES);
        FeatureType parent = null;
        // use this to instruct the filter splitter which filters can be encoded depending on
        // whether a db mapping for a given property name exists
        ClientTransactionAccessor transactionAccessor =
                new ClientTransactionAccessor() {

                    @Override
                    public Filter getUpdateFilter(final String attributePath) {
                        Set<PropertyType> propertyTypes;
                        propertyTypes = dbMappings.getPropertyTypes(queryType, attributePath);

                        final boolean isMappedProp = !propertyTypes.isEmpty();

                        if (isMappedProp) {
                            // continue normally
                            return null;
                        }
                        // tell the caps filter splitter this property name is not encodable
                        return Filter.EXCLUDE;
                    }

                    @Override
                    public Filter getDeleteFilter() {
                        return null;
                    }
                };

        CapabilitiesFilterSplitter filterSplitter;
        filterSplitter = new CapabilitiesFilterSplitter(fcs, parent, transactionAccessor);

        final Filter filter = (Filter) this.originalFilter.accept(filterSimplifier, null);
        filter.accept(filterSplitter, null);

        Filter supported = filterSplitter.getFilterPre();
        Filter unsupported = filterSplitter.getFilterPost();
        Filter demultipliedFilter =
                (Filter) supported.accept(new LiteralDemultiplyingFilterVisitor(), null);
        this.supportedFilter = (Filter) demultipliedFilter.accept(filterSimplifier, null);
        this.unsupportedFilter = (Filter) unsupported.accept(filterSimplifier, null);

        StringBuilder whereClause = new StringBuilder();
        return (StringBuilder) this.supportedFilter.accept(predicateBuilder, whereClause);
    }

    public StringBuilder build() {

        StringBuilder whereClause = buildWhereClause();

        StringBuilder query = new StringBuilder();
        if (isCountQuery) {
            if (Filter.INCLUDE.equals(this.originalFilter)) {
                query.append("select count(oid) from object where type_id in (:types)");
            } else {
                query.append("select count(oid) from object where type_id in (:types) AND (\n");
                query.append(whereClause).append("\n)");
            }
        } else {
            SortBy[] orders = this.sortOrder;
            if (orders == null) {
                query.append("select id from object where type_id in (:types) AND (\n");
                query.append(whereClause).append(")\n");
                query.append(" ORDER BY oid");
            } else {
                querySortBy(query, whereClause, orders);
            }
            applyOffsetLimit(query);
        }

        return query;
    }

    /** When the query was built, were the offset and limit included. */
    public boolean isOffsetLimitApplied() {
        return offsetLimitApplied;
    }

    private static String ascDesc(SortBy order) {
        return SortOrder.ASCENDING.equals(order.getSortOrder()) ? "ASC" : "DESC";
    }

    protected void applyOffsetLimit(StringBuilder sql) {
        if (unsupportedFilter.equals(Filter.INCLUDE)) {
            dialect.applyOffsetLimit(sql, offset, limit);
            offsetLimitApplied = true;
        } else {
            offsetLimitApplied = false;
        }
    }
}
