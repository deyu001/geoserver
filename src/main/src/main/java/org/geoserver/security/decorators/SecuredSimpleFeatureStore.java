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

import static org.geoserver.security.SecurityUtils.getWriteQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * The secure version of {@link SecuredFeatureStore}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredSimpleFeatureStore extends SecuredFeatureStore<SimpleFeatureType, SimpleFeature>
        implements SimpleFeatureStore {

    public SecuredSimpleFeatureStore(SimpleFeatureStore delegate, WrapperPolicy policy) {
        super(delegate, policy);
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return DataUtilities.simple(super.getFeatures());
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return DataUtilities.simple(super.getFeatures(filter));
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        return DataUtilities.simple(super.getFeatures(query));
    }

    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException {
        modifyFeatures(new String[] {name}, new Object[] {attributeValue}, filter);
    }

    public void modifyFeatures(String[] names, Object[] values, Filter filter) throws IOException {
        // are we limiting anything?
        Query writeQuery = getWriteQuery(policy);
        if (writeQuery == Query.ALL) {
            ((SimpleFeatureStore) storeDelegate).modifyFeatures(names, values, filter);
            return;
        } else if (writeQuery.getFilter() == Filter.EXCLUDE
                || writeQuery.getPropertyNames() == Query.NO_NAMES) {
            throw unsupportedOperation();
        }

        // get the mixed filter
        final Query local = new Query(null, filter);
        Query mixed = mixQueries(local, writeQuery);

        if (writeQuery.getPropertyNames() == Query.ALL_NAMES) {
            // it was just a matter of filtering.
            ((SimpleFeatureStore) storeDelegate).modifyFeatures(names, values, mixed.getFilter());
        } else {
            // get the writable attribute set
            Set<String> queryNames = new HashSet<>(Arrays.asList(writeQuery.getPropertyNames()));

            // check the update fields
            for (String name : names) {
                if (!queryNames.contains(name)) {
                    String typeName = getSchema().getName().getLocalPart();
                    if (policy.getResponse() == Response.CHALLENGE) {
                        throw SecureCatalogImpl.unauthorizedAccess(typeName);
                    } else {
                        throw new UnsupportedOperationException(
                                "Trying to write on the write protected attribute " + name);
                    }
                }
            }

            ((SimpleFeatureStore) storeDelegate).modifyFeatures(names, values, mixed.getFilter());
        }
    }
}
