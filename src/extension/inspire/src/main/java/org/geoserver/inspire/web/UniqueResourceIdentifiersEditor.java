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

package org.geoserver.inspire.web;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.inspire.UniqueResourceIdentifier;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.URIValidator;
import org.geoserver.wfs.WFSInfo;

/**
 * Shows and allows editing of the {@link UniqueResourceIdentifiers} attached to a {@link WFSInfo}
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class UniqueResourceIdentifiersEditor extends FormComponentPanel<UniqueResourceIdentifiers> {

    private GeoServerTablePanel<UniqueResourceIdentifier> identifiers;
    private AjaxButton button;

    /** @param identifiersModel Must return a {@link ResourceInfo} */
    public UniqueResourceIdentifiersEditor(
            String id, final IModel<UniqueResourceIdentifiers> identifiersModel) {
        super(id, identifiersModel);

        if (identifiersModel.getObject() == null) {
            identifiersModel.setObject(new UniqueResourceIdentifiers());
        }

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        UniqueResourceIdentifiersProvider provider =
                new UniqueResourceIdentifiersProvider(identifiersModel.getObject());

        // the link list
        identifiers =
                new GeoServerTablePanel<UniqueResourceIdentifier>("identifiers", provider, false) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<UniqueResourceIdentifier> itemModel,
                            Property<UniqueResourceIdentifier> property) {
                        String name = property.getName();
                        if ("code".equals(name)) {
                            Fragment codeFragment =
                                    new Fragment(
                                            id,
                                            "txtFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            FormComponentFeedbackBorder codeBorder =
                                    new FormComponentFeedbackBorder("border");
                            codeFragment.add(codeBorder);
                            TextField<String> code =
                                    new TextField<>("txt", new PropertyModel<>(itemModel, "code"));
                            code.setLabel(
                                    new ParamResourceModel(
                                            "th.code", UniqueResourceIdentifiersEditor.this));
                            code.setRequired(true);
                            codeBorder.add(code);
                            return codeFragment;
                        } else if ("namespace".equals(name)) {
                            Fragment nsFragment =
                                    new Fragment(
                                            id,
                                            "txtFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            FormComponentFeedbackBorder namespaceBorder =
                                    new FormComponentFeedbackBorder("border");
                            nsFragment.add(namespaceBorder);
                            TextField<String> namespace =
                                    new TextField<>(
                                            "txt", new PropertyModel<>(itemModel, "namespace"));
                            namespace.setLabel(
                                    new ParamResourceModel(
                                            "th.namespace", UniqueResourceIdentifiersEditor.this));
                            namespace.add(new URIValidator());
                            namespaceBorder.add(namespace);
                            return nsFragment;
                        } else if ("metadataURL".equals(name)) {
                            Fragment urlFragment =
                                    new Fragment(
                                            id,
                                            "txtFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            FormComponentFeedbackBorder namespaceBorder =
                                    new FormComponentFeedbackBorder("border");
                            urlFragment.add(namespaceBorder);
                            TextField<String> url =
                                    new TextField<>(
                                            "txt", new PropertyModel<>(itemModel, "metadataURL"));
                            url.add(new URIValidator());
                            namespaceBorder.add(url);
                            return urlFragment;
                        } else if ("remove".equals(name)) {
                            Fragment removeFragment =
                                    new Fragment(
                                            id,
                                            "removeFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            GeoServerAjaxFormLink removeLink =
                                    new GeoServerAjaxFormLink("remove") {

                                        @Override
                                        protected void onClick(
                                                AjaxRequestTarget target, Form form) {
                                            UniqueResourceIdentifiers identifiers =
                                                    provider.getItems();
                                            UniqueResourceIdentifier sdi = itemModel.getObject();
                                            identifiers.remove(sdi);
                                            target.add(container);
                                        }
                                    };
                            removeFragment.add(removeLink);
                            return removeFragment;
                        }
                        return null;
                    }
                };
        identifiers.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        identifiers.setPageable(false);
        identifiers.setSortable(false);
        identifiers.setFilterable(false);
        container.add(identifiers);

        // add new link button
        button =
                new AjaxButton("addIdentifier") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        UniqueResourceIdentifiersProvider provider =
                                (UniqueResourceIdentifiersProvider) identifiers.getDataProvider();
                        provider.getItems().add(new UniqueResourceIdentifier());

                        target.add(container);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        // the form validator triggered, but we don't want the msg to display
                        Session.get()
                                .getFeedbackMessages()
                                .clear(); // formally cleanupFeedbackMessages()
                        Session.get().dirty();
                        onSubmit(target, form);
                    }
                };
        add(button);

        // grab a seat... the way I'm adding this validator in onBeforeRender will be hard
        // to stomach... however, could not find other way to add a validation to an editabl table,
        // grrr
        add(
                new IValidator<UniqueResourceIdentifiers>() {

                    @Override
                    public void validate(IValidatable<UniqueResourceIdentifiers> validatable) {
                        UniqueResourceIdentifiers identifiers = provider.getItems();
                        if (identifiers.size() == 0) {
                            ValidationError error = new ValidationError();
                            String message =
                                    new ParamResourceModel(
                                                    "noSpatialDatasetIdentifiers",
                                                    UniqueResourceIdentifiersEditor.this)
                                            .getString();
                            error.setMessage(message);
                            validatable.error(error);
                        }
                    }
                });
    }

    @Override
    public void convertInput() {
        UniqueResourceIdentifiersProvider provider =
                (UniqueResourceIdentifiersProvider) identifiers.getDataProvider();
        setConvertedInput(provider.getItems());
    }
}
