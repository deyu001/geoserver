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

package org.geoserver.importer;

import java.util.NoSuchElementException;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geotools.data.DataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.DecoratingFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

/**
 * FeatureCollection that does two things required by the importer; a) provide cancel functionality
 * b) Do some FeatureType Transforming
 *
 * <p>This class is simply wraps the FeatureIterator with two iterators wrappers that provide the
 * above functionality.
 */
class ImportTransformFeatureCollection<T extends FeatureType, F extends Feature>
        extends DecoratingFeatureCollection<T, F> {

    ProgressMonitor monitor;

    FeatureDataConverter featureDataConverter;

    FeatureType resultingFT;

    VectorTransformChain vectorTransformChain;

    ImportTask task;

    DataStore dataStoreDestination;

    public ImportTransformFeatureCollection(
            FeatureCollection<T, F> fc,
            FeatureDataConverter featureDataConverter,
            FeatureType resultingFT,
            VectorTransformChain vectorTransformChain,
            ImportTask task,
            DataStore dataStoreDestination) {
        super(fc);
        this.monitor = task.progress();
        this.featureDataConverter = featureDataConverter;
        this.resultingFT = resultingFT;
        this.vectorTransformChain = vectorTransformChain;
        this.task = task;
        this.dataStoreDestination = dataStoreDestination;
    }

    @Override
    public FeatureIterator<F> features() {
        return new TransformingFeatureIterator<>(
                new CancelableFeatureIterator<>(super.features(), monitor),
                resultingFT,
                featureDataConverter,
                vectorTransformChain,
                task,
                dataStoreDestination);
    }

    /**
     * Simple FeatureIterator that does some transforming of the features.
     *
     * <p>The emulates the behavior of the Importer's low-level feature transformation.
     */
    private class TransformingFeatureIterator<F extends Feature>
            extends DecoratingFeatureIterator<F> {

        SimpleFeatureBuilder featureBuilder;

        FeatureDataConverter featureDataConverter;

        VectorTransformChain vectorTransformChain;

        ImportTask task;

        DataStore dataStore;

        int cnt = 0;

        public TransformingFeatureIterator(
                FeatureIterator<F> fi,
                FeatureType resultingFT,
                FeatureDataConverter featureDataConverter,
                VectorTransformChain vectorTransformChain,
                ImportTask task,
                DataStore dataStore) {
            super(fi);
            this.featureBuilder = new SimpleFeatureBuilder((SimpleFeatureType) resultingFT);
            this.featureDataConverter = featureDataConverter;
            this.vectorTransformChain = vectorTransformChain;
            this.task = task;
            this.dataStore = dataStore;
        }

        @Override
        public F next() throws NoSuchElementException {
            // the xform could produce null features - we eat them
            while (super.hasNext()) {
                F result = attemptNext();
                if (result != null) {
                    return result;
                }
            }
            throw new NoSuchElementException();
        }

        /* for details, see the low-level api version in the Importer */
        private F attemptNext() {
            SimpleFeature input = (SimpleFeature) super.next();
            SimpleFeature result = featureBuilder.buildFeature(null);
            featureDataConverter.convert(input, result);

            // @hack #45678 - mask empty geometry or postgis will complain
            Geometry geom = (Geometry) result.getDefaultGeometry();
            if (geom != null && geom.isEmpty()) {
                result.setDefaultGeometry(null);
            }

            try {
                result = vectorTransformChain.inline(task, dataStore, input, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            task.setNumberProcessed(++cnt);

            // The above only works with simple features, even if the rest pretends to be generic
            @SuppressWarnings("unchecked")
            F f = (F) result;
            return f;
        }
    }

    /**
     * Simple FeatureIterator that will handle canceling. If the monitor cancels, the iterator will
     * say there are no more elementss (hasNext() will be false)
     */
    private class CancelableFeatureIterator<F extends Feature>
            extends DecoratingFeatureIterator<F> {
        ProgressMonitor monitor;

        public CancelableFeatureIterator(FeatureIterator<F> fi, ProgressMonitor monitor) {
            super(fi);
            this.monitor = monitor;
        }

        /** if cancelled, then report no more features */
        @Override
        public boolean hasNext() {
            if (monitor.isCanceled()) {
                return false;
            }
            return super.hasNext();
        }
    }
}
