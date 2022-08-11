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

package org.geoserver.security.cas;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;

/**
 * Configuration for cas authentication receiving proxy tickets
 *
 * @author mcr
 */
public class CasAuthenticationFilterConfig extends PreAuthenticatedUserNameFilterConfig {

    private static final long serialVersionUID = 1L;

    /**
     * RoleSource list specific to CAS. To be used in addition to {@link
     * org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource}
     *
     * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
     */
    public static enum CasSpecificRoleSource implements RoleSource {
        CustomAttribute;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    };

    /** if true, no single sign on possible */
    private boolean sendRenew;

    /**
     * The CAS server URL including context root
     *
     * <p>example "https://localhost:9443/cas"
     */
    private String casServerUrlPrefix;

    /**
     * The geoserver url for the proxy callback
     *
     * <p>example: http://localhost:8080/geoserver
     */
    private String proxyCallbackUrlPrefix;

    /**
     * Optional:
     *
     * <p>After a successful CAS logout triggered by geoserver, a cas response page is rendered.
     *
     * <p>This url should be rendered as a link in the CAS response page.
     *
     * <p>example: https://myhost:8443/geoserver
     */
    private String urlInCasLogoutPage;

    /**
     * Name of the custom attribute originating roles when using {@link
     * CasSpecificRoleSource#CustomAttribute}
     */
    private String customAttributeName;

    /** Participate in Single Sign Out. */
    private boolean singleSignOut;

    public boolean isSendRenew() {
        return sendRenew;
    }

    public void setSendRenew(boolean sendRenew) {
        this.sendRenew = sendRenew;
    }

    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }

    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }

    public String getProxyCallbackUrlPrefix() {
        return proxyCallbackUrlPrefix;
    }

    public void setProxyCallbackUrlPrefix(String proxyCallbackUrlPrefix) {
        this.proxyCallbackUrlPrefix = proxyCallbackUrlPrefix;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    public String getUrlInCasLogoutPage() {
        return urlInCasLogoutPage;
    }

    public void setUrlInCasLogoutPage(String urlInCasLogoutPage) {
        this.urlInCasLogoutPage = urlInCasLogoutPage;
    }

    public boolean isSingleSignOut() {
        return singleSignOut;
    }

    public void setSingleSignOut(boolean singleSignOut) {
        this.singleSignOut = singleSignOut;
    }

    public String getCustomAttributeName() {
        return customAttributeName;
    }

    public void setCustomAttributeName(String customAttributeName) {
        this.customAttributeName = customAttributeName;
    }
}
