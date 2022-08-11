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

package org.geoserver.metadata.web.panel.attribute;

import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.dto.OccurrenceEnum;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.GeneratorService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Entry point for the gui generation. This parses the configuration and adds simple fields, complex
 * fields (composition of multiple simple fields) and lists of simple or complex fields.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class AttributesTablePanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    private ResourceInfo rInfo;

    public AttributesTablePanel(
            String id,
            GeoServerDataProvider<AttributeConfiguration> dataProvider,
            IModel<ComplexMetadataMap> metadataModel,
            Map<String, List<Integer>> derivedAtts,
            ResourceInfo rInfo) {
        super(id, metadataModel);
        this.rInfo = rInfo;

        GeoServerTablePanel<AttributeConfiguration> tablePanel =
                createAttributesTablePanel(dataProvider, derivedAtts);
        tablePanel.setFilterVisible(false);
        tablePanel.setFilterable(false);
        tablePanel.getTopPager().setVisible(false);
        tablePanel.getBottomPager().setVisible(false);
        tablePanel.setOutputMarkupId(false);
        tablePanel.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        tablePanel.setSelectable(false);
        tablePanel.setSortable(false);
        add(tablePanel);
    }

    private GeoServerTablePanel<AttributeConfiguration> createAttributesTablePanel(
            GeoServerDataProvider<AttributeConfiguration> dataProvider,
            Map<String, List<Integer>> derivedAtts) {

        return new GeoServerTablePanel<AttributeConfiguration>(
                "attributesTablePanel", dataProvider) {
            private static final long serialVersionUID = 5267842353156378075L;

            @Override
            protected Component getComponentForProperty(
                    String id,
                    IModel<AttributeConfiguration> itemModel,
                    GeoServerDataProvider.Property<AttributeConfiguration> property) {
                if (property.equals(AttributeDataProvider.NAME)) {
                    String labelValue = resolveLabelValue(itemModel.getObject());
                    return new Label(id, labelValue);
                }
                if (property.equals(AttributeDataProvider.VALUE)) {
                    AttributeConfiguration attributeConfiguration = itemModel.getObject();
                    if (OccurrenceEnum.SINGLE.equals(attributeConfiguration.getOccurrence())) {
                        Component component =
                                EditorFactory.getInstance()
                                        .create(
                                                attributeConfiguration,
                                                id,
                                                getMetadataModel().getObject(),
                                                rInfo);
                        // disable components with values from the templates
                        if (component != null
                                && derivedAtts != null
                                && derivedAtts.containsKey(attributeConfiguration.getKey())) {
                            boolean disableInput =
                                    derivedAtts.get(attributeConfiguration.getKey()).size() > 0;
                            component.setEnabled(!disableInput);
                        }
                        return component;
                    } else if (attributeConfiguration.getFieldType() == FieldTypeEnum.COMPLEX) {
                        RepeatableComplexAttributeDataProvider repeatableDataProvider =
                                new RepeatableComplexAttributeDataProvider(
                                        attributeConfiguration, getMetadataModel());

                        return new RepeatableComplexAttributesTablePanel(
                                id,
                                repeatableDataProvider,
                                getMetadataModel(),
                                attributeConfiguration,
                                GeoServerApplication.get()
                                        .getBeanOfType(GeneratorService.class)
                                        .findGeneratorByType(attributeConfiguration.getTypename()),
                                derivedAtts,
                                rInfo);
                    } else {
                        RepeatableAttributeDataProvider<String> repeatableDataProvider =
                                new RepeatableAttributeDataProvider<String>(
                                        EditorFactory.getInstance()
                                                .getItemClass(attributeConfiguration),
                                        attributeConfiguration,
                                        getMetadataModel());
                        return new RepeatableAttributesTablePanel(
                                id, repeatableDataProvider, getMetadataModel(), derivedAtts, rInfo);
                    }
                }
                return null;
            }
        };
    }

    /** Try to find the label from the resource bundle */
    private String resolveLabelValue(AttributeConfiguration attribute) {
        return getString(
                AttributeConfiguration.PREFIX + attribute.getKey(), null, attribute.getLabel());
    }

    @SuppressWarnings("unchecked")
    public IModel<ComplexMetadataMap> getMetadataModel() {
        return (IModel<ComplexMetadataMap>) getDefaultModel();
    }
}
