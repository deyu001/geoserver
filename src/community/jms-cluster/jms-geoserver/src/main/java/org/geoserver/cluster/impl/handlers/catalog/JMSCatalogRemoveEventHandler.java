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

package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cluster.events.ToggleSwitch;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public class JMSCatalogRemoveEventHandler extends JMSCatalogEventHandler {
    private final Catalog catalog;
    private final ToggleSwitch producer;

    public JMSCatalogRemoveEventHandler(
            Catalog catalog, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.catalog = catalog;
        this.producer = producer;
    }

    @Override
    public boolean synchronize(CatalogEvent event) throws Exception {
        if (event == null) {
            throw new NullPointerException("Incoming object is null");
        }
        try {
            if (event instanceof CatalogRemoveEvent) {
                final CatalogRemoveEvent removeEv = ((CatalogRemoveEvent) event);

                // get the source
                final CatalogInfo info = removeEv.getSource();

                // disable the producer to avoid recursion
                producer.disable();
                // remove the selected CatalogInfo
                JMSCatalogRemoveEventHandler.remove(catalog, info, getProperties());

            } else {
                // incoming object not recognized
                if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                    LOGGER.severe("Unrecognized event type");
                return false;
            }

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(
                        this.getClass() + " is unable to synchronize the incoming event: " + event);
            throw e;
        } finally {
            // re enable the producer
            producer.enable();
        }
        return true;
    }

    private static void remove(final Catalog catalog, CatalogInfo info, Properties options)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if (info instanceof LayerGroupInfo) {

            final LayerGroupInfo deserObject =
                    CatalogUtils.localizeLayerGroup((LayerGroupInfo) info, catalog);
            catalog.remove(deserObject);
            // catalog.save(CatalogUtils.getProxy(deserObject));
            // info=CatalogUtils.localizeLayerGroup((LayerGroupInfo) info,
            // catalog);

        } else if (info instanceof LayerInfo) {

            final LayerInfo layer = CatalogUtils.localizeLayer((LayerInfo) info, catalog);
            catalog.remove(layer);
            // catalog.save(CatalogUtils.getProxy(layer));
            // info=CatalogUtils.localizeLayer((LayerInfo) info, catalog);

        } else if (info instanceof MapInfo) {

            final MapInfo localObject = CatalogUtils.localizeMapInfo((MapInfo) info, catalog);
            catalog.remove(localObject);
            // catalog.save(CatalogUtils.getProxy(localObject));
            // info= CatalogUtils.localizeMapInfo((MapInfo) info,catalog);

        } else if (info instanceof NamespaceInfo) {

            final NamespaceInfo namespace =
                    CatalogUtils.localizeNamespace((NamespaceInfo) info, catalog);
            catalog.remove(namespace);
            // catalog.save(CatalogUtils.getProxy(namespace));
            // info =CatalogUtils.localizeNamespace((NamespaceInfo) info,
            // catalog);
        } else if (info instanceof StoreInfo) {

            StoreInfo store = CatalogUtils.localizeStore((StoreInfo) info, catalog);
            catalog.remove(store);
            // catalog.save(CatalogUtils.getProxy(store));

            // info=CatalogUtils.localizeStore((StoreInfo)info,catalog);
        } else if (info instanceof ResourceInfo) {

            final ResourceInfo resource =
                    CatalogUtils.localizeResource((ResourceInfo) info, catalog);
            catalog.remove(resource);
            // catalog.save(CatalogUtils.getProxy(resource));
            // info =CatalogUtils.localizeResource((ResourceInfo)info,catalog);
        } else if (info instanceof StyleInfo) {

            final StyleInfo style = CatalogUtils.localizeStyle((StyleInfo) info, catalog);

            catalog.remove(style);

            // check options
            final String purge = (String) options.get("purge");
            if (purge != null && Boolean.parseBoolean(purge)) {
                try {
                    catalog.getResourcePool().deleteStyle(style, true);
                } catch (IOException e) {
                    if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                        LOGGER.severe(e.getLocalizedMessage());
                    }
                }
            }

            // catalog.detach(CatalogUtils.getProxy(deserializedObject));
            // info = CatalogUtils.localizeStyle((StyleInfo) info, catalog);

        } else if (info instanceof WorkspaceInfo) {

            final WorkspaceInfo workspace =
                    CatalogUtils.localizeWorkspace((WorkspaceInfo) info, catalog);
            catalog.remove(workspace);
            // catalog.detach(workspace);
            // info = CatalogUtils.localizeWorkspace((WorkspaceInfo) info,
            // catalog);
        } else if (info instanceof CatalogInfo) {
            // TODO may we don't want to send this empty message!
            // TODO check the producer
            // DO NOTHING
            if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
                LOGGER.warning("info - ID: " + info.getId() + " toString: " + info.toString());
            }
        } else {
            throw new IllegalArgumentException("Bad incoming object: " + info.toString());
        }
    }
}
