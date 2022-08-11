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

package org.geoserver.web.publish;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ServiceResourceProvider;
import org.geoserver.web.GeoServerApplication;

/**
 * Configuration Panel for Service enable/disable in a layer basis
 *
 * @author Fernando Miño - Geosolutions
 */
public class ServiceLayerConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {
    private static final long serialVersionUID = 1L;

    private WebMarkupContainer serviceSelectionContainer;
    private Palette<String> servicesMultiSelector;

    public ServiceLayerConfigurationPanel(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);
        final AjaxCheckBox configEnabledCheck =
                new AjaxCheckBox(
                        "configEnabled",
                        new PropertyModel<>(layerModel, "resource.serviceConfiguration")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        ServiceLayerConfigurationPanel.this.servicesMultiSelector.setVisible(
                                getModelObject());
                        target.add(ServiceLayerConfigurationPanel.this.serviceSelectionContainer);
                    }
                };
        add(configEnabledCheck);
        PropertyModel<List<String>> dsModel =
                new PropertyModel<>(layerModel, "resource.disabledServices");
        final IChoiceRenderer<String> renderer =
                new ChoiceRenderer<String>() {
                    @Override
                    public String getObject(
                            String id, IModel<? extends List<? extends String>> choices) {
                        return id;
                    }

                    @Override
                    public Object getDisplayValue(String object) {
                        if (object == null) return null;
                        return super.getDisplayValue(object);
                    }

                    @Override
                    public String getIdValue(String object, int index) {
                        return object;
                    }
                };

        servicesMultiSelector =
                new Palette<String>(
                        "servicesSelection",
                        dsModel,
                        servicesVotedModel(layerModel.getObject().getResource()),
                        renderer,
                        10,
                        false) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("DisabledServicesPalette.selectedHeader"));
                    }

                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("DisabledServicesPalette.availableHeader"));
                    }
                };
        servicesMultiSelector.add(new DefaultTheme());
        servicesMultiSelector.setVisible(
                layerModel.getObject().getResource().isServiceConfiguration());
        serviceSelectionContainer = new WebMarkupContainer("serviceSelectionContainer");
        serviceSelectionContainer.setOutputMarkupPlaceholderTag(true);
        add(serviceSelectionContainer);
        serviceSelectionContainer.add(servicesMultiSelector);
    }

    protected ServiceResourceProvider getServiceResourceUtil() {
        return GeoServerApplication.get().getBeanOfType(ServiceResourceProvider.class);
    }

    private LoadableDetachableModel<List<String>> servicesVotedModel(ResourceInfo resource) {
        return new LoadableDetachableModel<List<String>>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load() {
                return getServiceResourceUtil().getServicesForResource(resource);
            }
        };
    }
}
