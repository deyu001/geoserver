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

package org.geoserver.security.web.group;

import java.io.IOException;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** A page listing users, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class GroupPanel extends Panel {

    protected GeoServerTablePanel<GeoServerUserGroup> groups;
    protected GeoServerDialog dialog;
    protected SelectionGroupRemovalLink removal, removalWithRoles;
    protected Link<?> add;
    protected String serviceName;

    protected GeoServerUserGroupService getService() {
        try {
            return GeoServerApplication.get()
                    .getSecurityManager()
                    .loadUserGroupService(serviceName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GroupPanel(String id, String serviceName) {
        super(id);

        this.serviceName = serviceName;
        GroupListProvider provider = new GroupListProvider(serviceName);
        add(
                groups =
                        new GeoServerTablePanel<GeoServerUserGroup>("table", provider, true) {

                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<GeoServerUserGroup> itemModel,
                                    Property<GeoServerUserGroup> property) {
                                if (property == GroupListProvider.GROUPNAME) {
                                    return editGroupLink(id, itemModel, property);
                                } else if (property == GroupListProvider.ENABLED) {
                                    if ((Boolean) property.getModel(itemModel).getObject())
                                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                                    else return new Label(id, "");
                                }
                                throw new RuntimeException("Uknown property " + property);
                            }

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                removal.setEnabled(groups.getSelection().size() > 0);
                                target.add(removal);
                                removalWithRoles.setEnabled(groups.getSelection().size() > 0);
                                target.add(removalWithRoles);
                            }
                        });
        groups.setItemReuseStrategy(new DefaultItemReuseStrategy());
        groups.setOutputMarkupId(true);
        add(dialog = new GeoServerDialog("dialog"));
        headerComponents();
    }

    public GroupPanel setHeaderVisible(boolean visible) {
        get("header").setVisible(visible);
        return this;
    }

    public GroupPanel setPagersVisible(boolean top, boolean bottom) {
        groups.getTopPager().setVisible(top);
        groups.getBottomPager().setVisible(bottom);
        return this;
    }

    protected void headerComponents() {

        boolean canCreateStore = getService().canCreateStore();
        // the add button

        WebMarkupContainer h = new WebMarkupContainer("header");
        add(h);
        if (!canCreateStore) {
            h.add(
                    new Label("message", new StringResourceModel("noCreateStore", this, null))
                            .add(new AttributeAppender("class", new Model<>("info-link"), " ")));
        } else {
            h.add(new Label("message", new Model()));
        }

        h.add(
                add =
                        new Link("addNew") {
                            @Override
                            public void onClick() {
                                setResponsePage(
                                        new NewGroupPage(serviceName).setReturnPage(getPage()));
                            }
                        });
        add.setVisible(canCreateStore);

        // the removal button
        h.add(
                removal =
                        new SelectionGroupRemovalLink(
                                serviceName, "removeSelected", groups, dialog, false));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        removal.setVisibilityAllowed(canCreateStore);

        // the removal button
        h.add(
                removalWithRoles =
                        new SelectionGroupRemovalLink(
                                serviceName, "removeSelectedWithRoles", groups, dialog, true));
        removalWithRoles.setOutputMarkupId(true);
        removalWithRoles.setEnabled(false);
        removalWithRoles.setVisibilityAllowed(
                canCreateStore
                        && GeoServerApplication.get()
                                .getSecurityManager()
                                .getActiveRoleService()
                                .canCreateStore());

        // enable header only for full admin
        h.setEnabled(getService().getSecurityManager().checkAuthenticationForAdminRole());
    }

    Component editGroupLink(
            String id,
            IModel<GeoServerUserGroup> itemModel,
            Property<GeoServerUserGroup> property) {
        return new SimpleAjaxLink<GeoServerUserGroup>(id, itemModel, property.getModel(itemModel)) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                setResponsePage(
                        new EditGroupPage(serviceName, getModelObject()).setReturnPage(getPage()));
            }
        };
    }

    protected void onBeforeRender() {
        groups.clearSelection();
        removal.setEnabled(false);
        removalWithRoles.setEnabled(false);
        super.onBeforeRender();
    }
}
