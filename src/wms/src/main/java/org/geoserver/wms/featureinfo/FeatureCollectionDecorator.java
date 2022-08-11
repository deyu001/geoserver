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

package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.util.Collection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

/**
 * This class is just a mean trick to smuggle in the resource name in to a FeatureCollection, when
 * returned as result of a GetFeatureInfo request Previously this was assumed to be equal to the
 * name of the type of the FeatureCollection But this is not the case for complex features in
 * app-schema.
 *
 * <p>The other thing this does is providing an implementation of size(), since the app-schema one
 * always returns 0. This is used for limiting features to a maximum.
 *
 * <p>The decorator never needs to be used for SimpleFeatureCollections.
 *
 * @author Niels Charlier, Curtin University of Technology
 */
@SuppressWarnings("unchecked")
public class FeatureCollectionDecorator implements FeatureCollection<FeatureType, Feature> {

    /**
     * Get Resource Name of a Feature Collection
     *
     * @param fc Feature Collection
     * @return Name of Resource
     */
    public static Name getName(FeatureCollection fc) {
        if (fc instanceof FeatureCollectionDecorator) {
            return ((FeatureCollectionDecorator) fc).getName();
        } else {
            return fc.getSchema().getName();
        }
    }

    protected FeatureCollection fc;
    protected Name name;

    public FeatureCollectionDecorator(Name name, FeatureCollection fc) {
        this.name = name;
        this.fc = fc;
    }

    public Name getName() {
        return name;
    }

    public FeatureIterator<Feature> features() {
        return (FeatureIterator<Feature>) fc.features();
    }

    public FeatureType getSchema() {
        return fc.getSchema();
    }

    public String getID() {
        return fc.getID();
    }

    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        fc.accepts(visitor, progress);
    }

    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return fc.subCollection(filter);
    }

    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        return fc.sort(order);
    }

    public ReferencedEnvelope getBounds() {
        return fc.getBounds();
    }

    public boolean contains(Object o) {
        return fc.contains(o);
    }

    public boolean containsAll(Collection<?> o) {
        return fc.containsAll(o);
    }

    public boolean isEmpty() {
        return fc.isEmpty();
    }

    public int size() {
        // overriding size implementation
        // simply counting!
        try (FeatureIterator iterator = features()) {
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                iterator.next();
            }
            return count;
        }
    }

    public Object[] toArray() {
        return fc.toArray();
    }

    public <O> O[] toArray(O[] a) {
        return (O[]) fc.toArray(a);
    }
}
