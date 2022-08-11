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


package org.geoserver.notification;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.notification.common.Bounds;
import org.geoserver.notification.common.Notification;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.type.FeatureType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class NotificationTransactionListener extends NotificationListener
        implements INotificationTransactionListener {

    protected static final String INSERTED = "inserted";

    protected static final String DELETED = "deleted";

    protected static final String UPDATED = "updated";

    protected static final String TYPE = "type";

    protected static final String BOUNDS = "bounds";

    private Catalog catalog;

    private ThreadLocal<Map<String, Map<String, Object>>> layersChangesResume =
            new ThreadLocal<Map<String, Map<String, Object>>>();

    public NotificationTransactionListener(Catalog catalog) {
        super();
        this.catalog = catalog;
    }

    @Override
    public TransactionRequest beforeTransaction(TransactionRequest request) throws WFSException {
        layersChangesResume =
                new ThreadLocal<Map<String, Map<String, Object>>>() {
                    @Override
                    protected Map<String, Map<String, Object>> initialValue() {
                        return new HashMap<String, Map<String, Object>>();
                    }
                };
        return request;
    }

    @Override
    public void beforeCommit(TransactionRequest request) throws WFSException {}

    @Override
    public void afterTransaction(
            TransactionRequest request, TransactionResponse result, boolean committed) {
        if (committed) {
            String handle = request.getHandle();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : null;
            Map<String, Map<String, Object>> lcrs = layersChangesResume.get();
            for (String layer : lcrs.keySet()) {
                Map<String, Object> prop = lcrs.get(layer);
                Object ft = prop.remove(TYPE);
                Notification n =
                        new NotificationImpl(Notification.Type.Data, handle, null, ft, prop, user);
                notify(n);
            }
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void dataStoreChange(TransactionEvent event) throws WFSException {
        TransactionEventType eventType = event.getType();
        Integer affectedFeatures = event.getAffectedFeatures().size();
        FeatureType featureType = event.getAffectedFeatures().getSchema();
        ReferencedEnvelope featureBound = event.getAffectedFeatures().getBounds();
        FeatureTypeInfo fti = this.catalog.getFeatureTypeByName(featureType.getName());
        CatalogInfo info = ModificationProxy.unwrap(fti);
        String featureTypeName = featureType.getName().getURI();
        Map<String, Map<String, Object>> map = layersChangesResume.get();
        Map<String, Object> properties = map.get(featureTypeName);
        if (properties == null) {
            properties = new HashMap<String, Object>();
            properties.put(TYPE, info);
            map.put(featureTypeName, properties);
        }
        if (eventType == TransactionEventType.POST_INSERT) {
            Integer inserted =
                    properties.get(INSERTED) != null ? (Integer) properties.get(INSERTED) : 0;
            properties.put(INSERTED, inserted + affectedFeatures);
        }
        if (eventType == TransactionEventType.POST_UPDATE) {
            Integer inserted =
                    properties.get(UPDATED) != null ? (Integer) properties.get(UPDATED) : 0;
            properties.put(UPDATED, inserted + affectedFeatures);
        }
        if (eventType == TransactionEventType.PRE_DELETE) {
            Integer inserted =
                    properties.get(DELETED) != null ? (Integer) properties.get(DELETED) : 0;
            properties.put(DELETED, inserted + affectedFeatures);
        }
        if (properties.get(BOUNDS) != null) {
            featureBound.expandToInclude(((Bounds) properties.get(BOUNDS)).getBb());
        }
        properties.put(BOUNDS, new Bounds(featureBound));
    }

    @Override
    public void setMessageMultiplexer(MessageMultiplexer messageMultiplexer) {
        this.messageMultiplexer = messageMultiplexer;
    }

    @Override
    public MessageMultiplexer getMessageMultiplexer() {
        return messageMultiplexer;
    }
}
