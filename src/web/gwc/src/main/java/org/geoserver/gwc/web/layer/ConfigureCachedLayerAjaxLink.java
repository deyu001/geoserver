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

package org.geoserver.gwc.web.layer;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.web.data.layergroup.LayerGroupEditPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geowebcache.layer.TileLayer;

/**
 * A simple ajax link that links to the edit page for the given {@link GeoServerTileLayer} (that is,
 * either to the layerinfo edit page or layergroup edit page, as appropriate)
 */
class ConfigureCachedLayerAjaxLink extends SimpleAjaxLink<TileLayer> {

    private static final long serialVersionUID = 1L;

    private Class<? extends Page> returnPage;

    /**
     * @param id component id
     * @param itemModel model over the tile layer to configure
     * @param returnPage which page to instruct the LayerInfo or LayerGroupInfo edit page to return
     *     to
     */
    public ConfigureCachedLayerAjaxLink(
            String id, IModel<TileLayer> itemModel, Class<? extends Page> returnPage) {
        super(id, itemModel, new PropertyModel<String>(itemModel, "name"));
        this.returnPage = returnPage;
    }

    @Override
    protected void onClick(AjaxRequestTarget target) {
        final TileLayer layer = getModelObject();
        if (!(layer instanceof GeoServerTileLayer)) {
            return;
        }
        final GeoServerTileLayer geoserverTileLayer = (GeoServerTileLayer) getModelObject();
        PublishedInfo publishedInfo = geoserverTileLayer.getPublishedInfo();
        if (publishedInfo instanceof LayerInfo) {
            ResourceConfigurationPage resourceConfigPage;
            resourceConfigPage = new ResourceConfigurationPage((LayerInfo) publishedInfo, false);
            // tell the resource/layer edit page to start up on the tile cache tab
            resourceConfigPage.setSelectedTab(LayerCacheOptionsTabPanel.class);
            if (returnPage != null) {
                resourceConfigPage.setReturnPage(returnPage);
            }
            setResponsePage(resourceConfigPage);
        } else if (publishedInfo instanceof LayerGroupInfo) {
            LayerGroupInfo layerGroup = (LayerGroupInfo) publishedInfo;
            WorkspaceInfo workspace = layerGroup.getWorkspace();
            String wsName = workspace == null ? null : workspace.getName();
            PageParameters parameters = new PageParameters();
            parameters.add(LayerGroupEditPage.GROUP, layerGroup.getName());
            if (wsName != null) {
                parameters.add(LayerGroupEditPage.WORKSPACE, wsName);
            }
            LayerGroupEditPage layerGroupEditPage = new LayerGroupEditPage(parameters);
            if (returnPage != null) {
                layerGroupEditPage.setReturnPage(returnPage);
            }
            setResponsePage(layerGroupEditPage);
        }
    }
}
