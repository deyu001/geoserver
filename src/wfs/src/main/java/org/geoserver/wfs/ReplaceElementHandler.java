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

package org.geoserver.wfs;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.request.Replace;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;

public class ReplaceElementHandler extends AbstractTransactionElementHandler {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    public ReplaceElementHandler(GeoServer geoServer) {
        super(geoServer);
    }

    public Class getElementClass() {
        return Replace.class;
    }

    public QName[] getTypeNames(TransactionRequest request, TransactionElement element)
            throws WFSTransactionException {
        Replace replace = (Replace) element;

        List<QName> typeNames = new ArrayList<>();

        List features = replace.getFeatures();
        if (!features.isEmpty()) {
            for (Object o : features) {
                SimpleFeature feature = (SimpleFeature) o;

                String name = feature.getFeatureType().getTypeName();
                String namespaceURI = feature.getFeatureType().getName().getNamespaceURI();

                typeNames.add(new QName(namespaceURI, name));
            }
        }

        return typeNames.toArray(new QName[typeNames.size()]);
    }

    public void checkValidity(TransactionElement element, Map featureTypeInfos)
            throws WFSTransactionException {
        if (!getInfo().getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_REPLACE)) {
            throw new WFSException(element, "Transaction REPLACE support is not enabled");
        }

        if (featureTypeInfos.size() != 1) {
            throw new WFSException(
                    element,
                    "Transaction REPLACE must only specify features from a"
                            + " single feature type");
        }
    }

    public void execute(
            TransactionElement element,
            TransactionRequest request,
            Map featureStores,
            TransactionResponse response,
            TransactionListener listener)
            throws WFSTransactionException {

        Replace replace = (Replace) element;

        @SuppressWarnings("unchecked")
        List<SimpleFeature> newFeatures = replace.getFeatures();
        SimpleFeatureStore featureStore =
                DataUtilities.simple((FeatureStore) featureStores.values().iterator().next());
        if (featureStore == null) {
            throw new WFSException(element, "Could not obtain feature store");
        }

        // ids of replaced features
        Collection<FeatureId> replaced = new ArrayList<>();

        try {
            SimpleFeatureCollection features = featureStore.getFeatures(replace.getFilter());
            if (newFeatures.size() != features.size()) {
                throw new WFSException(
                        element,
                        String.format(
                                "Specified filter matched %d features but " + "%d were supplied",
                                features.size(), newFeatures.size()));
            }

            // replace the features in order...
            // JD, TODO: a better mechanism for replace... this is sort of a hack based on a combo
            // of
            // feature ids and orders
            // may want to check if the store is making feature id's writable and attempt
            // to actually update the ID's

            // load all the existing features into memory
            Map<String, SimpleFeature> oldFeatures = new LinkedHashMap<>();

            try (SimpleFeatureIterator it = features.features()) {
                while (it.hasNext()) {
                    SimpleFeature f = it.next();
                    oldFeatures.put(f.getID(), f);
                }
            }

            // first pass update all the features that match by id
            List<SimpleFeature> leftovers = new ArrayList<>();

            for (SimpleFeature newFeature : newFeatures) {
                SimpleFeature oldFeature = oldFeatures.get(newFeature.getID());
                if (oldFeature == null) {
                    leftovers.add(newFeature);
                    continue;
                }

                // matching feature found, update it
                replace(oldFeature, newFeature, featureStore, oldFeatures, replaced);
            }

            // do left overs
            for (SimpleFeature newFeature : leftovers) {
                // grab the "next" old feature
                SimpleFeature oldFeature = oldFeatures.values().iterator().next();
                replace(oldFeature, newFeature, featureStore, oldFeatures, replaced);
            }
        } catch (IOException e) {
            throw new WFSException(element, "Transaction REPLACE failed", e);
        }

        response.setTotalReplaced(BigInteger.valueOf(replaced.size()));
        response.addReplacedFeatures(replace.getHandle(), replaced);
    }

    void replace(
            SimpleFeature oldFeature,
            SimpleFeature newFeature,
            SimpleFeatureStore featureStore,
            Map<String, SimpleFeature> oldFeatures,
            Collection<FeatureId> ids)
            throws IOException {
        String[] names = new String[oldFeature.getAttributeCount()];
        Object[] valus = new Object[names.length];

        int i = 0;
        for (AttributeDescriptor att : oldFeature.getType().getAttributeDescriptors()) {
            String name = att.getLocalName();
            Object valu = newFeature.getAttribute(name);

            names[i] = name;
            valus[i++] = valu;
        }

        FeatureId id = filterFactory.featureId(oldFeature.getID());
        featureStore.modifyFeatures(names, valus, filterFactory.id(Collections.singleton(id)));

        ids.add(id);
        oldFeatures.remove(oldFeature.getID());
    }
}
