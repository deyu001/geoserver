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

package org.geoserver.wms.web.data;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.DateTimeLabel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/** Page listing all the styles, allows to edit, add, remove styles */
@SuppressWarnings("serial")
public class StylePage extends GeoServerSecuredPage {

    GeoServerTablePanel<StyleInfo> table;

    SelectionRemovalLink removal;

    GeoServerDialog dialog;

    public StylePage() {
        StyleProvider provider = new StyleProvider();
        add(
                table =
                        new GeoServerTablePanel<StyleInfo>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<StyleInfo> itemModel,
                                    Property<StyleInfo> property) {

                                if (property == StyleProvider.NAME) {
                                    return styleLink(id, itemModel);
                                }
                                if (property == StyleProvider.WORKSPACE) {
                                    return workspaceLink(id, itemModel);
                                }
                                if (property == StyleProvider.MODIFIED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            StyleProvider.MODIFIED_TIMESTAMP.getModel(itemModel));
                                }
                                if (property == StyleProvider.CREATED_TIMESTAMP) {
                                    return new DateTimeLabel(
                                            id,
                                            StyleProvider.CREATED_TIMESTAMP.getModel(itemModel));
                                }
                                return null;
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
        header.add(new BookmarkablePageLink<StyleNewPage>("addNew", StyleNewPage.class));

        // the removal button
        header.add(
                removal =
                        new SelectionRemovalLink("removeSelected", table, dialog) {
                            @Override
                            protected StringResourceModel canRemove(CatalogInfo object) {
                                if (isDefaultStyle(object)) {
                                    return new StringResourceModel(
                                            "cantRemoveDefaultStyle", StylePage.this, null);
                                }
                                return null;
                            }
                        });
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    Component styleLink(String id, IModel<StyleInfo> model) {
        IModel<?> nameModel = StyleProvider.NAME.getModel(model);
        IModel<?> wsModel = StyleProvider.WORKSPACE.getModel(model);

        String name = (String) nameModel.getObject();
        String wsName = (String) wsModel.getObject();

        return new SimpleBookmarkableLink(
                id,
                StyleEditPage.class,
                nameModel,
                StyleEditPage.NAME,
                name,
                StyleEditPage.WORKSPACE,
                wsName);
    }

    Component workspaceLink(String id, IModel<StyleInfo> model) {
        IModel<?> wsNameModel = StyleProvider.WORKSPACE.getModel(model);
        String wsName = (String) wsNameModel.getObject();
        if (wsName != null) {
            return new SimpleBookmarkableLink(
                    id, WorkspaceEditPage.class, new Model<>(wsName), "name", wsName);
        } else {
            return new WebMarkupContainer(id);
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    protected static boolean isDefaultStyle(CatalogInfo catalogInfo) {
        if (catalogInfo instanceof StyleInfo) {
            StyleInfo s = (StyleInfo) catalogInfo;

            return s.getWorkspace() == null
                    && (StyleInfo.DEFAULT_POINT.equals(s.getName())
                            || StyleInfo.DEFAULT_LINE.equals(s.getName())
                            || StyleInfo.DEFAULT_POLYGON.equals(s.getName())
                            || StyleInfo.DEFAULT_RASTER.equals(s.getName())
                            || StyleInfo.DEFAULT_GENERIC.equals(s.getName()));
        } else {
            return false;
        }
    }
}
