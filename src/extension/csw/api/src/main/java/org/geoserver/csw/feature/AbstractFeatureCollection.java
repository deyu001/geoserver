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

package org.geoserver.csw.feature;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DelegateFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * A derivation of GeoTools {@link org.geotools.feature.collection.AbstractFeatureCollection} that
 * works on top of complex features
 *
 * @author Jody Garnett (Refractions Research Inc)
 * @author Andrea Aime - GeoSolutions
 * @source $URL$
 */
public abstract class AbstractFeatureCollection<T extends FeatureType, F extends Feature>
        implements FeatureCollection<T, F> {

    /** id used when serialized to gml */
    protected String id;

    protected T schema;

    protected AbstractFeatureCollection(T memberType) {
        this.id = id == null ? "featureCollection" : id;
        this.schema = memberType;
    }

    //
    // FeatureCollection - Feature Access
    //
    @SuppressWarnings("unchecked")
    public FeatureIterator<F> features() {
        FeatureIterator iter = new DelegateFeatureIterator(openIterator());
        getOpenIterators().add(iter);
        return iter;
    }

    /**
     * Clean up after any resources associated with this iteartor in a manner similar to JDO
     * collections. Example (safe) use:
     *
     * <pre>
     * <code>
     * Iterator iterator = collection.iterator();
     * try {
     *     for( Iterator i=collection.iterator(); i.hasNext();){
     *          Feature feature = (Feature) i.hasNext();
     *          System.out.println( feature.getID() );
     *     }
     * }
     * finally {
     *     collection.close( iterator );
     * }
     * </code>
     * </pre>
     *
     * @param close iterator to close
     */
    @SuppressWarnings("unchecked")
    public final void close(Iterator close) {
        if (close == null) return;
        try {
            closeIterator(close);
        } catch (Throwable e) {
            // TODO Log e = ln
        } finally {
            open.remove(close);
        }
    }

    public void close(FeatureIterator<F> close) {
        if (close != null) {
            close.close();
        }
    }

    /**
     * Open a resource based Iterator, we will call close( iterator ).
     *
     * <p>Please subclass to provide your own iterator for the the ResourceCollection, note <code>
     * iterator()</code> is implemented to call <code>open()</code> and track the results in for
     * later <code>purge()</code>.
     *
     * @return Iterator based on resource use
     */
    protected abstract Iterator<F> openIterator();

    /**
     * Please override to cleanup after your own iterators, and any used resources.
     *
     * <p>As an example if the iterator was working off a File then the inputstream should be
     * closed.
     *
     * <p>Subclass must call super.close( close ) to allow the list of open iterators to be
     * adjusted.
     *
     * @param close Iterator, will not be <code>null</code>
     */
    protected abstract void closeIterator(Iterator<F> close);

    /**
     * Close any outstanding resources released by this resources.
     *
     * <p>This method should be used with great caution, it is however available to allow the use of
     * the ResourceCollection with algorthims that are unaware of the need to close iterators after
     * use.
     *
     * <p>Example of using a normal Collections utility method:
     *
     * <pre>
     * <code>
     * Collections.sort( collection );
     * collection.purge();
     * </code>
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public void purge() {
        for (Iterator i = open.iterator(); i.hasNext(); ) {
            Object resource = i.next();
            if (resource instanceof Iterator) {
                Iterator resourceIterator = (Iterator) resource;
                try {
                    closeIterator(resourceIterator);
                } catch (Throwable e) {
                    // TODO: Log e = ln
                } finally {
                    i.remove();
                }
            }
        }
    }

    /**
     * Removes all of the elements from this collection (optional operation).
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> method is not supported by this
     *     collection.
     */
    public void clear() {
        Iterator<F> e = iterator();
        try {
            while (e.hasNext()) {
                e.next();
                e.remove();
            }
        } finally {
            close(e);
        }
    }

    /**
     * Returns <tt>true</tt> if this collection contains the specified element. <tt></tt>.
     *
     * <p>This implementation iterates over the elements in the collection, checking each element in
     * turn for equality with the specified element.
     *
     * @param o object to be checked for containment in this collection.
     * @return <tt>true</tt> if this collection contains the specified element.
     */
    public boolean contains(Object o) {
        Iterator<F> e = null;
        try {
            e = iterator();
            if (o == null) {
                while (e.hasNext()) if (e.next() == null) return true;
            } else {
                while (e.hasNext()) if (o.equals(e.next())) return true;
            }
            return false;
        } finally {
            close(e);
        }
    }

    /**
     * Returns <tt>true</tt> if this collection contains all of the elements in the specified
     * collection.
     *
     * <p>
     *
     * @param c collection to be checked for containment in this collection.
     * @return <tt>true</tt> if this collection contains all of the elements in the specified
     *     collection.
     * @throws NullPointerException if the specified collection is null.
     * @see #contains(Object)
     */
    public boolean containsAll(Collection<?> c) {
        Iterator<?> e = c.iterator();
        try {
            while (e.hasNext()) if (!contains(e.next())) return false;
            return true;
        } finally {
            close(e);
        }
    }

    //
    // Contents
    //
    //
    /** Set of open resource iterators */
    @SuppressWarnings("unchecked")
    protected final Set open = new HashSet<>();

    /**
     * Returns the set of open iterators.
     *
     * <p>Contents are a mix of Iterator<F> and FeatureIterator
     */
    @SuppressWarnings("unchecked")
    public final Set getOpenIterators() {
        return open;
    }

    /**
     * Please implement!
     *
     * <p>Note: If you return a ResourceIterator, the default implemntation of close( Iterator )
     * will know what to do.
     */
    @SuppressWarnings("unchecked")
    public final Iterator<F> iterator() {
        Iterator<F> iterator = openIterator();
        getOpenIterators().add(iterator);
        return iterator;
    }

    /** @return <tt>true</tt> if this collection contains no elements. */
    public boolean isEmpty() {
        Iterator<F> iterator = iterator();
        try {
            return !iterator.hasNext();
        } finally {
            close(iterator);
        }
    }

    /**
     * Array of all the elements.
     *
     * @return an array containing all of the elements in this collection.
     */
    public Object[] toArray() {
        Object[] result = new Object[size()];
        Iterator<F> e = null;
        try {
            e = iterator();
            for (int i = 0; e.hasNext(); i++) result[i] = e.next();
            return result;
        } finally {
            close(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T2> T2[] toArray(T2[] a) {
        int size = size();
        if (a.length < size) {
            a = (T2[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        Iterator<F> it = iterator();
        try {

            Object[] result = a;
            for (int i = 0; i < size; i++) result[i] = it.next();
            if (a.length > size) a[size] = null;
            return a;
        } finally {
            close(it);
        }
    }

    public void accepts(
            org.opengis.feature.FeatureVisitor visitor,
            org.opengis.util.ProgressListener progress) {
        Iterator<F> iterator = null;
        if (progress == null) progress = new NullProgressListener();
        try {
            float size = size();
            float position = 0;
            progress.started();
            for (iterator = iterator(); !progress.isCanceled() && iterator.hasNext(); ) {
                if (size > 0) progress.progress(position++ / size);
                try {
                    Feature feature = iterator.next();
                    visitor.visit(feature);
                } catch (Exception erp) {
                    progress.exceptionOccurred(erp);
                }
            }
        } finally {
            progress.complete();
            close(iterator);
        }
    }

    public String getID() {
        return id;
    }

    public T getSchema() {
        return schema;
    }

    @Override
    public int size() {
        FeatureIterator<F> fi = null;
        int count = 0;
        try {
            fi = features();
            while (fi.hasNext()) {
                fi.next();
                count++;
            }
        } finally {
            if (fi != null) {
                fi.close();
            }
        }

        return count;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        FeatureIterator<F> fi = null;
        ReferencedEnvelope bounds = null;
        try {
            fi = features();
            while (fi.hasNext()) {
                Feature f = fi.next();
                ReferencedEnvelope re = ReferencedEnvelope.reference(f.getBounds());
                if (bounds == null) {
                    bounds = re;
                } else {
                    bounds.expandToInclude(re);
                }
            }
        } finally {
            if (fi != null) {
                fi.close();
            }
        }

        return bounds;
    }
}
