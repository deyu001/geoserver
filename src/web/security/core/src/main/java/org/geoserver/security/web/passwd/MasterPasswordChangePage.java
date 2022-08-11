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

package org.geoserver.security.web.passwd;

import java.io.IOException;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.web.AbstractSecurityPage;

public class MasterPasswordChangePage extends AbstractSecurityPage {

    public MasterPasswordChangePage() {
        MasterPasswordConfigModel configModel = new MasterPasswordConfigModel();

        Form form = new Form<>("form", new CompoundPropertyModel<>(configModel));
        add(form);

        form.add(new Label("providerName"));

        MasterPasswordConfig config = configModel.getObject();
        MasterPasswordProviderConfig providerConfig = null;
        try {
            providerConfig =
                    getSecurityManager()
                            .loadMasterPassswordProviderConfig(config.getProviderName());
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }

        // TODO: this will cause the master password to stored as a string in plain text, without
        // the
        // ability to scramble it... not much we can do because wicket works with strings...
        // potentially look into a way to store as char or byte array so string never gets
        // created
        form.add(new PasswordTextField("currentPassword", new Model<>()));
        form.add(
                new PasswordTextField("newPassword", new Model<>())
                        .setEnabled(!providerConfig.isReadOnly()));
        form.add(new PasswordTextField("newPasswordConfirm", new Model<>()));

        form.add(
                new SubmitLink("save", form) {
                    @Override
                    public void onSubmit() {
                        Form f = getForm();
                        // @Justin, we cannot use getDefaultModelObjectAsString() because of special
                        // chars.
                        // example: The password "mcrmcr&1" is converted to "mcrmcr&amp;1".
                        String currPasswd =
                                // f.get("currentPassword").getDefaultModelObjectAsString();
                                (String) f.get("currentPassword").getDefaultModelObject();
                        String newPasswd =
                                // f.get("newPassword").getDefaultModelObjectAsString();
                                (String) f.get("newPassword").getDefaultModelObject();
                        String newPasswdConfirm =
                                // f.get("newPasswordConfirm").getDefaultModelObjectAsString();
                                (String) f.get("newPasswordConfirm").getDefaultModelObject();

                        MasterPasswordConfig mpConfig =
                                (MasterPasswordConfig) getForm().getModelObject();
                        try {
                            getSecurityManager()
                                    .saveMasterPasswordConfig(
                                            mpConfig,
                                            currPasswd.toCharArray(),
                                            newPasswd != null ? newPasswd.toCharArray() : null,
                                            newPasswdConfirm.toCharArray());
                            doReturn();
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                });
        form.add(
                new AjaxLink("cancel") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn();
                    }
                });
    }
}
