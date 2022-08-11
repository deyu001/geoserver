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

package org.vfny.geoserver.global;

import java.io.IOException;
import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLocking;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 * GeoServer wrapper for backend Geotools2 DataStore.
 *
 * <p>Support FeatureSource decorator for FeatureTypeInfo that takes care of mapping the
 * FeatureTypeInfo's FeatureSource with the schema and definition query configured for it.
 *
 * <p>Because GeoServer requires that attributes always be returned in the same order we need a way
 * to smoothly inforce this. Could we use this class to do so? It would need to support writing and
 * locking though.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GeoServerFeatureLocking extends GeoServerFeatureStore implements SimpleFeatureLocking {
    /**
     * Creates a new DEFQueryFeatureLocking object.
     *
     * @param locking GeoTools2 FeatureSource
     * @param settings Settings for this store
     */
    GeoServerFeatureLocking(
            FeatureLocking<SimpleFeatureType, SimpleFeature> locking, Settings settings) {
        super(locking, settings);
    }

    @SuppressWarnings("unchecked")
    FeatureLocking<SimpleFeatureType, SimpleFeature> locking() {
        return (FeatureLocking<SimpleFeatureType, SimpleFeature>) source;
    }

    /**
     * Description ...
     *
     * @see
     *     org.vfny.geoserver.global.GeoServerFeatureStore#setFeatureLock(org.geotools.data.FeatureLock)
     */
    @SuppressWarnings("unchecked")
    public void setFeatureLock(FeatureLock lock) {
        if (source instanceof FeatureLocking) {
            ((FeatureLocking<SimpleFeatureType, SimpleFeature>) source).setFeatureLock(lock);
        } else {
            throw new UnsupportedOperationException("FeatureTypeConfig does not supports locking");
        }
    }

    /** */
    @SuppressWarnings("unchecked")
    public int lockFeatures(Query query) throws IOException {
        if (source instanceof FeatureLocking) {
            return ((FeatureLocking<SimpleFeatureType, SimpleFeature>) source).lockFeatures(query);
        } else {
            throw new DataSourceException("FeatureTypeConfig does not supports locking");
        }
    }

    //    /**
    //     * A custom hack for PostgisFeatureLocking?
    //     *

    //     *

    //     *

    //     */
    //    public int lockFeature(Feature feature) throws IOException {
    //        if (source instanceof PostgisFeatureLocking) {
    //            return ((PostgisFeatureLocking) source).lockFeature(feature);
    //        }
    //
    //        throw new IOException("FeatureTypeConfig does not support single FeatureLock");
    //    }

    /** */
    public int lockFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        return locking().lockFeatures(filter);
    }

    /** */
    public int lockFeatures() throws IOException {
        return locking().lockFeatures();
    }

    /** */
    public void unLockFeatures() throws IOException {
        locking().lockFeatures();
    }

    /** */
    public void unLockFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        locking().unLockFeatures(filter);
    }

    public void unLockFeatures(Query query) throws IOException {
        query = makeDefinitionQuery(query, schema);

        locking().lockFeatures(query);
    }
}
