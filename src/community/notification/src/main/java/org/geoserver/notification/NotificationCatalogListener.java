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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.notification.common.Notification;
import org.geoserver.notification.common.Notification.Action;
import org.geoserver.notification.common.Notification.Type;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class NotificationCatalogListener extends NotificationListener
        implements INotificationCatalogListener {

    private Boolean filterEvent(CatalogInfo source) {
        return (source instanceof WorkspaceInfo
                || source instanceof NamespaceInfo
                || source instanceof FeatureTypeInfo
                || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo
                || source instanceof StoreInfo
                || source instanceof LayerInfo
                || source instanceof LayerGroupInfo);
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (filterEvent(event.getSource())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : null;
            CatalogInfo info = ModificationProxy.unwrap(event.getSource());
            Notification notification =
                    new NotificationImpl(
                            Type.Catalog,
                            event.getSource().getId(),
                            Action.Remove,
                            info,
                            null,
                            user);
            notify(notification);
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        if (filterEvent(event.getSource())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : null;
            CatalogInfo info = ModificationProxy.unwrap(event.getSource());
            Notification notification =
                    new NotificationImpl(
                            Type.Catalog,
                            event.getSource().getId(),
                            Action.Update,
                            info,
                            handleModifiedProperties(event),
                            user);
            notify(notification);
        }
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        if (filterEvent(event.getSource())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : null;
            CatalogInfo info = ModificationProxy.unwrap(event.getSource());
            Notification notification =
                    new NotificationImpl(
                            Type.Catalog, event.getSource().getId(), Action.Add, info, null, user);
            notify(notification);
        }
    }

    private Map<String, Object> handleModifiedProperties(CatalogModifyEvent event) {
        final Map<String, Object> properties = new HashMap<String, Object>();
        final CatalogInfo source = event.getSource();
        final List<String> changedProperties = event.getPropertyNames();
        final List<Object> oldValues = event.getOldValues();
        final List<Object> newValues = event.getNewValues();
        if (source instanceof FeatureTypeInfo
                || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo
                || source instanceof LayerGroupInfo) {
            if (changedProperties.contains("name")
                    || changedProperties.contains("namespace")
                    || changedProperties.contains("workspace")) {
                handleRename(properties, source, changedProperties, oldValues, newValues);
            }
        } else if (source instanceof WorkspaceInfo) {
            if (changedProperties.contains("name")) {
                handleWorkspaceRename(properties, source, changedProperties, oldValues, newValues);
            }
        }
        if (source instanceof LayerInfo) {
            final LayerInfo li = (LayerInfo) source;
            handleLayerInfoChange(properties, changedProperties, oldValues, newValues, li);
        } else if (source instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) source;
            handleLayerGroupInfoChange(properties, changedProperties, oldValues, newValues, lgInfo);
        }
        return properties;
    }

    private void handleLayerGroupInfoChange(
            Map<String, Object> properties,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues,
            final LayerGroupInfo lgInfo) {

        if (changedProperties.contains("layers")) {
            final int layersIndex = changedProperties.indexOf("layers");
            Object oldLayers = oldValues.get(layersIndex);
            Object newLayers = newValues.get(layersIndex);
        }

        if (changedProperties.contains("styles")) {
            final int stylesIndex = changedProperties.indexOf("styles");
            BeanToPropertyValueTransformer transformer = new BeanToPropertyValueTransformer("name");
            String oldStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) oldValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            String newStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) newValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            if (!oldStyles.equals(newStyles)) {
                properties.put("styles", newStyles);
            }
        }
    }

    private void handleLayerInfoChange(
            Map<String, Object> properties,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues,
            final LayerInfo li) {

        if (changedProperties.contains("defaultStyle")) {
            final int propIndex = changedProperties.indexOf("defaultStyle");
            final StyleInfo oldStyle = (StyleInfo) oldValues.get(propIndex);
            final StyleInfo newStyle = (StyleInfo) newValues.get(propIndex);

            final String oldStyleName = oldStyle.prefixedName();
            final String newStyleName = newStyle.prefixedName();
            if (!oldStyleName.equals(newStyleName)) {
                properties.put("defaultStyle", newStyleName);
            }
        }

        if (changedProperties.contains("styles")) {
            final int stylesIndex = changedProperties.indexOf("styles");
            BeanToPropertyValueTransformer transformer = new BeanToPropertyValueTransformer("name");
            String oldStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) oldValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            String newStyles =
                    StringUtils.join(
                            CollectionUtils.collect(
                                            (Set<StyleInfo>) newValues.get(stylesIndex),
                                            transformer)
                                    .toArray());
            if (!oldStyles.equals(newStyles)) {
                properties.put("styles", newStyles);
            }
        }
    }

    private void handleWorkspaceRename(
            Map<String, Object> properties,
            final CatalogInfo source,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues) {
        final int nameIndex = changedProperties.indexOf("name");
        final String oldWorkspaceName = (String) oldValues.get(nameIndex);
        final String newWorkspaceName = (String) newValues.get(nameIndex);
    }

    private void handleRename(
            Map<String, Object> properties,
            final CatalogInfo source,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues) {

        final int nameIndex = changedProperties.indexOf("name");
        final int namespaceIndex = changedProperties.indexOf("namespace");

        String oldLayerName;
        String newLayerName;
        if (source instanceof ResourceInfo) { // covers LayerInfo, CoverageInfo, and WMSLayerInfo
            // must cover prefix:name
            final ResourceInfo resourceInfo = (ResourceInfo) source;
            final NamespaceInfo currNamespace = resourceInfo.getNamespace();
            final NamespaceInfo oldNamespace;
            if (namespaceIndex > -1) {
                oldNamespace = (NamespaceInfo) oldValues.get(namespaceIndex);
            } else {
                oldNamespace = currNamespace;
            }

            newLayerName = resourceInfo.prefixedName();
            if (nameIndex > -1) {
                oldLayerName = (String) oldValues.get(nameIndex);
            } else {
                oldLayerName = resourceInfo.getName();
            }
            oldLayerName = oldNamespace.getPrefix() + ":" + oldLayerName;
        }
    }

    @Override
    public void setMessageMultiplexer(MessageMultiplexer messageMultiplexer) {
        this.messageMultiplexer = messageMultiplexer;
    }

    @Override
    public MessageMultiplexer getMessageMultiplexer() {
        return messageMultiplexer;
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        int a = 1;
    }

    @Override
    public void reloaded() {}
}
