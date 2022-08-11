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
import java.util.NoSuchElementException;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReTypingFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

/**
 * FeatureCollection with "casts" features from on feature type to another.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class RetypingFeatureCollection extends DecoratingSimpleFeatureCollection {

    protected SimpleFeatureType target;

    public RetypingFeatureCollection(SimpleFeatureCollection delegate, SimpleFeatureType target) {
        super(delegate);
        this.target = target;
    }

    public SimpleFeatureType getSchema() {
        return target;
    }

    public SimpleFeatureIterator features() {
        return new RetypingIterator(delegate.features(), target);
    }

    @Override
    protected boolean canDelegate(FeatureVisitor visitor) {
        return ReTypingFeatureCollection.isTypeCompatible(visitor, target);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {

        SimpleFeatureCollection delegateCollection = delegate.subCollection(filter);

        return new ReTypingFeatureCollection(delegateCollection, target);
    }

    static SimpleFeature retype(SimpleFeature source, SimpleFeatureBuilder builder)
            throws IllegalAttributeException {
        SimpleFeatureType target = builder.getFeatureType();
        for (int i = 0; i < target.getAttributeCount(); i++) {
            AttributeDescriptor attributeType = target.getDescriptor(i);
            Object value = null;

            if (source.getFeatureType().getDescriptor(attributeType.getName()) != null) {
                value = source.getAttribute(attributeType.getName());
            }

            builder.add(value);
        }

        FeatureId id = reTypeId(source.getIdentifier(), source.getFeatureType(), target);
        SimpleFeature retyped = builder.buildFeature(id.getID());
        retyped.getUserData().putAll(source.getUserData());
        return retyped;
    }

    /**
     * Given a feature id following the <typename>.<internalId> convention, the original type and
     * the destination type, this converts the id from <original>.<internalid> to
     * <target>.<internalid>
     */
    public static FeatureId reTypeId(
            FeatureId sourceId, SimpleFeatureType original, SimpleFeatureType target) {
        final String originalTypeName = original.getName().getLocalPart();
        final String destTypeName = target.getName().getLocalPart();
        if (destTypeName.equals(originalTypeName)) return sourceId;

        final String prefix = originalTypeName + ".";
        if (sourceId.getID().startsWith(prefix)) {
            return new FeatureIdImpl(
                    destTypeName + "." + sourceId.getID().substring(prefix.length()));
        } else return sourceId;
    }

    public static class RetypingIterator implements SimpleFeatureIterator {
        protected SimpleFeatureBuilder builder;
        protected SimpleFeatureIterator delegate;

        public RetypingIterator(SimpleFeatureIterator delegate, SimpleFeatureType target) {
            this.delegate = delegate;
            this.builder = new SimpleFeatureBuilder(target);
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() {
            try {
                return RetypingFeatureCollection.retype(delegate.next(), builder);
            } catch (IllegalAttributeException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            delegate.close();
        }
    }

    public static class RetypingFeatureReader
            implements FeatureReader<SimpleFeatureType, SimpleFeature> {
        FeatureReader<SimpleFeatureType, SimpleFeature> delegate;
        SimpleFeatureBuilder builder;

        public RetypingFeatureReader(
                FeatureReader<SimpleFeatureType, SimpleFeature> delegate,
                SimpleFeatureType target) {
            this.delegate = delegate;
            this.builder = new SimpleFeatureBuilder(target);
        }

        public void close() throws IOException {
            delegate.close();
            delegate = null;
            builder = null;
        }

        public SimpleFeatureType getFeatureType() {
            return builder.getFeatureType();
        }

        public boolean hasNext() throws IOException {
            return delegate.hasNext();
        }

        public SimpleFeature next()
                throws IOException, IllegalAttributeException, NoSuchElementException {
            return RetypingFeatureCollection.retype(delegate.next(), builder);
        }
    }

    public static class RetypingFeatureWriter
            implements FeatureWriter<SimpleFeatureType, SimpleFeature> {
        FeatureWriter<SimpleFeatureType, SimpleFeature> delegate;

        SimpleFeatureBuilder builder;

        private SimpleFeature current;

        private SimpleFeature retyped;

        public RetypingFeatureWriter(
                FeatureWriter<SimpleFeatureType, SimpleFeature> delegate,
                SimpleFeatureType target) {
            this.delegate = delegate;
            this.builder = new SimpleFeatureBuilder(target);
        }

        public void close() throws IOException {
            delegate.close();
            delegate = null;
            builder = null;
        }

        public SimpleFeatureType getFeatureType() {
            return builder.getFeatureType();
        }

        public boolean hasNext() throws IOException {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws IOException {
            try {
                current = delegate.next();
                retyped = RetypingFeatureCollection.retype(current, builder);
                return retyped;
            } catch (IllegalAttributeException e) {
                throw (IOException)
                        new IOException("Error occurred while retyping feature").initCause(e);
            }
        }

        public void remove() throws IOException {
            delegate.write();
        }

        public void write() throws IOException {
            try {
                SimpleFeatureType target = getFeatureType();
                for (int i = 0; i < target.getAttributeCount(); i++) {
                    AttributeDescriptor at = target.getDescriptor(i);
                    Object value = retyped.getAttribute(i);
                    current.setAttribute(at.getLocalName(), value);
                }
                delegate.write();
            } catch (IllegalAttributeException e) {
                throw (IOException)
                        new IOException("Error occurred while retyping feature").initCause(e);
            }
        }
    }
}
