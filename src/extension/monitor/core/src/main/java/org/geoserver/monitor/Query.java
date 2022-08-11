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

package org.geoserver.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Query implements Serializable {

    public enum SortOrder {
        ASC,
        DESC
    }

    public enum Comparison {
        EQ {
            @Override
            public String toString() {
                return "=";
            }
        },

        NEQ {
            @Override
            public String toString() {
                return "!=";
            }
        },

        LT {
            @Override
            public String toString() {
                return "<";
            }
        },

        LTE {
            @Override
            public String toString() {
                return "<=";
            }
        },

        GT {
            @Override
            public String toString() {
                return ">";
            }
        },

        GTE {
            @Override
            public String toString() {
                return ">=";
            }
        },
        IN {
            @Override
            public String toString() {
                return "IN";
            }
        }
    }

    List<String> properties = new ArrayList<>();

    String sortBy;
    SortOrder sortOrder;

    Date fromDate;
    Date toDate;

    Long offset;
    Long count;

    Filter filter;

    List<String> aggregates = new ArrayList<>();
    List<String> groupBy = new ArrayList<>();

    public Query properties(String... props) {
        for (String p : props) {
            properties.add(p);
        }
        return this;
    }

    public Query sort(String property, SortOrder order) {
        sortBy = property;
        sortOrder = order;
        return this;
    }

    public Query filter(Object left, Object right, Comparison type) {
        return filter(new Filter(left, right, type));
    }

    public Query filter(Filter filter) {
        return and(filter);
    }

    public Query and(Object left, Object right, Comparison type) {
        return and(new Filter(left, right, type));
    }

    public Query and(Filter f) {
        if (filter == null) {
            filter = f;
        } else if (filter instanceof And) {
            ((And) filter).getFilters().add(f);
        } else {
            filter = new And(filter, f);
        }
        return this;
    }

    public Query or(Object left, Object right, Comparison type) {
        return or(new Filter(left, right, type));
    }

    public Query or(Filter f) {
        if (filter == null) {
            filter = f;
        } else if (filter instanceof Or) {
            ((Or) filter).getFilters().add(f);
        } else {
            filter = new Or(filter, f);
        }
        return this;
    }

    public Query between(Date from, Date to) {
        fromDate = from;
        toDate = to;
        return this;
    }

    public Query page(Long offset, Long count) {
        this.offset = offset;
        this.count = count;
        return this;
    }

    public Query aggregate(String... aggregates) {
        this.aggregates.addAll(Arrays.asList(aggregates));
        return this;
    }

    public Query group(String... properties) {
        this.groupBy.addAll(Arrays.asList(properties));
        return this;
    }

    public List<String> getProperties() {
        return properties;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<String> getAggregates() {
        return aggregates;
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    @Override
    public Query clone() {
        Query clone = new Query();
        clone.properties.addAll(properties);
        clone.aggregates.addAll(aggregates);
        clone.groupBy.addAll(groupBy);
        clone.count = count;
        clone.offset = offset;
        clone.fromDate = fromDate;
        clone.toDate = toDate;
        clone.sortBy = sortBy;
        clone.sortOrder = sortOrder;
        clone.filter = filter;
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("    SELECT: ").append(properties).append("\n");
        sb.append(" AGGREGATE: ").append(aggregates).append("\n");
        sb.append("    FILTER: ").append(filter).append("\n");
        sb.append("   BETWEEN: ").append(fromDate).append(", ").append(toDate).append("\n");
        sb.append("    OFFSET: ").append(offset).append(" LIMIT:").append(count).append("\n");
        sb.append("   SORT BY: ").append(sortBy).append(", ").append(sortOrder).append("\n");
        sb.append("  GROUP BY: ").append(groupBy);
        return sb.toString();
    }
}
