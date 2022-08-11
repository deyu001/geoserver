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

package org.geoserver.security.web;

import static org.geoserver.security.impl.GeoServerUser.ADMIN_USERNAME;
import static org.geoserver.security.impl.GeoServerUser.DEFAULT_ADMIN_PASSWD;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.web.passwd.MasterPasswordChangePage;
import org.geoserver.security.web.user.EditUserPage;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.geotools.util.logging.Logging;

public class SecurityHomePageContentProvider implements GeoServerHomePageContentProvider {

    static Logger LOGGER = Logging.getLogger(SecurityHomePageContentProvider.class);

    @Override
    public Component getPageBodyComponent(String id) {
        // do a check that the keystore password is not set
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        if (secMgr.checkAuthenticationForAdminRole()) {
            return new SecurityWarningsPanel(id);
        }
        return null;
    }

    // PasswordChangeWarningPanel
    static class SecurityWarningsPanel extends Panel {

        public SecurityWarningsPanel(String id) {
            super(id);

            GeoServerSecurityManager manager = GeoServerApplication.get().getSecurityManager();

            // warn in case of an existing masterpw.info
            Resource mpInfo = null;
            Label mpInfoLabel = null;
            try {
                mpInfo =
                        manager.get("security")
                                .get(GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME);
                mpInfoLabel =
                        new Label(
                                "mpfile",
                                new StringResourceModel("masterPasswordFile", this)
                                        .setParameters(mpInfo.path()));
                mpInfoLabel.setEscapeModelStrings(false);
                add(mpInfoLabel);
                mpInfoLabel.setVisible(Resources.exists(mpInfo));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            // warn in case of an existing user.properties.old
            Resource userprops = null;
            Label userpropsLabel = null;
            try {
                userprops = manager.get("security").get("users.properties.old");
                userpropsLabel =
                        new Label(
                                "userpropsold",
                                new StringResourceModel("userPropertiesOldFile", this)
                                        .setParameters(userprops.path()));
                userpropsLabel.setEscapeModelStrings(false);
                add(userpropsLabel);
                userpropsLabel.setVisible(Resources.exists(userprops));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            // check for default master password
            boolean visibility = manager.checkMasterPassword(DEFAULT_ADMIN_PASSWD, false);

            Label label =
                    new Label(
                            "mpmessage",
                            new StringResourceModel("changeMasterPassword", this, null));
            label.setEscapeModelStrings(false);
            add(label);
            Link link = null;
            add(
                    link =
                            new Link("mplink") {
                                @Override
                                public void onClick() {
                                    setResponsePage(new MasterPasswordChangePage());
                                }
                            });
            label.setVisible(visibility);
            link.setVisible(visibility);

            // check for default admin password
            visibility = manager.checkForDefaultAdminPassword();
            Page changeItPage = null;
            String passwordEncoderName = null;
            try {
                GeoServerUserGroupService ugService =
                        manager.loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);
                if (ugService != null) {
                    passwordEncoderName = ugService.getPasswordEncoderName();
                    GeoServerUser user = ugService.getUserByUsername(ADMIN_USERNAME);
                    if (user != null) {
                        changeItPage = new EditUserPage(ugService.getName(), user);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error looking up admin user", e);
            }
            if (changeItPage == null) {
                changeItPage = new UserGroupRoleServicesPage();
            }

            final Page linkPage = changeItPage;
            label =
                    new Label(
                            "adminmessage",
                            new StringResourceModel("changeAdminPassword", this, null));
            label.setEscapeModelStrings(false);
            add(label);
            add(
                    link =
                            new Link("adminlink") {
                                @Override
                                public void onClick() {
                                    setResponsePage(linkPage);
                                }
                            });
            label.setVisible(visibility);
            link.setVisible(visibility);

            // inform about strong encryption
            if (manager.isStrongEncryptionAvailable()) {
                add(
                        new Label(
                                        "strongEncryptionMsg",
                                        new StringResourceModel(
                                                "strongEncryption",
                                                new SecuritySettingsPage(),
                                                null))
                                .add(
                                        new AttributeAppender(
                                                "class", new Model<>("info-link"), " ")));
            } else {
                add(
                        new Label(
                                        "strongEncryptionMsg",
                                        new StringResourceModel(
                                                "noStrongEncryption",
                                                new SecuritySettingsPage(),
                                                null))
                                .add(
                                        new AttributeAppender(
                                                "class", new Model<>("warning-link"), " ")));
            }

            // check for password encoding in the default user group service
            visibility = false;
            if (passwordEncoderName != null) {
                GeoServerPasswordEncoder encoder = manager.loadPasswordEncoder(passwordEncoderName);
                if (encoder != null) {
                    visibility = encoder.isReversible();
                }
            }

            label =
                    new Label(
                            "digestEncoding",
                            new StringResourceModel("digestEncoding", this, null));
            add(label);
            label.setVisible(visibility);
        }
    }
}
