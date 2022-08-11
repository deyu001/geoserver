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

package org.geoserver.security.web.user;

import java.io.IOException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;

/** A page listing users, allowing for removal, addition and linking to an edit page */
@SuppressWarnings("serial")
public class UserPanel extends Panel {

    protected GeoServerTablePanel<GeoServerUser> users;
    protected GeoServerDialog dialog;
    protected SelectionUserRemovalLink removal, removalWithRoles;
    protected Link<NewUserPage> add;
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

    public UserPanel(String id, String serviceName) {
        super(id);

        this.serviceName = serviceName;
        UserListProvider provider = new UserListProvider(this.serviceName);
        add(
                users =
                        new UserTablePanel("table", serviceName, provider, true) {
                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                removal.setEnabled(users.getSelection().size() > 0);
                                target.add(removal);
                                removalWithRoles.setEnabled(users.getSelection().size() > 0);
                                target.add(removalWithRoles);
                            }
                        });
        users.setOutputMarkupId(true);
        add(dialog = new GeoServerDialog("dialog"));
        headerComponents();
    }

    public UserPanel setHeaderVisible(boolean visible) {
        get("header").setVisible(visible);
        return this;
    }

    public UserPanel setPagersVisible(boolean top, boolean bottom) {
        users.getTopPager().setVisible(top);
        users.getBottomPager().setVisible(bottom);
        return this;
    }

    protected void headerComponents() {

        boolean canCreateStore = getService().canCreateStore();

        WebMarkupContainer h = new WebMarkupContainer("header");
        add(h);

        if (!canCreateStore) {
            h.add(
                    new Label("message", new StringResourceModel("noCreateStore", this, null))
                            .add(new AttributeAppender("class", new Model<>("info-link"), " ")));
        } else {
            h.add(
                    new Label("message", new Model())
                            .add(new AttributeAppender("class", new Model<>("displayNone"), " ")));
        }

        // the add button
        h.add(
                add =
                        new Link<NewUserPage>("addNew") {
                            @Override
                            public void onClick() {
                                setResponsePage(
                                        new NewUserPage(serviceName).setReturnPage(this.getPage()));
                            }
                        });

        // <NewUserPage><NewUserPage>("addNew", NewUserPage.class));
        // add.setParameter(AbstractSecurityPage.ServiceNameKey, serviceName);
        add.setVisible(canCreateStore);

        // the removal button
        h.add(
                removal =
                        new SelectionUserRemovalLink(
                                serviceName, "removeSelected", users, dialog, false));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);
        removal.setVisible(canCreateStore);

        h.add(
                removalWithRoles =
                        new SelectionUserRemovalLink(
                                serviceName, "removeSelectedWithRoles", users, dialog, true));
        removalWithRoles.setOutputMarkupId(true);
        removalWithRoles.setEnabled(false);
        removalWithRoles.setVisible(
                canCreateStore
                        && GeoServerApplication.get()
                                .getSecurityManager()
                                .getActiveRoleService()
                                .canCreateStore());
    }

    protected void onBeforeRender() {
        users.clearSelection();
        removal.setEnabled(false);
        removalWithRoles.setEnabled(false);
        super.onBeforeRender();
    }
}
