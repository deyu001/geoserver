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

package org.geoserver.feature;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.util.factory.FactoryRegistryException;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;

/**
 * Decorating feature collection which reprojects feature geometries to a particular coordinate
 * reference system on the fly.
 *
 * <p>The coordinate reference system of feature geometries is looked up using {@link
 * org.locationtech.jts.geom.Geometry#getUserData()}.
 *
 * <p>The {@link #defaultSource} attribute can be set to specify a coordinate refernence system to
 * transform from when one is not specified by teh geometry itself. Leaving the property null
 * specifies that the geometry will not be transformed.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class ReprojectingFeatureCollection extends DecoratingSimpleFeatureCollection {
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2(null);

    /** The schema of reprojected features */
    SimpleFeatureType schema;

    /** The target coordinate reference system */
    CoordinateReferenceSystem target;

    /** Coordinate reference system to use when one is not specified on an encountered geometry. */
    CoordinateReferenceSystem defaultSource;

    /** MathTransform cache, keyed by source CRS */
    Map<CoordinateReferenceSystem, GeometryCoordinateSequenceTransformer> transformers;

    /** Transformation hints */
    Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

    public ReprojectingFeatureCollection(
            SimpleFeatureCollection delegate, CoordinateReferenceSystem target)
            throws SchemaException, OperationNotFoundException, FactoryRegistryException,
                    FactoryException {
        super(delegate);

        this.target = target;
        this.schema = FeatureTypes.transform(delegate.getSchema(), target);

        // create transform cache
        transformers = new HashMap<>();

        // cache "default" transform
        CoordinateReferenceSystem source = delegate.getSchema().getCoordinateReferenceSystem();

        if (source != null) {
            MathTransform tx =
                    ReferencingFactoryFinder.getCoordinateOperationFactory(hints)
                            .createOperation(source, target)
                            .getMathTransform();

            GeometryCoordinateSequenceTransformer transformer =
                    new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(tx);
            transformers.put(source, transformer);
        } else {
            throw new RuntimeException(
                    "Source was null in trying to create a reprojected feature collection!");
        }
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) {
        try (SimpleFeatureIterator it = features()) {
            while (it.hasNext()) {
                visitor.visit(it.next());
            }
        }
    }

    public void setDefaultSource(CoordinateReferenceSystem defaultSource) {
        this.defaultSource = defaultSource;
    }

    public SimpleFeatureIterator features() {
        return new ReprojectingFeatureIterator(delegate.features());
    }

    public SimpleFeatureType getFeatureType() {
        return schema;
    }

    public SimpleFeatureType getSchema() {
        return schema;
    }

    public SimpleFeatureCollection subCollection(Filter filter) {
        // reproject the filter to the delegate native crs
        CoordinateReferenceSystem crs = getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem crsDelegate = delegate.getSchema().getCoordinateReferenceSystem();
        if (crs != null) {
            DefaultCRSFilterVisitor defaulter = new DefaultCRSFilterVisitor(FF, crs);
            filter = (Filter) filter.accept(defaulter, null);
            if (crsDelegate != null && !CRS.equalsIgnoreMetadata(crs, crsDelegate)) {
                ReprojectingFilterVisitor reprojector =
                        new ReprojectingFilterVisitor(FF, delegate.getSchema());
                filter = (Filter) filter.accept(reprojector, null);
            }
        }

        SimpleFeatureCollection sub = delegate.subCollection(filter);

        if (sub != null) {
            try {
                ReprojectingFeatureCollection wrapper =
                        new ReprojectingFeatureCollection(sub, target);
                wrapper.setDefaultSource(defaultSource);

                return wrapper;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    public Object[] toArray() {
        Object[] array = delegate.toArray();

        for (int i = 0; i < array.length; i++) {
            try {
                array[i] = reproject((SimpleFeature) array[i]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return array;
    }

    public <F> F[] toArray(F[] a) {
        F[] array = delegate.toArray(a);

        for (int i = 0; i < array.length; i++) {
            try {
                @SuppressWarnings("unchecked")
                F cast = (F) reproject((SimpleFeature) array[i]);
                array[i] = cast;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return array;
    }

    public ReferencedEnvelope getBounds() {
        ReferencedEnvelope bounds = null;

        try (SimpleFeatureIterator i = features()) {
            if (!i.hasNext()) {
                bounds = new ReferencedEnvelope();
                bounds.setToNull();

            } else {
                SimpleFeature first = i.next();
                bounds = new ReferencedEnvelope(first.getBounds());
            }

            while (i.hasNext()) {
                SimpleFeature f = i.next();
                bounds.include(f.getBounds());
            }

            return bounds;
        }
    }

    public SimpleFeatureCollection collection() throws IOException {
        return this;
    }

    SimpleFeature reproject(SimpleFeature feature) throws IOException {
        Object[] attributes = new Object[schema.getAttributeCount()];

        for (int i = 0; i < attributes.length; i++) {
            AttributeDescriptor type = schema.getDescriptor(i);
            Object object = feature.getAttribute(type.getName());

            if (object instanceof Geometry) {
                // check for crs
                Geometry geometry = (Geometry) object;
                CoordinateReferenceSystem crs = (CoordinateReferenceSystem) geometry.getUserData();

                if (crs == null) {
                    // no crs specified on geometry, check default
                    if (defaultSource != null) {
                        crs = defaultSource;
                    }
                }

                if (crs != null) {
                    // if equal, nothing to do
                    if (!crs.equals(target)) {
                        GeometryCoordinateSequenceTransformer transformer = transformers.get(crs);

                        if (transformer == null) {
                            transformer = new GeometryCoordinateSequenceTransformer();

                            MathTransform2D tx;

                            try {
                                tx =
                                        (MathTransform2D)
                                                ReferencingFactoryFinder
                                                        .getCoordinateOperationFactory(hints)
                                                        .createOperation(crs, target)
                                                        .getMathTransform();
                            } catch (Exception e) {
                                String msg = "Could not transform for crs: " + crs;
                                throw (IOException) new IOException(msg).initCause(e);
                            }

                            transformer.setMathTransform(tx);
                            transformers.put(crs, transformer);
                        }

                        // do the transformation
                        try {
                            object = transformer.transform(geometry);
                        } catch (TransformException e) {
                            String msg = "Error occured transforming " + geometry.toString();
                            throw (IOException) new IOException(msg).initCause(e);
                        }
                    }
                }
            }

            attributes[i] = object;
        }

        try {
            SimpleFeature f = SimpleFeatureBuilder.build(schema, attributes, feature.getID());
            // copy over the user data from original
            f.getUserData().putAll(feature.getUserData());
            return f;
        } catch (IllegalAttributeException e) {
            String msg = "Error creating reprojeced feature";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    class ReprojectingFeatureIterator implements SimpleFeatureIterator {
        SimpleFeatureIterator delegate;

        public ReprojectingFeatureIterator(SimpleFeatureIterator delegate) {
            this.delegate = delegate;
        }

        public SimpleFeatureIterator getDelegate() {
            return delegate;
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature feature = delegate.next();

            try {
                return reproject(feature);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void close() {
            if (delegate != null) delegate.close();
            delegate = null;
        }
    }

    class ReprojectingIterator implements Iterator<SimpleFeature> {
        Iterator<SimpleFeature> delegate;

        public ReprojectingIterator(Iterator<SimpleFeature> delegate) {
            this.delegate = delegate;
        }

        public Iterator<SimpleFeature> getDelegate() {
            return delegate;
        }

        public void remove() {
            delegate.remove();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() {
            SimpleFeature feature = delegate.next();

            try {
                return reproject(feature);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
