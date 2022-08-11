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

package org.geoserver.security;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Filters viewable layers based on the registered CatalogFilter
 *
 * @author Justin Deoliveira, OpenGeo
 * @author David Winslow, OpenGeo
 * @author Andrea Aime, GeoSolutions
 */
public class CatalogFilterAccessManager extends ResourceAccessManagerWrapper {

    private List<? extends CatalogFilter> filters;

    private DataAccessLimits hide(ResourceInfo info) {
        if (info instanceof FeatureTypeInfo) {
            return new VectorAccessLimits(
                    CatalogMode.HIDE, null, Filter.EXCLUDE, null, Filter.EXCLUDE);
        } else if (info instanceof CoverageInfo) {
            return new CoverageAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, null);
        } else if (info instanceof WMSLayerInfo) {
            return new WMSAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, false);
        } else {
            // TODO: Log warning about unknown resource type
            return new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE);
        }
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        if (hideLayer(layer) || hideResource(layer.getResource())) {
            return hide(layer.getResource());
        }
        return super.getAccessLimits(user, layer);
    }

    @Override
    public DataAccessLimits getAccessLimits(
            Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        if (hideLayer(layer) || hideResource(layer.getResource())) {
            return hide(layer.getResource());
        }
        return super.getAccessLimits(user, layer, containers);
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        if (hideResource(resource)) {
            return hide(resource);
        } else {
            return super.getAccessLimits(user, resource);
        }
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        if (hideWorkspace(workspace)) {
            return new WorkspaceAccessLimits(CatalogMode.HIDE, false, false, false);
        } else {
            return super.getAccessLimits(user, workspace);
        }
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        if (hideStyle(style)) {
            return new StyleAccessLimits(CatalogMode.HIDE);
        } else {
            return super.getAccessLimits(user, style);
        }
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        if (hideLayerGroup(layerGroup)) {
            return new LayerGroupAccessLimits(CatalogMode.HIDE);
        }
        return super.getAccessLimits(user, layerGroup);
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        if (hideLayerGroup(layerGroup)) {
            return new LayerGroupAccessLimits(CatalogMode.HIDE);
        } else {
            return super.getAccessLimits(user, layerGroup, containers);
        }
    }

    private boolean hideResource(ResourceInfo resource) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideResource(resource)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideLayer(LayerInfo layer) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideLayer(layer)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideWorkspace(WorkspaceInfo workspace) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideWorkspace(workspace)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideStyle(StyleInfo style) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideStyle(style)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideLayerGroup(layerGroup)) {
                return true;
            }
        }
        return false;
    }

    private List<? extends CatalogFilter> getCatalogFilters() {
        if (filters == null) {
            filters = GeoServerExtensions.extensions(CatalogFilter.class);
        }
        return filters;
    }

    /**
     * Designed for testing, allows to manually configure the catalog filters bypassing the Spring
     * context lookup
     */
    public void setCatalogFilters(List<? extends CatalogFilter> filters) {
        this.filters = filters;
    }

    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        // If there are no CatalogFilters, just get the delegate's filter
        if (filters == null || filters.isEmpty()) return delegate.getSecurityFilter(user, clazz);

        // Result is the conjunction of delegate's filter, and those of all the CatalogFilters
        ArrayList<Filter> convertedFilters = new ArrayList<>(this.filters.size() + 1);
        convertedFilters.add(delegate.getSecurityFilter(user, clazz)); // Delegate's filter

        for (CatalogFilter filter : getCatalogFilters()) {
            convertedFilters.add(filter.getSecurityFilter(clazz)); // Each CatalogFilter's filter
        }
        return Predicates.and(convertedFilters.toArray(new Filter[convertedFilters.size()]));
    }
}
