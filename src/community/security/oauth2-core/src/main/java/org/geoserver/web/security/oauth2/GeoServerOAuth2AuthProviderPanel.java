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

package org.geoserver.web.security.oauth2;

import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.geoserver.security.oauth2.GeoServerOAuth2FilterConfig;
import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoServerOAuth2AuthProviderPanel<T extends GeoServerOAuth2FilterConfig>
        extends PreAuthenticatedUserNameFilterPanel<T> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3025321797363970302L;

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    GeoServerDialog dialog;

    IModel<? extends GeoServerOAuth2FilterConfig> model;

    public GeoServerOAuth2AuthProviderPanel(String id, IModel<T> model) {
        super(id, model);

        this.dialog = (GeoServerDialog) get("dialog");
        this.model = model;

        add(getTopPanel("topPanel"));
        add(new HelpLink("enableRedirectAuthenticationEntryPointHelp", this).setDialog(dialog));
        add(new HelpLink("connectionParametersHelp", this).setDialog(dialog));
        add(new HelpLink("accessTokenUriHelp", this).setDialog(dialog));
        add(new HelpLink("userAuthorizationUriHelp", this).setDialog(dialog));
        add(new HelpLink("redirectUriHelp", this).setDialog(dialog));
        add(new HelpLink("checkTokenEndpointUrlHelp", this).setDialog(dialog));
        add(new HelpLink("logoutUriHelp", this).setDialog(dialog));
        add(new HelpLink("scopesHelp", this).setDialog(dialog));
        add(new HelpLink("cliendIdHelp", this).setDialog(dialog));
        add(new HelpLink("clientSecretHelp", this).setDialog(dialog));

        add(new CheckBox("enableRedirectAuthenticationEntryPoint"));
        add(new TextField<String>("loginEndpoint"));
        add(new TextField<String>("logoutEndpoint"));
        add(new CheckBox("forceAccessTokenUriHttps"));
        add(new CheckBox("forceUserAuthorizationUriHttps"));
        add(new TextField<String>("accessTokenUri"));
        add(new TextField<String>("userAuthorizationUri"));
        add(new TextField<String>("redirectUri"));
        add(new TextField<String>("checkTokenEndpointUrl"));
        add(new TextField<String>("logoutUri"));
        add(new TextField<String>("scopes"));
        add(new TextField<String>("cliendId"));
        add(new TextField<String>("clientSecret"));
    }

    /**
     * Allows subclasses to add a panel at the top of the parameter list, in addition to normal
     * Wicket extension mechanism, which places them at the bottom instead. The default
     * implementation just hides the top panel.
     *
     * @return
     * @param panelId
     */
    protected Component getTopPanel(String panelId) {
        EmptyPanel topPanel = new EmptyPanel(panelId);
        topPanel.setVisible(false);
        return topPanel;
    }
}
