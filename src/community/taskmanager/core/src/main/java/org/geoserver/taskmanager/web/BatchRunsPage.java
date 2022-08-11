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

package org.geoserver.taskmanager.web;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.BatchRunsModel;
import org.geoserver.taskmanager.web.panel.SimpleAjaxSubmitLink;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class BatchRunsPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = -5111795911981486778L;

    private IModel<Batch> batchModel;

    private GeoServerTablePanel<BatchRun> runsPanel;

    public BatchRunsPage(IModel<Batch> batchModel, Page parentPage) {
        this.batchModel = batchModel;
        setReturnPage(parentPage);
    }

    public BatchRunsPage(Batch batch, Page parentPage) {
        this(new Model<Batch>(batch), parentPage);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(
                new SimpleAjaxLink<String>(
                        "nameLink", new PropertyModel<String>(batchModel, "fullName")) {
                    private static final long serialVersionUID = -9184383036056499856L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(new BatchPage(batchModel, getPage()));
                    }
                });

        add(
                new AjaxLink<Object>("refresh") {

                    private static final long serialVersionUID = 3905640474193868255L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        batchModel.setObject(
                                TaskManagerBeans.get()
                                        .getDao()
                                        .initHistory(batchModel.getObject()));
                        ((MarkupContainer) runsPanel.get("listContainer").get("items")).removeAll();
                        target.add(runsPanel);
                    }
                });

        // the tasks panel
        add(new Form<>("form").add(runsPanel = runPanel()));
        runsPanel.setOutputMarkupId(true);
        runsPanel.setSelectable(false);
        runsPanel.getDataProvider().setSort(BatchRunsModel.START.getName(), SortOrder.DESCENDING);

        add(
                new AjaxLink<Object>("close") {
                    private static final long serialVersionUID = -6892944747517089296L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn();
                    }
                });
    }

    protected GeoServerTablePanel<BatchRun> runPanel() {
        return new GeoServerTablePanel<BatchRun>(
                "runsPanel", new BatchRunsModel(batchModel), true) {

            private static final long serialVersionUID = -8943273843044917552L;

            @SuppressWarnings("unchecked")
            @Override
            protected Component getComponentForProperty(
                    String id, IModel<BatchRun> runModel, Property<BatchRun> property) {
                if (property.equals(BatchRunsModel.START)) {
                    return new SimpleAjaxLink<String>(
                            id, (IModel<String>) property.getModel(runModel)) {
                        private static final long serialVersionUID = -9184383036056499856L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            setResponsePage(new BatchRunPage(batchModel, runModel, getPage()));
                        }
                    };
                } else if (property.equals(BatchRunsModel.STOP)) {
                    if (runModel.getObject().getStatus().isClosed()
                            || !TaskManagerBeans.get()
                                    .getSecUtil()
                                    .isWritable(
                                            ((GeoServerSecuredPage) getPage())
                                                    .getSession()
                                                    .getAuthentication(),
                                            runModel.getObject().getBatch())) {
                        return new Label(id);
                    } else {
                        SimpleAjaxSubmitLink link =
                                new SimpleAjaxSubmitLink(id, null) {
                                    private static final long serialVersionUID =
                                            -9184383036056499856L;

                                    @Override
                                    protected void onSubmit(
                                            AjaxRequestTarget target, Form<?> form) {
                                        TaskManagerBeans.get()
                                                .getBjService()
                                                .interrupt(runModel.getObject());
                                        info(
                                                new ParamResourceModel(
                                                                "runInterrupted",
                                                                BatchRunsPage.this)
                                                        .getString());

                                        ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                                    }
                                };
                        link.getLink().add(new AttributeAppender("class", "stop-link", ","));
                        return link;
                    }
                }
                return null;
            }
        };
    }

    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
