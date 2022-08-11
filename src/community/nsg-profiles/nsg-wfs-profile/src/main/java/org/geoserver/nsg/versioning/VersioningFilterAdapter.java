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

package org.geoserver.nsg.versioning;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.identity.ResourceId;

final class VersioningFilterAdapter extends DuplicatingFilterVisitor {

    private final String namePropertyName;
    private final String timePropertyName;
    private final String typeName;

    private VersioningFilterAdapter(FeatureTypeInfo featureTypeInfo) {
        this.typeName = featureTypeInfo.getName();
        this.namePropertyName = TimeVersioning.getNamePropertyName(featureTypeInfo);
        this.timePropertyName = TimeVersioning.getTimePropertyName(featureTypeInfo);
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        FilterFactory filterFactory = getFactory(extraData);
        Set<Identifier> ids = filter.getIdentifiers();
        Set<Identifier> finalIds = new HashSet<>();
        Filter versioningFilter = null;
        for (Identifier id : ids) {
            if (id instanceof ResourceId) {
                Filter newFilter = buildVersioningFilter(filterFactory, (ResourceId) id);
                versioningFilter = addFilter(filterFactory, versioningFilter, newFilter);
            } else {
                finalIds.add(id);
            }
        }
        if (finalIds.isEmpty()) {
            return versioningFilter;
        }
        Filter newIdFilter = getFactory(extraData).id(finalIds);
        if (versioningFilter != null) {
            return filterFactory.and(newIdFilter, versioningFilter);
        }
        return newIdFilter;
    }

    private Filter buildVersioningFilter(FilterFactory filterFactory, ResourceId resourceId) {
        Filter idFilter = buildIdFilter(filterFactory, resourceId.getID());
        Filter timeFilter =
                buildTimeFilter(filterFactory, resourceId.getStartTime(), resourceId.getEndTime());
        if (idFilter != null && timeFilter != null) {
            return filterFactory.and(idFilter, timeFilter);
        }
        if (idFilter != null) {
            return idFilter;
        }
        if (timeFilter != null) {
            return timeFilter;
        }
        return null;
    }

    private Filter buildIdFilter(FilterFactory factory, String id) {
        if (id == null) {
            return null;
        }
        String prefix = typeName + ".";
        if (id.startsWith(prefix)) {
            id = id.substring(prefix.length());
        } else {
            // was not generated by GeoServer
            return Filter.EXCLUDE;
        }
        return factory.equals(factory.property(namePropertyName), factory.literal(id));
    }

    private Filter buildTimeFilter(FilterFactory filterFactory, Date start, Date end) {
        Expression timeProperty = filterFactory.property(timePropertyName);
        Expression startLiteral = filterFactory.literal(start);
        Expression endLiteral = filterFactory.literal(end);
        Filter after = filterFactory.after(timeProperty, startLiteral);
        Filter before = filterFactory.before(timeProperty, endLiteral);
        if (start != null && end != null) {
            return filterFactory.or(after, before);
        }
        if (start != null) {
            return after;
        }
        if (end != null) {
            return before;
        }
        return null;
    }

    private Filter addFilter(FilterFactory filterFactory, Filter versioningFilter, Filter filter) {
        if (versioningFilter != null) {
            return filterFactory.or(versioningFilter, filter);
        }
        return filter;
    }

    static Filter adapt(FeatureTypeInfo featureTypeInfo, Filter filter) {
        if (filter == null) {
            return null;
        }
        VersioningFilterAdapter adapter = new VersioningFilterAdapter(featureTypeInfo);
        return (Filter) filter.accept(adapter, null);
    }
}
