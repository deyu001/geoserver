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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DataLinkInfoImpl;

/**
 * Shows and allows editing of the {@link DataLinkInfo} attached to a {@link ResourceInfo}
 *
 * @author Marcus Sen - British Geological Survey
 */
@SuppressWarnings("serial")
public class DataLinkEditor extends Panel {

    private ListView<DataLinkInfo> links;
    private Label noData;
    private WebMarkupContainer table;

    /** @param resourceModel Must return a {@link ResourceInfo} */
    public DataLinkEditor(String id, final IModel<ResourceInfo> resourceModel) {
        super(id, resourceModel);

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);
        links =
                new ListView<DataLinkInfo>(
                        "links", new PropertyModel<>(resourceModel, "dataLinks")) {

                    @Override
                    protected void populateItem(ListItem<DataLinkInfo> item) {

                        // odd/even style
                        item.add(
                                AttributeModifier.replace(
                                        "class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                        // link info
                        FormComponentFeedbackBorder urlBorder =
                                new FormComponentFeedbackBorder("urlBorder");
                        item.add(urlBorder);
                        TextField<String> format =
                                new TextField<>(
                                        "format", new PropertyModel<>(item.getModel(), "type"));
                        format.setRequired(true);
                        item.add(format);
                        TextField<String> url =
                                new TextField<>(
                                        "dataLinkURL",
                                        new PropertyModel<>(item.getModel(), "content"));
                        url.add(new UrlValidator());
                        url.setRequired(true);
                        urlBorder.add(url);

                        // remove link
                        AjaxLink<DataLinkInfo> link =
                                new AjaxLink<DataLinkInfo>("removeLink", item.getModel()) {

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        ResourceInfo ri = resourceModel.getObject();
                                        ri.getDataLinks().remove(getModelObject());
                                        updateLinksVisibility();
                                        target.add(container);
                                    }
                                };
                        item.add(link);
                    }
                };
        // this is necessary to avoid loosing item contents on edit/validation checks
        links.setReuseItems(true);
        table.add(links);

        // the no data links label
        noData = new Label("noLinks", new ResourceModel("noDataLinksSoFar"));
        container.add(noData);
        updateLinksVisibility();

        // add new link button
        AjaxButton button =
                new AjaxButton("addlink") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ResourceInfo ri = resourceModel.getObject();
                        DataLinkInfo link = ri.getCatalog().getFactory().createDataLink();
                        link.setType("text/plain");
                        ri.getDataLinks().add(link);
                        updateLinksVisibility();

                        target.add(container);
                    }
                };
        add(button);
    }

    private void updateLinksVisibility() {
        ResourceInfo ri = (ResourceInfo) getDefaultModelObject();
        boolean anyLink = ri.getDataLinks().size() > 0;
        table.setVisible(anyLink);
        noData.setVisible(!anyLink);
    }

    public class UrlValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> validatable) {
            String url = validatable.getValue();
            if (url != null) {
                try {
                    DataLinkInfoImpl.validate(url);
                } catch (IllegalArgumentException ex) {
                    IValidationError err =
                            new ValidationError("invalidDataLinkURL")
                                    .addKey("invalidDataLinkURL")
                                    .setVariable("url", url);
                    validatable.error(err);
                }
            }
        }
    }
}
