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

package org.geoserver.gwc.web;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.GeoserverAjaxSubmitLink;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geotools.image.io.ImageIOExt;
import org.geotools.util.logging.Logging;

public class GWCSettingsPage extends GeoServerSecuredPage {

    private static final Logger LOGGER = Logging.getLogger(GWCSettingsPage.class);

    public GWCSettingsPage() {
        setHeaderPanel(headerPanel());

        GWC gwc = GWC.get();
        // use a detached copy of gwc config to support the tabbed pane
        final GWCConfig gwcConfig = gwc.getConfig().clone();

        IModel<GWCConfig> formModel = new Model<>(gwcConfig);

        final Form<GWCConfig> form = new Form<>("form", formModel);
        add(form);

        final GWCServicesPanel gwcServicesPanel =
                new GWCServicesPanel("gwcServicesPanel", formModel);
        final CachingOptionsPanel defaultCachingOptionsPanel =
                new CachingOptionsPanel("cachingOptionsPanel", formModel);

        form.add(gwcServicesPanel);
        form.add(defaultCachingOptionsPanel);

        form.add(
                new Button("submit") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        save(form, true);
                    }
                });
        form.add(applyLink(form));
        form.add(
                new GeoServerAjaxFormLink("cancel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onClick(
                            AjaxRequestTarget target, @SuppressWarnings("rawtypes") Form form) {
                        doReturn();
                    }
                });

        checkWarnings();
    }

    public void save(Form<GWCConfig> form, boolean doReturn) {
        GWC gwc = GWC.get();
        final IModel<GWCConfig> gwcConfigModel = form.getModel();
        GWCConfig gwcConfig = gwcConfigModel.getObject();
        try {
            gwc.saveConfig(gwcConfig);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error saving GWC config", e);
            form.error("Error saving GWC config: " + e.getMessage());
            return;
        }
        // Update ConfigurableBlobStore
        ConfigurableBlobStore blobstore = GeoServerExtensions.bean(ConfigurableBlobStore.class);
        if (blobstore != null) {
            blobstore.setChanged(gwcConfig, false);
        }
        // Do return
        if (doReturn) doReturn();
    }

    private GeoserverAjaxSubmitLink applyLink(Form form) {
        return new GeoserverAjaxSubmitLink("apply", form, this) {

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                super.onError(target, form);
                target.add(form);
            }

            @Override
            protected void onSubmitInternal(AjaxRequestTarget target, Form<?> form) {
                try {
                    @SuppressWarnings("unchecked")
                    Form<GWCConfig> cast = (Form<GWCConfig>) form;
                    save(cast, false);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.add(form);
                }
            }
        };
    }

    private void checkWarnings() {
        Long imageIOFileCachingThreshold = ImageIOExt.getFilesystemThreshold();
        if (null == imageIOFileCachingThreshold || 0L >= imageIOFileCachingThreshold.longValue()) {
            String warningMsg =
                    new ResourceModel("GWC.ImageIOFileCachingThresholdUnsetWarning").getObject();
            super.warn(warningMsg);
        }
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);
        return header;
    }

    static CheckBox checkbox(String id, IModel<Boolean> model, String titleKey) {
        CheckBox checkBox = new CheckBox(id, model);
        if (null != titleKey) {
            AttributeModifier attributeModifier =
                    new AttributeModifier("title", new StringResourceModel(titleKey, null, null));
            checkBox.add(attributeModifier);
        }
        return checkBox;
    }
}
