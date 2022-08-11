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

package org.geogig.geoserver.web.repository;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.PatternValidator;

public class ConfigEditPanel extends Panel {

    private static final long serialVersionUID = -1015911960516043997L;

    Form<ConfigEntry> form;

    TextField<String> name;

    TextField<String> value;

    ConfigEditPanel(
            String id,
            IModel<ConfigEntry> model,
            final ModalWindow parentWindow,
            final ConfigListPanel table) {
        super(id, model);

        form = new Form<>("form", model);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        boolean isNew = true;
        for (ConfigEntry config : table.getConfigs()) {
            if (config.equals(model.getObject())) {
                isNew = false;
                break;
            }
        }
        final boolean isInTable = !isNew;
        name = new TextField<>("name", new PropertyModel<>(model, "name"));
        name.setRequired(true);
        name.add(new PatternValidator("[^\\s]+"));
        name.add(
                new IValidator<String>() {
                    private static final long serialVersionUID = 2927770353770055054L;

                    final String previousName = isInTable ? form.getModelObject().getName() : null;

                    @Override
                    public void validate(IValidatable<String> validatable) {
                        String name = validatable.getValue();
                        if (ConfigEntry.isRestricted(name)) {
                            form.error(
                                    String.format(
                                            "Modifying %s through this interface can have unintended consequences and is not allowed.",
                                            name));
                        } else {
                            for (ConfigEntry config : table.getConfigs()) {
                                if (!config.equals(model.getObject())) {
                                    String newName = config.getName();
                                    if (newName != null
                                            && !newName.equals(previousName)
                                            && newName.equals(name)) {
                                        form.error(
                                                String.format(
                                                        "A config entry called %s already exists",
                                                        name));
                                    }
                                }
                            }
                        }
                    }
                });

        value = new TextField<>("value", new PropertyModel<>(model, "value"));
        value.setRequired(true);

        form.add(name);
        form.add(value);

        form.add(
                new AjaxSubmitLink("submit", form) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        target.add(feedback);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ConfigEntry newConfig = (ConfigEntry) form.getModelObject();
                        if (!isInTable) {
                            table.add(newConfig);
                        }
                        parentWindow.close(target);
                        target.add(table);
                    }
                });

        form.add(
                new AjaxLink<Void>("cancel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        parentWindow.close(target);
                        target.add(table);
                    }
                });
    }
}
