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

package org.geogig.geoserver.web.security;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geogig.geoserver.config.WhitelistRule;

public class WhitelistRuleEditor extends Panel {

    private static final long serialVersionUID = 1508347689900059344L;

    private boolean isNew;

    public WhitelistRuleEditor(
            String id,
            IModel<WhitelistRule> model,
            final ModalWindow window,
            final WhitelistRulePanel table,
            boolean isNew) {
        super(id, model);
        this.isNew = isNew;
        Form<WhitelistRule> form = new Form<>("form", model);
        add(form);
        TextField<String> name = new TextField<>("name", new PropertyModel<>(model, "name"));
        name.setRequired(true);
        TextField<String> pattern =
                new TextField<>("pattern", new PropertyModel<>(model, "pattern"));
        pattern.setRequired(true);

        form.add(name);
        form.add(pattern);
        form.add(new CheckBox("requireSSL", new PropertyModel<>(model, "requireSSL")));

        final FeedbackPanel feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        form.add(feedbackPanel);

        form.add(
                new AjaxSubmitLink("submit", form) {

                    private static final long serialVersionUID = 1080309070367012502L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        target.add(feedbackPanel);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        boolean isNew = WhitelistRuleEditor.this.isNew;
                        if (isNew) {
                            WhitelistRule rule = (WhitelistRule) form.getModelObject();
                            table.add(rule);
                        }
                        table.save();
                        window.close(target);
                        target.add(table);
                    }
                });

        form.add(
                new AjaxLink<Void>("cancel") {
                    private static final long serialVersionUID = 4762717512666965125L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        window.close(target);
                        target.add(table);
                    }
                });
    }
}
