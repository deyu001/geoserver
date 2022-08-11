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

package org.geoserver.web.data.workspace;

import static org.geoserver.web.data.workspace.WorkspaceProvider.DEFAULT;
import static org.geoserver.web.data.workspace.WorkspaceProvider.ISOLATED;
import static org.geoserver.web.data.workspace.WorkspaceProvider.NAME;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/** Lists available workspaces, links to them, allows for addition and removal */
public class WorkspacePage extends GeoServerSecuredPage {
    private static final long serialVersionUID = 3084639304127909774L;
    WorkspaceProvider provider = new WorkspaceProvider();
    GeoServerTablePanel<WorkspaceInfo> table;
    GeoServerDialog dialog;
    SelectionRemovalLink removal;

    public WorkspacePage() {
        // the middle table
        add(
                table =
                        new GeoServerTablePanel<WorkspaceInfo>("table", provider, true) {
                            private static final long serialVersionUID = 8028081894753417294L;

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<WorkspaceInfo> itemModel,
                                    Property<WorkspaceInfo> property) {
                                if (property == NAME) {
                                    return workspaceLink(id, itemModel);
                                } else if (property == DEFAULT) {
                                    if (getCatalog()
                                            .getDefaultWorkspace()
                                            .equals(itemModel.getObject()))
                                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                                    else return new Label(id, "");
                                } else if (property == ISOLATED) {
                                    if (itemModel.getObject().isIsolated())
                                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                                    else return new Label(id, "");
                                } else if (property == WorkspaceProvider.MODIFIED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            WorkspaceProvider.MODIFIED_TIMESTAMP.getModel(
                                                    itemModel));
                                } else if (property == WorkspaceProvider.CREATED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            WorkspaceProvider.CREATED_TIMESTAMP.getModel(
                                                    itemModel));
                                }

                                throw new IllegalArgumentException(
                                        "No such property " + property.getName());
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                removal.setEnabled(table.getSelection().size() > 0);
                                target.add(removal);
                            }
                        });
        table.setOutputMarkupId(true);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<WorkspaceNewPage>("addNew", WorkspaceNewPage.class));

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        // check for full admin, we don't allow workspace admins to add new workspaces
        header.setEnabled(isAuthenticatedAsAdmin());
        return header;
    }

    Component workspaceLink(String id, final IModel<WorkspaceInfo> itemModel) {
        IModel<?> nameModel = NAME.getModel(itemModel);
        return new SimpleBookmarkableLink(
                id, WorkspaceEditPage.class, nameModel, "name", (String) nameModel.getObject());
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
