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
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;

/**
 * Generate the gui as a list of simple inputs (text, double, dropdown, ..). Add ui components to
 * manage the list.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class RepeatableAttributesTablePanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    private GeoServerTablePanel<ComplexMetadataAttribute<String>> tablePanel;

    private Label noData;

    private ResourceInfo rInfo;

    public RepeatableAttributesTablePanel(
            String id,
            RepeatableAttributeDataProvider<String> dataProvider,
            IModel<ComplexMetadataMap> metadataModel,
            Map<String, List<Integer>> derivedAtts,
            ResourceInfo rInfo) {
        super(id, metadataModel);
        this.rInfo = rInfo;

        setOutputMarkupId(true);

        tablePanel = createAttributesTablePanel(dataProvider, derivedAtts);
        tablePanel.setFilterVisible(false);
        tablePanel.setFilterable(false);
        tablePanel.getTopPager().setVisible(false);
        tablePanel.getBottomPager().setVisible(false);
        tablePanel.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        tablePanel.setSelectable(true);
        tablePanel.setSortable(false);
        tablePanel.setOutputMarkupId(true);
        add(tablePanel);

        // the no data links label
        noData = new Label("noData", new ResourceModel("noData"));
        add(noData);
        updateTable(dataProvider);

        add(
                new AjaxSubmitLink("addNew") {

                    private static final long serialVersionUID = 6840006565079316081L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        dataProvider.addField();
                        updateTable(dataProvider);
                        ((MarkupContainer) tablePanel.get("listContainer").get("items"))
                                .removeAll();
                        target.add(tablePanel);
                        target.add(RepeatableAttributesTablePanel.this);
                    }
                }.setVisible(isEnabledInHierarchy()));
    }

    private GeoServerTablePanel<ComplexMetadataAttribute<String>> createAttributesTablePanel(
            RepeatableAttributeDataProvider<String> dataProvider,
            Map<String, List<Integer>> derivedAtts) {

        GeoServerTablePanel<ComplexMetadataAttribute<String>> tablePanel =
                new GeoServerTablePanel<ComplexMetadataAttribute<String>>(
                        "attributesTablePanel", dataProvider) {

                    private IModel<ComplexMetadataAttribute<String>> disabledValue = null;

                    private static final long serialVersionUID = 4333335931795175790L;

                    @SuppressWarnings("unchecked")
                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<ComplexMetadataAttribute<String>> itemModel,
                            GeoServerDataProvider.Property<ComplexMetadataAttribute<String>>
                                    property) {
                        AttributeConfiguration attributeConfiguration =
                                dataProvider.getConfiguration();
                        boolean enableInput = true;
                        // disable input values from template
                        if (derivedAtts != null
                                && derivedAtts.containsKey(attributeConfiguration.getKey())) {
                            List<Integer> indexes =
                                    derivedAtts.get(attributeConfiguration.getKey());
                            if (indexes.contains(itemModel.getObject().getIndex())) {
                                enableInput = false;
                                disabledValue = itemModel;
                            }
                        }
                        if (property.getName().equals(RepeatableAttributeDataProvider.KEY_VALUE)) {

                            Component component =
                                    EditorFactory.getInstance()
                                            .create(
                                                    attributeConfiguration,
                                                    id,
                                                    itemModel.getObject(),
                                                    dataProvider,
                                                    rInfo);

                            if (component != null) {
                                component.setEnabled(enableInput);
                            }
                            return component;

                        } else if (property.getName()
                                .equals(RepeatableAttributeDataProvider.KEY_REMOVE_ROW)) {
                            if (itemModel.equals(disabledValue)) {
                                // If the object is for a row that is not editable don't show the
                                // remove button
                                disabledValue = null;
                                return new Label(id, "");
                            } else {
                                AjaxSubmitLink deleteAction =
                                        new AjaxSubmitLink(id) {

                                            private static final long serialVersionUID =
                                                    -8829474855848647384L;

                                            @Override
                                            public void onSubmit(
                                                    AjaxRequestTarget target, Form<?> form) {
                                                removeFields(target, itemModel);
                                            }
                                        };
                                deleteAction.add(new AttributeAppender("class", "remove-link"));
                                deleteAction.setVisible(isEnabledInHierarchy());
                                return deleteAction;
                            }
                        } else if (property.getName()
                                .equals(RepeatableAttributeDataProvider.KEY_UPDOWN_ROW)) {
                            return new AttributePositionPanel(
                                            id,
                                            (IModel<ComplexMetadataMap>)
                                                    RepeatableAttributesTablePanel.this
                                                            .getDefaultModel(),
                                            dataProvider.getConfiguration(),
                                            itemModel.getObject().getIndex(),
                                            derivedAtts == null
                                                    ? null
                                                    : derivedAtts.get(
                                                            dataProvider
                                                                    .getConfiguration()
                                                                    .getKey()),
                                            this)
                                    .setVisible(isEnabledInHierarchy());
                        }
                        return null;
                    }

                    private void removeFields(
                            AjaxRequestTarget target,
                            IModel<ComplexMetadataAttribute<String>> itemModel) {
                        ComplexMetadataAttribute<String> object = itemModel.getObject();
                        dataProvider.removeField(object);
                        updateTable(dataProvider);
                        ((MarkupContainer) get("listContainer").get("items")).removeAll();
                        target.add(this);
                        target.add(RepeatableAttributesTablePanel.this);
                    }
                };
        return tablePanel;
    }

    private void updateTable(RepeatableAttributeDataProvider<String> dataProvider) {
        boolean isEmpty = dataProvider.getItems().isEmpty();
        tablePanel.setVisible(!isEmpty);
        noData.setVisible(isEmpty);
    }
}
