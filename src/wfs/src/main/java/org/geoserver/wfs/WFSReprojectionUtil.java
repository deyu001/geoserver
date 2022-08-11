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

package org.geoserver.wfs;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Utility class used to handle common WFS reprojection issues
 *
 * @author Andrea Aime, TOPP
 */
class WFSReprojectionUtil {

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    /** Returns the declared CRS given the native CRS and the request WFS version */
    public static CoordinateReferenceSystem getDeclaredCrs(
            CoordinateReferenceSystem nativeCRS, String wfsVersion) {
        try {
            if (nativeCRS == null) return null;

            if (wfsVersion.equals("1.0.0")) {
                return nativeCRS;
            } else {
                String code = GML2EncodingUtils.epsgCode(nativeCRS);
                // it's possible that we can't do the CRS -> code -> CRS conversion...so we'll just
                // return what we have
                if (code == null) return nativeCRS;
                return CRS.decode("urn:x-ogc:def:crs:EPSG:6.11.2:" + code);
            }
        } catch (Exception e) {
            throw new WFSException("We have had issues trying to flip axis of " + nativeCRS, e);
        }
    }

    /** Returns the declared CRS given a feature type and the request WFS version */
    public static CoordinateReferenceSystem getDeclaredCrs(FeatureType schema, String wfsVersion) {
        if (schema == null) return null;

        CoordinateReferenceSystem crs =
                (schema.getGeometryDescriptor() != null)
                        ? schema.getGeometryDescriptor().getCoordinateReferenceSystem()
                        : null;

        return getDeclaredCrs(crs, wfsVersion);
    }

    /** Applies a default CRS to all geometric filter elements that do not already have one */
    public static Filter applyDefaultCRS(Filter filter, CoordinateReferenceSystem defaultCRS) {
        DefaultCRSFilterVisitor defaultVisitor = new DefaultCRSFilterVisitor(ff, defaultCRS);
        return (Filter) filter.accept(defaultVisitor, null);
    }

    /** Reprojects all geometric filter elements to the native CRS of the provided schema */
    public static Filter reprojectFilter(Filter filter, FeatureType schema) {
        ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(ff, schema);
        return (Filter) filter.accept(visitor, null);
    }

    /**
     * Reprojects all geometric filter elements to the native CRS of the provided schema or to the
     * target CRS if not NULL.
     */
    public static Filter reprojectFilter(
            Filter filter, FeatureType schema, CoordinateReferenceSystem targetCrs) {
        ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(ff, schema, targetCrs);
        return (Filter) filter.accept(visitor, null);
    }

    /**
     * Convenience method, same as calling {@link #applyDefaultCRS} and then {@link
     * #reprojectFilter(Filter, SimpleFeatureType)} in a row
     */
    public static Filter normalizeFilterCRS(
            Filter filter, FeatureType schema, CoordinateReferenceSystem defaultCRS) {
        Filter defaulted = applyDefaultCRS(filter, defaultCRS);
        return reprojectFilter(defaulted, schema);
    }

    /**
     * Convenience method, same as calling {@link #applyDefaultCRS} and then {@link
     * #reprojectFilter(Filter, FeatureType, CoordinateReferenceSystem)} in a row. If a non NULL
     * target CRS is provided it will be used as the target CRS overriding the native CRS.
     */
    public static Filter normalizeFilterCRS(
            Filter filter,
            FeatureType schema,
            CoordinateReferenceSystem defaultCRS,
            CoordinateReferenceSystem targetCRS) {
        Filter defaulted = applyDefaultCRS(filter, defaultCRS);
        return reprojectFilter(defaulted, schema, targetCRS);
    }
}
