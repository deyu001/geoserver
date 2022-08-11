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

package org.geoserver.security.xml;

import java.io.IOException;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Security provider for default XML-based implementation.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XMLSecurityProvider extends GeoServerSecurityProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("userGroupService", XMLUserGroupServiceConfig.class);
        xp.getXStream().alias("roleService", XMLRoleServiceConfig.class);
        xp.getXStream().alias("passwordPolicy", PasswordPolicyConfig.class);
        xp.getXStream()
                .alias("usernamePassword", UsernamePasswordAuthenticationProviderConfig.class);
    }

    @Override
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return XMLUserGroupService.class;
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return new XMLUserGroupService();
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return XMLRoleService.class;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return new XMLRoleService();
    }

    /** Create the standard password validator */
    public PasswordValidator createPasswordValidator(
            PasswordPolicyConfig config, GeoServerSecurityManager securityManager) {
        return new PasswordValidatorImpl(securityManager);
    }

    /**
     * Returns the specific class of the password validator created by {@link
     * #createPasswordValidator(PasswordPolicyConfig)}.
     *
     * <p>If the extension does not provide a user group service this method should simply return
     * <code>null</code>.
     */
    public Class<? extends PasswordValidator> getPasswordValidatorClass() {
        return PasswordValidatorImpl.class;
    }

    /**
     * Creates an authentication provider.
     *
     * <p>If the extension does not provide an authentication provider this method should simply
     * return <code>null</code>.
     */
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        return new UsernamePasswordAuthenticationProvider();
    }

    /**
     * Returns the concrete class of authentication provider created by {@link
     * #createAuthenticationProvider(SecurityNamedServiceConfig)}.
     *
     * <p>If the extension does not provide an authentication provider this method should simply
     * return <code>null</code>.
     */
    public Class<? extends GeoServerAuthenticationProvider> getAuthenticationProviderClass() {
        return UsernamePasswordAuthenticationProvider.class;
    }

    @Override
    public boolean roleServiceNeedsLockProtection() {
        return true;
    }

    @Override
    public boolean userGroupServiceNeedsLockProtection() {
        return true;
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new XMLSecurityConfigValidator(securityManager);
    }
}
