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

package org.geoserver.web.admin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.web.CatalogIconFactory;

public class ModuleStatusPanel extends Panel {

    private static final long serialVersionUID = 3892224318224575781L;

    final CatalogIconFactory icons = CatalogIconFactory.get();

    ModalWindow popup;

    AjaxLink msgLink;

    public ModuleStatusPanel(String id, AbstractStatusPage parent) {
        super(id);
        initUI();
    }

    public void initUI() {

        final WebMarkupContainer wmc = new WebMarkupContainer("listViewContainer");
        wmc.setOutputMarkupId(true);
        this.add(wmc);

        popup = new ModalWindow("popup");
        add(popup);

        // get the list of ModuleStatuses
        List<ModuleStatus> applicationStatus =
                GeoServerExtensions.extensions(ModuleStatus.class)
                        .stream()
                        .filter(status -> !status.getModule().matches("\\A[system-](.*)"))
                        .map(ModuleStatusImpl::new)
                        .sorted(Comparator.comparing(ModuleStatus::getModule))
                        .collect(Collectors.toList());

        final ListView<ModuleStatus> moduleView =
                new ListView<ModuleStatus>("modules", applicationStatus) {
                    private static final long serialVersionUID = 235576083712961710L;

                    @Override
                    protected void populateItem(ListItem<ModuleStatus> item) {
                        item.add(new Label("module", new PropertyModel(item.getModel(), "module")));
                        item.add(getIcons("available", item.getModelObject().isAvailable()));
                        item.add(getIcons("enabled", item.getModelObject().isEnabled()));
                        item.add(
                                new Label(
                                        "component",
                                        new Model<>(
                                                item.getModelObject().getComponent().orElse(""))));
                        item.add(
                                new Label(
                                        "version",
                                        new Model<>(
                                                item.getModelObject().getVersion().orElse(""))));
                        msgLink =
                                new AjaxLink("msg") {
                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        popup.setInitialHeight(325);
                                        popup.setInitialWidth(525);
                                        popup.setContent(
                                                new MessagePanel(popup.getContentId(), item));
                                        popup.setTitle("Module Info");
                                        popup.show(target);
                                    }
                                };
                        msgLink.setEnabled(true);
                        msgLink.add(
                                new Label("nameLink", new PropertyModel(item.getModel(), "name")));
                        item.add(msgLink);
                    }
                };
        wmc.add(moduleView);
    }

    final Fragment getIcons(String id, boolean status) {
        PackageResourceReference icon = status ? icons.getEnabledIcon() : icons.getDisabledIcon();
        Fragment f = new Fragment(id, "iconFragment", this);
        f.add(new Image("statusIcon", icon));
        return f;
    };

    class MessagePanel extends Panel {

        private static final long serialVersionUID = -3200098674603724915L;

        public MessagePanel(String id, ListItem<ModuleStatus> item) {
            super(id);

            Label name = new Label("name", new PropertyModel(item.getModel(), "name"));
            Label module = new Label("module", new PropertyModel(item.getModel(), "module"));
            Label component =
                    new Label(
                            "component",
                            new Model<>(item.getModelObject().getComponent().orElse("")));
            Label version =
                    new Label(
                            "version", new Model<>(item.getModelObject().getVersion().orElse("")));
            MultiLineLabel msgLabel =
                    new MultiLineLabel("msg", item.getModelObject().getMessage().orElse(""));

            add(name);
            add(module);
            add(component);
            add(version);
            add(msgLabel);
        }
    }
}
