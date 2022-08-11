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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.geoserver.csw.feature.sort.ComplexComparatorFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * A fully in memory feature collection
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    protected ArrayList<Feature> features;

    public MemoryFeatureCollection(FeatureType memberType) {
        this(memberType, null);
    }

    public MemoryFeatureCollection(FeatureType memberType, List<Feature> features) {
        super(memberType);
        this.features = new ArrayList<>();
        if (features != null) {
            for (Feature f : features) {
                if (!f.getType().equals(memberType)) {
                    // TODO: handle inheritance
                    throw new IllegalArgumentException(
                            "Found a feature whose feature type is not equal to the declared one: "
                                    + f);
                }
                this.features.add(f);
            }
        }
    }

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        List<Feature> results = new ArrayList<>();
        for (Feature f : features) {
            if (filter.evaluate(f)) {
                results.add(f);
            }
        }

        return new MemoryFeatureCollection(getSchema(), results);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        List<Feature> results = new ArrayList<>(features);
        Comparator<Feature> comparator = ComplexComparatorFactory.buildComparator(order);
        Collections.sort(results, comparator);

        return new MemoryFeatureCollection(getSchema(), results);
    }

    @Override
    protected Iterator<Feature> openIterator() {
        return features.iterator();
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {
        // nothing to do
    }

    @Override
    public int size() {
        return features.size();
    }

    /**
     * Removes a single instance of the specified element from this collection, if it is present
     * (optional operation).
     *
     * @param o element to be removed from this collection, if present.
     * @return <tt>true</tt> if the collection contained the specified element.
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is not supported by this
     *     collection.
     */
    public boolean remove(Object o) {
        return features.remove(o);
    }

    /**
     * Removes from this collection all of its elements that are contained in the specified
     * collection (optional operation).
     *
     * <p>
     *
     * @param c elements to be removed from this collection.
     * @return <tt>true</tt> if this collection changed as a result of the call.
     * @throws UnsupportedOperationException if the <tt>removeAll</tt> method is not supported by
     *     this collection.
     * @throws NullPointerException if the specified collection is null.
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public final boolean removeAll(Collection<?> c) {
        return features.removeAll(c);
    }

    /**
     * Retains only the elements in this collection that are contained in the specified collection
     * (optional operation).
     *
     * @param c elements to be retained in this collection.
     * @return <tt>true</tt> if this collection changed as a result of the call.
     * @throws UnsupportedOperationException if the <tt>retainAll</tt> method is not supported by
     *     this Collection.
     * @throws NullPointerException if the specified collection is null.
     * @see #remove(Object)
     * @see #contains(Object)
     */
    public final boolean retainAll(Collection<?> c) {
        return features.removeAll(c);
    }

    /**
     * Implement to support modification.
     *
     * @param o element whose presence in this collection is to be ensured.
     * @return <tt>true</tt> if the collection changed as a result of the call.
     * @throws UnsupportedOperationException if the <tt>add</tt> method is not supported by this
     *     collection.
     * @throws NullPointerException if this collection does not permit <tt>null</tt> elements, and
     *     the specified element is <tt>null</tt>.
     * @throws ClassCastException if the class of the specified element prevents it from being added
     *     to this collection.
     * @throws IllegalArgumentException if some aspect of this element prevents it from being added
     *     to this collection.
     */
    public boolean add(Feature o) {
        return features.add(o);
    }

    /**
     * Adds all of the elements in the specified collection to this collection (optional operation).
     *
     * @param c collection whose elements are to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the call.
     * @throws UnsupportedOperationException if this collection does not support the <tt>addAll</tt>
     *     method.
     * @throws NullPointerException if the specified collection is null.
     * @see #add(Feature)
     */
    public boolean addAll(Collection<Feature> c) {
        return features.addAll(c);
    }

    public boolean addAll(FeatureCollection<FeatureType, Feature> c) {
        Feature[] array = c.toArray(new Feature[c.size()]);
        return features.addAll(Arrays.asList(array));
    }
}
