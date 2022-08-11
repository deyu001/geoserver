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
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.parameter.GeneralParameterValue;
import org.springframework.security.core.Authentication;

/**
 * Abstract class for wrappers around an existing resource access manager.
 *
 * @author David Winslow, OpenGeo
 */
public abstract class ResourceAccessManagerWrapper implements ResourceAccessManager {
    protected ResourceAccessManager delegate;
    private static FilterFactory2 factory = CommonFactoryFinder.getFilterFactory2(null);
    private static GeometryFactory geomFactory = new GeometryFactory();

    protected CatalogMode intersection(CatalogMode a, CatalogMode b) {
        if (a == CatalogMode.HIDE || b == CatalogMode.HIDE) {
            return CatalogMode.HIDE;
        } else if (a == CatalogMode.MIXED || b == CatalogMode.MIXED) {
            return CatalogMode.MIXED;
        } else if (a == CatalogMode.CHALLENGE || b == CatalogMode.CHALLENGE) {
            return CatalogMode.CHALLENGE;
        }

        // TODO: Log error - neither of the modes was a known value!
        return CatalogMode.HIDE;
    }

    protected DataAccessLimits intersection(DataAccessLimits a, DataAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        if (a instanceof VectorAccessLimits && b instanceof VectorAccessLimits) {
            return intersection((VectorAccessLimits) a, (VectorAccessLimits) b);
        } else if (a instanceof CoverageAccessLimits && b instanceof CoverageAccessLimits) {
            return intersection((CoverageAccessLimits) a, (CoverageAccessLimits) b);
        } else if (a instanceof WMSAccessLimits && b instanceof WMSAccessLimits) {
            return intersection((WMSAccessLimits) a, (WMSAccessLimits) b);
        }

        throw new IllegalArgumentException(
                "Tried to get intersection of differing or unanticipated types of DataAccessLimits ("
                        + a
                        + " && "
                        + b
                        + ")");
    }

    protected VectorAccessLimits intersection(VectorAccessLimits a, VectorAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        CatalogMode mode = intersection(a.getMode(), b.getMode());
        List<PropertyName> readAttributes =
                intersection(a.getReadAttributes(), b.getReadAttributes());
        Filter readFilter = intersection(a.getReadFilter(), b.getReadFilter());
        List<PropertyName> writeAttributes =
                intersection(a.getReadAttributes(), b.getReadAttributes());
        Filter writeFilter = intersection(a.getWriteFilter(), b.getWriteFilter());

        return new VectorAccessLimits(
                mode, readAttributes, readFilter, writeAttributes, writeFilter);
    }

    protected CoverageAccessLimits intersection(CoverageAccessLimits a, CoverageAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        final CatalogMode mode = intersection(a.getMode(), b.getMode());
        final MultiPolygon rasterFilter;

        {
            MultiPolygon aFilter = a.getRasterFilter(), bFilter = b.getRasterFilter();
            if (aFilter == null) rasterFilter = bFilter;
            else if (bFilter == null) rasterFilter = aFilter;
            else {
                Geometry intersection = aFilter.intersection(bFilter);
                if (intersection instanceof MultiPolygon) {
                    rasterFilter = (MultiPolygon) intersection;
                } else {
                    final List<Polygon> accum = new ArrayList<>();
                    intersection.apply(
                            new GeometryComponentFilter() {
                                public void filter(Geometry geom) {
                                    if (geom instanceof Polygon) accum.add((Polygon) geom);
                                }
                            });

                    rasterFilter =
                            geomFactory.createMultiPolygon(
                                    accum.toArray(new Polygon[accum.size()]));
                }
            }
        }

        final Filter readFilter;
        if (rasterFilter != null && rasterFilter.getNumGeometries() == 0) {
            readFilter = Filter.EXCLUDE;
        } else {
            readFilter = intersection(a.getReadFilter(), b.getReadFilter());
        }

        GeneralParameterValue[] params = intersection(a.getParams(), b.getParams());

        return new CoverageAccessLimits(mode, readFilter, rasterFilter, params);
    }

    protected WMSAccessLimits intersection(WMSAccessLimits a, WMSAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        CatalogMode mode = intersection(a.getMode(), b.getMode());
        Filter readFilter = intersection(a.getReadFilter(), b.getReadFilter());
        MultiPolygon rasterFilter = null;

        {
            MultiPolygon aFilter = a.getRasterFilter(), bFilter = b.getRasterFilter();
            if (aFilter == null) rasterFilter = bFilter;
            else if (bFilter == null) rasterFilter = aFilter;
            else rasterFilter = (MultiPolygon) aFilter.intersection(bFilter);
        }

        boolean allowFeatureInfo = a.isAllowFeatureInfo() && b.isAllowFeatureInfo();

        return new WMSAccessLimits(mode, readFilter, rasterFilter, allowFeatureInfo);
    }

    protected Filter intersection(Filter a, Filter b) {
        if (a == null) return b;
        if (b == null) return a;

        if (a == Filter.INCLUDE && b == Filter.INCLUDE) {
            return Filter.INCLUDE;
        } else if (a == Filter.EXCLUDE || b == Filter.EXCLUDE) {
            return Filter.EXCLUDE;
        } else {
            return factory.and(a, b);
        }
    }

    protected GeneralParameterValue[] intersection(
            GeneralParameterValue[] a, GeneralParameterValue[] b) {
        if (a == null) return b;
        if (b == null) return a;

        List<Integer> indices = new ArrayList<>(Math.min(a.length, b.length));
        List<GeneralParameterValue> bAsList = Arrays.asList(b);

        for (int i = 0; i < a.length; i++) {
            if (bAsList.contains(a[i])) {
                indices.add(i);
            }
        }

        if (indices.size() == a.length) {
            return a;
        } else {
            GeneralParameterValue[] results = new GeneralParameterValue[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                results[i] = a[indices.get(i)];
            }
            return results;
        }
    }

    protected List<PropertyName> intersection(List<PropertyName> a, List<PropertyName> b) {
        if (a == null) return b;
        if (b == null) return a;

        List<PropertyName> results = new ArrayList<>();
        for (PropertyName p : a) {
            if (b.contains(p)) {
                results.add(p);
            }
        }

        return results;
    }

    protected WorkspaceAccessLimits intersection(WorkspaceAccessLimits a, WorkspaceAccessLimits b) {
        CatalogMode mode = intersection(a.getMode(), b.getMode());
        return new WorkspaceAccessLimits(
                mode,
                a.isReadable() && b.isReadable(),
                a.isWritable() && b.isWritable(),
                a.isAdminable() && b.isAdminable());
    }

    public void setDelegate(ResourceAccessManager delegate) {
        this.delegate = delegate;
    }

    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        return delegate.getAccessLimits(user, resource);
    }

    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return delegate.getAccessLimits(user, layer);
    }

    public DataAccessLimits getAccessLimits(
            Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        return delegate.getAccessLimits(user, layer, containers);
    }

    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        return delegate.getAccessLimits(user, workspace);
    }

    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        return delegate.getAccessLimits(user, style);
    }

    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        return delegate.getAccessLimits(user, layerGroup);
    }

    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        return delegate.getAccessLimits(user, layerGroup, containers);
    }

    public ResourceAccessManager unwrap() {
        return this.delegate;
    }
}
