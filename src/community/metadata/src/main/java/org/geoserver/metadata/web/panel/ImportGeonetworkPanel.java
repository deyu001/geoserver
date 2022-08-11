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

package org.geoserver.metadata.web.panel;

import java.util.ArrayList;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.metadata.data.dto.GeonetworkConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * A panel that lets the user select a geonetwork endpoint and input a uuid of the metadata record
 * in geonetwork.
 *
 * <p>The available geonetwork endpoints are configured in the yaml.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public abstract class ImportGeonetworkPanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    public ImportGeonetworkPanel(String id) {
        super(id);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog = new GeoServerDialog("importDialog");
        dialog.setInitialHeight(100);
        add(dialog);
        add(
                new FeedbackPanel("importFeedback", new ContainerFeedbackMessageFilter(this))
                        .setOutputMarkupId(true));

        DropDownChoice<String> dropDown = createDropDown();
        dropDown.setNullValid(true);
        add(dropDown);

        TextField<String> inputUUID = new TextField<>("textfield", new Model<String>(""));
        add(inputUUID);

        add(createImportAction(dropDown, inputUUID, dialog));
    }

    private AjaxSubmitLink createImportAction(
            final DropDownChoice<String> dropDown,
            final TextField<String> inputUUID,
            GeoServerDialog dialog) {
        return new AjaxSubmitLink("link") {
            private static final long serialVersionUID = -8718015688839770852L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                boolean valid = true;
                if (dropDown.getModelObject() == null || "".equals(dropDown.getModelObject())) {
                    error(
                            new ParamResourceModel(
                                            "errorSelectGeonetwork", ImportGeonetworkPanel.this)
                                    .getString());
                    valid = false;
                }
                final String uuId = inputUUID.getValue();
                if ("".equals(uuId)) {
                    error(
                            new ParamResourceModel("errorUuidRequired", ImportGeonetworkPanel.this)
                                    .getString());
                    valid = false;
                }
                if (valid) {
                    dialog.setTitle(
                            new ParamResourceModel(
                                    "confirmImportDialog.title", ImportGeonetworkPanel.this));
                    dialog.showOkCancel(
                            target,
                            new GeoServerDialog.DialogDelegate() {

                                private static final long serialVersionUID = -5552087037163833563L;

                                @Override
                                protected Component getContents(String id) {
                                    ParamResourceModel resource =
                                            new ParamResourceModel(
                                                    "confirmImportDialog.content",
                                                    ImportGeonetworkPanel.this);
                                    return new MultiLineLabel(id, resource.getString());
                                }

                                @Override
                                protected boolean onSubmit(
                                        AjaxRequestTarget target, Component contents) {
                                    handleImport(
                                            dropDown.getModelObject(),
                                            inputUUID.getModelObject(),
                                            target,
                                            getFeedbackPanel());
                                    return true;
                                }
                            });
                }

                target.add(getFeedbackPanel());
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
            }
        };
    }

    public abstract void handleImport(
            String geoNetwork, String uuid, AjaxRequestTarget target, FeedbackPanel feedbackPanel);

    private DropDownChoice<String> createDropDown() {
        ConfigurationService configService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        MetadataConfiguration configuration = configService.getMetadataConfiguration();
        ArrayList<String> optionsGeonetwork = new ArrayList<>();
        if (configuration != null && configuration.getGeonetworks() != null) {
            for (GeonetworkConfiguration geonetwork : configuration.getGeonetworks()) {
                optionsGeonetwork.add(geonetwork.getName());
            }
        }
        return new DropDownChoice<>("geonetworkName", new Model<String>(""), optionsGeonetwork);
    }

    public FeedbackPanel getFeedbackPanel() {
        return (FeedbackPanel) get("importFeedback");
    }
}
