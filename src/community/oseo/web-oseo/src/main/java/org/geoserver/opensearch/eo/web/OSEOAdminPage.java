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

package org.geoserver.opensearch.eo.web;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.ParamResourceModel;

public class OSEOAdminPage extends BaseServiceAdminPage<OSEOInfo> {

    private static final long serialVersionUID = 3056925400600634877L;

    DataStoreInfo backend;
    GeoServerTablePanel<ProductClass> productClasses;
    private IModel<OSEOInfo> model;

    public OSEOAdminPage() {
        super();
    }

    public OSEOAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public OSEOAdminPage(OSEOInfo service) {
        super(service);
    }

    protected Class<OSEOInfo> getServiceClass() {
        return OSEOInfo.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked", "serial"})
    protected void build(final IModel info, Form form) {
        this.model = info;
        OSEOInfo oseo = (OSEOInfo) info.getObject();

        this.backend = null;
        if (oseo.getOpenSearchAccessStoreId() != null) {
            this.backend = getCatalog().getDataStore(oseo.getOpenSearchAccessStoreId());
        }
        DropDownChoice<DataStoreInfo> openSearchAccessReference =
                new DropDownChoice<>(
                        "openSearchAccessId",
                        new PropertyModel<DataStoreInfo>(this, "backend"),
                        new OpenSearchAccessListModel(),
                        new StoreListChoiceRenderer());
        form.add(openSearchAccessReference);

        final TextField<Integer> recordsPerPage = new TextField<>("recordsPerPage", Integer.class);
        recordsPerPage.add(RangeValidator.minimum(0));
        recordsPerPage.setRequired(true);
        form.add(recordsPerPage);
        final TextField<Integer> maximumRecordsPerPage =
                new TextField<>("maximumRecordsPerPage", Integer.class);
        maximumRecordsPerPage.add(RangeValidator.minimum(0));
        maximumRecordsPerPage.setRequired(true);
        form.add(maximumRecordsPerPage);
        // check that records is lower or equal than maximum
        form.add(
                new AbstractFormValidator() {

                    @Override
                    public void validate(Form<?> form) {
                        Integer records = recordsPerPage.getConvertedInput();
                        Integer maximum = maximumRecordsPerPage.getConvertedInput();
                        if (recordsPerPage != null && maximum != null && records > maximum) {
                            form.error(
                                    new ParamResourceModel(
                                            "recordsGreaterThanMaximum", form, records, maximum));
                        }

                        // doing the validation here, as just making the text fields as
                        // required makes one lose edits, and error messages do not show up
                        productClasses.processInputs();
                        List<ProductClass> productClasses = oseo.getProductClasses();
                        for (ProductClass pc : productClasses) {
                            if (Strings.isEmpty(pc.getName())
                                    || Strings.isEmpty(pc.getPrefix())
                                    || Strings.isEmpty(pc.getNamespace())) {
                                form.error(
                                        new ParamResourceModel("paramClassNotEmpty", form)
                                                .getString());
                                break;
                            }
                        }
                    }

                    @Override
                    public FormComponent<?>[] getDependentFormComponents() {
                        return new FormComponent<?>[] {recordsPerPage, maximumRecordsPerPage};
                    }
                });

        productClasses =
                new GeoServerTablePanel<ProductClass>(
                        "productClasses", new ProductClassesProvider(info), true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<ProductClass> itemModel,
                            GeoServerDataProvider.Property<ProductClass> property) {
                        if (ProductClassesProvider.REMOVE.equals(property)) {
                            return removeLink(id, itemModel);
                        } else {
                            Fragment f;
                            if ("namespace".equals(property.getName())) {
                                f = new Fragment(id, "longtext", OSEOAdminPage.this);
                            } else {
                                f = new Fragment(id, "text", OSEOAdminPage.this);
                            }
                            TextField<?> text = new TextField("text", property.getModel(itemModel));
                            f.add(text);
                            return f;
                        }
                    }
                };
        productClasses.setFilterVisible(false);
        productClasses.setSortable(false);
        productClasses.setPageable(false);
        productClasses.setOutputMarkupId(true);
        productClasses.setItemReuseStrategy(new DefaultItemReuseStrategy());
        productClasses.setFilterable(false);
        productClasses.setSelectable(false);
        form.add(productClasses);
        form.add(new HelpLink("productClassesHelp", this).setDialog(dialog));

        form.add(addLink());
    }

    private GeoServerAjaxFormLink addLink() {
        return new GeoServerAjaxFormLink("addClass") {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                productClasses.processInputs();
                OSEOInfo oseo = model.getObject();
                oseo.getProductClasses().add(new ProductClass("", "", ""));
                target.add(productClasses);
            }
        };
    }

    private Component removeLink(String id, IModel<ProductClass> itemModel) {
        Fragment f = new Fragment(id, "imageLink", OSEOAdminPage.this);
        final ProductClass entry = itemModel.getObject();
        GeoServerAjaxFormLink link =
                new GeoServerAjaxFormLink("link") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form form) {
                        productClasses.processInputs();
                        OSEOInfo oseo = model.getObject();
                        oseo.getProductClasses().remove(entry);
                        target.add(productClasses);
                    }
                };
        f.add(link);
        Image image =
                new Image(
                        "image",
                        new PackageResourceReference(
                                GeoServerBasePage.class, "img/icons/silk/delete.png"));
        link.add(image);
        return f;
    }

    protected String getServiceName() {
        return "OSEO";
    }

    @Override
    protected void handleSubmit(OSEOInfo info) {
        if (backend != null) {
            info.setOpenSearchAccessStoreId(backend.getId());
        } else {
            info.setOpenSearchAccessStoreId(null);
        }
        super.handleSubmit(info);
    }
}
