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

package org.geoserver.security.config;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.rememberme.RememberMeServicesConfig;

/**
 * {@link GeoServerSecurityManager} configuration object.
 *
 * @author christian
 */
public class SecurityManagerConfig implements SecurityConfig {

    private static final long serialVersionUID = 1L;

    private String roleServiceName;
    private List<String> authProviderNames = new ArrayList<>();
    private String configPasswordEncrypterName;
    private boolean encryptingUrlParams;

    private GeoServerSecurityFilterChain filterChain = new GeoServerSecurityFilterChain();
    private RememberMeServicesConfig rememberMeService = new RememberMeServicesConfig();
    private BruteForcePreventionConfig bruteForcePrevention = new BruteForcePreventionConfig();

    public SecurityManagerConfig() {}

    public SecurityManagerConfig(SecurityManagerConfig config) {
        this.roleServiceName = config.getRoleServiceName();
        this.authProviderNames =
                config.getAuthProviderNames() != null
                        ? new ArrayList<>(config.getAuthProviderNames())
                        : null;
        this.filterChain =
                config.getFilterChain() != null
                        ? new GeoServerSecurityFilterChain(config.getFilterChain())
                        : null;
        this.rememberMeService = new RememberMeServicesConfig(config.getRememberMeService());
        this.bruteForcePrevention =
                new BruteForcePreventionConfig(config.getBruteForcePrevention());
        this.encryptingUrlParams = config.isEncryptingUrlParams();
        this.configPasswordEncrypterName = config.getConfigPasswordEncrypterName();
        // this.masterPasswordURL=config.getMasterPasswordURL();
        // this.masterPasswordStrategy=config.getMasterPasswordStrategy();
    }

    private Object readResolve() {
        authProviderNames = authProviderNames != null ? authProviderNames : new ArrayList<>();
        filterChain = filterChain != null ? filterChain : new GeoServerSecurityFilterChain();
        rememberMeService =
                rememberMeService != null ? rememberMeService : new RememberMeServicesConfig();
        bruteForcePrevention =
                bruteForcePrevention != null
                        ? bruteForcePrevention
                        : new BruteForcePreventionConfig();
        return this;
    }

    /** Name of {@link GeoServerRoleService} object. */
    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    /** @return list of names for {@link GeoServerAuthenticationProvider} objects */
    public List<String> getAuthProviderNames() {
        return authProviderNames;
    }

    /** The security filter chain. */
    public GeoServerSecurityFilterChain getFilterChain() {
        return filterChain;
    }

    public void setFilterChain(GeoServerSecurityFilterChain filterChain) {
        this.filterChain = filterChain;
    }

    /** The remember me service. */
    public RememberMeServicesConfig getRememberMeService() {
        return rememberMeService;
    }

    public void setRememberMeService(RememberMeServicesConfig rememberMeService) {
        this.rememberMeService = rememberMeService;
    }

    public BruteForcePreventionConfig getBruteForcePrevention() {
        return bruteForcePrevention;
    }

    /** The brute force attack prevention */
    public void setBruteForcePrevention(BruteForcePreventionConfig bruteForcePrevention) {
        this.bruteForcePrevention = bruteForcePrevention;
    }

    /** Flag controlling if web admin should encrypt url parameters. */
    public boolean isEncryptingUrlParams() {
        return encryptingUrlParams;
    }

    public void setEncryptingUrlParams(boolean encryptingUrlParams) {
        this.encryptingUrlParams = encryptingUrlParams;
    }

    /** The name of the password encrypter for encrypting password in configuration files. */
    public String getConfigPasswordEncrypterName() {
        return configPasswordEncrypterName;
    }

    public void setConfigPasswordEncrypterName(String configPasswordEncrypterName) {
        this.configPasswordEncrypterName = configPasswordEncrypterName;
    }

    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {

        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);

        SecurityManagerConfig target = SerializationUtils.clone(this);

        if (target != null) {
            if (allowEnvParametrization
                    && gsEnvironment != null
                    && GeoServerEnvironment.allowEnvParametrization()) {
                target.setConfigPasswordEncrypterName(
                        (String) gsEnvironment.resolveValue(configPasswordEncrypterName));
                target.setRoleServiceName((String) gsEnvironment.resolveValue(roleServiceName));
            }
        }

        return target;
    }
}
