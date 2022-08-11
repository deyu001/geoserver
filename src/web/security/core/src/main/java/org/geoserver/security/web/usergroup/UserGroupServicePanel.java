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

package org.geoserver.security.web.usergroup;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.Util;
import org.geoserver.security.web.SecurityNamedServicePanel;
import org.geoserver.security.web.SecurityNamedServiceTabbedPanel;
import org.geoserver.security.web.group.GroupPanel;
import org.geoserver.security.web.passwd.PasswordEncoderChoice;
import org.geoserver.security.web.passwd.PasswordPolicyChoice;
import org.geoserver.security.web.user.UserPanel;

/**
 * Base class for user group service panels.
 *
 * @author Justin Deoliveira, OpenGeo
 * @param <T>
 */
public class UserGroupServicePanel<T extends SecurityUserGroupServiceConfig>
        extends SecurityNamedServicePanel<T> implements SecurityNamedServiceTabbedPanel<T> {

    CheckBox recodeCheckBox = null;

    public UserGroupServicePanel(String id, IModel<T> model) {
        super(id, model);

        add(
                new PasswordEncoderChoice("passwordEncoderName")
                        .add(
                                new OnChangeAjaxBehavior() {
                                    @Override
                                    protected void onUpdate(AjaxRequestTarget target) {
                                        if (recodeCheckBox.isVisible()) {
                                            recodeCheckBox.setEnabled(true);
                                            target.add(recodeCheckBox);
                                        }
                                    }
                                }));

        boolean canCreateStore = false;
        SecurityUserGroupServiceConfig config = model.getObject();
        try {
            GeoServerUserGroupService s =
                    (GeoServerUserGroupService)
                            Class.forName(config.getClassName())
                                    .getDeclaredConstructor()
                                    .newInstance();
            canCreateStore = s.canCreateStore();
        } catch (Exception e) {
            // do nothing
        }

        recodeCheckBox = new CheckBox("recodeExistingPasswords", Model.of(false));
        recodeCheckBox.setOutputMarkupId(true);
        recodeCheckBox.setVisible(canCreateStore);
        recodeCheckBox.setEnabled(false);
        add(recodeCheckBox);
        add(new PasswordPolicyChoice("passwordPolicyName"));
    }

    @Override
    public List<ITab> createTabs(final IModel<T> model) {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(
                new AbstractTab(new StringResourceModel("users", this, null)) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new UserPanel(panelId, model.getObject().getName());
                    }
                });
        tabs.add(
                new AbstractTab(new StringResourceModel("groups", this, null)) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new GroupPanel(panelId, model.getObject().getName());
                    }
                });
        return tabs;
    }

    @Override
    public void doSave(T config) throws Exception {
        getSecurityManager().saveUserGroupService(config);
        if (recodeCheckBox.getModelObject()) {
            GeoServerUserGroupService s =
                    getSecurityManager().loadUserGroupService(config.getName());
            if (s.canCreateStore()) {
                Util.recodePasswords(s.createStore());
            }
        }
    }

    public void doLoad(T config) throws Exception {
        getSecurityManager().loadUserGroupServiceConfig(config.getName());
    }
}
