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

package org.geoserver.taskmanager.web.panel;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.wicket.ParamResourceModel;

public class DropDownPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;

    public DropDownPanel(
            String id,
            IModel<String> model,
            IModel<? extends List<? extends String>> choiceModel,
            boolean nullValid) {
        this(id, model, choiceModel, null, nullValid);
    }

    public DropDownPanel(
            String id,
            Model<String> model,
            Model<ArrayList<String>> choiceModel,
            ParamResourceModel paramResourceModel) {
        this(id, model, choiceModel, paramResourceModel, false);
    }

    public DropDownPanel(
            String id,
            IModel<String> model,
            IModel<? extends List<? extends String>> choiceModel,
            IModel<String> labelModel,
            boolean nullValid) {
        super(id, model);

        boolean custom = choiceModel.getObject().contains("");
        boolean inList = choiceModel.getObject().contains(model.getObject());
        boolean useDropDown = !custom || inList;
        IModel<? extends List<? extends String>> newChoiceModel = choiceModel;
        if (!custom && !inList && model.getObject() != null) {
            ArrayList<String> list = new ArrayList<>();
            list.addAll(choiceModel.getObject());
            list.add(model.getObject());
            newChoiceModel = new Model<ArrayList<? extends String>>(list);
        }
        add(new Label("message", labelModel));
        add(
                new DropDownChoice<String>(
                                "dropdown",
                                useDropDown ? model : new Model<String>(""),
                                newChoiceModel)
                        .setNullValid(nullValid && !custom));
        add(new TextField<String>("custom", model).setVisible(!useDropDown));

        if (custom) {
            setOutputMarkupId(true);
            getDropDownChoice()
                    .add(
                            new OnChangeAjaxBehavior() {
                                private static final long serialVersionUID = 7823984472638368286L;

                                @Override
                                protected void onUpdate(AjaxRequestTarget target) {
                                    boolean useDropDown =
                                            !getDropDownChoice().getModelObject().equals("");
                                    if (useDropDown) {
                                        model.setObject(getDropDownChoice().getModelObject());
                                        getDropDownChoice().setModel(model);
                                    } else {
                                        getDropDownChoice().setModel(new Model<String>(""));
                                    }
                                    getTextField().setVisible(!useDropDown);
                                    target.add(DropDownPanel.this);
                                }
                            });
        } else if (!inList) {
            getDropDownChoice()
                    .add(
                            new OnChangeAjaxBehavior() {
                                private static final long serialVersionUID = -7816987770470912413L;

                                @Override
                                protected void onUpdate(AjaxRequestTarget target) {
                                    getDropDownChoice().setChoices(choiceModel);
                                }
                            });
        }
    }

    @SuppressWarnings("unchecked")
    public DropDownChoice<String> getDropDownChoice() {
        return (DropDownChoice<String>) get("dropdown");
    }

    @SuppressWarnings("unchecked")
    public TextField<String> getTextField() {
        return (TextField<String>) get("custom");
    }
}
