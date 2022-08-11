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

package org.geoserver.template;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateSequenceModel;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;

/**
 * Create FeatureCollection Template Model without copying features to memory When using this in a
 * FeatureWrapper, it is necessary to call purge() method after processing template, to close any
 * open database connections
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class DirectTemplateFeatureCollectionFactory
        implements FeatureWrapper.TemplateFeatureCollectionFactory<
                DirectTemplateFeatureCollectionFactory.TemplateFeatureCollection> {

    static Logger LOGGER = Logging.getLogger(DirectTemplateFeatureCollectionFactory.class);

    /** thread local to track open iterators */
    static ThreadLocal<List<TemplateFeatureIterator>> ITERATORS = new ThreadLocal<>();

    public void purge() {
        List<TemplateFeatureIterator> its = ITERATORS.get();
        if (its != null) {
            for (TemplateFeatureIterator it : its) {
                try {
                    it.close();
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, "Error closing iterator", t);
                }
            }
            its.clear();
            ITERATORS.remove();
        }
    }

    public DirectTemplateFeatureCollectionFactory() {}

    public TemplateCollectionModel createTemplateFeatureCollection(
            FeatureCollection collection, BeansWrapper wrapper) {
        return new TemplateFeatureCollection(collection, wrapper);
    }

    protected class TemplateFeatureCollection
            implements TemplateCollectionModel, TemplateSequenceModel {
        protected BeansWrapper wrapper;

        protected FeatureCollection collection;

        protected TemplateFeatureIterator indexIterator = null;

        protected int currentIndex = -1;

        protected TemplateModel currentItem = null;

        public TemplateFeatureCollection(FeatureCollection collection, BeansWrapper wrapper) {
            this.collection = collection;
            this.wrapper = wrapper;
        }

        public TemplateModelIterator iterator() throws TemplateModelException {
            TemplateFeatureIterator it =
                    new TemplateFeatureIterator(collection.features(), wrapper);
            List<TemplateFeatureIterator> open = ITERATORS.get();
            if (open == null) {
                open = new LinkedList<>();
                ITERATORS.set(open);
            }
            open.add(it);
            return it;
        }

        @Override
        public TemplateModel get(int index) throws TemplateModelException {
            if (currentIndex > index) {
                // we have gone backwards, close iterator and clean up as we will need to start over
                if (indexIterator != null) {
                    ITERATORS.get().remove(indexIterator);
                    try {
                        indexIterator.close();
                    } catch (Throwable t) {
                        LOGGER.log(Level.WARNING, "Error closing iterator", t);
                    }
                    indexIterator = null;
                }
                currentIndex = -1;
                currentItem = null;
            }
            if (indexIterator == null) {
                indexIterator = (TemplateFeatureIterator) iterator();
            }
            while (currentIndex < index && indexIterator.hasNext()) {
                // forward to correct index
                currentItem = indexIterator.next();
                currentIndex++;
            }
            return index == currentIndex ? currentItem : null;
        }

        @Override
        public int size() throws TemplateModelException {
            return collection.size();
        }
    }

    protected class TemplateFeatureIterator implements TemplateModelIterator {

        protected BeansWrapper wrapper;

        protected FeatureIterator iterator;

        public TemplateFeatureIterator(FeatureIterator iterator, BeansWrapper wrapper) {
            this.iterator = iterator;
            this.wrapper = wrapper;
        }

        public TemplateModel next() throws TemplateModelException {
            return wrapper.wrap(iterator.next());
        }

        public boolean hasNext() throws TemplateModelException {
            return iterator.hasNext();
        }

        public void close() {
            iterator.close();
        }
    }
}
