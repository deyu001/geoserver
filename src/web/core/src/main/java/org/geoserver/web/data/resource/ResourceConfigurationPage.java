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

package org.geoserver.web.data.resource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.data.layer.LayerPage;
import org.geoserver.web.publish.PublishedConfigurationPage;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.publish.PublishedEditTabPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.GeoTools;
import org.opengis.coverage.grid.GridGeometry;

/**
 * Page allowing to configure a layer and its resource.
 *
 * <p>The page is completely pluggable, the UI will be made up by scanning the Spring context for
 * implementations of {@link ResourceConfigurationPanel} and {@link PublishedConfigurationPanel}.
 *
 * <p>WARNING: one crucial aspect of this page is its ability to not loose edits when one switches
 * from one tab to the other. I did not find any effective way to unit test this, so _please_, if
 * you do modify anything in this class (especially the models), manually retest that the edits are
 * not lost on tab switch.
 */
public class ResourceConfigurationPage extends PublishedConfigurationPage<LayerInfo> {

    private static final long serialVersionUID = 7870938096047218989L;

    IModel<ResourceInfo> myResourceModel;

    public ResourceConfigurationPage(PageParameters parameters) {
        this(parameters.get(WORKSPACE).toOptionalString(), parameters.get(NAME).toString());
    }

    public ResourceConfigurationPage(String workspaceName, String layerName) {
        super(false);
        this.returnPageClass = LayerPage.class;
        LayerInfo layer;
        if (workspaceName != null) {
            NamespaceInfo ns = getCatalog().getNamespaceByPrefix(workspaceName);
            if (ns == null) {
                // unlikely to happen, requires someone making modifications on the workspaces
                // with a layer page open in another tab/window
                throw new RuntimeException("Could not find workspace " + workspaceName);
            }
            layer = getCatalog().getLayerByName(ns.getName() + ":" + layerName);
        } else {
            layer = getCatalog().getLayerByName(layerName);
        }

        if (layer == null) {
            error(
                    new ParamResourceModel("ResourceConfigurationPage.notFound", this, layerName)
                            .getString());
            setResponsePage(returnPage);
            return;
        }

        setupPublished(layer);
        setupResource(layer.getResource());
    }

    public ResourceConfigurationPage(ResourceInfo info, boolean isNew) {
        super(isNew);
        this.returnPageClass = LayerPage.class;
        setupPublished(getCatalog().getLayers(info).get(0));
        setupResource(info);
    }

    public ResourceConfigurationPage(LayerInfo info, boolean isNew) {
        super(info, isNew);
        this.returnPageClass = LayerPage.class;
        setupResource(
                isNew
                        ? info.getResource()
                        : getCatalog().getResource(info.getResource().getId(), ResourceInfo.class));
    }

    private void updateResourceInLayerModel(ResourceInfo resource) {
        LayerInfo layer = getPublishedInfo();
        layer.setResource(resource);
        myModel.setObject(layer);
    }

    private void setupResource(ResourceInfo resource) {
        updateResourceInLayerModel(resource);
        myResourceModel = new CompoundPropertyModel<>(new ResourceModel(resource));
    }

    private List<ResourceConfigurationPanelInfo> filterResourcePanels(
            List<ResourceConfigurationPanelInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).canHandle(getResourceInfo())) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }

    protected class DataLayerEditTabPanel extends ListEditTabPanel {

        private static final long serialVersionUID = -3442310698941800127L;

        public DataLayerEditTabPanel(String id) {
            super(id);
        }

        protected ListView<ResourceConfigurationPanelInfo> createList(String id) {
            List<ResourceConfigurationPanelInfo> dataPanels =
                    filterResourcePanels(
                            getGeoServerApplication()
                                    .getBeansOfType(ResourceConfigurationPanelInfo.class));
            ListView<ResourceConfigurationPanelInfo> dataPanelList =
                    new ListView<ResourceConfigurationPanelInfo>(id, dataPanels) {

                        private static final long serialVersionUID = -845785165778837024L;

                        @Override
                        protected void populateItem(ListItem<ResourceConfigurationPanelInfo> item) {
                            ResourceConfigurationPanelInfo panelInfo = item.getModelObject();
                            try {
                                final Class<ResourceConfigurationPanel> componentClass =
                                        panelInfo.getComponentClass();
                                final Constructor<ResourceConfigurationPanel> constructor;
                                constructor =
                                        componentClass.getConstructor(String.class, IModel.class);
                                ResourceConfigurationPanel panel =
                                        constructor.newInstance("content", myResourceModel);
                                item.add(panel);
                            } catch (Exception e) {
                                throw new WicketRuntimeException(
                                        "Failed to add pluggable resource configuration panels", e);
                            }
                        }
                    };
            return dataPanelList;
        }
    }

    /** Returns the {@link ResourceInfo} contained in this page */
    public ResourceInfo getResourceInfo() {
        return myResourceModel.getObject();
    }

    /** Allows collaborating pages to update the resource info object */
    public void updateResource(ResourceInfo info) {
        updateResource(info, null);
    }

    /**
     * Allows collaborating pages to update the resource info object
     *
     * @param info the resource info to update
     */
    public void updateResource(ResourceInfo info, final AjaxRequestTarget target) {
        myResourceModel.setObject(info);
        updateResourceInLayerModel(info);
        visitChildren(
                (component, visit) -> {
                    if (component instanceof ResourceConfigurationPanel) {
                        ResourceConfigurationPanel rcp = (ResourceConfigurationPanel) component;
                        rcp.resourceUpdated(target);
                        visit.dontGoDeeper();
                    }
                });
    }

    @Override
    protected PublishedEditTabPanel<LayerInfo> createMainTab(String panelID) {
        return new DataLayerEditTabPanel(panelID);
    }

    @Override
    protected void doSaveInternal() throws IOException {
        Catalog catalog = getCatalog();
        ResourceInfo resourceInfo = getResourceInfo();
        validateByChildren(resourceInfo);
        if (isNew) {
            // updating grid if is a coverage
            if (resourceInfo instanceof CoverageInfo) {
                // the coverage bounds computation path is a bit more linear, the
                // readers always return the bounds and in the proper CRS (afaik)
                CoverageInfo cinfo = (CoverageInfo) resourceInfo;
                GridCoverage2DReader reader =
                        (GridCoverage2DReader)
                                cinfo.getGridCoverageReader(null, GeoTools.getDefaultHints());

                // get bounds
                final ReferencedEnvelope bounds =
                        new ReferencedEnvelope(reader.getOriginalEnvelope());
                // apply the bounds, taking into account the reprojection policy if need be
                final ProjectionPolicy projectionPolicy = resourceInfo.getProjectionPolicy();
                if (projectionPolicy != ProjectionPolicy.NONE && bounds != null) {
                    // we need to fix the registered grid for this coverage
                    final GridGeometry grid = cinfo.getGrid();
                    cinfo.setGrid(
                            new GridGeometry2D(
                                    grid.getGridRange(),
                                    grid.getGridToCRS(),
                                    resourceInfo.getCRS()));
                }
            }

            catalog.validate(resourceInfo, true).throwIfInvalid();
            LayerInfo publishedInfo = getPublishedInfo();
            catalog.add(resourceInfo);
            try {
                catalog.add(publishedInfo);
            } catch (Exception e) {
                catalog.remove(resourceInfo);
                throw e;
            }
        } else {
            ResourceInfo oldState = catalog.getResource(resourceInfo.getId(), ResourceInfo.class);

            catalog.validate(resourceInfo, true).throwIfInvalid();
            LayerInfo publishedInfo = getPublishedInfo();
            catalog.save(resourceInfo);
            try {
                LayerInfo layer = publishedInfo;
                layer.setResource(resourceInfo);
                catalog.save(layer);
            } catch (Exception e) {
                catalog.save(oldState);
                throw e;
            }
        }
    }

    private void validateByChildren(final ResourceInfo resourceInfo) {
        if (resourceInfo == null || resourceInfo.getMetadata() == null) return;
        visitChildren(
                (component, visitor) -> {
                    if (component instanceof MetadataMapValidator) {
                        ((MetadataMapValidator) component).validate(resourceInfo.getMetadata());
                    }
                });
    }
}
