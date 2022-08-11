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

package org.geoserver.nsg.versioning.web;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.nsg.versioning.TimeVersioning;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;

public class WfsVersioningConfig extends PublishedConfigurationPanel<LayerInfo> {

    public WfsVersioningConfig(String id, IModel<LayerInfo> model) {
        super(id, model);
        // get the needed information from the model
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(model);
        boolean isVersioningActivated = TimeVersioning.isEnabled(featureTypeInfo);
        String idAttributeName =
                isVersioningActivated ? TimeVersioning.getNamePropertyName(featureTypeInfo) : null;
        String timeAttributeName =
                isVersioningActivated ? TimeVersioning.getTimePropertyName(featureTypeInfo) : null;
        List<String> attributesNames = getAttributesNames(featureTypeInfo);
        List<String> timeAttributesNames = getTimeAttributesNames(featureTypeInfo);
        // create dropdown choice for the id attribute name
        PropertyModel metadata = new PropertyModel(model, "resource.metadata");
        DropDownChoice<String> idAttributeChoice =
                new DropDownChoice<>(
                        "idAttributeChoice",
                        new MapModel<>(metadata, TimeVersioning.NAME_PROPERTY_KEY),
                        attributesNames);
        idAttributeChoice.setOutputMarkupId(true);
        idAttributeChoice.setOutputMarkupPlaceholderTag(true);
        idAttributeChoice.setRequired(true);
        idAttributeChoice.setVisible(isVersioningActivated);
        add(idAttributeChoice);
        // add label for id attribute name dropdown choice
        Label idAttributeChoiceLabel =
                new Label(
                        "idAttributeChoiceLabel",
                        new StringResourceModel("WfsVersioningConfig.idAttributeChoiceLabel"));
        idAttributeChoiceLabel.setOutputMarkupId(true);
        idAttributeChoiceLabel.setOutputMarkupPlaceholderTag(true);
        idAttributeChoiceLabel.setVisible(isVersioningActivated);
        add(idAttributeChoiceLabel);
        // create dropdown choice for the time attribute name

        DropDownChoice<String> timeAttributeChoice =
                new DropDownChoice<>(
                        "timeAttributeChoice",
                        new MapModel<>(metadata, TimeVersioning.TIME_PROPERTY_KEY),
                        timeAttributesNames);
        timeAttributeChoice.setOutputMarkupId(true);
        timeAttributeChoice.setOutputMarkupPlaceholderTag(true);
        timeAttributeChoice.setRequired(true);
        timeAttributeChoice.setVisible(isVersioningActivated);
        add(timeAttributeChoice);
        // add label for id attribute name dropdown choice
        Label timeAttributeChoiceLabel =
                new Label(
                        "timeAttributeChoiceLabel",
                        new StringResourceModel("WfsVersioningConfig.timeAttributeChoiceLabel"));
        timeAttributeChoiceLabel.setOutputMarkupId(true);
        timeAttributeChoiceLabel.setOutputMarkupPlaceholderTag(true);
        timeAttributeChoiceLabel.setVisible(isVersioningActivated);
        add(timeAttributeChoiceLabel);
        // checkbox for activating versioning
        CheckBox versioningActivateCheckBox =
                new AjaxCheckBox(
                        "versioningActivateCheckBox",
                        new MapModel<>(metadata, TimeVersioning.ENABLED_KEY)) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        boolean checked = getModelObject();
                        if (checked) {
                            // activate versioning attributes selection
                            idAttributeChoice.setVisible(true);
                            idAttributeChoiceLabel.setVisible(true);
                            timeAttributeChoice.setVisible(true);
                            timeAttributeChoiceLabel.setVisible(true);
                        } else {
                            // deactivate versioning attributes selection
                            idAttributeChoice.setVisible(false);
                            idAttributeChoiceLabel.setVisible(false);
                            timeAttributeChoice.setVisible(false);
                            timeAttributeChoiceLabel.setVisible(false);
                        }
                        // update the dropdown choices and labels
                        target.add(idAttributeChoice);
                        target.add(idAttributeChoiceLabel);
                        target.add(timeAttributeChoice);
                        target.add(timeAttributeChoiceLabel);
                    }
                };
        if (isVersioningActivated) {
            versioningActivateCheckBox.setModelObject(true);
        }
        versioningActivateCheckBox.setEnabled(!timeAttributesNames.isEmpty());
        add(versioningActivateCheckBox);
        // add versioning activating checkbox label
        Label versioningActivateCheckBoxLabel =
                new Label(
                        "versioningActivateCheckBoxLabel",
                        new StringResourceModel(
                                "WfsVersioningConfig.versioningActivateCheckBoxLabel"));
        add(versioningActivateCheckBoxLabel);
    }

    private List<String> getAttributesNames(FeatureTypeInfo featureTypeInfo) {
        try {
            return featureTypeInfo
                    .getFeatureType()
                    .getDescriptors()
                    .stream()
                    .map(attribute -> attribute.getName().getLocalPart())
                    .collect(Collectors.toList());
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error processing attributes of feature type '%s'.",
                            featureTypeInfo.getName()),
                    exception);
        }
    }

    private List<String> getTimeAttributesNames(FeatureTypeInfo featureTypeInfo) {
        try {
            return featureTypeInfo
                    .getFeatureType()
                    .getDescriptors()
                    .stream()
                    .filter(
                            attribute -> {
                                Class binding = attribute.getType().getBinding();
                                return Long.class.isAssignableFrom(binding)
                                        || Date.class.isAssignableFrom(binding)
                                        || Timestamp.class.isAssignableFrom(binding);
                            })
                    .map(attribute -> attribute.getName().getLocalPart())
                    .collect(Collectors.toList());
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error processing attributes of feature type '%s'.",
                            featureTypeInfo.getName()),
                    exception);
        }
    }

    private FeatureTypeInfo getFeatureTypeInfo(IModel<LayerInfo> model) {
        return (FeatureTypeInfo) model.getObject().getResource();
    }
}
