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

package org.geoserver.security.decorators;

import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.WrapperPolicy;
import org.geotools.util.logging.Logging;

/**
 * Creates security wrappers for the most common catalog objects
 *
 * @author Andrea Aime - TOPP
 */
public class DefaultSecureCatalogFactory implements SecuredObjectFactory {

    private static final Logger LOGGER = Logging.getLogger(DefaultSecureCatalogFactory.class);

    @Override
    public boolean canSecure(Class clazz) {
        return CoverageInfo.class.isAssignableFrom(clazz)
                || CoverageStoreInfo.class.isAssignableFrom(clazz)
                || DataStoreInfo.class.isAssignableFrom(clazz)
                || FeatureTypeInfo.class.isAssignableFrom(clazz)
                || LayerInfo.class.isAssignableFrom(clazz)
                || WMSLayerInfo.class.isAssignableFrom(clazz)
                || WMTSLayerInfo.class.isAssignableFrom(clazz);
    }

    @Override
    public Object secure(Object object, WrapperPolicy policy) {
        // null safe
        if (object == null) return null;

        Class clazz = object.getClass();
        // for each supported Info type, log a warning if the object to be secured is already
        // secured. If this happens,
        // it could lead to a StackOverflowError if the object is re-wrapped, over time, over and
        // over agian.
        if (CoverageInfo.class.isAssignableFrom(clazz))
            return new SecuredCoverageInfo(logIfSecured((CoverageInfo) object), policy);
        else if (CoverageStoreInfo.class.isAssignableFrom(clazz))
            return new SecuredCoverageStoreInfo(logIfSecured((CoverageStoreInfo) object), policy);
        else if (DataStoreInfo.class.isAssignableFrom(clazz))
            return new SecuredDataStoreInfo(logIfSecured((DataStoreInfo) object), policy);
        else if (FeatureTypeInfo.class.isAssignableFrom(clazz))
            return new SecuredFeatureTypeInfo(logIfSecured((FeatureTypeInfo) object), policy);
        else if (LayerInfo.class.isAssignableFrom(clazz))
            return new SecuredLayerInfo(logIfSecured((LayerInfo) object), policy);
        else if (WMSLayerInfo.class.isAssignableFrom(clazz))
            return new SecuredWMSLayerInfo(logIfSecured((WMSLayerInfo) object), policy);
        else if (WMTSLayerInfo.class.isAssignableFrom(clazz))
            return new SecuredWMTSLayerInfo(logIfSecured((WMTSLayerInfo) object), policy);
        else throw new IllegalArgumentException("Don't know how to wrap " + object);
    }
    /**
     * Returns {@link ExtensionPriority#LOWEST} since the wrappers generated by this factory
     *
     * @return {@link ExtensionPriority#LOWEST}
     */
    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    private void logDoubleWrap(Object unwrapped, Object orig) {
        String msg =
                String.format("Tried to double secure: %s already securing %s", orig, unwrapped);
        LOGGER.warning(msg);
    }

    /**
     * Generates a warning log if the Info object is already wrapped with a Secured decorator. This
     * method is only intended to log a situation where a Catalog Info object is being secured, but
     * is already secured. Repeated calls to this will keep adding additional wrapper layers and may
     * eventually cause a StackOverflowError. The log generated is merely to aid in finding the real
     * issue, as opposed to masking it here.
     *
     * @param object {@link WMTSLayerInfo} to check.
     * @return The original object to be checked.
     */
    private WMTSLayerInfo logIfSecured(WMTSLayerInfo object) {
        WMTSLayerInfo unwrapped = ModificationProxy.unwrap(object);
        if (unwrapped instanceof SecuredWMTSLayerInfo) {
            logDoubleWrap(unwrapped, object);
        }
        return object;
    }

    /**
     * Generates a warning log if the Info object is already wrapped with a Secured decorator. This
     * method is only intended to log a situation where a Catalog Info object is being secured, but
     * is already secured. Repeated calls to this will keep adding additional wrapper layers and may
     * eventually cause a StackOverflowError. The log generated is merely to aid in finding the real
     * issue, as opposed to masking it here.
     *
     * @param object {@link WMSLayerInfo} to check.
     * @return The original object to be checked.
     */
    private WMSLayerInfo logIfSecured(WMSLayerInfo object) {
        WMSLayerInfo unwrapped = ModificationProxy.unwrap(object);
        if (unwrapped instanceof SecuredWMSLayerInfo) {
            logDoubleWrap(unwrapped, object);
        }
        return object;
    }

    /**
     * Generates a warning log if the Info object is already wrapped with a Secured decorator. This
     * method is only intended to log a situation where a Catalog Info object is being secured, but
     * is already secured. Repeated calls to this will keep adding additional wrapper layers and may
     * eventually cause a StackOverflowError. The log generated is merely to aid in finding the real
     * issue, as opposed to masking it here.
     *
     * @param object {@link LayerInfo} to check.
     * @return The original object to be checked.
     */
    private LayerInfo logIfSecured(LayerInfo object) {
        LayerInfo unwrapped = ModificationProxy.unwrap(object);
        if (unwrapped instanceof SecuredLayerInfo) {
            logDoubleWrap(unwrapped, object);
        }
        return object;
    }

    /**
     * Generates a warning log if the Info object is already wrapped with a Secured decorator. This
     * method is only intended to log a situation where a Catalog Info object is being secured, but
     * is already secured. Repeated calls to this will keep adding additional wrapper layers and may
     * eventually cause a StackOverflowError. The log generated is merely to aid in finding the real
     * issue, as opposed to masking it here.
     *
     * @param object {@link FeatureTypeInfo} to check.
     * @return The original object to be checked.
     */
    private FeatureTypeInfo logIfSecured(FeatureTypeInfo object) {
        FeatureTypeInfo unwrapped = ModificationProxy.unwrap(object);
        if (unwrapped instanceof SecuredFeatureTypeInfo) {
            logDoubleWrap(unwrapped, object);
        }
        return object;
    }

    /**
     * Generates a warning log if the Info object is already wrapped with a Secured decorator. This
     * method is only intended to log a situation where a Catalog Info object is being secured, but
     * is already secured. Repeated calls to this will keep adding additional wrapper layers and may
     * eventually cause a StackOverflowError. The log generated is merely to aid in finding the real
     * issue, as opposed to masking it here.
     *
     * @param object {@link CoverageStoreInfo} to check.
     * @return The original object to be checked.
     */
    private CoverageStoreInfo logIfSecured(CoverageStoreInfo object) {
        CoverageStoreInfo unwrapped = ModificationProxy.unwrap(object);
        if (unwrapped instanceof SecuredCoverageStoreInfo) {
            logDoubleWrap(unwrapped, object);
        }
        return object;
    }

    /**
     * Generates a warning log if the Info object is already wrapped with a Secured decorator. This
     * method is only intended to log a situation where a Catalog Info object is being secured, but
     * is already secured. Repeated calls to this will keep adding additional wrapper layers and may
     * eventually cause a StackOverflowError. The log generated is merely to aid in finding the real
     * issue, as opposed to masking it here.
     *
     * @param object {@link CoverageInfo} to check.
     * @return The original object to be checked.
     */
    private CoverageInfo logIfSecured(CoverageInfo object) {
        CoverageInfo unwrapped = ModificationProxy.unwrap(object);
        if (unwrapped instanceof SecuredDataStoreInfo) {
            logDoubleWrap(unwrapped, object);
        }
        return object;
    }

    /**
     * Generates a warning log if the Info object is already wrapped with a Secured decorator. This
     * method is only intended to log a situation where a Catalog Info object is being secured, but
     * is already secured. Repeated calls to this will keep adding additional wrapper layers and may
     * eventually cause a StackOverflowError. The log generated is merely to aid in finding the real
     * issue, as opposed to masking it here.
     *
     * @param object {@link DataStoreInfo} to check.
     * @return The original object to be checked.
     */
    private DataStoreInfo logIfSecured(DataStoreInfo object) {
        DataStoreInfo unwrapped = ModificationProxy.unwrap(object);
        if (unwrapped instanceof SecuredDataStoreInfo) {
            logDoubleWrap(unwrapped, object);
        }
        return object;
    }
}
