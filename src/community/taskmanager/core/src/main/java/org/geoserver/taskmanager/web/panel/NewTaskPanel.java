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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.TaskManagerBeans;

public class NewTaskPanel extends Panel {

    private static final long serialVersionUID = -1678565286034119572L;

    public NewTaskPanel(String id, Configuration config) {
        super(id);
        add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        add(new TextField<String>("name", new Model<String>()).setRequired(true));
        add(
                new DropDownChoice<String>(
                                "type",
                                new Model<String>(),
                                new Model<ArrayList<String>>(getTaskTypeNames(config)))
                        .setRequired(true)
                        .setOutputMarkupId(true));
        add(
                new DropDownChoice<String>(
                                "copy",
                                new Model<String>(),
                                new Model<ArrayList<String>>(
                                        new ArrayList<String>(config.getTasks().keySet())))
                        .setOutputMarkupId(true));

        getCopyField()
                .add(
                        new OnChangeAjaxBehavior() {
                            private static final long serialVersionUID = -5575115165929413404L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                if (getCopyField().getConvertedInput() != null) {
                                    getTypeField()
                                            .getModel()
                                            .setObject(
                                                    config.getTasks()
                                                            .get(getCopyField().getConvertedInput())
                                                            .getType());
                                    target.add(getTypeField());
                                }
                            }
                        });

        getTypeField()
                .add(
                        new OnChangeAjaxBehavior() {
                            private static final long serialVersionUID = -1427899086435643578L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                getCopyField().getModel().setObject(null);
                                target.add(getCopyField());
                            }
                        });
    }

    private ArrayList<String> getTaskTypeNames(Configuration config) {
        if (config.isTemplate()) {
            return new ArrayList<String>(TaskManagerBeans.get().getTaskTypes().names());
        } else {
            ArrayList<String> list = new ArrayList<String>();
            for (TaskType taskType : TaskManagerBeans.get().getTaskTypes().all()) {
                if (!taskType.templateOnly()) {
                    list.add(taskType.getName());
                }
            }
            return list;
        }
    }

    @SuppressWarnings("unchecked")
    public DropDownChoice<String> getTypeField() {
        return ((DropDownChoice<String>) get("type"));
    }

    @SuppressWarnings("unchecked")
    public TextField<String> getNameField() {
        return ((TextField<String>) get("name"));
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("feedback");
    }

    @SuppressWarnings("unchecked")
    public DropDownChoice<String> getCopyField() {
        return ((DropDownChoice<String>) get("copy"));
    }
}
