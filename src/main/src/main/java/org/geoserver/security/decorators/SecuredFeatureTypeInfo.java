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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * Wraps a {@link FeatureTypeInfo} so that it will return a secured FeatureSource
 *
 * @author Andrea Aime - TOPP
 */
public class SecuredFeatureTypeInfo extends DecoratingFeatureTypeInfo {

    protected static final String GET_CAPABILITIES = "GetCapabilities";

    WrapperPolicy policy;

    public SecuredFeatureTypeInfo(FeatureTypeInfo info, WrapperPolicy policy) {
        super(info);
        this.policy = policy;
    }

    @Override
    public FeatureType getFeatureType() throws IOException {

        FeatureType ft = super.getFeatureType();

        if (policy.getLimits() == null) {
            return ft;
        } else if (policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits val = (VectorAccessLimits) policy.getLimits();

            // get what we can actually read (and it makes it easier to deal with property names)
            Query query = val.getReadQuery();

            // do we have any attribute filtering?
            if (query.getPropertyNames() == Query.ALL_NAMES) {
                return ft;
            }

            if (ft instanceof SimpleFeatureType) {
                SimpleFeatureType sft = (SimpleFeatureType) ft;
                Set<String> properties = new HashSet<>(Arrays.asList(query.getPropertyNames()));
                SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
                tb.init(sft);
                for (AttributeDescriptor at : sft.getAttributeDescriptors()) {
                    String attName = at.getLocalName();
                    if (!properties.contains(attName)) {
                        tb.remove(attName);
                    }
                }
                return tb.buildFeatureType();
            } else {
                // if it's a complex type, we don't have a type builder on all branches, so
                // we'll run an empty query instead

                query.setFilter(Filter.EXCLUDE);
                FeatureSource fs = getFeatureSource(null, null);
                FeatureCollection fc = fs.getFeatures(query);
                return fc.getSchema();
            }
        } else {
            throw new IllegalArgumentException(
                    "SecureFeatureSources has been fed "
                            + "with unexpected AccessLimits class "
                            + policy.getLimits().getClass());
        }
    }

    // --------------------------------------------------------------------------
    // WRAPPED METHODS TO ENFORCE SECURITY POLICY
    // --------------------------------------------------------------------------

    @Override
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(
            ProgressListener listener, Hints hints) throws IOException {
        final FeatureSource<? extends FeatureType, ? extends Feature> fs =
                delegate.getFeatureSource(listener, hints);
        Request request = Dispatcher.REQUEST.get();
        if (policy.level == AccessLevel.METADATA && !isGetCapabilities(request)) {
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        } else {
            return SecuredObjects.secure(fs, computeWrapperPolicy(request));
        }
    }

    /**
     * Checks if current request is GetCapabilities and returns a new WrapperPolicy with attributes
     * read allowed to compute the dimensions values. If current request is not a GetCapabilities
     * one, returns the same policy without changes.
     */
    private WrapperPolicy computeWrapperPolicy(Request request) {
        if (isGetCapabilities(request) && policy.getLimits() instanceof VectorAccessLimits) {
            VectorAccessLimits accessLimits = (VectorAccessLimits) policy.getLimits();
            VectorAccessLimits newLimits =
                    new VectorAccessLimits(
                            accessLimits.getMode(),
                            null,
                            Filter.INCLUDE,
                            accessLimits.getWriteAttributes(),
                            accessLimits.getWriteFilter());
            WrapperPolicy newPolicy = WrapperPolicy.readOnlyChallenge(newLimits);
            return newPolicy;
        }
        return policy;
    }

    /** Returns true only if current request is a GetCapabilities, otherwise returns false. */
    private boolean isGetCapabilities(Request request) {
        if (request == null) return false;
        return GET_CAPABILITIES.equalsIgnoreCase(request.getRequest());
    }

    @Override
    public DataStoreInfo getStore() {
        return SecuredObjects.secure(delegate.getStore(), policy);
    }

    @Override
    public void setStore(StoreInfo store) {
        // need to make sure the store isn't secured
        super.setStore((StoreInfo) SecureCatalogImpl.unwrap(store));
    }
}
