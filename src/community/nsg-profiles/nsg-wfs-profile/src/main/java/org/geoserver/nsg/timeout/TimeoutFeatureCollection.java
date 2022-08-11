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

package org.geoserver.nsg.timeout;

import java.io.IOException;
import java.util.Collection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

/**
 * A {@link FeatureCollection} decorator that checks if the timeout expired, and throws an exception
 * in case it is
 *
 * @param <T>
 * @param <F>
 */
class TimeoutFeatureCollection<T extends FeatureType, F extends Feature>
        implements FeatureCollection<T, F> {

    /**
     * Wraps a feature collection into a timing out decorator, keeping its {@link SimpleFeature}
     * nature if possible
     *
     * @param <R>
     */
    public static FeatureCollection wrap(TimeoutVerifier timeoutVerifier, FeatureCollection fc) {
        if (fc instanceof SimpleFeatureCollection) {
            return new SimpleTimeoutCollection(timeoutVerifier, fc);
        } else {
            return new TimeoutFeatureCollection<>(timeoutVerifier, fc);
        }
    }

    /** Simple feature version of {@link TimeoutFeatureCollection} */
    static class SimpleTimeoutCollection
            extends TimeoutFeatureCollection<SimpleFeatureType, SimpleFeature>
            implements SimpleFeatureCollection {

        public SimpleTimeoutCollection(
                TimeoutVerifier timeoutVerifier,
                FeatureCollection<SimpleFeatureType, SimpleFeature> delegate) {
            super(timeoutVerifier, delegate);
        }

        @Override
        public SimpleFeatureIterator features() {
            return new TimeoutFeatureIterator.SimpleTimeoutFeatureIterator(
                    timeoutVerifier, super.features());
        }

        @Override
        public SimpleFeatureCollection subCollection(Filter filter) {
            timeoutVerifier.checkTimeout();
            return new SimpleTimeoutCollection(timeoutVerifier, delegate.subCollection(filter));
        }

        @Override
        public SimpleFeatureCollection sort(SortBy order) {
            return new SimpleTimeoutCollection(timeoutVerifier, delegate.sort(order));
        }
    }

    TimeoutVerifier timeoutVerifier;
    FeatureCollection<T, F> delegate;

    public TimeoutFeatureCollection(
            TimeoutVerifier timeoutVerifier, FeatureCollection<T, F> delegate) {
        this.timeoutVerifier = timeoutVerifier;
        this.delegate = delegate;
    }

    // Timeout delegate creating methods

    @Override
    public FeatureIterator<F> features() {
        timeoutVerifier.checkTimeout();
        return new TimeoutFeatureIterator<>(timeoutVerifier, delegate.features());
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        timeoutVerifier.checkTimeout();
        TimeoutFeatureVisitor timeoutVisitor = new TimeoutFeatureVisitor(timeoutVerifier, visitor);
        delegate.accepts(timeoutVisitor, progress);
    }

    @Override
    public FeatureCollection<T, F> subCollection(Filter filter) {
        timeoutVerifier.checkTimeout();
        return new TimeoutFeatureCollection<>(timeoutVerifier, delegate.subCollection(filter));
    }

    @Override
    public FeatureCollection<T, F> sort(SortBy order) {
        timeoutVerifier.checkTimeout();
        return new TimeoutFeatureCollection<>(timeoutVerifier, delegate.sort(order));
    }

    // Simple check and delegate methods

    @Override
    public T getSchema() {
        timeoutVerifier.checkTimeout();
        return delegate.getSchema();
    }

    @Override
    public String getID() {
        timeoutVerifier.checkTimeout();
        return delegate.getID();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        timeoutVerifier.checkTimeout();
        return delegate.getBounds();
    }

    @Override
    public boolean contains(Object o) {
        timeoutVerifier.checkTimeout();
        return delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        timeoutVerifier.checkTimeout();
        return delegate.containsAll(o);
    }

    @Override
    public boolean isEmpty() {
        timeoutVerifier.checkTimeout();
        return delegate.isEmpty();
    }

    @Override
    public int size() {
        timeoutVerifier.checkTimeout();
        return delegate.size();
    }

    @Override
    public Object[] toArray() {
        timeoutVerifier.checkTimeout();
        return delegate.toArray();
    }

    @Override
    public <O> O[] toArray(O[] a) {
        timeoutVerifier.checkTimeout();
        return delegate.toArray(a);
    }
}
