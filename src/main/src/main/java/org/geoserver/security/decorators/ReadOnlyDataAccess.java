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
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Given a {@link DataAccess} subclass makes sure no write operations can be performed through it.
 * Regardless of the policy the data access is kept read only as services are supposed to perform
 * writes via {@link FeatureStore} instances returned by {@link FeatureTypeInfo} and not via direct
 * calls to data access.
 *
 * @author Andrea Aime - TOPP
 * @param <T>
 * @param <F>
 */
public class ReadOnlyDataAccess<T extends FeatureType, F extends Feature>
        extends DecoratingDataAccess<T, F> {

    static final String READ_ONLY = "This data access is read only";
    private WrapperPolicy policy;

    ReadOnlyDataAccess(DataAccess<T, F> delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public FeatureSource<T, F> getFeatureSource(Name typeName) throws IOException {
        final FeatureSource<T, F> fs = super.getFeatureSource(typeName);
        if (fs == null) return null;

        @SuppressWarnings("unchecked")
        FeatureSource<T, F> cast = SecuredObjects.secure(fs, policy);
        return cast;
    }

    @Override
    public void createSchema(T featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(Name typeName, T featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw notifyUnsupportedOperation();
    }
    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link
     * UnsupportedOperationException} in case we have to conceal the fact the data is actually
     * writable, using an Spring Security security exception otherwise to force an authentication
     * from the user
     */
    RuntimeException notifyUnsupportedOperation() {
        if (policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess();
        } else
            return new UnsupportedOperationException(
                    "This data access is read only, service code is supposed to perform writes via FeatureStore instead");
    }
}
