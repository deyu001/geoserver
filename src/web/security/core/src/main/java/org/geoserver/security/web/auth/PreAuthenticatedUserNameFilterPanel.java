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

package org.geoserver.security.web.auth;

import java.util.Arrays;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.wicket.HelpLink;

/**
 * Configuration panel for {@link GeoServerPreAuthenticatedUserNameFilter}.
 *
 * @author mcr
 */
public abstract class PreAuthenticatedUserNameFilterPanel<
                T extends PreAuthenticatedUserNameFilterConfig>
        extends AuthenticationFilterPanel<T> {

    protected DropDownChoice<RoleSource> roleSourceChoice;

    public PreAuthenticatedUserNameFilterPanel(String id, IModel<T> model) {
        super(id, model);

        add(new HelpLink("roleSourceHelp", this).setDialog(dialog));

        add(roleSourceChoice = createRoleSourceDropDown());

        roleSourceChoice.setNullValid(false);

        roleSourceChoice.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Panel p = getRoleSourcePanel(roleSourceChoice.getModelObject());

                        WebMarkupContainer c = (WebMarkupContainer) get("container");
                        c.addOrReplace(p);
                        target.add(c);
                    }
                });

        WebMarkupContainer container = new WebMarkupContainer("container");
        add(container.setOutputMarkupId(true));

        // show correct panel for existing configuration
        RoleSource rs = model.getObject().getRoleSource();
        addRoleSourceDropDown(container, rs);
    }

    protected Panel getRoleSourcePanel(RoleSource model) {
        if (PreAuthenticatedUserNameRoleSource.UserGroupService.equals(model)) {
            return new UserGroupServicePanel("panel");
        } else if (PreAuthenticatedUserNameRoleSource.RoleService.equals(model)) {
            return new RoleServicePanel("panel");
        } else if (PreAuthenticatedUserNameRoleSource.Header.equals(model)) {
            return new HeaderPanel("panel");
        }
        return new EmptyPanel("panel");
    }

    protected DropDownChoice<RoleSource> createRoleSourceDropDown() {
        return new DropDownChoice<>(
                "roleSource",
                Arrays.asList(PreAuthenticatedUserNameRoleSource.values()),
                new RoleSourceChoiceRenderer());
    }

    protected void addRoleSourceDropDown(WebMarkupContainer container, RoleSource rs) {
        container.addOrReplace(getRoleSourcePanel(rs));
    }

    static class HeaderPanel extends Panel {
        public HeaderPanel(String id) {
            super(id, new Model());
            add(new TextField("rolesHeaderAttribute").setRequired(true).setRequired(true));
        }
    }

    static class UserGroupServicePanel extends Panel {
        public UserGroupServicePanel(String id) {
            super(id, new Model());
            add(new UserGroupServiceChoice("userGroupServiceName").setRequired(true));
        }
    }

    static class RoleServicePanel extends Panel {
        public RoleServicePanel(String id) {
            super(id, new Model());
            add(new RoleServiceChoice("roleServiceName"));
        }
    }
}
