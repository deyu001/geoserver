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

package org.geoserver.web.data.layer;

import static org.geoserver.web.data.layer.LayerProvider.CREATED_TIMESTAMP;
import static org.geoserver.web.data.layer.LayerProvider.ENABLED;
import static org.geoserver.web.data.layer.LayerProvider.MODIFIED_TIMESTAMP;
import static org.geoserver.web.data.layer.LayerProvider.NAME;
import static org.geoserver.web.data.layer.LayerProvider.SRS;
import static org.geoserver.web.data.layer.LayerProvider.STORE;
import static org.geoserver.web.data.layer.LayerProvider.TITLE;
import static org.geoserver.web.data.layer.LayerProvider.TYPE;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.data.store.CoverageStoreEditPage;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.WMSStoreEditPage;
import org.geoserver.web.data.store.WMTSStoreEditPage;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/**
 * Page listing all the available layers. Follows the usual filter/sort/page approach, provides ways
 * to bulk delete layers and to add new ones
 */
@SuppressWarnings("serial")
public class LayerPage extends GeoServerSecuredPage {
    LayerProvider provider = new LayerProvider();
    GeoServerTablePanel<LayerInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public LayerPage() {
        final CatalogIconFactory icons = CatalogIconFactory.get();
        table =
                new GeoServerTablePanel<LayerInfo>("table", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id, IModel<LayerInfo> itemModel, Property<LayerInfo> property) {
                        if (property == TYPE) {
                            Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                            f.add(
                                    new Image(
                                            "layerIcon",
                                            icons.getSpecificLayerIcon(itemModel.getObject())));
                            return f;
                        } else if (property == STORE) {
                            return storeLink(id, itemModel);
                        } else if (property == NAME) {
                            return layerLink(id, itemModel);
                        } else if (property == ENABLED) {
                            LayerInfo layerInfo = itemModel.getObject();
                            // ask for enabled() instead of isEnabled() to account for disabled
                            // resource/store
                            boolean enabled = layerInfo.enabled();
                            PackageResourceReference icon =
                                    enabled ? icons.getEnabledIcon() : icons.getDisabledIcon();
                            Fragment f = new Fragment(id, "iconFragment", LayerPage.this);
                            f.add(new Image("layerIcon", icon));
                            return f;
                        } else if (property == SRS) {
                            return new Label(id, SRS.getModel(itemModel));
                        } else if (property == TITLE) {
                            return titleLink(id, itemModel);
                        } else if (property == MODIFIED_TIMESTAMP) {
                            return new DateTimeLabel(id, MODIFIED_TIMESTAMP.getModel(itemModel));
                        } else if (property == CREATED_TIMESTAMP) {
                            return new DateTimeLabel(id, CREATED_TIMESTAMP.getModel(itemModel));
                        }
                        throw new IllegalArgumentException(
                                "Don't know a property named " + property.getName());
                    }

                    @Override
                    protected void onSelectionUpdate(AjaxRequestTarget target) {
                        removal.setEnabled(table.getSelection().size() > 0);
                        target.add(removal);
                    }
                };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    private Component titleLink(String id, IModel<LayerInfo> itemModel) {

        @SuppressWarnings("unchecked")
        IModel<String> layerNameModel = (IModel<String>) NAME.getModel(itemModel);
        @SuppressWarnings("unchecked")
        IModel<String> layerTitleModel = (IModel<String>) TITLE.getModel(itemModel);
        String layerTitle = layerTitleModel.getObject();
        String layerName = layerNameModel.getObject();
        String wsName = getWorkspaceNameFromLayerInfo(itemModel.getObject());

        IModel linkModel = layerTitleModel;
        if (StringUtils.isEmpty(layerTitle)) {
            linkModel = layerNameModel;
        }

        return new SimpleBookmarkableLink(
                id,
                ResourceConfigurationPage.class,
                linkModel,
                ResourceConfigurationPage.NAME,
                layerName,
                ResourceConfigurationPage.WORKSPACE,
                wsName);
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<Void>("addNew", NewLayerPage.class));

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    private Component layerLink(String id, final IModel<LayerInfo> model) {
        @SuppressWarnings("unchecked")
        IModel<String> layerNameModel = (IModel<String>) NAME.getModel(model);
        String wsName = getWorkspaceNameFromLayerInfo(model.getObject());
        String layerName = layerNameModel.getObject();
        String linkTitle = wsName + ":" + layerName;
        return new SimpleBookmarkableLink(
                id,
                ResourceConfigurationPage.class,
                new Model<>(linkTitle),
                ResourceConfigurationPage.NAME,
                layerName,
                ResourceConfigurationPage.WORKSPACE,
                wsName);
    }

    private Component storeLink(String id, final IModel<LayerInfo> model) {
        @SuppressWarnings("unchecked")
        IModel<String> storeModel = (IModel<String>) STORE.getModel(model);
        String wsName = getWorkspaceNameFromLayerInfo(model.getObject());
        String storeName = storeModel.getObject();
        LayerInfo layer = model.getObject();
        StoreInfo store = layer.getResource().getStore();
        if (store instanceof DataStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    DataAccessEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else if (store instanceof WMTSStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    WMTSStoreEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else if (store instanceof WMSStoreInfo) {
            return new SimpleBookmarkableLink(
                    id,
                    WMSStoreEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        } else {
            return new SimpleBookmarkableLink(
                    id,
                    CoverageStoreEditPage.class,
                    storeModel,
                    DataAccessEditPage.STORE_NAME,
                    storeName,
                    DataAccessEditPage.WS_NAME,
                    wsName);
        }
    }

    /**
     * Helper to grab the workspace name from the layer info
     *
     * @param li the li
     * @return the workspace name of the ws the layer belong
     */
    private String getWorkspaceNameFromLayerInfo(LayerInfo li) {
        return li.getResource().getStore().getWorkspace().getName();
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
