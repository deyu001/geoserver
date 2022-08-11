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

package org.geoserver.metadata.data.service.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexAttributeGenerator;
import org.geoserver.metadata.web.layer.MetadataTabPanel;
import org.geoserver.metadata.web.panel.GenerateDomainPanel;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

@org.springframework.stereotype.Component
public class DomainGenerator implements ComplexAttributeGenerator {

    private static final long serialVersionUID = 3179273148205046941L;

    private static final Logger LOGGER = Logging.getLogger(MetadataTabPanel.class);

    @Override
    public String getType() {
        return MetadataConstants.DOMAIN_TYPENAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void generate(
            AttributeConfiguration attributeConfiguration,
            ComplexMetadataMap metadata,
            LayerInfo layerInfo,
            Object data) {
        String attName =
                metadata.get(String.class, MetadataConstants.FEATURE_ATTRIBUTE_NAME).getValue();

        FeatureTypeInfo fti = (FeatureTypeInfo) layerInfo.getResource();

        // clear everything and build again
        metadata.delete(attributeConfiguration.getKey());
        try {
            Map<String, Object> map = (Map<String, Object>) data;
            if ((Boolean) map.get("method")) {
                Name tableName = (Name) map.get("tableName");
                Name valueAttributeName = (Name) map.get("valueAttributeName");
                Name defAttributeName = (Name) map.get("defAttributeName");
                DataAccess<? extends FeatureType, ? extends Feature> dataAccess =
                        getDataAccess(fti);
                if (dataAccess == null) {
                    return;
                }
                FeatureCollection<? extends FeatureType, ? extends Feature> features =
                        dataAccess.getFeatureSource(tableName).getFeatures(Filter.INCLUDE);
                AtomicInteger index = new AtomicInteger(0);
                features.accepts(
                        new FeatureVisitor() {
                            @Override
                            public void visit(Feature feature) {
                                Object value = feature.getProperty(valueAttributeName).getValue();
                                Object def = feature.getProperty(defAttributeName).getValue();
                                ComplexMetadataMap domainMap =
                                        metadata.subMap(
                                                attributeConfiguration.getKey(),
                                                index.getAndIncrement());
                                domainMap
                                        .get(String.class, MetadataConstants.DOMAIN_ATT_VALUE)
                                        .setValue(Converters.convert(value, String.class));
                                domainMap
                                        .get(String.class, MetadataConstants.DOMAIN_ATT_DEFINITION)
                                        .setValue(Converters.convert(def, String.class));
                            }
                        },
                        null);
            } else {
                FeatureCollection<? extends FeatureType, ? extends Feature> features =
                        fti.getFeatureSource(null, null).getFeatures(Filter.INCLUDE);
                final UniqueVisitor visitor = new UniqueVisitor(attName);
                features.accepts(visitor, null);
                AtomicInteger index = new AtomicInteger(0);
                visitor.getUnique()
                        .stream()
                        .filter(value -> value != null)
                        .sorted()
                        .forEach(
                                value -> {
                                    ComplexMetadataMap domainMap =
                                            metadata.subMap(
                                                    attributeConfiguration.getKey(),
                                                    index.getAndIncrement());
                                    domainMap
                                            .get(String.class, MetadataConstants.DOMAIN_ATT_VALUE)
                                            .setValue(Converters.convert(value, String.class));
                                });
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve domain for " + fti.getName(), e);
        }
    }

    @Override
    public boolean supports(ComplexMetadataMap metadata, LayerInfo layerInfo) {
        return layerInfo.getResource() instanceof FeatureTypeInfo;
    }

    @Override
    public Component getDialogContent(String id, LayerInfo layerInfo) {
        return new GenerateDomainPanel(id, (FeatureTypeInfo) layerInfo.getResource());
    }

    @Override
    public int getDialogContentHeight() {
        return 360;
    }

    public static DataAccess<? extends FeatureType, ? extends Feature> getDataAccess(
            FeatureTypeInfo fti) {
        Map<String, Serializable> connectionParams =
                new HashMap<>(fti.getStore().getConnectionParameters());
        connectionParams.put(JDBCDataStoreFactory.EXPOSE_PK.getName(), true);
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess;
        try {
            dataAccess = DataAccessFinder.getDataStore(connectionParams);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to access datastore for " + fti.getName(), e);
            dataAccess = null;
        }
        return dataAccess;
    }
}
