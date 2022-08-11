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
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Given a {@link DataStore} subclass makes sure no write operations can be performed through it.
 * Regardless of the policy the store is kept read only as services are supposed to perform writes
 * via {@link FeatureStore} instances returned by {@link FeatureTypeInfo} and not via direct data
 * store access.
 *
 * @author Andrea Aime - TOPP
 */
public class ReadOnlyDataStore extends org.geotools.data.store.DecoratingDataStore {

    WrapperPolicy policy;

    protected ReadOnlyDataStore(DataStore delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        final SimpleFeatureSource fs = super.getFeatureSource(typeName);
        return wrapFeatureSource(fs);
    }

    @Override
    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        final SimpleFeatureSource fs = super.getFeatureSource(typeName);
        return wrapFeatureSource(fs);
    }

    @SuppressWarnings("unchecked")
    SimpleFeatureSource wrapFeatureSource(final SimpleFeatureSource fs) {
        if (fs == null) return null;

        WrapperPolicy childPolicy = buildPolicyForFeatureSource();
        return DataUtilities.simple(SecuredObjects.secure(fs, childPolicy));
    }

    private WrapperPolicy buildPolicyForFeatureSource() {
        WrapperPolicy childPolicy;
        if (policy.getLimits() instanceof VectorAccessLimits) {
            childPolicy = policy;
        } else {
            final AccessLimits limits = policy.getLimits();
            VectorAccessLimits vectorLimits =
                    new VectorAccessLimits(
                            limits.getMode(), null, Filter.INCLUDE, null, Filter.EXCLUDE);
            childPolicy = this.policy.derive(vectorLimits);
        }
        return childPolicy;
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Filter filter, Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(
            String typeName, Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void removeSchema(String typeName) throws IOException {
        throw notifyUnsupportedOperation();
    }

    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link
     * UnsupportedOperationException} in case we have to conceal the fact the data is actually
     * writable, using an Spring security exception otherwise to force an authentication from the
     * user
     */
    protected RuntimeException notifyUnsupportedOperation() {
        if (policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess();
        } else
            return new UnsupportedOperationException(
                    "This datastore is read only, service code is supposed to perform writes via FeatureStore instead");
    }
}
