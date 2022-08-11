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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.layer.CoverageViewEditPage;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.ColorPickerPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * A configuration panel for CoverageInfo properties that related to WCS publication
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class CoverageResourceConfigurationPanel extends ResourceConfigurationPanel {

    Map<String, DefaultParameterDescriptor> parameterDescriptorMap;

    public CoverageResourceConfigurationPanel(
            final String panelId, final IModel<CoverageInfo> model) {
        super(panelId, model);
        initParameterDescriptors();

        final CoverageInfo coverage = (CoverageInfo) getResourceInfo();

        final Map<String, Serializable> parameters = coverage.getParameters();
        TreeSet<String> keySet = new TreeSet<>(parameters.keySet());
        addMissingParameters(keySet, coverage);
        List<String> keys = new ArrayList<>(keySet);

        final IModel paramsModel = new PropertyModel<>(model, "parameters");
        ListView<String> paramsList =
                new ListView<String>("parameters", keys) {

                    @Override
                    protected void populateItem(ListItem item) {
                        Component inputComponent =
                                getInputComponent(
                                        "parameterPanel",
                                        paramsModel,
                                        item.getDefaultModelObjectAsString());
                        item.add(inputComponent);
                    }
                };

        WebMarkupContainer coverageViewContainer =
                new WebMarkupContainer("editCoverageViewContainer");
        add(coverageViewContainer);
        final CoverageView coverageView =
                coverage.getMetadata().get(CoverageView.COVERAGE_VIEW, CoverageView.class);
        coverageViewContainer.add(
                new Link("editCoverageView") {

                    @Override
                    public void onClick() {
                        CoverageInfo coverageInfo = model.getObject();
                        try {
                            CoverageStoreInfo store = coverageInfo.getStore();
                            WorkspaceInfo workspace = store.getWorkspace();
                            setResponsePage(
                                    new CoverageViewEditPage(
                                            workspace.getName(),
                                            store.getName(),
                                            coverageInfo.getName(),
                                            coverageInfo,
                                            ((ResourceConfigurationPage) this.getPage())));
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failure opening the Virtual Coverage edit page",
                                    e);
                            error(e.toString());
                        }
                    }
                });

        coverageViewContainer.setVisible(coverageView != null);

        // needed for form components not to loose state
        paramsList.setReuseItems(true);
        add(paramsList);

        if (keys.isEmpty()) setVisible(false);
    }

    /**
     * Adds into the keySet any read parameter key that's missing (due to reader growing new
     * parameters over time/releases
     */
    private void addMissingParameters(TreeSet<String> keySet, CoverageInfo coverage) {
        AbstractGridFormat format = coverage.getStore().getFormat();
        ParameterValueGroup readParameters = format.getReadParameters();
        List<GeneralParameterValue> parameterValues = readParameters.values();
        List<String> paramNames =
                parameterValues
                        .stream()
                        .map(p -> p.getDescriptor())
                        .filter(p -> p instanceof DefaultParameterDescriptor)
                        .map(p -> p.getName().getCode())
                        .collect(Collectors.toList());
        keySet.addAll(paramNames);
    }

    private void initParameterDescriptors() {
        try {
            final CoverageInfo coverage = (CoverageInfo) getResourceInfo();
            AbstractGridFormat format = coverage.getStore().getFormat();
            ParameterValueGroup readParameters = format.getReadParameters();
            List<GeneralParameterValue> parameterValues = readParameters.values();
            parameterDescriptorMap =
                    parameterValues
                            .stream()
                            .map(p -> p.getDescriptor())
                            .filter(p -> p instanceof DefaultParameterDescriptor)
                            .map(p -> (DefaultParameterDescriptor) p)
                            .collect(
                                    Collectors.toMap(
                                            p -> p.getName().getCode(), Function.identity()));
        } catch (Exception e) {
            LOGGER.log(
                    Level.INFO,
                    "Failed to initialize parameter descriptors, the UI will use generic text "
                            + "editors",
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private Component getInputComponent(String id, IModel paramsModel, String keyName) {
        ResourceModel labelModel = new ResourceModel(keyName, keyName);
        if (keyName.contains("Color")) {
            return new ColorPickerPanel(
                    id, new MapModel<>(paramsModel, keyName), labelModel, false);
        }

        DefaultParameterDescriptor descriptor = parameterDescriptorMap.get(keyName);
        if (descriptor != null) {
            Class valueClass = descriptor.getValueClass();

            // checkbox for booleans
            if (valueClass.equals(Boolean.class)) {
                return new CheckBoxParamPanel(id, new MapModel<>(paramsModel, keyName), labelModel);
            }

            // dropdown for enumerations (don't use the enum value but its name to avoid
            // breaking configuration save (XStream whitelist) and backwards compatibility
            if (descriptor.getValueClass().isEnum()) {
                List<? extends Serializable> values =
                        Arrays.stream(descriptor.getValueClass().getEnumConstants())
                                .map(v -> ((Enum) v).name())
                                .collect(Collectors.toList());
                return new DropDownChoiceParamPanel(
                        id, new MapModel<>(paramsModel, keyName), labelModel, values, false);
            }

            // dropdown for cases in which there is a set of valid values
            Set validValues = descriptor.getValidValues();
            if (Serializable.class.isAssignableFrom(descriptor.getValueClass())
                    && validValues != null
                    && !validValues.isEmpty()) {
                List<? extends Serializable> values = new ArrayList<>(validValues);
                return new DropDownChoiceParamPanel(
                        id, new MapModel<>(paramsModel, keyName), labelModel, values, false);
            }

            // anything else is a text, with some target type and eventual validation
            TextParamPanel panel =
                    new TextParamPanel(id, new MapModel<>(paramsModel, keyName), labelModel, false);
            if (Number.class.isAssignableFrom(valueClass)) {
                panel.getFormComponent().setType(valueClass);

                Number minimum = (Number) descriptor.getMinimumValue();
                if (minimum != null) {
                    panel.getFormComponent().add(RangeValidator.minimum(minimum.doubleValue()));
                }
                Number maximum = (Number) descriptor.getMaximumValue();
                if (maximum != null && maximum instanceof Serializable) {
                    panel.getFormComponent().add(RangeValidator.maximum(maximum.doubleValue()));
                }
            }

            return panel;
        }

        return new TextParamPanel(id, new MapModel<>(paramsModel, keyName), labelModel, false);
    }
}
