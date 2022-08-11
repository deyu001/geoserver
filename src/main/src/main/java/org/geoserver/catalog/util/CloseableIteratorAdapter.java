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

package org.geoserver.catalog.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.io.Closeables;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

public class CloseableIteratorAdapter<T> implements CloseableIterator<T> {

    private static final Logger LOGGER = Logging.getLogger(CloseableIteratorAdapter.class);

    protected final Iterator<T> wrapped;

    protected Closeable whatToClose;

    public CloseableIteratorAdapter(Iterator<T> wrapped) {
        this.wrapped = wrapped;
        if (wrapped instanceof Closeable) {
            this.whatToClose = (Closeable) wrapped;
        } else {
            this.whatToClose = null;
        }
    }

    public CloseableIteratorAdapter(Iterator<T> filteredNotCloseable, Closeable closeMe) {
        this.wrapped = filteredNotCloseable;
        this.whatToClose = closeMe;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = wrapped.hasNext();
        if (!hasNext) {
            // auto close
            close();
        }
        return hasNext;
    }

    @Override
    public T next() {
        return wrapped.next();
    }

    @Override
    public void remove() {
        wrapped.remove();
    }

    /**
     * Closes the wrapped iterator if its an instance of {@code CloseableIterator}, does nothing
     * otherwise; override if needed.
     *
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() {
        try {
            Closeables.close(whatToClose, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            whatToClose = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation") // finalize is deprecated in Java 9
    protected void finalize() {
        if (whatToClose != null) {
            try {
                close();
            } finally {
                LOGGER.warning(
                        "There is code not closing CloseableIterator!!! Auto closing at finalize().");
            }
        }
    }

    public static <T> CloseableIterator<T> filter(final Iterator<T> iterator, final Filter filter) {

        Predicate<T> predicate = filterAdapter(filter);
        return filter(iterator, predicate);
    }

    public static <T> CloseableIterator<T> filter(
            final Iterator<T> iterator, final Predicate<T> predicate) {

        UnmodifiableIterator<T> filteredNotCloseable = Iterators.filter(iterator, predicate);
        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        Closeable closeable = iterator instanceof Closeable ? (Closeable) iterator : null;
        return new CloseableIteratorAdapter<>(filteredNotCloseable, closeable);
    }

    public static <F, T> CloseableIterator<T> transform(
            Iterator<F> iterator, Function<? super F, ? extends T> function) {

        Iterator<T> transformedNotCloseable = Iterators.transform(iterator, function);
        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        Closeable closeable = (Closeable) (iterator instanceof CloseableIterator ? iterator : null);
        return new CloseableIteratorAdapter<>(transformedNotCloseable, closeable);
    }

    public static <T> CloseableIterator<T> limit(final Iterator<T> iterator, int maxElements) {

        Iterator<T> limitedNotCloseable = Iterators.limit(iterator, maxElements);
        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        Closeable closeable = iterator instanceof Closeable ? (Closeable) iterator : null;
        return new CloseableIteratorAdapter<>(limitedNotCloseable, closeable);
    }

    public static void close(Iterator<?> iterator) {
        if (iterator instanceof Closeable) {
            try {
                Closeables.close((Closeable) iterator, false);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Ignoring exception on CloseableIteratorAdapter.close()", e);
            }
        }
    }

    public static <T> CloseableIterator<T> empty() {
        Iterator<T> empty = Collections.emptyIterator();
        return new CloseableIteratorAdapter<>(empty);
    }

    private static <T> com.google.common.base.Predicate<T> filterAdapter(
            final Filter catalogPredicate) {

        return new com.google.common.base.Predicate<T>() {

            @Override
            public boolean apply(T input) {
                return catalogPredicate.evaluate(input);
            }
        };
    }
}
